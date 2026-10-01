package io.github.supermonster003.autojs6.plugin.three.shell.terminal

import android.app.Service
import android.content.Intent
import android.os.IBinder
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.binder.TerminalPluginBinder
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.node.NodeCliState

/**
 * Host-facing terminal service answering `org.autojs.plugin.TERMINAL` (category `terminal`).
 *
 * Since roadmap P2.4 the Binder is the [TerminalPluginBinder] implementing `ITerminalPlugin.Stub`
 * of the host `terminal-api` module, guarded by `HostCallerGuard`. Sessions live in the
 * process-wide registry and survive the service; what dies with the binding are the host's
 * callbacks and output subscriptions.
 * zh-CN: 自 P2.4 起 Binder 为实现 `ITerminalPlugin.Stub` 的 [TerminalPluginBinder], 由 `HostCallerGuard` 守卫;
 * 会话存于进程级注册表并在服务之后存活, 随绑定消亡的只有宿主的回调与输出订阅.
 */
class ThreeShellTerminalPluginService : Service() {

    private lateinit var binder: TerminalPluginBinder

    override fun onCreate() {
        super.onCreate()
        binder = sharedBinder ?: TerminalPluginBinder(applicationContext).also { sharedBinder = it }
        NodeCliState.warmUp(this)
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onUnbind(intent: Intent?): Boolean {
        binder.onClientsGone()
        return false
    }

    override fun onDestroy() {
        // Pending sessions are user assets too. Keep their planner and registry across service rebinds.
        binder.onClientsGone()
        super.onDestroy()
    }

    private companion object {
        var sharedBinder: TerminalPluginBinder? = null
    }

}
