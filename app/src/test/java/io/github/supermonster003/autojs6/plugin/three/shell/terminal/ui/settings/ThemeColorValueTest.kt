package io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Roadmap P5.1: the theme color picker's pure value policy (presets, parsing, formatting, contrast). */
class ThemeColorValueTest {

    @Test
    fun sixteenPresetsStartWithTheAutoJs6Default() {
        assertEquals(16, ThemeColorValue.PRESETS.size)
        assertEquals(16, ThemeColorValue.PRESETS.toSet().size)
        assertEquals(0xFFFFDEAD.toInt(), ThemeColorValue.PRESETS.first())
        ThemeColorValue.PRESETS.forEach { assertEquals(0xFF, it ushr 24) }
    }

    @Test
    fun parsesHexAndRgbNotations() {
        assertEquals(0xFF2196F3.toInt(), ThemeColorValue.parse("#2196F3"))
        assertEquals(0xFF2196F3.toInt(), ThemeColorValue.parse("  2196f3 "))
        assertEquals(0xFF2196F3.toInt(), ThemeColorValue.parse("rgb(33, 150, 243)"))
        assertEquals(0xFF000000.toInt(), ThemeColorValue.parse("RGB(0,0,0)"))
        assertNull(ThemeColorValue.parse("#2196F"))
        assertNull(ThemeColorValue.parse("#2196F3AA"))
        assertNull(ThemeColorValue.parse("rgb(256, 0, 0)"))
        assertNull(ThemeColorValue.parse("rgb(1, 2)"))
        assertNull(ThemeColorValue.parse(""))
        assertNull(ThemeColorValue.parse("blue"))
    }

    @Test
    fun formatsWithoutAlpha() {
        assertEquals("#2196F3", ThemeColorValue.hex(0xFF2196F3.toInt()))
        assertEquals("#2196F3", ThemeColorValue.hex(0x002196F3))
        assertEquals("#000000", ThemeColorValue.hex(0xFF000000.toInt()))
        assertEquals("rgb(33, 150, 243)", ThemeColorValue.rgb(0xFF2196F3.toInt()))
        assertEquals(ThemeColorValue.hex(0xFF2196F3.toInt()), ThemeColorValue.hex(ThemeColorValue.parse(ThemeColorValue.rgb(0xFF2196F3.toInt()))!!))
    }

    @Test
    fun textColorContrastsWithTheSwatch() {
        assertEquals(0xFF000000.toInt(), ThemeColorValue.onColor(0xFFFFDEAD.toInt()))
        assertEquals(0xFF000000.toInt(), ThemeColorValue.onColor(0xFFFFFFFF.toInt()))
        assertEquals(0xFFFFFFFF.toInt(), ThemeColorValue.onColor(0xFF000000.toInt()))
        assertEquals(0xFFFFFFFF.toInt(), ThemeColorValue.onColor(0xFF3F51B5.toInt()))
    }

}
