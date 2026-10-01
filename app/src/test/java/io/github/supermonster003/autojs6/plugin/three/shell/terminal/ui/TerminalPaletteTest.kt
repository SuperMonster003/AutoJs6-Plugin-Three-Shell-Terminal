package io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.math.abs

/**
 * The runtime palette of the terminal screens (roadmap P3.1, standalone settings specification
 * section 6): fed with the neutrals of `colors.xml` in both night modes, the accent derived from
 * any theme color stays readable (4.5:1) on every surface it is drawn on, the neutral text roles
 * meet the same bar, the HCT accent rule behaves as specified, and the appearance resolution
 * falls back to the system values plus `#FFDEAD` without a host snapshot.
 */
class TerminalPaletteTest {

    private val light: IntArray by lazy { neutrals("app/src/main/res/values/colors.xml") }
    private val dark: IntArray by lazy { neutrals("app/src/main/res/values-night/colors.xml") }

    @Test
    fun accentsFromAnyThemeColorStayReadableOnEverySurface() {
        for ((mode, neutral) in listOf(false to light, true to dark)) {
            for (seed in SEEDS) {
                val palette = TerminalPalette.build(neutral, seed, mode)
                val surfaces = mapOf(
                    "background" to palette.background,
                    "surface" to palette.surface,
                    "surfaceVariant" to palette.surfaceVariant,
                    "terminalBackground" to palette.terminalBackground,
                    "toned background" to ColorPolicy.blend(palette.background, palette.accent, TerminalPalette.TONE_ALPHA / 255.0),
                    "toned surface" to ColorPolicy.blend(palette.surface, palette.accent, TerminalPalette.TONE_ALPHA / 255.0),
                )
                for ((name, surface) in surfaces) {
                    val ratio = ColorPolicy.contrastRatio(palette.accent, surface)
                    assertTrue(
                        "seed ${hex(seed)} dark=$mode accent ${hex(palette.accent)} on $name ${hex(surface)}: $ratio",
                        ratio >= ColorPolicy.MINIMUM_TEXT_CONTRAST,
                    )
                }
                assertEquals(mode, palette.isDark)
                assertEquals(0xFF, palette.accent ushr 24)
                assertTrue(ColorPolicy.contrastRatio(palette.onAccent, palette.accent) >= 3.0)
            }
        }
    }

    @Test
    fun neutralTextRolesMeetTheContrastBarInBothModes() {
        for ((mode, neutral) in listOf(false to light, true to dark)) {
            val palette = TerminalPalette.build(neutral, Appearance.DEFAULT_COLOR, mode)
            for (surface in listOf(palette.background, palette.surface, palette.surfaceVariant)) {
                assertTrue("text dark=$mode", ColorPolicy.contrastRatio(palette.text, surface) >= ColorPolicy.MINIMUM_TEXT_CONTRAST)
                assertTrue("muted dark=$mode", ColorPolicy.contrastRatio(palette.muted, surface) >= ColorPolicy.MINIMUM_TEXT_CONTRAST)
                assertTrue("danger dark=$mode", ColorPolicy.contrastRatio(palette.danger, surface) >= ColorPolicy.MINIMUM_TEXT_CONTRAST)
            }
            assertTrue("terminal dark=$mode", ColorPolicy.contrastRatio(palette.terminalForeground, palette.terminalBackground) >= 7.0)
            assertEquals(mode, ColorPolicy.luminance(palette.background) < 0.5)
        }
    }

