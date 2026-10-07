package io.github.supermonster003.autojs6.plugin.three.shell.terminal.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.R
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.core.CommandNoticePolicy
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.core.FinishedCommands
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.core.TerminalPreferences
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.core.TerminalSessionManager
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui.CommandResultActivity
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Command completion notices (roadmap P8): when a command that ran without an interactive shell
 * (`keepOpen: false`, for example a script's `exec`) ends by itself, the plugin keeps the end of its output
 * in memory ([FinishedCommands]) and posts a notice whose tap opens that output ([CommandResultActivity]).
 * The notice names only the session and its directory, never the command or its output, so session text
 * still leaves the process only when the user copies or shares it (AGENTS.md 15). No notice is posted
 * when the switch is off, the notification permission is missing, the session was closed on request, or
 * the user is looking at that session.
 *
 * zh-CN: 命令完成通知 (P8): 不带交互 shell 运行的命令 (`keepOpen: false`, 例如脚本的 `exec`) 自行结束时, 插件在内存中
 * 保留其输出末尾 ([FinishedCommands]) 并发出通知, 点击打开该输出 ([CommandResultActivity]). 通知只写会话编号与目录,
 * 不含命令与输出, 会话文本仍只在用户复制或分享时离开进程. 开关关闭, 缺少通知权限, 会话被主动关闭, 或用户正看着该会话时不发通知.
 */
object CommandNotifications {

    const val CHANNEL_ID = "three.shell.terminal.commands"
    private const val GROUP_KEY = "three.shell.terminal.commands"
    private const val FINISH_GRACE_MILLIS = 2_000L
    private const val TAG = "ThreeShellTerminalCmd"

    /** The session the terminal screen shows while resumed; its end needs no notice. zh-CN: 终端界面前台显示的会话. */
    @Volatile
    var visibleSessionId: String? = null

    private val main = Handler(Looper.getMainLooper())
    private val registered = AtomicBoolean()

    @Volatile
    private var appContext: Context? = null

    private val listener = TerminalSessionManager.ExitListener { session, exitCode -> onExited(session, exitCode) }

    /**
     * Called by the registry for a non-interactive command session: keep its final output and listen for its exit.
     * zh-CN: 注册表为非交互命令会话调用: 保留其最终输出并监听退出.
     */
    fun watch(context: Context, session: TerminalSessionManager.Session) {
        session.pty.keepFinalTranscript = true
        appContext = context.applicationContext
        if (registered.compareAndSet(false, true)) TerminalSessionManager.addExitListener(listener)
    }

    private fun onExited(session: TerminalSessionManager.Session, exitCode: Int) {
        val context = appContext ?: return
        val pty = session.pty
        if (!pty.keepFinalTranscript) return
        val notify = CommandNoticePolicy.shouldNotify(
            keepOpen = session.keepOpen,
            closeRequested = session.closeRequested,
            enabled = TerminalPreferences(context).commandNotificationsEnabled,
            permissionGranted = SessionNotifications.isPermissionGranted(context),
            visible = visibleSessionId == session.id,
        )
        if (!notify) return
        val done = AtomicBoolean()
        val complete = Runnable {
            if (!done.compareAndSet(false, true)) return@Runnable
            val result = FinishedCommands.Result(
                session.id, session.initialDirectory, exitCode, System.currentTimeMillis(),
                CommandNoticePolicy.withoutCompletionMarker(pty.finalTranscript ?: pty.transcriptTail()),
            )
            FinishedCommands.put(result)
            post(context, session, result)
        }
        // The exit can be reaped before the reader drains the last output; finish() keeps the final text first.
        if (pty.isFinished) {
            complete.run()
        } else {
            pty.addFinishListener { main.post(complete) }
            main.postDelayed(complete, FINISH_GRACE_MILLIS)
        }
    }

    private fun post(context: Context, session: TerminalSessionManager.Session, result: FinishedCommands.Result) {
        ensureChannel(context)
        val id = CommandNoticePolicy.notificationId(session.id)
        val title = if (result.exitCode == 0) {
            context.getString(R.string.notification_command_finished)
        } else {
            context.getString(R.string.notification_command_failed, result.exitCode)
        }
        val text = context.getString(R.string.notification_command_session, session.id, session.paths.toTildePath(result.directory))
        val open = CommandResultActivity.intent(context, session.id, result.directory)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_terminal)
            .setContentTitle(title)
            .setContentText(text)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setWhen(result.finishedAt)
            .setShowWhen(true)
            .setContentIntent(PendingIntent.getActivity(context, id, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            // A script may run many commands: the notices form one group whose summary alerts once.
            .setGroup(GROUP_KEY)
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
            .build()
        val summary = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_terminal)
            .setContentTitle(context.getString(R.string.notification_commands_channel_name))
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setGroup(GROUP_KEY)
            .setGroupSummary(true)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .build()
        runCatching {
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            manager.notify(id, notification)
            manager.notify(CommandNoticePolicy.SUMMARY_ID, summary)
        }.onFailure { Log.w(TAG, "Could not post the notice of session ${session.id}: ${it.javaClass.simpleName}") }
    }

    @JvmStatic
    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, context.getString(R.string.notification_commands_channel_name), NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = context.getString(R.string.notification_commands_channel_description)
                setShowBadge(true)
            },
        )
    }

}
