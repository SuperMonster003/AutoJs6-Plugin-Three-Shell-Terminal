package io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui

import android.Manifest
import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.SystemClock
import android.util.Log
import android.widget.FrameLayout
import androidx.appcompat.app.AlertDialog
import androidx.core.content.res.ResourcesCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.R
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.core.TerminalPaths
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.core.TerminalPreferences
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.core.TerminalSessionManager
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui.settings.SettingsActivity
import jackpal.androidterm.emulatorview.TerminalTypeface
import org.autojs.plugin.terminal.api.TerminalContract
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicReference

/**
 * Roadmap P8 terminal colors and font on a device: each color scheme reaches the terminal surface after the
 * screen resumes (the screen recreates and keeps its session), the bundled font replaces the renderer typeface
 * and remeasures the cell grid (JetBrains Mono and the system face share a 0.6 em advance, so the column count
 * usually stays), and the settings page writes both choices through choose-then-confirm dialogs.
 */
@RunWith(AndroidJUnit4::class)
class TerminalAppearanceInstrumentationTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val preferences = TerminalPreferences(context)
    private var scheme = TerminalPreferences.COLOR_SCHEME_FOLLOW
    private var font = TerminalPreferences.FONT_SYSTEM
    private val sessions = ArrayList<TerminalSessionManager.Session>()

    @Before
    fun remember() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            instrumentation.uiAutomation.grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
        }
        scheme = preferences.colorScheme
        font = preferences.font
    }

    @After
    fun restore() {
        finishAll()
        preferences.colorScheme = scheme
        preferences.font = font
        onMain {
            TerminalFonts.apply(context)
            sessions.forEach { TerminalSessionManager.close(it.id) }
        }
    }

    @Test
    fun everyColorSchemeReachesTheTerminalAfterResumeAndKeepsTheSession() {
        preferences.colorScheme = TerminalPreferences.COLOR_SCHEME_FOLLOW
        val session = newSession()
        var terminal = start(session)
        val pid = session.pty.pid
        val seen = LinkedHashMap<String, Int>()
        seen[TerminalPreferences.COLOR_SCHEME_FOLLOW] = onMain { terminal.palette.terminalBackground }
        for (next in listOf(TerminalPreferences.COLOR_SCHEME_DARK, TerminalPreferences.COLOR_SCHEME_LIGHT, TerminalPreferences.COLOR_SCHEME_THEME)) {
            preferences.colorScheme = next
            val previous = terminal
            instrumentation.runOnMainSync {
                instrumentation.callActivityOnPause(previous)
                instrumentation.callActivityOnResume(previous)
            }
            // The resumed screen sees a different scheme and recreates itself.
            terminal = awaitValue { resumedTerminal()?.takeIf { it !== previous && onMain { it.palette.scheme } == next } }
            val current = terminal
            await { onMain { current.findViewById<TerminalEmulatorView>(R.id.terminal)?.termSession === session.pty } }
            onMain {
                val container = current.findViewById<FrameLayout>(R.id.terminal_container)
                assertEquals(next, current.palette.terminalBackground, (container.background as ColorDrawable).color)
                assertTrue(next, androidx.core.graphics.ColorUtils.calculateContrast(current.palette.terminalForeground, current.palette.terminalBackground) >= 7.0)
            }
            seen[next] = onMain { current.palette.terminalBackground }
        }
        assertEquals("the same shell survives every recreation", pid, session.pty.pid)
        assertTrue(session.pty.isProcessAlive)
        // Always-dark and always-light differ from each other; the theme pair differs from both.
        assertTrue(seen.toString(), seen.getValue(TerminalPreferences.COLOR_SCHEME_DARK) != seen.getValue(TerminalPreferences.COLOR_SCHEME_LIGHT))
        Log.i(TAG, "schemes: ${seen.mapValues { "#%08X".format(it.value) }}")
    }

    @Test
    fun theBundledFontReplacesTheRendererTypefaceAndKeepsTheSession() {
        preferences.font = TerminalPreferences.FONT_SYSTEM
        val session = newSession()
        val terminal = start(session)
        val view = onMain { terminal.findViewById<TerminalEmulatorView>(R.id.terminal) }
        assertSame(Typeface.MONOSPACE, onMain { view.rendererTypeface })
        val systemColumns = onMain { view.visibleColumns }
        preferences.font = TerminalPreferences.FONT_JETBRAINS_MONO
        instrumentation.runOnMainSync {
            instrumentation.callActivityOnPause(terminal)
            instrumentation.callActivityOnResume(terminal)
        }
        val bundled = ResourcesCompat.getFont(context, R.font.jetbrains_mono_nl_regular)
        await { onMain { terminal.findViewById<TerminalEmulatorView>(R.id.terminal).rendererTypeface === bundled } }
        val current = onMain { terminal.findViewById<TerminalEmulatorView>(R.id.terminal) }
        assertSame(bundled, TerminalTypeface.get())
        assertNotSame(Typeface.MONOSPACE, bundled)
        val bundledColumns = onMain { current.visibleColumns }
        onMain { session.pty.write("printf '__%s__\\n' font-ok\r") }
        await { onMain { session.pty.transcriptText.contains("__font-ok__") } }
        assertTrue(session.pty.isProcessAlive)
        Log.i(TAG, "font: system $systemColumns columns, JetBrains Mono $bundledColumns columns")
    }

    @Test
    fun theSettingsPageWritesTheColorSchemeAndTheFont() {
        preferences.colorScheme = TerminalPreferences.COLOR_SCHEME_FOLLOW
        preferences.font = TerminalPreferences.FONT_SYSTEM
        val settings = instrumentation.startActivitySync(SettingsActivity.intent(context).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as SettingsActivity
        choose(settings, SettingsActivity.KEY_COLOR_SCHEME, TerminalPreferences.COLOR_SCHEMES.indexOf(TerminalPreferences.COLOR_SCHEME_THEME))
        await { preferences.colorScheme == TerminalPreferences.COLOR_SCHEME_THEME }
        assertEquals(context.getString(R.string.settings_color_scheme_theme), onMain { settings.rows.getValue(SettingsActivity.KEY_COLOR_SCHEME).summary.text.toString() })
        choose(settings, SettingsActivity.KEY_FONT, TerminalPreferences.FONTS.indexOf(TerminalPreferences.FONT_JETBRAINS_MONO))
        await { preferences.font == TerminalPreferences.FONT_JETBRAINS_MONO }
        assertEquals(context.getString(R.string.settings_terminal_font_jetbrains_mono), onMain { settings.rows.getValue(SettingsActivity.KEY_FONT).summary.text.toString() })
        Log.i(TAG, "settings: scheme ${preferences.colorScheme}, font ${preferences.font}")
    }

    private fun choose(settings: SettingsActivity, key: String, index: Int) {
        onMain { settings.rows.getValue(key).view.performClick() }
        val dialog = awaitValue { onMain { settings.prompt?.takeIf { it.isShowing && it.listView != null && it.listView.childCount > index } } }
        onMain {
            dialog.listView.performItemClick(dialog.listView.getChildAt(index), index, index.toLong())
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
        }
        await { onMain { !dialog.isShowing } }
    }

    private fun newSession(): TerminalSessionManager.Session {
        val home = TerminalPaths.of(context).ensureLayout().home.path
        return onMain { TerminalSessionManager.create(context, home) }.also { sessions += it }
    }

    private fun start(session: TerminalSessionManager.Session): TerminalActivity {
        val terminal = instrumentation.startActivitySync(
            TerminalActivity.intent(context).putExtra(TerminalContract.EXTRA_SESSION_ID, session.id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        ) as TerminalActivity
        await { onMain { terminal.findViewById<TerminalEmulatorView>(R.id.terminal)?.termSession === session.pty } }
        await { onMain { session.pty.transcriptText.contains("$") } }
        return terminal
    }

    private fun resumedTerminal(): TerminalActivity? = onMain {
        ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).filterIsInstance<TerminalActivity>().singleOrNull()
    }

    private fun finishAll() {
        onMain {
            for (stage in listOf(Stage.RESUMED, Stage.PAUSED, Stage.STOPPED, Stage.CREATED, Stage.STARTED)) {
                ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(stage).forEach { if (!it.isFinishing) it.finish() }
            }
        }
        SystemClock.sleep(300)
    }

    private fun await(condition: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + 15_000
        while (!condition()) {
            check(SystemClock.uptimeMillis() < deadline) { "Appearance state did not settle" }
            SystemClock.sleep(50)
        }
    }

    private fun <T : Any> awaitValue(supplier: () -> T?): T {
        val deadline = SystemClock.uptimeMillis() + 15_000
        while (true) {
            supplier()?.let { return it }
            check(SystemClock.uptimeMillis() < deadline) { "Appearance state did not settle" }
            SystemClock.sleep(50)
        }
    }

    private fun <T> onMain(block: () -> T): T {
        val value = AtomicReference<Result<T>>()
        instrumentation.runOnMainSync { value.set(runCatching(block)) }
        return value.get().getOrThrow()
    }

    private companion object {
        const val TAG = "ThreeShellTerminalLook"
    }

}
