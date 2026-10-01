package io.github.supermonster003.autojs6.plugin.three.shell.terminal.binder

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The bounded FIFO behind an output subscription drops the oldest bytes and accounts for them (D21). */
class OutputBufferTest {

    @Test
    fun bytesComeOutInOrderUntilEmpty() {
        val buffer = OutputBuffer(1024)
        assertEquals(0, buffer.append("abc".toByteArray(), 0, 3))
        assertEquals(0, buffer.append("defgh".toByteArray(), 1, 2))
        assertEquals(5, buffer.size)
        assertArrayEquals("abc".toByteArray(), buffer.poll(3))
        assertArrayEquals("ef".toByteArray(), buffer.poll())
        assertNull(buffer.poll())
        assertTrue(buffer.isEmpty)
        assertEquals(0L, buffer.dropped)
    }

    @Test
    fun overflowDropsTheOldestBytesAcrossChunkBoundaries() {
        val buffer = OutputBuffer(8)
        buffer.append("0123".toByteArray(), 0, 4)
        buffer.append("4567".toByteArray(), 0, 4)
        assertEquals("the ninth byte evicts one byte of the oldest chunk", 1, buffer.append("8".toByteArray(), 0, 1))
        assertEquals(8, buffer.size)
        assertArrayEquals("123".toByteArray(), buffer.poll(3))
        assertEquals("a larger chunk evicts whole older chunks and part of the next", 5, buffer.append("abcdefgh".toByteArray(), 0, 8))
        assertEquals(8, buffer.size)
        assertArrayEquals("abcdefgh".toByteArray(), buffer.poll())
        assertNull(buffer.poll())
        assertEquals(6L, buffer.dropped)
    }

    @Test
    fun aChunkLargerThanTheCapacityKeepsOnlyItsTail() {
        val buffer = OutputBuffer(4)
        assertEquals(6, buffer.append("0123456789".toByteArray(), 0, 10))
        assertArrayEquals("67".toByteArray(), buffer.poll(2))
        assertArrayEquals("89".toByteArray(), buffer.poll(2))
        assertTrue(buffer.isEmpty)
    }

    @Test
    fun clearForgetsPendingBytesButNotTheDropCount() {
        val buffer = OutputBuffer(2)
        buffer.append("abc".toByteArray(), 0, 3)
        buffer.clear()
        assertTrue(buffer.isEmpty)
        assertNull(buffer.poll())
        assertEquals(1L, buffer.dropped)
        assertEquals(0, buffer.append("z".toByteArray(), 0, 1))
        assertArrayEquals("z".toByteArray(), buffer.poll())
    }

}
