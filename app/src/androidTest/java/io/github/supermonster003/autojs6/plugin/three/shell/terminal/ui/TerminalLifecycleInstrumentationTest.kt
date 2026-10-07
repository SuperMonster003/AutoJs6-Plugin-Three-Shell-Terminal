package io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui

import android.Manifest
import android.app.ActivityManager
import android.app.NotificationManager
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.util.Log
import android.view.inputmethod.InputMethodManager
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.R
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.core.TerminalPaths
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.core.TerminalSessionManager
import org.autojs.plugin.terminal.api.TerminalContract
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicReference

/** P6 lifecycle tests preserve existing sessions and only close their own shell. */
@RunWith(AndroidJUnit4::class)
class TerminalLifecycleInstrumentationTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    @Test
    fun aRealDockedStackResizesThePtyWithoutReplacingTheShell() {
        assumeTrue("coordinated API 24 split-screen check", Build.VERSION.SDK_INT == 24 &&
            InstrumentationRegistry.getArguments().getString("p6SplitScreen") == "true")
        val session = onMain { TerminalSessionManager.create(context, TerminalPaths.of(context).home.path) }
        var taskId = -1
        fun shell(command: String) = ParcelFileDescriptor.AutoCloseInputStream(
            instrumentation.uiAutomation.executeShellCommand(command),
        ).bufferedReader().use { it.readText() }
        try {
            ActivityScenario.launch<TerminalActivity>(TerminalActivity.intent(context).putExtra(TerminalContract.EXTRA_SESSION_ID, session.id)).use { scenario ->
                await("shell starts") { session.pty.pid > 0 }
                val pid = session.pty.pid
                scenario.onActivity { taskId = it.taskId }
                shell("am stack movetask $taskId 3 true")
                var docked = false
                await("Android enters split screen") { scenario.onActivity { docked = it.isInMultiWindowMode }; docked }
                assertGeometry(scenario, session, "docked")
                assertEquals(pid, session.pty.pid)
                shell("am stack movetask $taskId 1 true")
                await("Android restores fullscreen") { scenario.onActivity { docked = it.isInMultiWindowMode }; !docked }
                assertGeometry(scenario, session, "undocked")
                Log.i("ThreeShellP6", "splitScreen=real-docked-stack samePid=true geometry=matched")
            }
        } finally {
            if (taskId >= 0) runCatching { shell("am stack movetask $taskId 1 true") }
            onMain { TerminalSessionManager.close(session.id) }
        }
    }

    @Test
    fun recreationRotationAndKeyboardKeepTheShellAndSynchronizeThePty() {
        if (Build.VERSION.SDK_INT >= 33) instrumentation.uiAutomation.grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
        val session = onMain { TerminalSessionManager.create(context, TerminalPaths.of(context).home.path) }
        val intent = TerminalActivity.intent(context).putExtra(TerminalContract.EXTRA_SESSION_ID, session.id)
        try {
            ActivityScenario.launch<TerminalActivity>(intent).use { scenario ->
                await("prompt") { onMain { session.pty.isRunning && session.pty.transcriptText.contains("$") } }
                val pid = session.pty.pid
                assertGeometry(scenario, session, "initial")
                scenario.recreate()
                assertGeometry(scenario, session, "recreated")
                scenario.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
                SystemClock.sleep(700)
                assertGeometry(scenario, session, "landscape")
                scenario.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT }
                SystemClock.sleep(700)
                assertGeometry(scenario, session, "portrait")
                scenario.onActivity {
                    val view = it.findViewById<TerminalEmulatorView>(R.id.terminal)
                    it.getSystemService(InputMethodManager::class.java).hideSoftInputFromWindow(view.windowToken, 0)
                }
                SystemClock.sleep(700)
                assertGeometry(scenario, session, "keyboard-hidden")
                scenario.onActivity {
                    val view = it.findViewById<TerminalEmulatorView>(R.id.terminal)
                    view.requestFocus()
                    it.getSystemService(InputMethodManager::class.java).showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
                }
                SystemClock.sleep(700)
                assertGeometry(scenario, session, "keyboard-shown")
                assertEquals("recreation must not replace the user's shell", pid, session.pty.pid)
            }
            assertTrue("leaving the terminal keeps its shell alive", session.isAlive)
        } finally { onMain { TerminalSessionManager.close(session.id) } }
    }

    @Test
    fun removingTheTaskKeepsTheSessionAndItsForegroundNotification() {
        if (Build.VERSION.SDK_INT >= 33) instrumentation.uiAutomation.grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
        val session = onMain { TerminalSessionManager.create(context, TerminalPaths.of(context).home.path) }
        try {
            ActivityScenario.launch<TerminalActivity>(TerminalActivity.intent(context).putExtra(TerminalContract.EXTRA_SESSION_ID, session.id)).use { scenario ->
                val restricted = Build.VERSION.SDK_INT >= 28 && context.getSystemService(ActivityManager::class.java).isBackgroundRestricted
                if (!restricted) await("notification") { notifications() > 0 }
                scenario.onActivity { it.finishAndRemoveTask() }
                SystemClock.sleep(800)
                assertTrue(session.isAlive)
                if (!restricted) assertTrue("notification remains after task removal", notifications() > 0)
                onMain { session.pty.write("printf '__P6_%s__\\n' BACKGROUND\r") }
                await("background shell answers") { onMain { session.pty.transcriptText.contains("__P6_BACKGROUND__") } }
                Log.i("ThreeShellP6", "removeTask=session-alive,shell-responsive backgroundRestricted=$restricted notifications=${notifications()}")
            }
        } finally { onMain { TerminalSessionManager.close(session.id) } }
    }

    private fun assertGeometry(scenario: ActivityScenario<TerminalActivity>, session: TerminalSessionManager.Session, stage: String) {
        var columns = 0
        var rows = 0
        scenario.onActivity {
            val view = it.findViewById<TerminalEmulatorView>(R.id.terminal)
            assertTrue(view.termSession === session.pty)
            columns = view.visibleColumns
            rows = view.visibleRows
            session.pty.write("printf '__P6_${stage}_%s__\\n' \"\$LINES \$COLUMNS\"\r")
        }
        assertTrue(columns > 0 && rows > 0)
        await("view/kernel geometry at $stage ($rows x $columns)") {
            onMain { session.pty.transcriptText.contains("__P6_${stage}_${rows} $columns" + "__") }
        }
        Log.i("ThreeShellP6", "viewStage=$stage columns=$columns rows=$rows")
    }

    private fun notifications() = context.getSystemService(NotificationManager::class.java).activeNotifications.size

    private fun await(what: String, condition: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + 15_000
        while (!condition()) { check(SystemClock.uptimeMillis() < deadline) { "Timed out: $what" }; SystemClock.sleep(30) }
    }

    private fun <T> onMain(block: () -> T): T {
        val value = AtomicReference<Result<T>>()
        instrumentation.runOnMainSync { value.set(runCatching(block)) }
        return value.get().getOrThrow()
    }
}
