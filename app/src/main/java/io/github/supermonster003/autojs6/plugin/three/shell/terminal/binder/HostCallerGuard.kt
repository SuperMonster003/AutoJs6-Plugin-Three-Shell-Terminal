package io.github.supermonster003.autojs6.plugin.three.shell.terminal.binder

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Binder
import android.os.Build
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.ThreeShellTerminalPlugin

/**
 * Decides who may call the `ITerminalPlugin` methods. [enforceHost] returns the calling uid or
 * throws `SecurityException`; nothing of the method body runs for a rejected caller.
 * zh-CN: 决定谁可以调用 `ITerminalPlugin` 的方法. [enforceHost] 返回调用方 uid 或抛出 `SecurityException`;
 * 被拒的调用方不会执行方法体的任何部分.
 */
interface CallerGuard {
    fun enforceHost(): Int
}

/**
 * The public protocol requires the installed official host, matching signers and a supported
 * build (protocol V1 security boundary; same rule as 3-Setup Installer).
 * zh-CN: 公开协议要求调用方是已安装的官方宿主, 签名集合一致且版本满足要求 (协议 V1 安全边界, 与 3-Setup Installer 同规则).
 */
object CallerPolicy {

    /**
     * @param uid           `Binder.getCallingUid()`
     * @param packages      packages sharing that uid
     * @param hostUid       uid of the installed host package, null when it is not installed
     * @param hostVersion   `versionCode` of the installed host, 0 when it is not installed
     * @param hostSigners   SHA-256 digests of the host's current signers
     * @param pluginSigners SHA-256 digests of this plugin's current signers
     */
    @JvmStatic
    fun allowed(uid: Int, packages: Set<String>, hostUid: Int?, hostVersion: Long, hostSigners: Set<String>, pluginSigners: Set<String>): Boolean =
        uid == hostUid && ThreeShellTerminalPlugin.HOST_PACKAGE_NAME in packages &&
            hostVersion >= ThreeShellTerminalPlugin.REQUIRED_HOST_VERSION &&
            pluginSigners.isNotEmpty() && hostSigners == pluginSigners
}

/** [CallerGuard] of the production service: the AutoJs6 host, same signers, required build. zh-CN: 生产服务的调用方校验: AutoJs6 宿主, 同签名, 满足版本. */
class HostCallerGuard(context: Context) : CallerGuard {

    private val context = context.applicationContext
    private val packages = context.applicationContext.packageManager
    private val plugin = context.applicationContext.packageName

    override fun enforceHost(): Int {
        val uid = Binder.getCallingUid()
        if (context.checkPermission(ThreeShellTerminalPlugin.PLUGIN_PERMISSION, Binder.getCallingPid(), uid) != PackageManager.PERMISSION_GRANTED) {
            throw SecurityException("Caller does not hold the plugin permission")
        }
        val host = packageInfo(ThreeShellTerminalPlugin.HOST_PACKAGE_NAME)
        @Suppress("DEPRECATION")
        val version = host?.let { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) it.longVersionCode else it.versionCode.toLong() } ?: 0L
        val allowed = CallerPolicy.allowed(
            uid,
            packages.getPackagesForUid(uid).orEmpty().toSet(),
            host?.applicationInfo?.uid,
            version,
            signers(host),
            signers(packageInfo(plugin)),
        )
        if (!allowed) throw SecurityException("Caller is not the installed same-signer AutoJs6 host of the required version")
        return uid
    }

    private fun packageInfo(name: String): PackageInfo? = PackageSigners.packageInfo(packages, name)

    private fun signers(info: PackageInfo?): Set<String> = PackageSigners.digests(info)
}
