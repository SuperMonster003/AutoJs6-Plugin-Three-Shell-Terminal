package io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui

/** Compact uptime labels for the session manager (`1h 02m 03s`, `5m 12s`, `9s`). zh-CN: 会话管理器的紧凑运行时长标签. */
internal object ElapsedTime {

    fun format(millis: Long): String {
        val total = (millis / 1000).coerceAtLeast(0)
        val hours = total / 3600
        val minutes = total % 3600 / 60
        val seconds = total % 60
        return when {
            hours > 0 -> "${hours}h ${minutes.pad()}m ${seconds.pad()}s"
            minutes > 0 -> "${minutes}m ${seconds.pad()}s"
            else -> "${seconds}s"
        }
    }

    private fun Long.pad(): String = if (this < 10) "0$this" else toString()

}
