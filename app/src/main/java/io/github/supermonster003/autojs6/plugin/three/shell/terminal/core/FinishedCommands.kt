package io.github.supermonster003.autojs6.plugin.three.shell.terminal.core

/**
 * The last results of commands that ran without an interactive shell (roadmap P8 command completion notices),
 * kept in memory only so a notification can open them; a process restart forgets them. Bounded to
 * [MAX_RESULTS] entries, each transcript already cut to `TerminalPtySession.FINAL_TRANSCRIPT_CHARS`.
 * zh-CN: 不带交互 shell 运行的命令的最近结果 (P8 命令完成通知), 只保存在内存中供通知打开, 进程重启后即遗忘;
 * 最多 [MAX_RESULTS] 条, 每条转录已截为 `TerminalPtySession.FINAL_TRANSCRIPT_CHARS`.
 */
object FinishedCommands {

    data class Result(
        val sessionId: String,
        val directory: String,
        val exitCode: Int,
        val finishedAt: Long,
        /** The end of the output, or null when the emulator never ran or had already released it. */
        val transcript: String?,
    )

    const val MAX_RESULTS = 8

    private val results = LinkedHashMap<String, Result>()

    @Synchronized
    fun put(result: Result) {
        results.remove(result.sessionId)
        results[result.sessionId] = result
        while (results.size > MAX_RESULTS) results.remove(results.keys.first())
    }

    @Synchronized
    fun get(sessionId: String?): Result? = sessionId?.let(results::get)

    @Synchronized
    fun ids(): List<String> = results.keys.toList()

    @Synchronized
    internal fun clear() = results.clear()

}
