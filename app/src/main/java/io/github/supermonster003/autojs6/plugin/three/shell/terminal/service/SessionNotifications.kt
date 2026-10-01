package io.github.supermonster003.autojs6.plugin.three.shell.terminal.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.R
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.core.TerminalSessionManager

/**
 * Builds the running-sessions notification of [ThreeShellTerminalSessionService] (plugin roadmap D15).
 *
 * The notification names the newest session's working directory and the session count, offers a
 * "Close sessions" action and, once the launcher entry of roadmap P3.1 exists, opens the terminal on
 * tap. On API 33+ it is only visible after the user granted `POST_NOTIFICATIONS`; the request belongs
 * to the first session-creating Activity (P3.1), sessions started through Binder never prompt and
 * simply run without a visible notification when the permission is missing.
 *
 * zh-CN: 构建 [ThreeShellTerminalSessionService] 的运行中会话通知 (路线图 D15). 通知显示最新会话的工作目录与会话数,
 * 提供 "关闭会话" 操作, P3.1 的启动器入口落地后点击可打开终端. API 33+ 仅在用户授予通知权限后可见;
 * 请求点在首次创建会话的 Activity (P3.1), Binder 发起的会话不弹权限, 缺少权限时静默运行.
 */
object SessionNotifications {

    /** Notification channel of the running sessions. zh-CN: 运行中会话的通知渠道. */
    const val CHANNEL_ID = "three.shell.terminal.sessions"
    const val NOTIFICATION_ID = 0x5445_524D

    @JvmStatic
    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = context.getString(R.string.notification_channel_description)
                setShowBadge(false)
            },
        )
    }

    @JvmStatic
    fun build(context: Context, sessions: List<TerminalSessionManager.Session>): Notification {
        val title = if (sessions.size > 1) {
            context.getString(R.string.notification_title_multiple, sessions.size)
        } else {
            context.getString(R.string.notification_title)
        }
        val newest = sessions.lastOrNull()
        val content = newest?.let { session ->
            val directory = session.pty.currentDirectory()?.path ?: session.initialDirectory
            session.paths.toTildePath(directory)
        }
        val closeIntent = PendingIntent.getService(
            context,
            1,
            Intent(context, ThreeShellTerminalSessionService::class.java).setAction(ThreeShellTerminalSessionService.ACTION_CLOSE_ALL),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_terminal)
            .setContentTitle(title)
            .setContentText(content)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setShowWhen(false)
            .addAction(R.drawable.ic_close_24dp, context.getString(R.string.notification_close_sessions), closeIntent)
        // The launcher entry (roadmap P3.1) resolves here once it exists; until then the notification has no tap target.
        context.packageManager.getLaunchIntentForPackage(context.packageName)?.let { launch ->
            builder.setContentIntent(
                PendingIntent.getActivity(context, 0, launch, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE),
            )
        }
        return builder.build()
    }

    /** True below API 33 and once the user granted `POST_NOTIFICATIONS`. zh-CN: API 33 以下恒为 true, 否则取决于通知权限. */
    @JvmStatic
    fun isPermissionGranted(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

}
