package io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.view.children
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.R
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.core.TerminalPaths
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.core.TerminalSessionManager
import org.autojs.plugin.terminal.api.TerminalContract
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicReference

/**
 * Roadmap P3.2 device evidence for the session manager: the dialog survives the terminal
 * Activities it opens (new session, resumed session) and the original terminal comes back
 * underneath it (ported from the host), the standalone manager Activity shows the dialog without
 * a terminal and Back leaves it without one appearing, and the registry listener keeps the list
 * in step with sessions created elsewhere. Results are logged under [TAG].
 */
@RunWith(AndroidJUnit4::class)
class TerminalManagerInstrumentationTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    @Before
    fun grantNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            instrumentation.uiAutomation.grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    @Test
    fun managerSurvivesNewAndResumedSessionActivitiesAndRestoresTheOriginalTerminal() {
        val paths = TerminalPaths.of(context).ensureLayout()
        val initialIds = TerminalSessionManager.activeSessions.map { it.id }.toSet()
        val first = onMain { TerminalSessionManager.create(context, paths.home.path) }
        val host = instrumentation.startActivitySync(
            TerminalActivity.intent(context).putExtra(TerminalContract.EXTRA_SESSION_ID, first.id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        ) as TerminalActivity
        var manager: AlertDialog? = null
        var child: TerminalActivity? = null
        try {
            await { onMain { first.pty.transcriptText.contains("$") } }
            manager = onMain { TerminalManagerDialog.show(host) }
            val dialog = manager
            await { onMain { dialog.findViewById<View>(R.id.manager_session_list)?.let { sessionRow(it, first.id) != null } == true } }
            onMain { dialog.findViewById<View>(R.id.manager_new_session)!!.performClick() }
            await { onMain { resumedTerminal()?.let { it !== host } == true } }
            val createdTerminal = onMain { resumedTerminal()!! }
            child = createdTerminal
            val second = awaitValue { TerminalSessionManager.activeSessions.singleOrNull { it.id !in initialIds && it.id != first.id } }
            await { onMain { createdTerminal.findViewById<TerminalEmulatorView>(R.id.terminal)?.termSession === second.pty } }
            onMain {
                assertTrue(dialog.isShowing)
                createdTerminal.onSupportNavigateUp()
            }
            await { onMain { resumedTerminal() === host && dialog.window?.decorView?.hasWindowFocus() == true } }
            // The registry listener added the new session to the list while the dialog stayed up.
            await { onMain { sessionRow(dialog.findViewById(R.id.manager_session_list)!!, second.id) != null } }
            onMain {
                assertTrue(dialog.isShowing)
                val row = sessionRow(dialog.findViewById(R.id.manager_session_list)!!, first.id)!!
                row.findViewById<View>(R.id.manager_session_open).performClick()
            }
            await { onMain { resumedTerminal()?.let { it !== host } == true } }
            val resumed = onMain { resumedTerminal()!! }
            child = resumed
            await { onMain { resumed.findViewById<TerminalEmulatorView>(R.id.terminal)?.termSession === first.pty } }
            onMain {
                assertTrue(dialog.isShowing)
                resumed.onSupportNavigateUp()
            }
            await { onMain { resumedTerminal() === host && dialog.window?.decorView?.hasWindowFocus() == true } }
            // Closing from the row removes the session and the list follows without a reopen.
            onMain { sessionRow(dialog.findViewById(R.id.manager_session_list)!!, second.id)!!.findViewById<View>(R.id.manager_session_close).performClick() }
            await { second.pty.exitCode != null }
            await { onMain { sessionRow(dialog.findViewById(R.id.manager_session_list)!!, second.id) == null } }
            onMain {
                dialog.dismiss()
                first.pty.write("printf '__RETURNED_%s__\\n' alive\r")
            }
            await { onMain { first.pty.transcriptText.contains("__RETURNED_alive__") } }
            Log.i(TAG, "manager: survived new session ${second.id} and resumed ${first.id}, host terminal returned on API ${Build.VERSION.SDK_INT}")
        } finally {
            onMain {
                manager?.takeIf { it.isShowing }?.dismiss()
                child?.takeUnless { it.isDestroyed }?.finish()
                host.finish()
                TerminalSessionManager.activeSessions.filter { it.id !in initialIds }.forEach { TerminalSessionManager.close(it.id) }
            }
        }
    }

    @Test
    fun standaloneManagerShowsWithoutATerminalAndBackLeavesWithoutOne() {
        val before = TerminalSessionManager.activeSessions.map { it.id }.toSet()
        val activity = instrumentation.startActivitySync(
            TerminalManagerActivity.intent(context).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        ) as TerminalManagerActivity
        try {
            await { onMain { activity.dialog?.isShowing == true } }
            val dialog = onMain { activity.dialog!! }
            await { onMain { dialog.window?.decorView?.hasWindowFocus() == true } }
            onMain {
                assertNull("no terminal screen may appear for the manager entry", resumedTerminal())
                val list = dialog.findViewById<View>(R.id.manager_session_list)!!
                val empty = dialog.findViewById<View>(R.id.manager_no_sessions)!!
                if (before.isEmpty()) {
                    assertEquals(View.VISIBLE, empty.visibility)
                    assertEquals(0, (list as ViewGroup).childCount)
                } else {
                    assertEquals(View.GONE, empty.visibility)
                }
                assertNotNull(dialog.findViewById<View>(R.id.manager_new_session))
            }
            // Back goes to the focused dialog: it cancels, the Activity finishes, nothing else comes up.
            val sent = runCatching { instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK) }.isSuccess
            if (!sent) onMain { dialog.cancel() }
            await { activity.isFinishing || activity.isDestroyed }
            await { onMain { !dialog.isShowing } }
            SystemClock.sleep(300)
            onMain { assertNull(resumedTerminal()) }
            assertEquals(before, TerminalSessionManager.activeSessions.map { it.id }.toSet())
            Log.i(TAG, "standalone manager: shown without a terminal, Back (key=$sent) finished it without opening one")
        } finally {
            onMain { activity.takeUnless { it.isDestroyed }?.finish() }
        }
    }

    @Test
    fun managerFollowsSessionsCreatedOutsideTheDialog() {
        val paths = TerminalPaths.of(context).ensureLayout()
        val activity = instrumentation.startActivitySync(
            TerminalManagerActivity.intent(context).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        ) as TerminalManagerActivity
        var session: TerminalSessionManager.Session? = null
        try {
            await { onMain { activity.dialog?.isShowing == true } }
            val dialog = onMain { activity.dialog!! }
            val created = onMain { TerminalSessionManager.create(context, paths.tmp.path) }
            session = created
            await { onMain { sessionRow(dialog.findViewById(R.id.manager_session_list)!!, created.id) != null } }
            // The PID is known only after the launcher thread forked the shell; the periodic refresh picks it up.
            await { created.pty.pid > 0 }
            await { onMain { rowSummary(dialog, created.id)?.contains("PID: ${created.pty.pid}") == true } }
            onMain {
                assertEquals(View.GONE, dialog.findViewById<View>(R.id.manager_no_sessions)!!.visibility)
            }
            onMain { TerminalSessionManager.close(created.id) }
            await { created.pty.exitCode != null }
            await { onMain { sessionRow(dialog.findViewById(R.id.manager_session_list)!!, created.id) == null } }
            assertFalse(created.pty.isProcessAlive)
            Log.i(TAG, "manager: listed session ${created.id} created by the registry and dropped it after close")
        } finally {
            onMain {
                session?.let { TerminalSessionManager.close(it.id) }
                activity.dialog?.takeIf { it.isShowing }?.dismiss()
                activity.takeUnless { it.isDestroyed }?.finish()
            }
        }
    }

    private fun sessionRow(list: View, id: String): View? = (list as ViewGroup).children.firstOrNull { it.tag == id }

    private fun rowSummary(dialog: AlertDialog, id: String): String? =
        sessionRow(dialog.findViewById(R.id.manager_session_list)!!, id)?.findViewById<android.widget.TextView>(R.id.manager_session_summary)?.text?.toString()

    private fun resumedTerminal() = ActivityLifecycleMonitorRegistry.getInstance()
        .getActivitiesInStage(Stage.RESUMED).filterIsInstance<TerminalActivity>().singleOrNull()

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
        const val TAG = "ThreeShellTerminalManager"
    }

}
