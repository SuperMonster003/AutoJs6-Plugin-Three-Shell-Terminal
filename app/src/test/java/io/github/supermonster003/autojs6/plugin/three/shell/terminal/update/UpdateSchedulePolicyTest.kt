package io.github.supermonster003.autojs6.plugin.three.shell.terminal.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The 12-hour reuse window of the manual update check (AGENTS.md 12). */
class UpdateSchedulePolicyTest {

    private val now = 1_800_000_000_000L

    @Test
    fun theFirstCheckAndAnExpiredRecordFetch() {
        assertTrue(UpdateSchedulePolicy.fetchDue(null, now))
        assertTrue(UpdateSchedulePolicy.fetchDue(now - UpdateSchedulePolicy.INTERVAL_MS, now))
        assertTrue(UpdateSchedulePolicy.fetchDue(now - UpdateSchedulePolicy.INTERVAL_MS - 1, now))
    }

    @Test
    fun aRecentAnswerIsReusedForTwelveHours() {
        assertFalse(UpdateSchedulePolicy.fetchDue(now, now))
        assertFalse(UpdateSchedulePolicy.fetchDue(now - UpdateSchedulePolicy.INTERVAL_MS + 1, now))
        assertFalse(UpdateSchedulePolicy.fetchDue(now - 11 * 60 * 60 * 1000L, now))
    }

    @Test
    fun aClockThatMovedBackwardsFetchesAgain() {
        assertTrue(UpdateSchedulePolicy.fetchDue(now + 1, now))
    }

}
