package io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui.settings

import android.content.pm.PackageManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The pure alias-state rules behind the launcher icon choice (roadmap P5.3). */
class LauncherIconStatePolicyTest {

    private val defaults = LauncherIconMode.entries.associateWith { PackageManager.COMPONENT_ENABLED_STATE_DEFAULT }

    @Test
    fun theManifestDefaultIsAutoAndCountsAsEnabledOnlyInTheDefaultState() {
        assertEquals(LauncherIconMode.AUTO, LauncherIconStatePolicy.resolve(defaults))
        assertTrue(LauncherIconStatePolicy.isNormalized(defaults))
        assertTrue(LauncherIconStatePolicy.enabled(LauncherIconMode.AUTO, PackageManager.COMPONENT_ENABLED_STATE_DEFAULT))
        LauncherIconMode.entries.filter { it != LauncherIconMode.AUTO }.forEach {
            assertFalse("$it is disabled in the manifest", LauncherIconStatePolicy.enabled(it, PackageManager.COMPONENT_ENABLED_STATE_DEFAULT))
        }
        assertEquals(listOf("AdaptiveLightIconAlias", "AdaptiveDarkIconAlias", "AdaptiveAutoIconAlias", "TransparentIconAlias"), LauncherIconMode.entries.map { it.alias })
        assertEquals(
            "io.github.supermonster003.autojs6.plugin.three.shell.terminal.launcher.TransparentIconAlias",
            LauncherIconMode.TRANSPARENT.className("io.github.supermonster003.autojs6.plugin.three.shell.terminal"),
        )
    }

    @Test
    fun anExplicitChoiceSurvivesTheManifestDefaultAndMixedStates() {
        val dark = LauncherIconStatePolicy.statesFor(LauncherIconMode.DARK)
        assertEquals(LauncherIconMode.DARK, LauncherIconStatePolicy.resolve(dark))
        assertTrue(LauncherIconStatePolicy.isNormalized(dark))
        // An old build enabled Dark explicitly while the new manifest default (Auto) is still DEFAULT: two visible entries.
        val mixed = defaults + (LauncherIconMode.DARK to PackageManager.COMPONENT_ENABLED_STATE_ENABLED)
        assertFalse(LauncherIconStatePolicy.isNormalized(mixed))
        assertEquals("the explicit choice wins over the manifest default", LauncherIconMode.DARK, LauncherIconStatePolicy.resolve(mixed))
        // Two explicit entries (interrupted switch): Auto first, then the option order.
        val twoExplicit = mixed + (LauncherIconMode.TRANSPARENT to PackageManager.COMPONENT_ENABLED_STATE_ENABLED)
        assertEquals(LauncherIconMode.DARK, LauncherIconStatePolicy.resolve(twoExplicit))
        assertEquals(LauncherIconMode.AUTO, LauncherIconStatePolicy.resolve(twoExplicit + (LauncherIconMode.AUTO to PackageManager.COMPONENT_ENABLED_STATE_ENABLED)))
        // Everything disabled (externally edited) falls back to the manifest default.
        val allOff = LauncherIconMode.entries.associateWith { PackageManager.COMPONENT_ENABLED_STATE_DISABLED }
        assertEquals(LauncherIconMode.AUTO, LauncherIconStatePolicy.resolve(allOff))
        assertFalse(LauncherIconStatePolicy.isNormalized(allOff))
    }

    @Test
    fun statesForEnableExactlyTheChosenAlias() {
        LauncherIconMode.entries.forEach { mode ->
            val states = LauncherIconStatePolicy.statesFor(mode)
            assertEquals(4, states.size)
            assertEquals(1, states.count { it.value == PackageManager.COMPONENT_ENABLED_STATE_ENABLED })
            assertEquals(PackageManager.COMPONENT_ENABLED_STATE_ENABLED, states[mode])
            assertEquals(mode, LauncherIconStatePolicy.resolve(states))
        }
    }

}
