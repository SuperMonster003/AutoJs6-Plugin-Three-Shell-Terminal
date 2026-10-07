package io.github.supermonster003.autojs6.plugin.three.shell.terminal.service

import android.Manifest
import android.app.Instrumentation
import android.app.Notification
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.core.CommandNoticePolicy
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.core.FinishedCommands
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.core.TerminalPaths
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.core.TerminalPreferences
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.core.TerminalSessionLauncher
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.core.TerminalSessionManager
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui.CommandResultActivity
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui.TerminalActivity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicReference

/**
 * Roadmap P8 command completion notices on a device: a command without an interactive shell that ends by itself
 * posts a notice on its own channel and keeps the end of its output for the result screen; an interactive shell,
 * a session closed on request, the switch turned off and a session the user is watching post nothing.
 */
@RunWith(AndroidJUnit4::class)
class CommandNotificationsInstrumentationTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val preferences = TerminalPreferences(context)
    private val notifications = context.getSystemService(NotificationManager::class.java)
    private var enabled = true
    private val created = ArrayList<TerminalSessionManager.Session>()

    @Before
    fun setUp() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            instrumentation.uiAutomation.grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
        }
        enabled = preferences.commandNotificationsEnabled
        preferences.commandNotificationsEnabled = true
        CommandNotifications.visibleSessionId = null
    }

    @After
    fun tearDown() {
        preferences.commandNotificationsEnabled = enabled
        CommandNotifications.visibleSessionId = null
        onMain { created.forEach { TerminalSessionManager.close(it.id) } }
        created.forEach { notifications.cancel(CommandNoticePolicy.notificationId(it.id)) }
        notifications.cancel(CommandNoticePolicy.SUMMARY_ID)
        FinishedCommands.clear()
    }

    @Test
    fun aFinishedCommandPostsANoticeAndItsResultScreenShowsTheOutput() {
        val home = TerminalPaths.of(context).ensureLayout().home.path
        val session = start(home, "printf '__%s__\\n' finished-command; exit 3")
        val result = awaitValue { FinishedCommands.get(session.id) }
        assertEquals(3, result.exitCode)
        assertTrue(result.transcript.orEmpty(), result.transcript.orEmpty().contains("__finished-command__"))
        val notice = awaitValue { active(session.id) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) assertEquals(CommandNotifications.CHANNEL_ID, notice.channelId)
        val title = notice.extras.getCharSequence(Notification.EXTRA_TITLE).toString()
        val text = notice.extras.getCharSequence(Notification.EXTRA_TEXT).toString()
        assertEquals(context.getString(io.github.supermonster003.autojs6.plugin.three.shell.terminal.R.string.notification_command_failed, 3), title)
        // The notice names the session and directory only, never the command or its output.
        assertTrue(text, text.contains(session.id) && !text.contains("finished-command"))
        assertNotNull(notice.contentIntent)
        assertEquals(GROUP_KEY, notice.group)
        assertNotNull("the group summary alerts once for a burst of notices", notifications.activeNotifications.firstOrNull { it.id == CommandNoticePolicy.SUMMARY_ID })

        val screen = instrumentation.startActivitySync(CommandResultActivity.intent(context, session.id, home).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as CommandResultActivity
        val output = onMain { screen.scaffold.content.findViewWithTag<TextView>(CommandResultActivity.TAG_OUTPUT).text.toString() }
        val status = onMain { screen.scaffold.content.findViewWithTag<TextView>(CommandResultActivity.TAG_STATUS).text.toString() }
        assertTrue(output, output.contains("__finished-command__"))
        assertTrue(status, status.contains("3"))
        val monitor = Instrumentation.ActivityMonitor(TerminalActivity::class.java.name, null, false)
        instrumentation.addMonitor(monitor)
        val before = TerminalSessionManager.activeSessions.map { it.id }.toSet()
        try {
            onMain { screen.scaffold.content.findViewWithTag<View>(CommandResultActivity.TAG_OPEN).performClick() }
            val terminal = monitor.waitForActivityWithTimeout(15_000) as TerminalActivity?
            assertNotNull("Open terminal here must start a terminal", terminal)
            await { TerminalSessionManager.activeSessions.any { it.id !in before } }
            created += TerminalSessionManager.activeSessions.filter { it.id !in before }
            onMain { terminal!!.finish() }
        } finally {
            instrumentation.removeMonitor(monitor)
        }
        Log.i(TAG, "notice: '$title' / '$text', ${result.transcript?.length} chars kept, result screen and terminal opened")
    }

    @Test
    fun interactiveClosedDisabledAndWatchedSessionsPostNothing() {
        val home = TerminalPaths.of(context).ensureLayout().home.path
        // An interactive shell that exits by itself.
        val interactive = onMain { TerminalSessionManager.create(context, home).also { created += it; it.pty.updateSize(80, 24) } }
        onMain { interactive.pty.write("exit 0\r") }
        await { interactive.exitCode != null }
        // A command session closed on request.
        val closed = start(home, "sleep 30")
        await { closed.pty.isProcessAlive }
        onMain { TerminalSessionManager.close(closed.id) }
        await { closed.exitCode != null }
        // The switch is off.
        preferences.commandNotificationsEnabled = false
        val disabled = start(home, "exit 0")
        // The exit code is set on the watcher thread before the main-thread exit callback reads the switch;
        // leaving the registry happens after that callback.
        await { disabled.exitCode != null && TerminalSessionManager.get(disabled.id) == null }
        preferences.commandNotificationsEnabled = true
        // The user is looking at the session.
        val watched = onMain {
            val reserved = TerminalSessionManager.reserveId()
            CommandNotifications.visibleSessionId = reserved
            createCommand(home, "sleep 1; exit 0", reserved)
        }
        await { watched.exitCode != null && TerminalSessionManager.get(watched.id) == null }
        SystemClock.sleep(2_500)
        for (session in listOf(interactive, closed, disabled, watched)) {
            assertNull(session.id, FinishedCommands.get(session.id))
            assertNull(session.id, active(session.id))
        }
        Log.i(TAG, "quiet: interactive, closed, disabled and watched sessions posted no notice")
    }

    private fun start(directory: String, command: String): TerminalSessionManager.Session = onMain { createCommand(directory, command, null) }

    /** Main thread: a session like the Binder's `keepOpen: false` exec, with the default emulator size the Binder gives it. */
    private fun createCommand(directory: String, command: String, id: String?): TerminalSessionManager.Session {
        val session = TerminalSessionManager.create(
            context, directory, emptyMap(), null, command, id, TerminalSessionLauncher.buildCommand(directory, command, false), keepOpen = false,
        )
        session.pty.updateSize(80, 24)
        created += session
        return session
    }

    private fun active(sessionId: String): Notification? =
        notifications.activeNotifications.firstOrNull { it.id == CommandNoticePolicy.notificationId(sessionId) }?.notification

    private fun await(condition: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + 15_000
        while (!condition()) {
            check(SystemClock.uptimeMillis() < deadline) { "Command notice state did not settle" }
            SystemClock.sleep(50)
        }
    }

    private fun <T : Any> awaitValue(supplier: () -> T?): T {
        val deadline = SystemClock.uptimeMillis() + 15_000
        while (true) {
            supplier()?.let { return it }
            check(SystemClock.uptimeMillis() < deadline) { "Command notice state did not settle" }
            SystemClock.sleep(50)
        }
    }

    private fun <T> onMain(block: () -> T): T {
        val value = AtomicReference<Result<T>>()
        instrumentation.runOnMainSync { value.set(runCatching(block)) }
        return value.get().getOrThrow()
    }

    private companion object {
        const val TAG = "ThreeShellTerminalCmd"
        const val GROUP_KEY = "three.shell.terminal.commands"
    }

}
