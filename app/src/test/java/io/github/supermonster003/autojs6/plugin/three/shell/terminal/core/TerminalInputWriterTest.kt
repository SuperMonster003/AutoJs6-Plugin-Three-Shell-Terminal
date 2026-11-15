package io.github.supermonster003.autojs6.plugin.three.shell.terminal.core

import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class TerminalInputWriterTest {
    @Test
    fun blockedOutputDoesNotBlockSubmissionAndWholeChunksStayOrdered() {
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val finished = CountDownLatch(2)
        val received = ArrayList<String>()
        TerminalInputWriter { bytes ->
            entered.countDown()
            release.await()
            received.add(String(bytes, Charsets.UTF_8))
            finished.countDown()
        }.use { writer ->
            try {
                val first = "中文".toByteArray()
                assertTrue(writer.offer(first))
                assertTrue(entered.await(2, TimeUnit.SECONDS))
                first.fill(0)
                assertTrue(writer.offer("second".toByteArray()))
                assertEquals(2L, finished.count)
            } finally { release.countDown() }
            assertTrue(finished.await(2, TimeUnit.SECONDS))
            assertEquals(listOf("中文", "second"), received)
        }
    }

    @Test
    fun overflowRejectsTheWholeChunkAndClosingRejectsFurtherInput() {
        val release = CountDownLatch(1)
        val writer = TerminalInputWriter { release.await() }
        try {
            assertTrue(writer.offer(ByteArray(TerminalInputWriter.MAX_PENDING_BYTES)))
            assertFalse(writer.offer(byteArrayOf(1)))
        } finally {
            release.countDown()
            writer.close()
        }
        assertFalse(writer.offer(byteArrayOf(2)))
    }
}
