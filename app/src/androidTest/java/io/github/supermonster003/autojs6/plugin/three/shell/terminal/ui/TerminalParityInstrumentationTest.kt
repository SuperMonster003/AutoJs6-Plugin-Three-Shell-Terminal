package io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui

import android.Manifest
import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.SystemClock
import android.util.Log
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.appcompat.widget.Toolbar
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.R
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.core.TerminalPaths
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.core.TerminalPreferences
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.core.TerminalSessionManager
import org.autojs.plugin.terminal.api.TerminalContract
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicReference

/**
 * Automated parts of the P3 manual parity checklist (`docs/dev/p3-ui-evidence.md`): a real long
 * press opens the frozen-transcript selection, select-all + copy return logical lines, Back closes
 * only the selection, the transcript menu actions stay within the Binder transaction limit even
 * after a long session, and a two-finger pinch changes and persists the text size. Toolbar
 * taps go through [TerminalTextSelection.perform], the same path as the selection toolbar.
 */
@RunWith(AndroidJUnit4::class)
class TerminalParityInstrumentationTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    @Before
    fun grantNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            instrumentation.uiAutomation.grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    @Test
    fun longPressSelectionCopiesLogicalLinesAndBackOnlyClosesTheSelection() {
        val paths = TerminalPaths.of(context).ensureLayout()
        val session = onMain { TerminalSessionManager.create(context, paths.home.path) }
        var activity: TerminalActivity? = null
        try {
            activity = startTerminal(session.id)
            val terminal = activity
            val view = awaitAttached(terminal, session)
            val columns = onMain { view.visibleColumns }
            // A literal token: the API 24 mksh printf mangles wide numeric padding such as %0133d.
            val longLine = "LONG_" + "a".repeat(columns + 20) + "_END"
            onMain { session.pty.write("printf '__SEL_%s__\\n' one two; printf '%s\\n' '$longLine'\r") }
            await { onMain { session.pty.transcriptText.contains("a_END\n") } }

            longPress(view)
            await { onMain { terminal.textSelectionForTest != null } }
            val overlay = onMain { terminal.findViewById<android.widget.TextView>(R.id.terminal_selection) }
            await(timeoutMillis = 5_000, failure = {
                onMain { "selection overlay=${overlay != null} focused=${overlay?.isFocused} hasSelection=${overlay?.hasSelection()} actionMode=${terminal.textSelectionForTest?.isActionModeShown}" }
            }) { onMain { terminal.textSelectionForTest?.isActionModeShown == true } }
            onMain {
                val selection = terminal.textSelectionForTest!!
                assertNotNull("the frozen transcript overlay must be shown", terminal.findViewById<View>(R.id.terminal_selection))
                assertTrue(selection.perform(android.R.id.selectAll))
                assertTrue(selection.perform(android.R.id.copy))
            }
            await { onMain { terminal.textSelectionForTest == null && terminal.findViewById<View>(R.id.terminal_selection) == null } }
            val copied = readClipboard(terminal)
            assertTrue("copied text: ${copied.takeLast(400)}", copied.contains("__SEL_one__") && copied.contains("__SEL_two__"))
            // The wrapped line comes back as the single line the shell printed, not as screen rows.
            assertTrue(
                "logical line must survive wrapping at $columns columns: ${copied.lines().filter { it.contains("LONG_") || it.contains("_END") }}",
                copied.lines().any { it == longLine },
            )

            longPress(view)
            await { onMain { terminal.textSelectionForTest?.isActionModeShown == true } }
            onMain { terminal.onBackPressedDispatcher.onBackPressed() }
            await { onMain { terminal.textSelectionForTest == null } }
            onMain {
                assertFalse("Back must close the selection, not the screen", terminal.isFinishing)
                assertTrue(session.pty.isProcessAlive)
                assertTrue(view.hasFocus() || terminal.findViewById<TerminalEmulatorView>(R.id.terminal).hasFocus())
            }
            Log.i(TAG, "selection: $columns columns, copied ${copied.length} chars with the wrapped line intact; Back closed only the selection")
        } finally {
            onMain {
                activity?.takeUnless { it.isDestroyed }?.finish()
                TerminalSessionManager.close(session.id)
            }
        }
    }

    @Test
    fun transcriptCopyAndShareStayWithinTheBinderLimitAfterALongSession() {
        val paths = TerminalPaths.of(context).ensureLayout()
        val preferences = TerminalPreferences(context)
        val originalTextSize = preferences.textSizeSp
        // The smallest text size gives the most columns, so the 10000-row transcript holds the most characters.
        onMain { preferences.textSizeSp = TerminalPreferences.MIN_TEXT_SIZE_SP }
        val session = onMain { TerminalSessionManager.create(context, paths.home.path) }
        var activity: TerminalActivity? = null
        val captured = AtomicReference<Intent>()
        val monitor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            object : Instrumentation.ActivityMonitor() {
                override fun onStartActivity(intent: Intent): Instrumentation.ActivityResult? {
                    if (intent.action != Intent.ACTION_CHOOSER) return null
                    captured.set(intent)
                    return Instrumentation.ActivityResult(Activity.RESULT_CANCELED, null)
                }
            }
        } else {
            Instrumentation.ActivityMonitor(IntentFilter(Intent.ACTION_CHOOSER), Instrumentation.ActivityResult(Activity.RESULT_CANCELED, null), true)
        }
        var monitorAdded = false
        try {
            activity = startTerminal(session.id)
            val terminal = activity
            awaitAttached(terminal, session)
            // Fill the 10000-row transcript with full rows; the menu actions used to hand all of it to Binder.
            onMain { session.pty.write("yes \"\$(printf '%0230d' 7)\" | head -n 4000; printf '__TAIL_%s__\\n' done\r") }
            await(timeoutMillis = 90_000) { onMain { session.pty.transcriptText.contains("__TAIL_done__") } }
            val transcriptLength = onMain { session.pty.transcriptText.length }

            onMain { terminal.onOptionsItemSelected(menuItem(terminal, R.id.action_copy_transcript)) }
            val copied = readClipboard(terminal)
            Log.i(TAG, "transfer: transcript $transcriptLength chars, clipboard ${copied.length} chars")
            assertTrue("copied text must keep the latest output", copied.takeLast(2_000).contains("__TAIL_done__"))
            assertTrue("copied ${copied.length} of $transcriptLength chars", copied.length <= TextTransfer.MAX_CHARS)

            // Added only now: below API 26 the filter monitor also matches the action-less terminal intent.
            instrumentation.addMonitor(monitor)
            monitorAdded = true
            onMain { terminal.onOptionsItemSelected(menuItem(terminal, R.id.action_share_transcript)) }
            await { monitor.hits > 0 }
            val chooser = captured.get()
            if (chooser != null) {
                @Suppress("DEPRECATION")
                val send = chooser.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)!!
                val shared = send.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
                assertEquals(Intent.ACTION_SEND, send.action)
                assertTrue("shared text must keep the latest output", shared.takeLast(2_000).contains("__TAIL_done__"))
                assertTrue("shared ${shared.length} of $transcriptLength chars", shared.length <= TextTransfer.MAX_CHARS)
            }
            assertTrue(onMain { session.pty.isProcessAlive && !terminal.isFinishing })
            Log.i(TAG, "transfer: transcript $transcriptLength chars, copied ${copied.length}, shared ${captured.get()?.let { "inspected" } ?: "counted (API < 26)"}")
        } finally {
            if (monitorAdded) instrumentation.removeMonitor(monitor)
            onMain {
                activity?.takeUnless { it.isDestroyed }?.finish()
                TerminalSessionManager.close(session.id)
                preferences.textSizeSp = originalTextSize
            }
        }
    }

    @Test
    fun pinchChangesTheTextSizeAndPersistsIt() {
        val paths = TerminalPaths.of(context).ensureLayout()
        val preferences = TerminalPreferences(context)
        val original = preferences.textSizeSp
        val session = onMain { TerminalSessionManager.create(context, paths.home.path) }
        var activity: TerminalActivity? = null
        try {
            onMain { preferences.textSizeSp = TerminalPreferences.DEFAULT_TEXT_SIZE_SP }
            activity = startTerminal(session.id)
            val terminal = activity
            val view = awaitAttached(terminal, session)
            await { onMain { view.textSizeSp == TerminalPreferences.DEFAULT_TEXT_SIZE_SP } }
            pinch(view, outward = true)
            await { onMain { view.textSizeSp > TerminalPreferences.DEFAULT_TEXT_SIZE_SP } }
            val enlarged = onMain { view.textSizeSp }
            assertEquals(enlarged, preferences.textSizeSp)
            pinch(view, outward = false)
            await { onMain { view.textSizeSp < enlarged } }
            assertEquals(onMain { view.textSizeSp }, preferences.textSizeSp)
            assertTrue(session.pty.isProcessAlive)
            Log.i(TAG, "pinch: ${TerminalPreferences.DEFAULT_TEXT_SIZE_SP} -> $enlarged -> ${preferences.textSizeSp} sp, persisted")
        } finally {
            onMain {
                activity?.takeUnless { it.isDestroyed }?.finish()
                TerminalSessionManager.close(session.id)
                preferences.textSizeSp = original
            }
        }
    }

    private fun menuItem(activity: TerminalActivity, id: Int) =
        requireNotNull(activity.findViewById<Toolbar>(R.id.toolbar).menu.findItem(id)) { "menu item $id is missing" }

    private fun awaitAttached(activity: TerminalActivity, session: TerminalSessionManager.Session): TerminalEmulatorView {
        await { onMain { activity.findViewById<TerminalEmulatorView>(R.id.terminal)?.termSession === session.pty } }
        await { onMain { session.pty.transcriptText.contains("$") } }
        return onMain { activity.findViewById(R.id.terminal) }
    }

    /**
     * A long press dispatched to the view itself: the view's own gesture detector times it on the main looper.
     * Injecting through the input system is avoided here because the system clipboard overlay of API 33+ (shown
     * after any copy) takes injected touches for itself.
     */
    private fun longPress(view: View) {
        val (x, y) = onMain { view.width / 2f to view.height / 5f }
        val down = SystemClock.uptimeMillis()
        onMain { dispatch(view, MotionEvent.obtain(down, down, MotionEvent.ACTION_DOWN, x, y, 0)) }
        SystemClock.sleep(ViewConfiguration.getLongPressTimeout() * 2L)
        onMain { dispatch(view, MotionEvent.obtain(down, SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, x, y, 0)) }
    }

    private fun dispatch(view: View, event: MotionEvent) {
        event.source = InputDevice.SOURCE_TOUCHSCREEN
        try {
            view.dispatchTouchEvent(event)
        } finally {
            event.recycle()
        }
    }

    /** Android 10+ lets only the focused app read the clipboard; the clipboard overlay can hold focus briefly. */
    private fun readClipboard(activity: Activity): String {
        await { onMain { activity.hasWindowFocus() } }
        return onMain { Clipboard.get(activity)?.toString().orEmpty() }
    }

    /**
     * A two-finger gesture whose span starts beyond the platform's minimum scaling span, dispatched to the view for
     * the same reason as [longPress].
     */
    private fun pinch(view: View, outward: Boolean) {
        val (centerX, centerY, width) = onMain { Triple(view.width / 2f, view.height / 2f, view.width.toFloat()) }
        val narrow = width * 0.4f
        val wide = width * 0.9f
        val from = if (outward) narrow else wide
        val to = if (outward) wide else narrow
        val downTime = SystemClock.uptimeMillis()
        val properties = Array(2) { index -> MotionEvent.PointerProperties().apply { id = index; toolType = MotionEvent.TOOL_TYPE_FINGER } }
        fun send(action: Int, pointers: Int, span: Float) {
            val coordinates = Array(2) { index ->
                MotionEvent.PointerCoords().apply {
                    x = centerX + (if (index == 0) -span else span) / 2f
                    y = centerY
                    pressure = 1f
                    size = 1f
                }
            }
            val event = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), action, pointers, properties, coordinates, 0, 0, 1f, 1f, 0, 0, InputDevice.SOURCE_TOUCHSCREEN, 0)
            onMain { dispatch(view, event) }
        }
        val second = 1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT
        send(MotionEvent.ACTION_DOWN, 1, from)
        send(MotionEvent.ACTION_POINTER_DOWN or second, 2, from)
        val steps = 24
        for (step in 1..steps) {
            send(MotionEvent.ACTION_MOVE, 2, from + (to - from) * step / steps)
            SystemClock.sleep(12)
        }
        send(MotionEvent.ACTION_POINTER_UP or second, 2, to)
        send(MotionEvent.ACTION_UP, 1, to)
    }

    private fun startTerminal(sessionId: String): TerminalActivity = instrumentation.startActivitySync(
        TerminalActivity.intent(context).putExtra(TerminalContract.EXTRA_SESSION_ID, sessionId).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    ) as TerminalActivity

    private fun await(timeoutMillis: Long = 15_000, failure: () -> String = { "Terminal state did not settle" }, condition: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + timeoutMillis
        while (!condition()) {
            check(SystemClock.uptimeMillis() < deadline) { failure() }
            SystemClock.sleep(50)
        }
    }

    private fun <T> onMain(block: () -> T): T {
        val value = AtomicReference<Result<T>>()
        instrumentation.runOnMainSync { value.set(runCatching(block)) }
        return value.get().getOrThrow()
    }

    private companion object {
        const val TAG = "ThreeShellTerminalParity"
    }

}
