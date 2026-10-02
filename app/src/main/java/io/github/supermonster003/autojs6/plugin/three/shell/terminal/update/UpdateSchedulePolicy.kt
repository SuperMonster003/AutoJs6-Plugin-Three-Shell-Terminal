package io.github.supermonster003.autojs6.plugin.three.shell.terminal.update

/**
 * Rate limit of the manual update check (AGENTS.md 12): a successful answer is reused for 12 hours,
 * a clock that moved backwards or a missing record fetches again. There is no automatic check.
 * zh-CN: 手动检查更新的频率限制 (AGENTS.md 12): 成功结果复用 12 小时, 时钟回拨或没有记录时重新请求; 没有自动检查.
 */
internal object UpdateSchedulePolicy {

    const val INTERVAL_MS = 12L * 60 * 60 * 1000

    fun fetchDue(last: Long?, now: Long): Boolean = last == null || now < last || now - last >= INTERVAL_MS

}
