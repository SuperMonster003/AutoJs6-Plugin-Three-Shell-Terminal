package io.github.supermonster003.autojs6.plugin.three.shell.terminal

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.binder.PackageSigners
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui.TerminalActivity
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui.TerminalManagerActivity
import org.autojs.plugin.terminal.api.TerminalContract

/**
 * The host's way into the terminal UI (roadmap D19 / P3.3): an exported, invisible forwarder for
 * `org.autojs.plugin.TERMINAL_OPEN` behind the `org.autojs.permission.PLUGIN` signature permission.
 * Android enforces that permission before `onCreate` runs; the Activity additionally refuses a
 * caller it can name that does not hold the permission or is not signed like this plugin, parses
 * the contract extras ([EntryRequest]) and starts either the terminal screen in its own task or the
 * session manager over the caller. It never shows anything itself and finishes at once.
 *
 * zh-CN: 宿主进入终端界面的入口 (D19 / P3.3): 导出的不可见转发器, 响应 `org.autojs.plugin.TERMINAL_OPEN`,
 * 受 `org.autojs.permission.PLUGIN` 签名权限保护. Android 在 `onCreate` 之前已校验该权限; Activity 对可识别的
 * 调用方再校验权限持有与签名一致, 解析契约 extras ([EntryRequest]), 然后在自有任务中启动终端界面或在调用方
 * 之上打开会话管理器. 自身不显示任何内容并立即结束.
 */
class ThreeShellTerminalEntryActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            forward()
        } finally {
            finish()
        }
    }

    private fun forward() {
        val caller = EntryCaller.identify(this)
        if (!EntryCaller.accepts(this, caller)) {
            Log.w(TAG, "refused ${ThreeShellTerminalPlugin.OPEN_TERMINAL_ACTION} from ${caller ?: "an unnamed caller"}")
            return
        }
        val request = EntryRequest.parse(intent)
        if (request == null) {
            Log.w(TAG, "ignored a ${ThreeShellTerminalPlugin.OPEN_TERMINAL_ACTION} request that breaks the contract")
            return
        }
        val target = if (request.manager) {
            // The manager is a dialog over whatever the caller shows; it stays in the caller's task.
            // zh-CN: 管理器是覆盖在调用方界面上的对话框, 留在调用方的任务中.
            TerminalManagerActivity.intent(this)
        } else {
            // The terminal owns a task of its own (D30): Back from it returns to the caller's task.
            // zh-CN: 终端拥有自己的任务 (D30): 从中返回即回到调用方的任务.
            request.applyTo(TerminalActivity.intent(this)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { startActivity(target) }.onFailure { error ->
            Log.w(TAG, "cannot start ${target.component?.shortClassName}: ${error.javaClass.simpleName}")
        }
    }

    private companion object {
        const val TAG = "ThreeShellTerminalEntry"
    }

}

/**
 * The extras of one `TERMINAL_OPEN` request after validation against the contract ceilings; the
 * same combinations the host refuses before sending are refused here (null) in case another
 * same-signer caller sends them anyway.
 * zh-CN: 一次 `TERMINAL_OPEN` 请求按契约上限校验后的 extras; 宿主发送前拒绝的组合在这里同样拒绝 (null).
 */
internal data class EntryRequest(
    val directory: String?,
    val sessionId: String?,
    val newSession: Boolean,
    val command: String?,
    val manager: Boolean,
) {

    /** Copies the session extras onto the terminal Intent under their contract names. zh-CN: 以契约键名把会话 extras 复制到终端 Intent. */
    fun applyTo(intent: Intent): Intent = intent.apply {
        directory?.let { putExtra(TerminalContract.EXTRA_DIRECTORY, it) }
        sessionId?.let { putExtra(TerminalContract.EXTRA_SESSION_ID, it) }
        if (newSession) putExtra(TerminalContract.EXTRA_NEW_SESSION, true)
        command?.let { putExtra(TerminalContract.EXTRA_COMMAND, it) }
    }

    companion object {

        fun parse(intent: Intent): EntryRequest? {
            val extras = intent.extras
            return of(
                directory = intent.getStringExtra(TerminalContract.EXTRA_DIRECTORY),
                sessionId = intent.getStringExtra(TerminalContract.EXTRA_SESSION_ID),
                newSession = flag(extras?.get(TerminalContract.EXTRA_NEW_SESSION)),
                command = intent.getStringExtra(TerminalContract.EXTRA_COMMAND),
                manager = flag(extras?.get(TerminalContract.EXTRA_MANAGER)),
            )
        }

        /**
         * Pure rule: blank strings count as absent, oversized values and contradictory combinations
         * (manager with a session request, sessionId with newSession or command) yield null.
         * zh-CN: 纯规则: 空白字符串视为缺省, 超长值与互斥组合 (manager 与会话请求, sessionId 与 newSession / command) 返回 null.
         */
        fun of(directory: String?, sessionId: String?, newSession: Boolean, command: String?, manager: Boolean): EntryRequest? {
            val dir = directory?.takeIf { it.isNotBlank() }
            val id = sessionId?.takeIf { it.isNotBlank() }
            val cmd = command?.takeIf { it.isNotEmpty() }
            return when {
                dir != null && utf8Size(dir) > TerminalContract.MAX_PATH_BYTES -> null
                cmd != null && utf8Size(cmd) > TerminalContract.MAX_COMMAND_BYTES -> null
                manager && (id != null || newSession || cmd != null) -> null
                id != null && (newSession || cmd != null) -> null
                else -> EntryRequest(dir, id, newSession, cmd, manager)
            }
        }

        /** Booleans as the host sends them, or their string spelling. zh-CN: 宿主发送的布尔值, 或其字符串写法. */
        fun flag(value: Any?): Boolean = when (value) {
            is Boolean -> value
            is String -> value.equals("true", ignoreCase = true)
            else -> false
        }

        private fun utf8Size(text: String): Int = text.toByteArray(Charsets.UTF_8).size

    }

}

/**
 * Who started the entry Activity, as far as the platform tells: `callingPackage` (start for
 * result), the attributed launcher package on API 34+, else the referrer the system derives from
 * the caller. Plain `startActivity` below API 34 leaves no name, and the manifest permission that
 * Android already enforced is then the whole check.
 * zh-CN: 平台所能告知的入口 Activity 调用方: `callingPackage` (带结果启动), API 34+ 的启动来源包, 否则系统派生的
 * referrer. API 34 以下的普通 `startActivity` 没有名字, 此时 Android 已强制的 Manifest 权限就是全部校验.
 */
internal object EntryCaller {

    fun identify(activity: Activity): String? =
        activity.callingPackage
            ?: launchedFromPackage(activity)
            ?: activity.referrer?.takeIf { it.scheme == "android-app" }?.host?.takeIf { it.isNotBlank() }

    fun accepts(context: Context, caller: String?): Boolean {
        val packages = context.packageManager
        return EntryCallerPolicy.accepts(
            caller = caller,
            self = context.packageName,
            holdsPermission = caller != null && packages.checkPermission(ThreeShellTerminalPlugin.PLUGIN_PERMISSION, caller) == PackageManager.PERMISSION_GRANTED,
            callerSigners = caller?.let { PackageSigners.of(packages, it) }.orEmpty(),
            pluginSigners = PackageSigners.of(packages, context.packageName),
        )
    }

    private fun launchedFromPackage(activity: Activity): String? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) activity.launchedFromPackage else null

}

/** The pure rule behind [EntryCaller.accepts]. zh-CN: [EntryCaller.accepts] 背后的纯规则. */
internal object EntryCallerPolicy {

    /**
     * @param caller        the attributed caller package, null when the platform named none
     * @param self          this plugin's package name
     * @param holdsPermission whether [caller] holds `org.autojs.permission.PLUGIN`
     * @param callerSigners SHA-256 digests of the caller's current signers
     * @param pluginSigners SHA-256 digests of this plugin's current signers
     */
    fun accepts(caller: String?, self: String, holdsPermission: Boolean, callerSigners: Set<String>, pluginSigners: Set<String>): Boolean = when {
        caller == null -> true
        caller == self -> true
        else -> holdsPermission && pluginSigners.isNotEmpty() && callerSigners == pluginSigners
    }

}
