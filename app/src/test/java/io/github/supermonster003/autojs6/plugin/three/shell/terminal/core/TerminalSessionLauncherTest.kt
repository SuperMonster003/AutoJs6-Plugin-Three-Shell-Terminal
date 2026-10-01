package io.github.supermonster003.autojs6.plugin.three.shell.terminal.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TerminalSessionLauncherTest {

    @Test
    fun commandExecsTheShellInsideTheRequestedDirectory() {
        val argv = TerminalSessionLauncher.buildCommand("/sdcard/Scripts/my project")
        assertEquals(5, argv.size)
        assertEquals("/system/bin/sh", argv[0])
        assertEquals("-c", argv[1])
        assertEquals(TerminalSessionLauncher.WRAPPER_SCRIPT, argv[2])
        // $0 names the shell to exec, $1 is the directory; the path is passed as data, never interpolated.
        // zh-CN: $0 为待 exec 的 shell, $1 为目录; 路径作为数据传入, 不做插值.
        assertEquals("/system/bin/sh", argv[3])
        assertEquals("/sdcard/Scripts/my project", argv[4])
    }

    @Test
    fun wrapperFallsBackToHomeAndExecsArgvZero() {
        val script = TerminalSessionLauncher.WRAPPER_SCRIPT
        assertTrue(script.startsWith("cd -- \"\$1\""))
        assertTrue(script.contains("|| cd \"\$HOME\""))
        assertTrue(script.endsWith("exec \"\$0\""))
    }

    @Test
    fun aCommandRunsAfterTheDirectoryChangeAndKeepsOrClosesTheShell() {
        val keep = TerminalSessionLauncher.buildCommand("/sdcard/Scripts", "npm run build", keepOpen = true)
        assertEquals(5, keep.size)
        assertEquals("/system/bin/sh", keep[0])
        assertEquals("-c", keep[1])
        assertEquals(TerminalSessionLauncher.CD_SCRIPT + "\nnpm run build\nexec \"\$0\"", keep[2])
        assertEquals("/sdcard/Scripts", keep[4])
        val close = TerminalSessionLauncher.buildCommand("/sdcard/Scripts", "npm run build", keepOpen = false)
        assertEquals(TerminalSessionLauncher.CD_SCRIPT + "\nnpm run build\nexit \$?", close[2])
        // The command is passed verbatim on its own line (D34 Q4): the host quotes, the plugin never rewrites.
        assertEquals("printf '%s' \"a b\" | cat", TerminalSessionLauncher.commandScript("printf '%s' \"a b\" | cat", true).lines()[1])
    }

    @Test
    fun aBlankCommandIsAPlainShell() {
        assertEquals(TerminalSessionLauncher.buildCommand("/tmp"), TerminalSessionLauncher.buildCommand("/tmp", null, keepOpen = false))
        assertEquals(TerminalSessionLauncher.buildCommand("/tmp"), TerminalSessionLauncher.buildCommand("/tmp", "  \n", keepOpen = true))
    }

    @Test
    fun customShellIsUsedForBothExecutableAndArgvZero() {
        val argv = TerminalSessionLauncher.buildCommand("/tmp", shell = "/system/bin/mksh")
        assertEquals("/system/bin/mksh", argv[0])
        assertEquals("/system/bin/mksh", argv[3])
    }

}
