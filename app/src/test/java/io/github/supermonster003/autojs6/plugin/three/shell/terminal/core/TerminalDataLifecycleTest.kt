package io.github.supermonster003.autojs6.plugin.three.shell.terminal.core

import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CancellationException

class TerminalDataLifecycleTest {
    @Test
    fun clearingRefusesNewPlansAndInvalidatesAlreadyPreparedPlans() {
        val previous = TerminalDataLifecycle.ticket()
        assertTrue(TerminalDataLifecycle.beginClear())
        try {
            assertFalse(TerminalDataLifecycle.beginClear())
            assertThrows(CancellationException::class.java) { TerminalDataLifecycle.ticket() }
            assertThrows(CancellationException::class.java) { TerminalDataLifecycle.checkTicket(previous) }
        } finally {
            TerminalDataLifecycle.endClear()
        }
        assertThrows(CancellationException::class.java) { TerminalDataLifecycle.checkTicket(previous) }
        TerminalDataLifecycle.checkTicket(TerminalDataLifecycle.ticket())
    }
}
