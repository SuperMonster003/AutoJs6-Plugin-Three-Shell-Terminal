package io.github.supermonster003.autojs6.plugin.three.shell.terminal

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.core.TerminalPaths
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.core.TerminalSessionManager
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui.LauncherActivity
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui.TerminalActivity
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui.TerminalEmulatorView
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui.TerminalManagerActivity
import org.autojs.plugin.terminal.api.TerminalContract
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.atomic.AtomicReference

/**
 * Roadmap P3.3 device evidence for the `TERMINAL_OPEN` entry and the launcher forwarder: a caller
 * without the PLUGIN permission (the adb shell) is refused by Android before the plugin runs, a
 * same-signer caller (the plugin's own uid stands in for the host) lands in the requested directory
 * in a new session whose screen is the root of its own task, `manager=true` shows the manager over
 * the caller without a terminal, and the launcher restores the most recent session or starts one at
 * home. Results are logged under [TAG].
 */
@RunWith(AndroidJUnit4::class)
class ThreeShellTerminalEntryInstrumentationTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val entry = ComponentName(context, ThreeShellTerminalEntryActivity::class.java)

    @Before
    fun grantNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            instrumentation.uiAutomation.grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    @Test
    fun aCallerWithoutThePluginPermissionIsRefusedByAndroid() {
        val before = sessionIds()
        shell("logcat -c")
        // The adb shell (uid 2000) holds no signature permission of the host; `am` prints the SecurityException on
        // stderr, which UiAutomation does not return, so the denial is read back from the activity manager's log.
        val output = shell(
            "am start -a ${ThreeShellTerminalPlugin.OPEN_TERMINAL_ACTION} -c android.intent.category.DEFAULT " +
                "-n ${entry.flattenToString()} --es ${TerminalContract.EXTRA_DIRECTORY} /sdcard --ez ${TerminalContract.EXTRA_NEW_SESSION} true",
        )
        SystemClock.sleep(1_500)
        val denial = (output + shell("logcat -d -s ActivityManager:W ActivityTaskManager:W")).lineSequence()
            .firstOrNull { it.contains("Permission Denial") && it.contains(ThreeShellTerminalPlugin.OPEN_TERMINAL_ACTION) }
        assertTrue("Android must refuse the shell uid before the entry runs: $output", denial != null)
        assertTrue(denial!!, denial.contains("uid=2000") && denial.contains("requires ${ThreeShellTerminalPlugin.PLUGIN_PERMISSION}"))
        onMain {
            assertNull("no terminal may appear for a refused caller", resumedTerminal())
            assertNull(resumedManager())
        }
        assertEquals("no session may start for a refused caller", before, sessionIds())
        Log.i(TAG, "entry: shell caller refused by Android on API ${Build.VERSION.SDK_INT}: ${denial.substringAfter("Permission Denial: ")}")
    }

    @Test
    fun aSameSignerCallerOpensTheRequestedDirectoryInANewSessionInItsOwnTask() {
        val paths = TerminalPaths.of(context).ensureLayout()
        val before = sessionIds()
        var created: TerminalSessionManager.Session? = null
        var terminal: TerminalActivity? = null
        try {
            // The instrumentation runs under the plugin's uid, which holds the permission and is signed like the host.
            instrumentation.startActivitySync(
                entryIntent().putExtra(TerminalContract.EXTRA_DIRECTORY, paths.tmp.path).putExtra(TerminalContract.EXTRA_NEW_SESSION, true),
            )
            val screen = awaitValue { onMain { resumedTerminal() } }
            terminal = screen
            val session = awaitValue { TerminalSessionManager.activeSessions.singleOrNull { it.id !in before } }
            created = session
            await { onMain { screen.findViewById<TerminalEmulatorView>(R.id.terminal)?.termSession === session.pty } }
            await { session.pty.pid > 0 && session.pty.currentDirectory() != null }
            assertSameDirectory(paths.tmp, session.pty.currentDirectory()!!)
            assertSameDirectory(paths.tmp, File(session.initialDirectory))
            onMain {
                assertTrue("the terminal started by the entry owns its task", screen.isTaskRoot)
                assertFalse("the entry never shows", resumedEntry())
            }
            // Back removes the terminal with its task and keeps the session.
            onMain { screen.onBackPressedDispatcher.onBackPressed() }
            await { screen.isFinishing || screen.isDestroyed }
            SystemClock.sleep(300)
            assertTrue(session.pty.isProcessAlive)
            onMain { assertNull(resumedTerminal()) }
            Log.i(TAG, "entry: same-signer caller opened ${session.id} in ${paths.tmp.path} as a task root on API ${Build.VERSION.SDK_INT}")
        } finally {
            onMain {
                terminal?.takeUnless { it.isDestroyed }?.finish()
                created?.let { TerminalSessionManager.close(it.id) }
            }
        }
    }

    @Test
    fun theManagerExtraShowsTheManagerOverTheCallerWithoutATerminal() {
        val before = sessionIds()
        var manager: TerminalManagerActivity? = null
        try {
            instrumentation.startActivitySync(entryIntent().putExtra(TerminalContract.EXTRA_MANAGER, true))
            val shown = awaitValue { onMain { resumedManager()?.takeIf { it.dialog?.isShowing == true } } }
            manager = shown
            onMain {
                assertNull("manager=true must not open a terminal", resumedTerminal())
                assertTrue(shown.dialog!!.isShowing)
            }
            onMain { shown.dialog!!.cancel() }
            await { shown.isFinishing || shown.isDestroyed }
            SystemClock.sleep(300)
            onMain { assertNull(resumedTerminal()) }
            assertEquals(before, sessionIds())
            Log.i(TAG, "entry: manager=true showed the manager without a terminal and left without one on API ${Build.VERSION.SDK_INT}")
        } finally {
            onMain {
                manager?.dialog?.takeIf { it.isShowing }?.dismiss()
                manager?.takeUnless { it.isDestroyed }?.finish()
            }
        }
    }

    @Test
    fun contradictoryExtrasFinishWithoutATerminal() {
        val before = sessionIds()
        instrumentation.startActivitySync(
            entryIntent().putExtra(TerminalContract.EXTRA_MANAGER, true).putExtra(TerminalContract.EXTRA_NEW_SESSION, true),
        )
        SystemClock.sleep(1_500)
        onMain {
            assertNull(resumedTerminal())
            assertNull(resumedManager())
        }
        assertEquals(before, sessionIds())
        Log.i(TAG, "entry: manager=true combined with newSession was ignored on API ${Build.VERSION.SDK_INT}")
    }

    @Test
    fun theLauncherRestoresTheMostRecentSessionOrStartsOneAtHome() {
        val paths = TerminalPaths.of(context).ensureLayout()
        val before = sessionIds()
        val previous = TerminalSessionManager.activeSessions.lastOrNull()
        var terminal: TerminalActivity? = null
        var recent: TerminalSessionManager.Session? = null
        try {
            instrumentation.startActivitySync(Intent(context, LauncherActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            val first = awaitValue { onMain { resumedTerminal() } }
            terminal = first
            if (previous == null) {
                val started = awaitValue { TerminalSessionManager.activeSessions.singleOrNull { it.id !in before } }
                await { onMain { first.findViewById<TerminalEmulatorView>(R.id.terminal)?.termSession === started.pty } }
                await { started.pty.pid > 0 && started.pty.currentDirectory() != null }
                assertSameDirectory(paths.home, started.pty.currentDirectory()!!)
                Log.i(TAG, "launcher: no session yet, started ${started.id} at home")
            } else {
                await { onMain { first.findViewById<TerminalEmulatorView>(R.id.terminal)?.termSession === previous.pty } }
                assertEquals(before, sessionIds())
                Log.i(TAG, "launcher: restored the most recent session ${previous.id}")
            }
            onMain { assertTrue("the launcher task is the terminal task", first.isTaskRoot) }
            onMain { first.onBackPressedDispatcher.onBackPressed() }
            await { first.isFinishing || first.isDestroyed }
            // A session created meanwhile is the most recent one and comes back on the next launch.
            val newest = onMain { TerminalSessionManager.create(context, paths.tmp.path) }
            recent = newest
            await { onMain { resumedTerminal() == null } }
            instrumentation.startActivitySync(Intent(context, LauncherActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            val second = awaitValue { onMain { resumedTerminal()?.takeIf { it !== first } } }
            terminal = second
            await { onMain { second.findViewById<TerminalEmulatorView>(R.id.terminal)?.termSession === newest.pty } }
            Log.i(TAG, "launcher: second launch restored the newest session ${newest.id} on API ${Build.VERSION.SDK_INT}")
        } finally {
            onMain {
                terminal?.takeUnless { it.isDestroyed }?.finish()
                recent?.let { TerminalSessionManager.close(it.id) }
                TerminalSessionManager.activeSessions.filter { it.id !in before }.forEach { TerminalSessionManager.close(it.id) }
            }
        }
    }

    private fun entryIntent(): Intent = Intent(ThreeShellTerminalPlugin.OPEN_TERMINAL_ACTION)
        .setComponent(entry)
        .addCategory(Intent.CATEGORY_DEFAULT)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    private fun sessionIds(): Set<String> = TerminalSessionManager.activeSessions.map { it.id }.toSet()

    private fun resumedTerminal() = ActivityLifecycleMonitorRegistry.getInstance()
        .getActivitiesInStage(Stage.RESUMED).filterIsInstance<TerminalActivity>().singleOrNull()

    private fun resumedManager() = ActivityLifecycleMonitorRegistry.getInstance()
        .getActivitiesInStage(Stage.RESUMED).filterIsInstance<TerminalManagerActivity>().singleOrNull()

    private fun resumedEntry() = ActivityLifecycleMonitorRegistry.getInstance()
        .getActivitiesInStage(Stage.RESUMED).any { it is ThreeShellTerminalEntryActivity }

    private fun sameDirectory(expected: File, actual: File): Boolean {
        if (expected.canonicalPath == actual.canonicalPath) return true
        val left = runCatching { android.system.Os.stat(expected.path) }.getOrNull() ?: return false
        val right = runCatching { android.system.Os.stat(actual.path) }.getOrNull() ?: return false
        return left.st_dev == right.st_dev && left.st_ino == right.st_ino
    }

    private fun assertSameDirectory(expected: File, actual: File) {
        assertTrue("expected ${expected.path} (${expected.canonicalPath}) but was ${actual.path} (${actual.canonicalPath})", sameDirectory(expected, actual))
    }

    private fun shell(command: String): String {
        val descriptor = instrumentation.uiAutomation.executeShellCommand(command)
        return ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { String(it.readBytes()) }.also {
            Log.i(TAG, "shell: $command -> ${it.trim()}")
        }
    }

    private fun await(condition: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + 15_000
        while (!condition()) {
            check(SystemClock.uptimeMillis() < deadline) { "Terminal state did not settle" }
            SystemClock.sleep(50)
        }
    }

    private fun <T : Any> awaitValue(supplier: () -> T?): T {
        val deadline = SystemClock.uptimeMillis() + 15_000
        while (true) {
            supplier()?.let { return it }
            check(SystemClock.uptimeMillis() < deadline) { "Terminal state did not settle" }
            SystemClock.sleep(50)
        }
    }

    private fun <T> onMain(block: () -> T): T {
        val value = AtomicReference<Result<T>>()
        instrumentation.runOnMainSync { value.set(runCatching(block)) }
        return value.get().getOrThrow()
    }

    private companion object {
        const val TAG = "ThreeShellTerminalEntry"
    }

}
