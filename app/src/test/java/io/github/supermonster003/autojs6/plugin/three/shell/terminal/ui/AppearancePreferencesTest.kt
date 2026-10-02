package io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Roadmap P5.1: the plugin's appearance choices layer over the host snapshot (explicit choice >
 * follow AutoJs6 > follow system), stored values are validated, and the picker orders are frozen.
 */
class AppearancePreferencesTest {

    private val host = HostAppearance(language = "ja", dark = true, primary = 0xFF2196F3.toInt(), accent = 0xFF03A9F4.toInt())

    @Test
    fun defaultsFollowTheHostAndFallBackToTheSystem() {
        val defaults = AppearancePreferences()
        assertTrue(defaults.followsHost)
        assertEquals(Appearance("ja", true, 0xFF2196F3.toInt(), 0xFF03A9F4.toInt()), defaults.resolve(host, "en-US", systemDark = false))
        assertEquals(Appearance("en-US", false, Appearance.DEFAULT_COLOR, Appearance.DEFAULT_COLOR), defaults.resolve(null, "en-US", systemDark = false))
    }

    @Test
    fun explicitChoicesWinOverTheHost() {
        val chosen = AppearancePreferences(language = "zh-Hant-TW", darkMode = AppearancePreferences.LIGHT, color = 0xFF009688.toInt())
        assertFalse(chosen.followsHost)
        assertEquals(Appearance("zh-Hant-TW", false, 0xFF009688.toInt(), 0xFF009688.toInt()), chosen.resolve(host, "en-US", systemDark = true))
        assertEquals(true, AppearancePreferences(darkMode = AppearancePreferences.DARK).resolve(HostAppearance("en", false, 1, 2), "en", systemDark = false).dark)
    }

    @Test
    fun followSystemIgnoresTheHost() {
        val system = AppearancePreferences(language = AppearancePreferences.SYSTEM, darkMode = AppearancePreferences.SYSTEM)
        val resolved = system.resolve(host, "fr", systemDark = false)
        assertEquals("fr", resolved.language)
        assertFalse(resolved.dark)
        // The color still follows the host when no explicit color is chosen.
        assertEquals(0xFF2196F3.toInt(), resolved.primarySeed)
    }

    @Test
    fun storedValuesAreValidatedAndColorsMadeOpaque() {
        assertEquals(AppearancePreferences(), AppearancePreferences.of("klingon", "sepia", null))
        assertEquals(AppearancePreferences(), AppearancePreferences.of(null, null, null))
        assertEquals(AppearancePreferences(language = "ar", darkMode = "dark", color = 0xFF112233.toInt()), AppearancePreferences.of("ar", "dark", 0x00112233))
        assertEquals(0xFF000000.toInt(), AppearancePreferences(color = 0).resolve(host, "en", systemDark = false).primarySeed)
    }

    @Test
    fun pickerOrdersAreFrozen() {
        assertEquals(listOf("host", "system", "zh-Hans", "zh-Hant-HK", "zh-Hant-TW", "en", "fr", "es", "ja", "ko", "ru", "ar"), AppearancePreferences.LANGUAGES)
        assertEquals(listOf("host", "system", "light", "dark"), AppearancePreferences.MODES)
        assertEquals("host", AppearancePreferences.HOST)
    }

}
