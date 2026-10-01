package io.github.supermonster003.autojs6.plugin.three.shell.terminal.binder

/**
 * A contract error of protocol V1, answered as an error document (`String` / `Bundle` methods) or
 * as an `IllegalArgumentException` whose message is encoded `CODE: detail` (void methods).
 * zh-CN: 协议 V1 的契约错误: `String` / `Bundle` 方法以错误文档回答, void 方法以 `CODE: detail` 编码的 `IllegalArgumentException` 回答.
 */
internal class TerminalFailure(
    val code: String,
    message: String,
    val sessionId: String? = null,
) : RuntimeException(message) {

    fun toJson(): String = TerminalDocuments.error(code, message.orEmpty(), sessionId)

    fun toIllegalArgument(): IllegalArgumentException = IllegalArgumentException("$code: ${message.orEmpty()}")

}
