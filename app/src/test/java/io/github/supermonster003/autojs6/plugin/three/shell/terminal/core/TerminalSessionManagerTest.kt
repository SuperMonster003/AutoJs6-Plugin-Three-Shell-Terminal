package io.github.supermonster003.autojs6.plugin.three.shell.terminal.core

import org.junit.Assert.assertEquals
import org.junit.Test

/** The session title the Binder and the manager list show (roadmap P2.1): explicit title, else command, else directory name. */
class TerminalSessionManagerTest {

    @Test
    fun explicitTitleWins() {
        assertEquals("Build", TerminalSessionManager.defaultTitle("/sdcard/Scripts/demo", title = " Build ", command = "npm run build"))
    }

    @Test
    fun theFirstNonBlankCommandLineIsUsedWhenNoTitleIsGiven() {
        assertEquals("npm run build", TerminalSessionManager.defaultTitle("/sdcard/Scripts/demo", command = "\n  npm run build\nnpm test"))
        assertEquals("demo", TerminalSessionManager.defaultTitle("/sdcard/Scripts/demo", title = "   ", command = " \n "))
    }

    @Test
    fun theDirectoryNameIsTheFallback() {
        assertEquals("demo", TerminalSessionManager.defaultTitle("/sdcard/Scripts/demo"))
        assertEquals("demo", TerminalSessionManager.defaultTitle("/sdcard/Scripts/demo/"))
        assertEquals("/", TerminalSessionManager.defaultTitle("/"))
    }

}
