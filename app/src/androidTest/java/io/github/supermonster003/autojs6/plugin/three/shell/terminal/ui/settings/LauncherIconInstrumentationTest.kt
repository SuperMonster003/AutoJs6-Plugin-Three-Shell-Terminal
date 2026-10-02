package io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui.settings

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui.LauncherActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Roadmap P5.3 device evidence: the installed package shows exactly one launcher entry, every one of
 * the four aliases can become that entry without killing the process, `MAIN` / `LAUNCHER` resolve to
 * the chosen alias whose target is the private `LauncherActivity`, and the previous alias states are
 * restored afterwards. Results are logged under [TAG].
 */
@RunWith(AndroidJUnit4::class)
class LauncherIconInstrumentationTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val pm = context.packageManager

    @Test
    fun exactlyOneAliasIsEnabledAndEachChoiceBecomesTheOnlyLauncherEntry() {
        val before = LauncherIcons.snapshot(context)
        val previous = LauncherIcons.current(context)
        val process = Process.myPid()
        try {
            LauncherIcons.normalize(context)
            assertTrue("one enabled alias after normalization: ${LauncherIcons.snapshot(context)}", LauncherIconStatePolicy.isNormalized(LauncherIcons.snapshot(context)))
            assertEquals(1, launcherEntries().size)
            for (mode in LauncherIconMode.entries) {
                LauncherIcons.select(context, mode)
                assertEquals(mode, LauncherIcons.current(context))
                assertEquals("DONT_KILL_APP keeps the process", process, Process.myPid())
                val entries = launcherEntries()
                assertEquals("exactly one MAIN / LAUNCHER entry for $mode: $entries", 1, entries.size)
                val entry = entries.single().activityInfo
                assertEquals(mode.component(context).className, entry.name)
                assertEquals(LauncherActivity::class.java.name, entry.targetActivity)
                assertNotNull("the real launcher Activity stays resolvable", pm.resolveActivity(Intent(context, LauncherActivity::class.java), 0))
                val states = LauncherIcons.snapshot(context)
                assertEquals(PackageManager.COMPONENT_ENABLED_STATE_ENABLED, states[mode])
                LauncherIconMode.entries.filter { it != mode }.forEach { assertEquals("$it must be disabled", PackageManager.COMPONENT_ENABLED_STATE_DISABLED, states[it]) }
                // Selecting the current mode again is a no-op.
                LauncherIcons.select(context, mode)
                assertEquals(states, LauncherIcons.snapshot(context))
            }
            Log.i(TAG, "launcher icons: four aliases switched one at a time on API ${Build.VERSION.SDK_INT}; previous=$previous")
        } finally {
            LauncherIcons.select(context, previous)
            before.entries.sortedBy { if (it.key == previous) 0 else 1 }.forEach { (mode, state) ->
                pm.setComponentEnabledSetting(mode.component(context), state, PackageManager.DONT_KILL_APP)
            }
        }
    }

    @Test
    fun theAliasesAreTheOnlyLauncherComponentsAndTargetThePrivateForwarder() {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(context.packageName)
        @Suppress("DEPRECATION")
        val declared = pm.queryIntentActivities(intent, PackageManager.MATCH_DISABLED_COMPONENTS)
        assertEquals(LauncherIconMode.entries.map { it.component(context) }.toSet(), declared.map { ComponentName(it.activityInfo.packageName, it.activityInfo.name) }.toSet())
        declared.forEach { info ->
            assertEquals(LauncherActivity::class.java.name, info.activityInfo.targetActivity)
            assertTrue(info.activityInfo.exported)
        }
        val forwarder = pm.getActivityInfo(ComponentName(context, LauncherActivity::class.java), 0)
        assertTrue("the forwarder itself is not exported", !forwarder.exported)
    }

    @Suppress("DEPRECATION")
    private fun launcherEntries() = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(context.packageName), 0)

    private companion object {
        const val TAG = "ThreeShellTerminalIcons"
    }

}
