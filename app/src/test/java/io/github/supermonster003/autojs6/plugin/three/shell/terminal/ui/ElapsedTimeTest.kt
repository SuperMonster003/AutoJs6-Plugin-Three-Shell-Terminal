package io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class ElapsedTimeTest {

    @Test
    fun formatsSecondsMinutesAndHours() {
        assertEquals("0s", ElapsedTime.format(0))
        assertEquals("0s", ElapsedTime.format(999))
        assertEquals("9s", ElapsedTime.format(9_500))
        assertEquals("1m 00s", ElapsedTime.format(60_000))
        assertEquals("5m 12s", ElapsedTime.format(5 * 60_000L + 12_000))
        assertEquals("1h 02m 03s", ElapsedTime.format(3_600_000L + 2 * 60_000 + 3_000))
        assertEquals("27h 00m 00s", ElapsedTime.format(27 * 3_600_000L))
    }

    @Test
    fun negativeDurationsClampToZero() {
        assertEquals("0s", ElapsedTime.format(-5_000))
    }

}
