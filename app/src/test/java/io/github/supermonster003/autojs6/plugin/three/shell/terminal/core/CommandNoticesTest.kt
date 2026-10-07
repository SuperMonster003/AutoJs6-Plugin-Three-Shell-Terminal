package io.github.supermonster003.autojs6.plugin.three.shell.terminal.core

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Roadmap P8 command completion notices: when a notice is due, its id range and the bounded result store. */
class CommandNoticesTest {

    @After
    fun clear() = FinishedCommands.clear()

    @Test
    fun aNoticeIsDueOnlyForAFinishedCommandTheUserIsNotWatching() {
        assertTrue(CommandNoticePolicy.shouldNotify(keepOpen = false, closeRequested = false, enabled = true, permissionGranted = true, visible = false))
        assertFalse("an interactive shell has no command result", CommandNoticePolicy.shouldNotify(true, false, true, true, false))
        assertFalse("closing on request is not a finished command", CommandNoticePolicy.shouldNotify(false, true, true, true, false))
        assertFalse("the settings switch is off", CommandNoticePolicy.shouldNotify(false, false, false, true, false))
        assertFalse("no notification permission", CommandNoticePolicy.shouldNotify(false, false, true, false, false))
        assertFalse("the user is looking at the session", CommandNoticePolicy.shouldNotify(false, false, true, true, true))
    }

    @Test
    fun noticeIdsStayInTheirOwnRange() {
        val running = 0x5445_524D
        for (id in listOf("1", "2", "65535", "65536", "99999999", "pending-x", "")) {
            val notificationId = CommandNoticePolicy.notificationId(id)
            assertTrue(id, notificationId in 0x5443_0000..0x5443_FFFF)
            assertNotEquals(running, notificationId)
        }
        assertNotEquals(CommandNoticePolicy.notificationId("1"), CommandNoticePolicy.notificationId("2"))
        assertTrue(CommandNoticePolicy.SUMMARY_ID !in 0x5443_0000..0x5443_FFFF)
        assertNotEquals(running, CommandNoticePolicy.SUMMARY_ID)
    }

    @Test
    fun theTerminalsOwnCompletionLineIsDroppedFromTheKeptOutput() {
        val raw = "\n[Process completed (code 2)]\nbuild step 1\nbuild step 2\ndone\n\n"
        assertEquals("build step 1\nbuild step 2\ndone", CommandNoticePolicy.withoutCompletionMarker(raw))
        assertEquals("a\nb", CommandNoticePolicy.withoutCompletionMarker("a\n[Process completed (code -1)]  \nb"))
        assertEquals("[Process completed (code x)]", CommandNoticePolicy.withoutCompletionMarker("[Process completed (code x)]"))
        assertNull(CommandNoticePolicy.withoutCompletionMarker("\n[Process completed (code 0)]\n"))
        assertNull(CommandNoticePolicy.withoutCompletionMarker(null))
    }

    @Test
    fun theResultStoreKeepsTheNewestResultsOnly() {
        (1..FinishedCommands.MAX_RESULTS + 3).forEach { index ->
            FinishedCommands.put(FinishedCommands.Result(index.toString(), "/dir/$index", index, index.toLong(), "out $index"))
        }
        assertEquals((4..FinishedCommands.MAX_RESULTS + 3).map(Int::toString), FinishedCommands.ids())
        assertNull(FinishedCommands.get("1"))
        assertEquals("out 11", FinishedCommands.get("11")?.transcript)
        // Storing the same session again makes it the newest without growing the store.
        FinishedCommands.put(FinishedCommands.Result("4", "/dir/4", 0, 99, null))
        assertEquals("4", FinishedCommands.ids().last())
        assertEquals(FinishedCommands.MAX_RESULTS, FinishedCommands.ids().size)
        assertNull(FinishedCommands.get(null))
    }

}
