package io.github.supermonster003.autojs6.plugin.three.shell.terminal.service

import android.app.Notification
import android.app.ActivityManager
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.ThreeShellTerminalPlugin
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.core.TerminalSessionManager

/**
 * Foreground lifetime for terminal sessions (plugin roadmap D15): while at least one shell is
 * registered in [TerminalSessionManager] the plugin process stays in the foreground, so
 * `npm install` or a long-running script survives leaving the terminal screen and the host. The
 * notification is rendered by [SessionNotifications] and re-rendered every few seconds because the
 * shell's working directory changes over time.
 *
 * API 24 / 25 start it with `startService`, API 26+ with `startForegroundService`; a refusal
 * (background start restrictions on API 31+) only costs the foreground protection, the session
 * itself keeps running.
 *
 * zh-CN: 终端会话的前台生命周期 (路线图 D15): 只要 [TerminalSessionManager] 中仍有 shell, 插件进程就保持前台,
 * 使 `npm install` 或长时间运行的脚本在离开终端界面与宿主后继续运行. 通知由 [SessionNotifications] 渲染并定期刷新.
 * API 24 / 25 用 `startService`, API 26+ 用 `startForegroundService`; 被拒绝时只失去前台保护, 会话本身照常运行.
 */
class ThreeShellTerminalSessionService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private var destroyed = false
    private val sessionListener: () -> Unit = { handler.post { refresh() } }
    private val pulse = object : Runnable {
        override fun run() {
            if (destroyed) return
            refresh()
            handler.postDelayed(this, PULSE_INTERVAL_MILLIS)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        SessionNotifications.ensureChannel(this)
        if (!SessionNotifications.isPermissionGranted(this)) {
            Log.i(TAG, "POST_NOTIFICATIONS not granted; sessions run without a visible notification")
        }
        val promoted = runCatching { startForegroundCompat(SessionNotifications.build(this, TerminalSessionManager.activeSessions)) }
            .onFailure { Log.w(TAG, "Could not enter the foreground: ${it.message}") }
            .isSuccess
        if (!promoted) {
            // Stop before the startForegroundService() deadline; the sessions keep running without foreground protection.
            stopSelf()
            return
        }
        TerminalSessionManager.addListener(sessionListener)
        handler.postDelayed(pulse, PULSE_INTERVAL_MILLIS)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_CLOSE_ALL) {
            TerminalSessionManager.closeAll()
        }
        refresh()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        destroyed = true
        handler.removeCallbacks(pulse)
        TerminalSessionManager.removeListener(sessionListener)
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    /**
     * Re-renders the notification and stops once no session is left.
     * zh-CN: 重新渲染通知, 没有会话时停止服务.
     */
    private fun refresh() {
        if (destroyed) return
        val sessions = TerminalSessionManager.activeSessions
        if (sessions.isEmpty()) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }
        runCatching {
            getSystemService(NotificationManager::class.java)?.notify(SessionNotifications.NOTIFICATION_ID, SessionNotifications.build(this, sessions))
        }.onFailure { Log.w(TAG, "Could not update the terminal notification: ${it.message}") }
    }

    private fun startForegroundCompat(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(SessionNotifications.NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(SessionNotifications.NOTIFICATION_ID, notification)
        }
    }

    companion object {

        private const val TAG = "ThreeShellTerminalSessions"
        private const val PULSE_INTERVAL_MILLIS = 5_000L

        /** Notification action that ends every session. zh-CN: 结束全部会话的通知操作. */
        const val ACTION_CLOSE_ALL = "${ThreeShellTerminalPlugin.PACKAGE_NAME}.action.CLOSE_ALL_SESSIONS"

        /**
         * Starts the foreground service; a refusal (background start restrictions) is only logged
         * because the session itself keeps working, just without foreground protection.
         * zh-CN: 启动前台服务; 被拒绝 (后台启动限制) 时仅记录日志, 会话本身照常工作, 只是没有前台保护.
         */
        @JvmStatic
        fun ensureStarted(context: Context) {
            val app = context.applicationContext
            // Some OEMs accept startForegroundService but silently reject startForeground under
            // this restriction. Stopping that service then crashes the process asynchronously.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && app.getSystemService(ActivityManager::class.java)?.isBackgroundRestricted == true) {
                Log.i(TAG, "Background activity is restricted; sessions run without foreground protection")
                return
            }
            val intent = Intent(app, ThreeShellTerminalSessionService::class.java)
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    app.startForegroundService(intent)
                } else {
                    app.startService(intent)
                }
            }.onFailure { Log.w(TAG, "Could not start the terminal session service: ${it.message}") }
        }

    }

}
