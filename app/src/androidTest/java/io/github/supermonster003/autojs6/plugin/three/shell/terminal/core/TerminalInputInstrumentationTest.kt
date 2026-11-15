package io.github.supermonster003.autojs6.plugin.three.shell.terminal.core

import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class TerminalInputInstrumentationTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    @Test
    fun supplementaryCharactersAndFollowingChineseSurviveKeyboardInputByteForByte() {
        val file = File.createTempFile("terminal-unicode-", ".txt", context.cacheDir).also { it.delete() }
        val paths = TerminalPaths.of(context).ensureLayout()
        // Read from a real terminal client. Android mksh's own UTF-8 editor only accepts BMP code points;
        // that shell limitation must not be mistaken for loss in the terminal's input encoder.
        val argv = TerminalSessionLauncher.buildCommand(paths.home.path,
            "stty -echo; printf '__%s__\\n' INPUT_READY; cat > ${ShellQuoting.quote(file.path)}; printf '__%s__\\n' INPUT_DONE", true)
        val session = onMain { TerminalSessionManager.create(context, paths.home.path, argv = argv).also { it.pty.updateSize(40, 24) } }
        try {
            val expected = "\uD83D\uDE42中文\uD840\uDC00aé\uD83D\uDE80\n"
            await { onMain { session.pty.transcriptText.contains("__INPUT_READY__") } }
            onMain {
                expected.codePoints().forEach { session.pty.write(it) }
                session.pty.write(4)
            }
            await { onMain { session.pty.transcriptText.contains("__INPUT_DONE__") } }
            assertArrayEquals(expected.toByteArray(Charsets.UTF_8), file.readBytes())
        } finally {
            onMain { TerminalSessionManager.close(session.id) }
            file.delete()
        }
    }

    @Test
    fun largeMultilinePastePreservesContentAndDoesNotBlockTheMainThread() {
        val file = File.createTempFile("terminal-paste-", ".txt", context.cacheDir).also { it.delete() }
        withShell { session ->
            try {
                val expected = (0 until 700).joinToString("\n", postfix = "\n") { "$it 中文 ${"x".repeat(70)}" }
                val command = "cat > ${ShellQuoting.quote(file.path)} <<'END_PASTE'\n${expected}END_PASTE\nprintf '__%s__\\n' PASTE_DONE\n"
                val start = SystemClock.uptimeMillis()
                onMain {
                    session.pty.toggleCtrl()
                    session.pty.toggleAlt()
                    assertTrue(session.pty.paste(command.replace("\n", "\r\n")))
                    assertFalse(session.pty.ctrlArmed)
                    assertFalse(session.pty.altArmed)
                }
                assertTrue("paste blocked the UI", SystemClock.uptimeMillis() - start < 1500)
                await { onMain { session.pty.transcriptText.contains("__PASTE_DONE__") } }
                assertArrayEquals(expected.toByteArray(Charsets.UTF_8), file.readBytes())
            } finally { file.delete() }
        }
    }

    @Test
    fun editorCommandKeepsHeredocTextAndRecordsFailureStatusAtNarrowWidth() {
        val file = File.createTempFile("terminal-command-", ".sh", context.cacheDir).also { it.delete() }
        val body = "#!/system/bin/sh\n# 中文 comment and \"quotes\"\nprintf '%s\\n' \"\uD83D\uDE42\"\n" + "# ${"long path ".repeat(16)}\n"
        // Same nested sh -c + quoted heredoc structure as the reported su command; only a private test file is written.
        val nested = "cat << \"EOF\" > ${ShellQuoting.quote(file.path)}\n${body}EOF\nchmod 700 ${ShellQuoting.quote(file.path)}"
        val command = "sh -c ${ShellQuoting.quote(nested)}\nfalse"
        val paths = TerminalPaths.of(context).ensureLayout()
        val argv = TerminalSessionLauncher.buildCommand(paths.home.path, TerminalCommand.script(command, "Command", "Exit code"), true)
        val output = ByteArrayOutputStream()
        val session = onMain { TerminalSessionManager.create(context, paths.home.path, argv = argv).also {
            it.pty.addOutputTap { bytes, offset, count -> output.write(bytes, offset, count) }
            it.pty.updateSize(30, 24)
        } }
        try {
            await { onMain { session.pty.transcriptText.contains("Exit code: 1") } }
            assertArrayEquals(body.toByteArray(Charsets.UTF_8), file.readBytes())
            val transcript = onMain { session.pty.transcriptText }
            // A terminal transcript trims trailing spaces and can coalesce a hard newline at an exact
            // column boundary. Compare the child's bytes here; the UI command viewer separately checks
            // exact, untrimmed source independently of terminal layout.
            val raw = onMain { output.toString("UTF-8").replace("\r\n", "\n") }
            assertTrue(raw, raw.contains(command))
            assertTrue(transcript, transcript.contains("--- Command ---"))
        } finally {
            onMain { TerminalSessionManager.close(session.id) }
            file.delete()
        }
    }

    @Test
    fun unicodeDirectoryAndEnvironmentReachTheNativeChildUnchanged() {
        val directory = File(context.cacheDir, "terminal-中文-\uD83D\uDE42").apply { mkdirs() }
        val result = File(directory, "result.txt")
        val expected = "中文\uD83D\uDE42"
        val command = "printf '%s' \"\$AJ6_UNICODE\" > result.txt; printf '__%s__\\n' ENV_DONE"
        val argv = TerminalSessionLauncher.buildCommand(directory.path, command, true)
        val session = onMain { TerminalSessionManager.create(context, directory.path, mapOf("AJ6_UNICODE" to expected), argv = argv).also { it.pty.updateSize(40, 24) } }
        try {
            await { onMain { session.pty.transcriptText.contains("__ENV_DONE__") } }
            assertArrayEquals(expected.toByteArray(Charsets.UTF_8), result.readBytes())
            assertEquals(directory.canonicalPath, session.pty.currentDirectory()?.canonicalPath)
        } finally {
            onMain { TerminalSessionManager.close(session.id) }
            directory.deleteRecursively()
        }
    }

    private fun withShell(block: (TerminalSessionManager.Session) -> Unit) {
        val paths = TerminalPaths.of(context).ensureLayout()
        val session = onMain { TerminalSessionManager.create(context, paths.home.path).also { it.pty.updateSize(40, 24) } }
        try {
            await { onMain { session.pty.transcriptText.contains("$") } }
            block(session)
        } finally { onMain { TerminalSessionManager.close(session.id) } }
    }

    private fun await(condition: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + 30000
        while (!condition()) {
            assertTrue("timed out", SystemClock.uptimeMillis() < deadline)
            SystemClock.sleep(25)
        }
    }

    private fun <T> onMain(action: () -> T): T {
        val result = AtomicReference<T>()
        instrumentation.runOnMainSync { result.set(action()) }
        return result.get()
    }
}