    @Test
    fun paletteKeepsTheNeutralsItWasBuiltFrom() {
        val palette = TerminalPalette.build(light, 0xFF1E88E5.toInt(), dark = false)
        assertEquals(0xFFF3F4F5.toInt(), palette.background)
        assertEquals(0xFFFFFFFF.toInt(), palette.surface)
        assertEquals(0xFFEBEDEF.toInt(), palette.surfaceVariant)
        assertEquals(0xFFC5C8CE.toInt(), palette.outline)
        assertEquals(0xFFE0E3E7.toInt(), palette.divider)
        assertEquals(0xFF1D1B20.toInt(), palette.text)
        assertEquals(0xFF5F6368.toInt(), palette.muted)
        assertEquals(0xFFB3261E.toInt(), palette.danger)
        assertEquals(0xFFFAFAFA.toInt(), palette.terminalBackground)
        assertEquals(0xFF1D1B20.toInt(), palette.terminalForeground)
        val night = TerminalPalette.build(dark, 0xFF1E88E5.toInt(), dark = true)
        assertEquals(0xFF121212.toInt(), night.background)
        assertEquals(0xFF1E1E1E.toInt(), night.surface)
        assertEquals(0xFFE6E1E5.toInt(), night.text)
        assertEquals(0xFFFFB4AB.toInt(), night.danger)
        assertEquals(0xFF121212.toInt(), night.terminalBackground)
    }

    @Test
    fun hctRolesFollowTheSharedAccentRule() {
        // Chroma below 4 collapses to a neutral grey: the three channels end up (nearly) equal.
        for (mode in listOf(false, true)) {
            val grey = ThemeAccentRoles.fromSeed(0xFF9E9E9E.toInt(), mode)
            val r = grey.primary shr 16 and 0xFF
            val g = grey.primary shr 8 and 0xFF
            val b = grey.primary and 0xFF
            assertTrue("grey seed must stay grey: ${hex(grey.primary)}", abs(r - g) <= 3 && abs(g - b) <= 3)
            assertTrue(ColorPolicy.contrastRatio(grey.primary, grey.onPrimary) >= ColorPolicy.MINIMUM_TEXT_CONTRAST)
        }
        // Tones 40 / 100 (light) and 80 / 20 (dark): the dark primary is the lighter of the two.
        val lightBlue = ThemeAccentRoles.fromSeed(0xFF1E88E5.toInt(), dark = false)
        val darkBlue = ThemeAccentRoles.fromSeed(0xFF1E88E5.toInt(), dark = true)
        assertEquals(0xFFFFFFFF.toInt(), lightBlue.onPrimary)
        assertTrue(ColorPolicy.luminance(darkBlue.primary) > ColorPolicy.luminance(lightBlue.primary))
        assertTrue(ColorPolicy.luminance(darkBlue.onPrimary) < ColorPolicy.luminance(darkBlue.primary))
        assertTrue(ColorPolicy.contrastRatio(lightBlue.primary, lightBlue.onPrimary) >= ColorPolicy.MINIMUM_TEXT_CONTRAST)
        assertTrue(ColorPolicy.contrastRatio(darkBlue.primary, darkBlue.onPrimary) >= ColorPolicy.MINIMUM_TEXT_CONTRAST)
        // The no-host fallback is a low-chroma peach: it is raised to chroma 48 instead of staying pastel.
        val fallback = ThemeAccentRoles.fromSeed(Appearance.DEFAULT_COLOR, dark = false)
        assertTrue(ColorPolicy.contrastRatio(fallback.primary, 0xFFFFFFFF.toInt()) >= ColorPolicy.MINIMUM_TEXT_CONTRAST)
    }

    @Test
    fun colorPolicyHelpersAreStable() {
        assertEquals(21.0, ColorPolicy.contrastRatio(0xFF000000.toInt(), 0xFFFFFFFF.toInt()), 0.01)
        assertEquals(1.0, ColorPolicy.contrastRatio(0xFF808080.toInt(), 0xFF808080.toInt()), 0.001)
        assertEquals(0xFF000000.toInt(), ColorPolicy.onFilledColor(0xFFFFFFFF.toInt()))
        assertEquals(0xFFFFFFFF.toInt(), ColorPolicy.onFilledColor(0xFF000000.toInt()))
        assertEquals(0x80123456.toInt(), ColorPolicy.withAlpha(0xFF123456.toInt(), 0x80))
        assertEquals(0xFF000000.toInt(), ColorPolicy.blend(0xFF000000.toInt(), 0xFFFFFFFF.toInt(), 0.0))
        assertEquals(0xFFFFFFFF.toInt(), ColorPolicy.blend(0xFF000000.toInt(), 0xFFFFFFFF.toInt(), 1.0))
        val pastel = ColorPolicy.readableAccent(0xFFFFDEAD.toInt(), 0xFFFFFFFF.toInt())
        assertTrue(ColorPolicy.contrastRatio(pastel, 0xFFFFFFFF.toInt()) >= ColorPolicy.MINIMUM_TEXT_CONTRAST)
        assertEquals("an already readable accent is returned unchanged", 0xFF1D1B20.toInt(), ColorPolicy.readableAccent(0xFF1D1B20.toInt(), 0xFFFFFFFF.toInt()))
    }

