package io.github.supermonster003.autojs6.plugin.three.shell.terminal.core

import java.util.concurrent.Executors

/** Ordered, bounded writes: waiting for the PTY must never stop the UI from draining its echo. */
internal class TerminalInputWriter(private val write: (ByteArray) -> Unit) : AutoCloseable {
    private val executor = Executors.newSingleThreadExecutor { task -> Thread(task, "TerminalInput") }
    private var pendingBytes = 0
    private var closed = false

    @Synchronized
    fun offer(data: ByteArray, offset: Int = 0, count: Int = data.size): Boolean {
        require(offset >= 0 && count >= 0 && offset <= data.size - count)
        if (closed || count > MAX_PENDING_BYTES - pendingBytes) return false
        if (count == 0) return true
        val copy = data.copyOfRange(offset, offset + count)
        pendingBytes += count
        executor.execute {
            try {
                // Older Android tty drivers can acknowledge a bulk write while dropping bytes as
                // mksh switches between its raw editor and canonical heredoc input. Bound each burst
                // and let the reader consume it; the UI remains free to drain the resulting echo.
                var position = 0
                while (position < copy.size && !Thread.currentThread().isInterrupted) {
                    val end = minOf(position + BURST_BYTES, copy.size)
                    write(if (position == 0 && end == copy.size) copy else copy.copyOfRange(position, end))
                    position = end
                    if (position < copy.size) Thread.sleep(BURST_DELAY_MS)
                }
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
            } finally {
                synchronized(this) { pendingBytes -= count }
            }
        }
        return true
    }

    @Synchronized
    override fun close() {
        closed = true
        executor.shutdownNow()
    }

    companion object {
        const val MAX_PENDING_BYTES = 256 * 1024
        private const val BURST_BYTES = 128
        private const val BURST_DELAY_MS = 4L
    }
}
