package io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TextTransferTest {

    @Test
    fun shortTextIsKeptWhole() {
        val bounded = TextTransfer.tail("abc\ndef", maxChars = 7)
        assertEquals("abc\ndef", bounded.text)
        assertFalse(bounded.truncated)
    }

    @Test
    fun longTextKeepsTheTailFromTheNextLineBreak() {
        val text = (1..50).joinToString("\n") { "line $it" }
        val bounded = TextTransfer.tail(text, maxChars = 30)
        assertTrue(bounded.truncated)
        assertTrue(bounded.text.length <= 30)
        assertTrue("starts at a line: ${bounded.text}", bounded.text.startsWith("line "))
        assertTrue(bounded.text.endsWith("line 50"))
    }

    @Test
    fun aTailWithoutNearbyLineBreakIsCutAtTheLimit() {
        val text = "x".repeat(10_000) + "END"
        val bounded = TextTransfer.tail(text, maxChars = 100)
        assertEquals(100, bounded.text.length)
        assertTrue(bounded.text.endsWith("END"))
    }

    @Test
    fun aSurrogatePairIsNeverSplit() {
        val emoji = "😀"
        val text = "a$emoji" + "b".repeat(5)
        // The cut would land on the low surrogate; the result starts after the whole pair instead.
        val bounded = TextTransfer.tail(text, maxChars = 6)
        assertEquals("b".repeat(5), bounded.text)
        assertFalse(Character.isLowSurrogate(bounded.text.first()))
    }

    @Test
    fun theDefaultLimitStaysFarBelowOneMebibyteOfUtf16() {
        assertTrue(TextTransfer.MAX_CHARS * 2 <= 256 * 1024)
    }

}
