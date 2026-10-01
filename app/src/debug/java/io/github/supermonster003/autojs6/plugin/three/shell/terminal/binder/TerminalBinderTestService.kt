package io.github.supermonster003.autojs6.plugin.three.shell.terminal.binder

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.os.Process
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.core.TerminalSessionManager

/**
 * Debug-only endpoint exposing the real [TerminalPluginBinder] in the `:binder_test` process with
 * a guard that only requires the plugin uid, so instrumentation tests drive it through genuine
 * Binder transactions (descriptor transfer, exception propagation, caller pid) without the host.
 * zh-CN: 仅调试构建的端点: 在 `:binder_test` 进程中暴露真实的 [TerminalPluginBinder], 守卫仅要求插件 uid, 使测试经真实 Binder 事务驱动.
 */
class TerminalBinderTestService : Service() {

    private lateinit var binder: TerminalPluginBinder

    override fun onCreate() {
        super.onCreate()
        // Sessions of this process are invisible to the session service, which lives in the main process.
        TerminalSessionManager.foregroundServiceEnabled = false
        binder = TerminalPluginBinder(
            this,
            object : CallerGuard {
                override fun enforceHost(): Int = Binder.getCallingUid().also {
                    if (it != Process.myUid()) throw SecurityException("Debug test endpoint requires the plugin uid")
                }
            },
        )
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onUnbind(intent: Intent?): Boolean {
        binder.onClientsGone()
        return false
    }

    override fun onDestroy() {
        binder.close()
        super.onDestroy()
    }

}
