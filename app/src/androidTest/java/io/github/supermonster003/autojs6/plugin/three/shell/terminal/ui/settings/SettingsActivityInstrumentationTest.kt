package io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui.settings

import android.app.Activity
import android.content.Intent
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.core.view.children
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.R
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.core.TerminalPreferences
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.core.TerminalSettingsActions
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.node.NodeCliState
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui.AppearancePreferences
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui.TerminalActivity
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui.TerminalEmulatorView
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui.TerminalSettingsDialogs
import org.autojs.plugin.terminal.api.TerminalActions
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicReference

/**
 * Roadmap P5.1 device evidence for the standalone settings page: every row of the three groups is
 * present, choice dialogs follow choose-then-confirm (Cancel saves nothing), a confirmed text size
 * reaches the terminal, the Node.js switch drives the contract state, appearance choices recreate
 * the screen, the launcher icon choice applies, and the host action opens the page while a shell
 * caller without the plugin permission is refused. Results are logged under [TAG].
 */
@RunWith(AndroidJUnit4::class)
class SettingsActivityInstrumentationTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val preferences = TerminalPreferences(context)

    private var textSize = 0
    private var nodeIntegration = true
    private lateinit var appearance: AppearancePreferences
    private lateinit var launcherIcon: LauncherIconMode

    @Before
    fun remember() {
        textSize = preferences.textSizeSp
        nodeIntegration = preferences.nodeIntegrationEnabled
        appearance = AppearancePreferences.read(context)
        launcherIcon = LauncherIcons.current(context)
    }

    @After
    fun restore() {
        finishAll()
        preferences.textSizeSp = textSize
        TerminalSettingsActions(context).setNodeIntegration(nodeIntegration)
        appearance.write(context)
        if (LauncherIcons.current(context) != launcherIcon) LauncherIcons.select(context, launcherIcon)
    }

    @Test
    fun everyRowIsPresentAndCancelledDialogsSaveNothing() {
        val settings = launchSettings()
        val expected = listOf(
            SettingsActivity.KEY_LANGUAGE, SettingsActivity.KEY_DARK_MODE, SettingsActivity.KEY_COLOR, SettingsActivity.KEY_LAUNCHER_ICON,
            SettingsActivity.KEY_TEXT_SIZE, SettingsActivity.KEY_REGISTRY, SettingsActivity.KEY_IGNORE_SCRIPTS, SettingsActivity.KEY_NODE_INTEGRATION,
            SettingsActivity.KEY_NODE_PROBE, SettingsActivity.KEY_STORAGE, SettingsActivity.KEY_CLEAR_DATA,
            SettingsActivity.KEY_UPDATE, SettingsActivity.KEY_HISTORY, SettingsActivity.KEY_ABOUT,
        )
        onMain {
            assertEquals(expected, settings.rows.keys.toList())
            settings.rows.forEach { (key, row) ->
                assertEquals(key, View.VISIBLE, row.view.visibility)
                assertTrue(key, row.title.text.isNotBlank())
                assertEquals(key, key, row.view.tag)
            }
            assertEquals(context.getString(R.string.terminal_settings), settings.scaffold.toolbar.title.toString())
            assertNotNull(settings.scaffold.content.findViewWithTag<View>(SettingsActivity.TAG_COLOR_DOT))
            assertEquals(context.getString(R.string.settings_clear_data), settings.rows.getValue(SettingsActivity.KEY_CLEAR_DATA).title.text.toString())
        }
        Log.i(TAG, "rows: ${onMain { settings.rows.map { (k, r) -> "$k=${r.summary.text}" } }}")
        val before = AppearancePreferences.read(context)

        // Language: choose another entry, then Cancel.
        onMain { settings.rows.getValue(SettingsActivity.KEY_LANGUAGE).view.performClick() }
        var dialog = awaitPrompt(settings)
        onMain {
            assertTrue(settings.hasUnconfirmedDialogForTest())
            dialog.listView.performItemClick(dialog.listView.getChildAt(2), 2, 2L)
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick()
        }
        await { onMain { !settings.hasUnconfirmedDialogForTest() } }
        assertEquals(before, AppearancePreferences.read(context))

        // Theme color: an invalid value disables OK, a valid one re-enables it, Cancel keeps the stored choice.
        onMain { settings.rows.getValue(SettingsActivity.KEY_COLOR).view.performClick() }
        dialog = awaitPrompt(settings)
        val input = awaitValue { onMain { dialog.window?.decorView?.findViewWithTag<EditText>(ThemeColorChooser.TAG_INPUT) } }
        onMain { input.setText("not a color") }
        await { onMain { dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled == false } }
        onMain { input.setText("rgb(33, 150, 243)") }
        await { onMain { dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled == true } }
        onMain { dialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick() }
        await { onMain { !settings.hasUnconfirmedDialogForTest() } }
        assertEquals(before, AppearancePreferences.read(context))

        // Clear data: the destructive confirmation can be dismissed.
        onMain { settings.rows.getValue(SettingsActivity.KEY_CLEAR_DATA).view.performClick() }
        dialog = awaitPrompt(settings)
        onMain {
            assertEquals(context.getString(R.string.settings_clear_action), dialog.getButton(AlertDialog.BUTTON_POSITIVE).text.toString())
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick()
        }
        await { onMain { !settings.hasUnconfirmedDialogForTest() } }
        assertFalse(onMain { settings.isFinishing })
        Log.i(TAG, "all 14 rows present; language, theme color and clear-data dialogs cancelled without saving")
    }

    @Test
    fun aConfirmedTextSizeReachesTheTerminal() {
        val settings = launchSettings()
        val sizes = TerminalSettingsDialogs.TEXT_SIZES_SP
        val current = preferences.textSizeSp
        val index = (sizes.indexOf(current).coerceAtLeast(0) + 1) % sizes.size
        val chosen = sizes[index]
        onMain { settings.rows.getValue(SettingsActivity.KEY_TEXT_SIZE).view.performClick() }
        val dialog = awaitPrompt(settings)
        onMain {
            dialog.listView.performItemClick(dialog.listView.getChildAt(index), index, index.toLong())
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
        }
        await { onMain { !settings.hasUnconfirmedDialogForTest() } }
        assertEquals(chosen, preferences.textSizeSp)
        assertEquals("$chosen sp", onMain { settings.rows.getValue(SettingsActivity.KEY_TEXT_SIZE).summary.text.toString() })
        val terminal = launch(Intent(context, TerminalActivity::class.java)) as TerminalActivity
        val applied = awaitValue {
            onMain {
                terminal.findViewById<ViewGroup>(android.R.id.content).descendants().filterIsInstance<TerminalEmulatorView>().firstOrNull()?.textSizeSp?.takeIf { it == chosen }
            }
        }
        Log.i(TAG, "text size $current -> $chosen sp, terminal view reports $applied sp")
        onMain { terminal.finish() }
        await { onMain { resumed<TerminalActivity>() == null } }
    }

    @Test
    fun theNodeIntegrationSwitchDrivesTheContractState() {
        val settings = launchSettings()
        val row = onMain { settings.rows.getValue(SettingsActivity.KEY_NODE_INTEGRATION) }
        val switch = requireNotNull(row.switch)
        val initiallyEnabled = preferences.nodeIntegrationEnabled
        assertEquals(initiallyEnabled, onMain { switch.isChecked })
        // The row owns the tap (the switch itself is not clickable), so one tap toggles exactly once.
        onMain { row.view.performClick() }
        await { preferences.nodeIntegrationEnabled == !initiallyEnabled }
        assertEquals(!initiallyEnabled, onMain { switch.isChecked })
        val flipped = NodeCliState.contractState(context)
        onMain { row.view.performClick() }
        await { preferences.nodeIntegrationEnabled == initiallyEnabled }
        assertEquals(initiallyEnabled, onMain { switch.isChecked })
        val restored = NodeCliState.contractState(context)
        Log.i(TAG, "node integration ${initiallyEnabled} -> ${!initiallyEnabled}: state '$flipped'; back: state '$restored'")
        if (initiallyEnabled) {
            assertEquals("disabled", flipped)
            assertNotEquals("disabled", restored)
        } else {
            assertEquals("disabled", restored)
            assertNotEquals("disabled", flipped)
        }
    }

    @Test
    fun appearanceChoicesRecreateTheScreen() {
        AppearancePreferences().write(context)
        var settings = launchSettings()
        val wasDark = onMain { requireNotNull(settings.appearance).dark }
        val targetMode = if (wasDark) AppearancePreferences.LIGHT else AppearancePreferences.DARK
        val index = AppearancePreferences.MODES.indexOf(targetMode)
        onMain { settings.rows.getValue(SettingsActivity.KEY_DARK_MODE).view.performClick() }
        var dialog = awaitPrompt(settings)
        onMain {
            dialog.listView.performItemClick(dialog.listView.getChildAt(index), index, index.toLong())
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
        }
        val first = settings
        settings = awaitValue { onMain { resumed<SettingsActivity>()?.takeIf { it !== first && it.appearance != null } } }
        assertEquals(targetMode, AppearancePreferences.read(context).darkMode)
        assertEquals(!wasDark, onMain { requireNotNull(settings.appearance).dark })
        assertEquals(context.getString(if (wasDark) R.string.app_settings_always_light else R.string.app_settings_always_dark), onMain { settings.rows.getValue(SettingsActivity.KEY_DARK_MODE).summary.text.toString() })

        // Theme color: a preset confirmed from the picker becomes the primary seed.
        val preset = 0xFF2196F3.toInt()
        onMain { settings.rows.getValue(SettingsActivity.KEY_COLOR).view.performClick() }
        dialog = awaitPrompt(settings)
        val swatch = awaitValue { onMain { dialog.window?.decorView?.findViewWithTag<View>(ThemeColorChooser.TAG_PRESET_PREFIX + ThemeColorValue.hex(preset)) } }
        onMain {
            swatch.performClick()
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
        }
        val second = settings
        settings = awaitValue { onMain { resumed<SettingsActivity>()?.takeIf { it !== second && it.appearance != null } } }
        assertEquals(preset, AppearancePreferences.read(context).color)
        assertEquals(preset, onMain { requireNotNull(settings.appearance).primarySeed })
        assertEquals(ThemeColorValue.hex(preset), onMain { settings.rows.getValue(SettingsActivity.KEY_COLOR).summary.text.toString() })
        Log.i(TAG, "dark ${wasDark} -> ${!wasDark} and color ${ThemeColorValue.hex(preset)} applied through recreate")
    }

    @Test
    fun theLauncherIconChoiceApplies() {
        val settings = launchSettings()
        val original = LauncherIcons.current(context)
        val target = if (original == LauncherIconMode.TRANSPARENT) LauncherIconMode.AUTO else LauncherIconMode.TRANSPARENT
        onMain { settings.rows.getValue(SettingsActivity.KEY_LAUNCHER_ICON).view.performClick() }
        val dialog = awaitPrompt(settings)
        onMain {
            dialog.listView.performItemClick(dialog.listView.getChildAt(target.ordinal), target.ordinal, target.ordinal.toLong())
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
        }
        await { LauncherIcons.current(context) == target }
        val label = context.getString(SettingsActivity.LAUNCHER_ICON_LABELS[target.ordinal])
        await { onMain { settings.rows.getValue(SettingsActivity.KEY_LAUNCHER_ICON).summary.text.toString() == label } }
        Log.i(TAG, "launcher icon $original -> $target via the settings page")
        LauncherIcons.select(context, original)
        assertEquals(original, LauncherIcons.current(context))
    }

    @Test
    fun theHostActionOpensThePageAndAnUnprivilegedShellIsRefused() {
        val settings = launch(Intent(TerminalActions.OPEN_SETTINGS).setPackage(context.packageName)) as SettingsActivity
        assertEquals(context.getString(R.string.terminal_settings), onMain { settings.scaffold.toolbar.title.toString() })
        onMain { settings.finish() }
        await { onMain { resumed<SettingsActivity>() == null } }
        val output = shell("am start -a ${TerminalActions.OPEN_SETTINGS} -n ${context.packageName}/.ui.settings.SettingsActivity")
        Log.i(TAG, "shell start output: ${output.trim().replace('\n', ' ')}")
        SystemClock.sleep(2_000)
        assertNull("the shell must not reach the settings page without the plugin permission", onMain { resumed<SettingsActivity>() })
        assertFalse(output.contains("Status: ok"))
    }

    private fun launchSettings(): SettingsActivity = launch(SettingsActivity.intent(context)) as SettingsActivity

    private fun launch(intent: Intent): Activity = instrumentation.startActivitySync(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))

    private fun awaitPrompt(settings: SettingsActivity): AlertDialog =
        awaitValue { onMain { settings.prompt?.takeIf { it.isShowing && (it.listView == null || it.listView.childCount > 0 || it.listView.adapter == null) } } }

    private fun shell(command: String): String {
        val descriptor = instrumentation.uiAutomation.executeShellCommand(command)
        return ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { it.readBytes().toString(Charsets.UTF_8) }
    }

    private fun View.descendants(): Sequence<View> = sequence {
        yield(this@descendants)
        if (this@descendants is ViewGroup) children.forEach { yieldAll(it.descendants()) }
    }

    private inline fun <reified T : Activity> resumed(): T? = ActivityLifecycleMonitorRegistry.getInstance()
        .getActivitiesInStage(Stage.RESUMED).filterIsInstance<T>().singleOrNull()

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
            check(SystemClock.uptimeMillis() < deadline) { "Settings state did not settle" }
            SystemClock.sleep(50)
        }
    }

    private fun <T : Any> awaitValue(supplier: () -> T?): T {
        val deadline = SystemClock.uptimeMillis() + 15_000
        while (true) {
            supplier()?.let { return it }
            check(SystemClock.uptimeMillis() < deadline) { "Settings state did not settle" }
            SystemClock.sleep(50)
        }
    }

    private fun <T> onMain(block: () -> T): T {
        val value = AtomicReference<Result<T>>()
        instrumentation.runOnMainSync { value.set(runCatching(block)) }
        return value.get().getOrThrow()
    }

    private companion object {
        const val TAG = "ThreeShellTerminalSettings"
    }

}
