package io.github.supermonster003.autojs6.plugin.three.shell.terminal.core

import org.junit.Assert.*
import org.junit.Test

class TerminalCommandTest {
    @Test
    fun pasteDetectionAndValidationKeepShortInputInTheTerminal() {
        assertFalse(TerminalCommand.needsEditor("echo 中文"))
        assertTrue(TerminalCommand.needsEditor("echo first\necho second"))
        assertTrue(TerminalCommand.needsEditor("a".repeat(81)))
        assertFalse(TerminalCommand.isValid(" \n"))
        assertFalse(TerminalCommand.isValid("echo \u0000"))
        assertFalse(TerminalCommand.isValid("中".repeat(TerminalCommand.MAX_BYTES / 3 + 1)))
        assertTrue(TerminalCommand.isValid("a".repeat(TerminalCommand.MAX_BYTES)))
        assertEquals("a\nb\nc", TerminalCommand.normalize("a\r\nb\rc"))
    }

    @Test
    fun controlCharactersCannotEraseTheDisplayedCommand() {
        assertEquals("printf '^[[2J'\n# 中文\tend", TerminalCommand.display("printf '\u001b[2J'\n# 中文\tend"))
    }
}
