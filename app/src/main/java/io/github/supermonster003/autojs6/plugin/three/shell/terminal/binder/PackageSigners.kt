package io.github.supermonster003.autojs6.plugin.three.shell.terminal.binder

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import java.security.MessageDigest

/**
 * SHA-256 digests of a package's current signing certificates, shared by the Binder caller guard
 * and the `TERMINAL_OPEN` entry Activity so both trust the same signer set.
 * zh-CN: 包当前签名证书的 SHA-256 摘要, Binder 调用方校验与 `TERMINAL_OPEN` 入口 Activity 共用同一信任集合.
 */
internal object PackageSigners {

    /** The package with its signing data, or null when it is not installed. zh-CN: 带签名数据的包信息, 未安装时为 null. */
    @Suppress("DEPRECATION")
    fun packageInfo(packages: PackageManager, name: String): PackageInfo? = try {
        val flags = PackageManager.GET_SIGNATURES or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) PackageManager.GET_SIGNING_CERTIFICATES else 0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packages.getPackageInfo(name, PackageManager.PackageInfoFlags.of(flags.toLong()))
        } else {
            packages.getPackageInfo(name, flags)
        }
    } catch (_: PackageManager.NameNotFoundException) {
        null
    }

    /** Lower-case hex SHA-256 of every current signer; empty for a missing package. zh-CN: 每个当前签名者的小写十六进制 SHA-256; 包不存在时为空. */
    @Suppress("DEPRECATION")
    fun digests(info: PackageInfo?): Set<String> {
        val modern = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info?.signingInfo?.apkContentsSigners?.takeIf { it.isNotEmpty() } else null
        val signatures = modern ?: info?.signatures
        return signatures.orEmpty().mapTo(hashSetOf()) { signature ->
            MessageDigest.getInstance("SHA-256").digest(signature.toByteArray()).joinToString("") { "%02x".format(it.toInt() and 0xff) }
        }
    }

    fun of(packages: PackageManager, name: String): Set<String> = digests(packageInfo(packages, name))

}
