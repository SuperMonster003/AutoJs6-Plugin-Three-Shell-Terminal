package io.github.supermonster003.autojs6.plugin.three.shell.terminal.core

import java.util.concurrent.CancellationException

/**
 * Clear-data invalidates old session plans before touching disk. The I/O lock is only held by
 * workers; main-thread session creation checks a generation and never waits for extraction.
 */
internal object TerminalDataLifecycle {
    val ioLock = Any()
    private var generation = 0L
    private var clearing = false

    @Synchronized
    fun ticket(): Long {
        if (clearing) throw CancellationException("Terminal data is being cleared")
        return generation
    }

    @Synchronized
    fun checkTicket(ticket: Long) {
        if (clearing || ticket != generation) throw CancellationException("Session plan was cancelled by clear-data")
    }

    /** Main thread, serialized with creation of live sessions. */
    @Synchronized
    fun beginClear(): Boolean {
        if (clearing) return false
        generation++
        clearing = true
        return true
    }

    @Synchronized
    fun endClear() {
        clearing = false
    }
}
