package io.github.supermonster003.autojs6.plugin.three.shell.terminal.core

import android.app.ActivityManager
import android.os.Build
import android.os.Process
import android.os.SystemClock
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.service.ThreeShellTerminalSessionService
import jackpal.androidterm.emulatorview.TerminalSelectionSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.atomic.AtomicReference

/**
 * Roadmap P2.1 device evidence for the session core without any Activity: two shells run in
 * separate ptys whose output never crosses, `closeAll` leaves no shell behind, the exit code is
 * recorded, startup cancellation never leaks a child, the transcript snapshot keeps logical lines,
 * and the foreground service follows the session list. Results are logged under [TAG].
 */
@RunWith(AndroidJUnit4::class)
class TerminalSessionsInstrumentationTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    @Test
    fun twoSessionsRunSeparateShellsAndCloseAllLeavesNoShellBehind() {
        val paths = TerminalPaths.of(context).ensureLayout()
        val first = onMain { TerminalSessionManager.create(context, paths.home.path).also { it.pty.updateSize(80, 24) } }
        val second = onMain { TerminalSessionManager.create(context, paths.tmp.path).also { it.pty.updateSize(80, 24) } }
        try {
            assertEquals("home", first.title)
            assertEquals("tmp", second.title)
            assertNotEquals(first.id, second.id)
            await { first.pty.pid > 0 && second.pty.pid > 0 }
            assertNotEquals(first.pty.pid, second.pty.pid)
            await { onMain { first.pty.transcriptText.contains("$") && second.pty.transcriptText.contains("$") } }

            onMain { first.pty.write("printf '__AJ6_PID_%s__\\n' \"\$\$\"\r") }
            val firstPid = awaitValue { onMain { PID_MARKER.find(first.pty.transcriptText)?.groupValues?.get(1)?.toInt() } }
            onMain { second.pty.write("printf '__AJ6_PID_%s__\\n' \"\$\$\"\r") }
            val secondPid = awaitValue { onMain { PID_MARKER.find(second.pty.transcriptText)?.groupValues?.get(1)?.toInt() } }
            // The wrapper execs the shell, so $$ is the pty child itself.
            assertEquals(first.pty.pid, firstPid)
            assertEquals(second.pty.pid, secondPid)
            assertNotEquals(firstPid, secondPid)
            onMain {
                assertFalse("second session output leaked into the first", first.pty.transcriptText.contains("__AJ6_PID_${secondPid}__"))
                assertFalse("first session output leaked into the second", second.pty.transcriptText.contains("__AJ6_PID_${firstPid}__"))
            }
            await { first.pty.currentDirectory()?.canonicalPath == paths.home.canonicalPath }
            await { second.pty.currentDirectory()?.canonicalPath == paths.tmp.canonicalPath }
            assertTrue(TerminalSessionManager.hasSessions)
            assertTrue(TerminalSessionManager.activeSessions.map { it.id }.containsAll(listOf(first.id, second.id)))
            val serviceSeen = awaitOrNull(5_000) { isSessionServiceRunning() }
            Log.i(TAG, "session service running while two sessions exist on API ${Build.VERSION.SDK_INT}: $serviceSeen")
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                // Below API 31 nothing restricts the start; from API 31 the instrumented process may be refused.
                assertTrue("the foreground service must run while sessions exist", serviceSeen)
            }

            val closed = onMain { TerminalSessionManager.closeAll() }
            assertTrue("closeAll must report the closed sessions", closed >= 2)
            await { first.pty.exitCode != null && second.pty.exitCode != null }
            assertFalse(first.pty.isProcessAlive)
            assertFalse(second.pty.isProcessAlive)
            assertFalse(first.isAlive)
            assertNull(TerminalSessionManager.get(first.id))
            assertNull(TerminalSessionManager.get(second.id))
            assertFalse(TerminalSessionManager.hasSessions)
            await { !File("/proc/$firstPid").exists() && !File("/proc/$secondPid").exists() }
            assertEquals("no shell may survive closeAll", emptyList<Int>(), liveShellChildren())
            await { !isSessionServiceRunning() }
            Log.i(TAG, "closeAll closed $closed sessions, shells $firstPid / $secondPid reaped, service stopped")
        } finally {
            onMain {
                TerminalSessionManager.close(first.id)
                TerminalSessionManager.close(second.id)
            }
        }
    }

    @Test
    fun theExitCodeIsRecordedAndTheSessionLeavesTheRegistry() {
        val paths = TerminalPaths.of(context).ensureLayout()
        val session = onMain { TerminalSessionManager.create(context, paths.home.path, command = "exit 7").also { it.pty.updateSize(80, 24) } }
        try {
            assertEquals("exit 7", session.title)
            await { onMain { session.pty.transcriptText.contains("$") } }
            onMain { session.pty.write("exit 7\r") }
            await { session.exitCode != null }
            assertEquals(7, session.exitCode)
            assertEquals(7, session.pty.exitCode)
            assertFalse(session.isAlive)
            await { onMain { TerminalSessionManager.get(session.id) == null } }
            assertFalse(TerminalSessionManager.allSessions.any { it.id == session.id })
            Log.i(TAG, "session ${session.id} exited with ${session.exitCode} and left the registry")
        } finally {
            onMain { TerminalSessionManager.close(session.id) }
        }
    }

    @Test
    fun closingDuringStartupDoesNotLeaveALiveShell() {
        val paths = TerminalPaths.of(context).ensureLayout()
        // Exercise immediate cancellation, the fork/setsid window, and background manager sessions.
        repeat(30) { iteration ->
            val session = onMain {
                TerminalSessionManager.create(context, paths.home.path).also {
                    if (iteration % 2 == 0) it.pty.updateSize(80, 24)
                    if (iteration % 3 == 0) TerminalSessionManager.close(it.id)
                }
            }
            try {
                if (iteration % 3 != 0) {
                    SystemClock.sleep((iteration % 3).toLong())
                    onMain { TerminalSessionManager.close(session.id) }
                }
                assertFalse(session.pty.isProcessAlive)
                await { session.pty.exitCode != null }
                assertNull(TerminalSessionManager.get(session.id))
                val pid = session.pty.pid
                if (pid > 0) assertFalse("Shell $pid survived cancellation", File("/proc/$pid").exists())
            } finally {
                onMain { TerminalSessionManager.close(session.id) }
            }
        }
        assertEquals(emptyList<Int>(), liveShellChildren())
    }

    @Test
    fun selectionSnapshotPreservesLogicalLinesAndSurvivesNewOutput() {
        val paths = TerminalPaths.of(context).ensureLayout()
        val session = onMain { TerminalSessionManager.create(context, paths.home.path).also { it.pty.updateSize(24, 8) } }
        val line = "select-中文-" + "abcdef0123456789".repeat(6)
        try {
            await { onMain { session.pty.transcriptText.contains("$") } }
            onMain { session.pty.write("printf '\\n%s\\n%s\\n' '$line' 'hard-line-END'\r") }
            await { onMain { session.pty.transcriptText.contains("$line\nhard-line-END") } }
            val snapshot = onMain { TerminalSelectionSnapshot.capture(session.pty, 24, 0) }
            val copied = snapshot.selectedText(0, snapshot.text.length)
            assertTrue(copied.contains("$line\nhard-line-END"))
            assertTrue(snapshot.text.contains("\n"))
            onMain { session.pty.write("printf '__FRESH_%s__\\n' output\r") }
            await { onMain { session.pty.transcriptText.contains("__FRESH_output__") } }
            assertEquals(copied, snapshot.selectedText(0, snapshot.text.length))
            assertFalse(snapshot.text.contains("__FRESH_output__"))
            val end = snapshot.text.indexOf("hard-line-END")
            assertEquals("hard-line-END", snapshot.selectedText(end, end + 13))
        } finally {
            onMain { TerminalSessionManager.close(session.id) }
        }
    }

    /** Pids of live (non-zombie) `sh` processes whose parent is this process. */
    private fun liveShellChildren(): List<Int> {
        val self = Process.myPid()
        val proc = File("/proc").listFiles { file -> file.isDirectory && file.name.all(Char::isDigit) } ?: return emptyList()
        return proc.mapNotNull { dir ->
            runCatching {
                val stat = File(dir, "stat").readText()
                val comm = stat.substringAfter('(').substringBeforeLast(')')
                val rest = stat.substringAfterLast(')').trim().split(' ')
                val state = rest.getOrNull(0)
                val ppid = rest.getOrNull(1)?.toIntOrNull()
                dir.name.toInt().takeIf { ppid == self && comm == "sh" && state != "Z" }
            }.getOrNull()
        }
    }

    @Suppress("DEPRECATION")
    private fun isSessionServiceRunning(): Boolean {
        val manager = context.getSystemService(ActivityManager::class.java) ?: return false
        return manager.getRunningServices(Int.MAX_VALUE).any { it.service.className == ThreeShellTerminalSessionService::class.java.name }
    }

    private fun await(condition: () -> Boolean) {
        check(awaitOrNull(15_000, condition)) { "Terminal state did not settle" }
    }

    private fun awaitOrNull(timeoutMillis: Long, condition: () -> Boolean): Boolean {
        val deadline = SystemClock.uptimeMillis() + timeoutMillis
        while (!condition()) {
            if (SystemClock.uptimeMillis() >= deadline) return false
            SystemClock.sleep(50)
        }
        return true
    }

    private fun <T : Any> awaitValue(supplier: () -> T?): T {
        var value: T? = null
        await { value = supplier(); value != null }
        return value!!
    }

    private fun <T> onMain(block: () -> T): T {
        val value = AtomicReference<Result<T>>()
        instrumentation.runOnMainSync { value.set(runCatching(block)) }
        return value.get().getOrThrow()
    }

    private companion object {
        const val TAG = "ThreeShellSessions"
        val PID_MARKER = Regex("__AJ6_PID_(\\d+)__")
    }

}
