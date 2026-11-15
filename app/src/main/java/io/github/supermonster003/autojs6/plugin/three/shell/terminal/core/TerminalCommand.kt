package io.github.supermonster003.autojs6.plugin.three.shell.terminal.core

/** Explicit command execution bypasses the interactive line editor, including its horizontal clipping. */
object TerminalCommand {
    const val MAX_BYTES = 16 * 1024

    fun needsEditor(text: String): Boolean = text.any { it == '\n' || it == '\r' } || text.codePointCount(0, text.length) > 80

    fun normalize(text: String): String = text.replace("\r\n", "\n").replace('\r', '\n')

    fun isValid(text: String): Boolean = text.isNotBlank() && '\u0000' !in text && text.toByteArray(Charsets.UTF_8).size <= MAX_BYTES

    /** Display control characters as text; never let a pasted escape sequence erase the command record. */
    fun display(text: String): String = buildString {
        text.forEach { c ->
            when {
                c == '\n' || c == '\t' -> append(c)
                c < ' ' -> append('^').append((c.code + 64).toChar())
                c == '\u007f' -> append("^?")
                else -> append(c)
            }
        }
    }

    fun script(command: String, commandLabel: String, exitLabel: String): String {
        require(isValid(command))
        val record = "\n--- $commandLabel ---\n${display(command)}\n---"
        return "printf '%s\\n' ${ShellQuoting.quote(record)}\n" + command +
            "\n__aj6_command_status=\$?\nprintf '\\n%s: %s\\n\\n' ${ShellQuoting.quote(exitLabel)} \"\$__aj6_command_status\"\n"
    }
}