    @Test
    fun appearanceFallsBackToTheSystemAndTheSharedColorWithoutAHost() {
        val resolved = Appearance.resolve(null, "zh-CN", systemDark = true)
        assertEquals("zh-CN", resolved.language)
        assertTrue(resolved.dark)
        assertEquals(Appearance.DEFAULT_COLOR, resolved.primarySeed)
        assertEquals(Appearance.DEFAULT_COLOR, resolved.accentSeed)
        assertEquals(0xFFFFDEAD.toInt(), Appearance.DEFAULT_COLOR)
    }

    @Test
    fun appearanceTakesEveryValueFromTheHostSnapshotAndForcesOpaqueSeeds() {
        val host = HostAppearance(language = "fr", dark = false, primary = 0x001E88E5, accent = 0x00F44336)
        val resolved = Appearance.resolve(host, "ja", systemDark = true)
        assertEquals("fr", resolved.language)
        assertFalse(resolved.dark)
        assertEquals(0xFF1E88E5.toInt(), resolved.primarySeed)
        assertEquals(0xFFF44336.toInt(), resolved.accentSeed)
    }

    @Test
    fun languageTagsAreValidatedBeforeTheyReachTheLocaleApi() {
        for (tag in listOf("en", "zh-CN", "zh-Hant-TW", "pt-BR", "es-419")) assertTrue(tag, HostAppearance.isLanguageTag(tag))
        for (tag in listOf("", " ", "zh_CN", "en-", "-en", "en--US", "zh CN", "../x")) assertFalse(tag, HostAppearance.isLanguageTag(tag))
    }

    private fun neutrals(relativePath: String): IntArray {
        val path = findProjectRoot().resolve(relativePath)
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(path.toFile())
        val colors = document.getElementsByTagName("color")
        val byName = (0 until colors.length).map { colors.item(it) }.associate { node ->
            node.attributes.getNamedItem("name").nodeValue to node.textContent.trim()
        }
        return NEUTRAL_ORDER.map { name ->
            val value = requireNotNull(byName[name]) { "$relativePath lacks <color name=\"$name\">" }
            require(Regex("#[0-9A-Fa-f]{6}").matches(value)) { "$name must be an opaque #RRGGBB color, was $value" }
            (0xFF000000L or value.substring(1).toLong(16)).toInt()
        }.toIntArray()
    }

    private fun findProjectRoot(): Path = generateSequence(Paths.get("").toAbsolutePath()) { path ->
        path.parent
    }.first { path -> Files.isDirectory(path.resolve("app/src/main")) }

    private fun hex(color: Int) = "#%08X".format(color)

    private companion object {

        /** The order `TerminalPalette.neutralColors` reads from resources. */
        val NEUTRAL_ORDER = listOf(
            "window_background", "surface", "surface_variant", "outline", "divider",
            "text_color_primary", "text_color_secondary", "status_danger",
            "terminal_background", "terminal_foreground",
        )

        val SEEDS = intArrayOf(
            Appearance.DEFAULT_COLOR, // no-host peach
            0xFF1E88E5.toInt(), // AutoJs6 blue
            0xFFF44336.toInt(), // red
            0xFF4CAF50.toInt(), // green
            0xFFFFEB3B.toInt(), // yellow
            0xFF9E9E9E.toInt(), // grey, chroma < 4
            0xFF000000.toInt(), // black
            0xFFFFFFFF.toInt(), // white
            0xFF673AB7.toInt(), // deep purple
            0xFF00BCD4.toInt(), // cyan
        )

    }

}
