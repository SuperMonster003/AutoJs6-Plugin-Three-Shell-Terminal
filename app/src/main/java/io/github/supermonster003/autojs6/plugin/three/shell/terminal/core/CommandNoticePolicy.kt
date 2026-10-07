package io.github.supermonster003.autojs6.plugin.three.shell.terminal.core

/**
 * Pure rules of the command completion notices (roadmap P8), kept free of Android types for the JVM table.
 * zh-CN: 命令完成通知的纯规则 (P8), 不含 Android 类型, 供 JVM 判定表使用.
 */
object CommandNoticePolicy {

    private const val NOTIFICATION_BASE = 0x5443_0000

    /** The group summary that alerts once for a burst of notices; below the per-session range. */
    const val SUMMARY_ID = NOTIFICATION_BASE - 1

    /**
     * A notice is due only for a command without an interactive shell that ended by itself, while the switch is on,
     * the notification permission is granted and the user is not looking at that session.
     * zh-CN: 仅当命令不带交互 shell 且自行结束, 开关打开, 已授予通知权限, 用户也未正在查看该会话时才发通知.
     */
    @JvmStatic
    fun shouldNotify(keepOpen: Boolean, closeRequested: Boolean, enabled: Boolean, permissionGranted: Boolean, visible: Boolean): Boolean =
        !keepOpen && !closeRequested && enabled && permissionGranted && !visible

    private val COMPLETION_MARKER = Regex("""^\[Process completed \(code -?\d+\)]$""")

    /**
     * The kept output without the terminal's own "[Process completed (code N)]" line: the result screen shows the
     * exit code separately, and the reaper can append that line before the last output has been drained.
     * zh-CN: 去掉终端自身追加的 "[Process completed (code N)]" 行: 结果页另行显示退出码, 且回收线程可能在最后的输出处理完之前追加该行.
     */
    @JvmStatic
    fun withoutCompletionMarker(transcript: String?): String? {
        val lines = transcript?.lines()?.filterNot { COMPLETION_MARKER.matches(it.trimEnd()) } ?: return null
        return lines.dropWhile { it.isBlank() }.joinToString("\n").trimEnd().takeIf { it.isNotEmpty() }
    }

    /** Notification id of a session, inside a range of its own (the running-sessions notice is 0x5445524D). */
    @JvmStatic
    fun notificationId(sessionId: String): Int = NOTIFICATION_BASE + (sessionId.toIntOrNull() ?: sessionId.hashCode()).mod(0x10000)

}
