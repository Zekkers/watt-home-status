package com.zekkers.watthome

import com.zekkers.watthome.data.HistoryFetchPolicy
import com.zekkers.watthome.data.HistoryFetchPolicy.Poll
import com.zekkers.watthome.data.HistoryFetchPolicy.Trigger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryFetchPolicyTest {
    private val openedAt = 1_700_000_000_000L

    @Test
    fun appOpenFetchesHistoryThenDebouncesForFiveMinutes() {
        assertTrue(
            "first app open fetches history",
            HistoryFetchPolicy.shouldFetchHistory(
                Trigger.APP_OPEN,
                lastHistorySuccessAtMs = null,
                nowMs = openedAt
            )
        )
        assertEquals(
            Poll.LATEST_AND_HISTORY,
            HistoryFetchPolicy.plan(Trigger.APP_OPEN, lastHistorySuccessAtMs = null, nowMs = openedAt)
        )

        val fourMinutesLater = openedAt + 4 * 60 * 1000L
        assertFalse(
            "a second open 4 minutes later does not",
            HistoryFetchPolicy.shouldFetchHistory(
                Trigger.APP_OPEN,
                lastHistorySuccessAtMs = openedAt,
                nowMs = fourMinutesLater
            )
        )
        assertEquals(
            Poll.LATEST_ONLY,
            HistoryFetchPolicy.plan(
                trigger = Trigger.APP_OPEN,
                lastHistorySuccessAtMs = openedAt,
                nowMs = fourMinutesLater,
                lastAnySuccessAtMs = openedAt
            )
        )

        val fiveMinutesLater = openedAt + HistoryFetchPolicy.FOREGROUND_HISTORY_MIN_MS
        assertTrue(
            "a later open after 5 minutes does",
            HistoryFetchPolicy.shouldFetchHistory(
                Trigger.APP_OPEN,
                lastHistorySuccessAtMs = openedAt,
                nowMs = fiveMinutesLater
            )
        )
        assertEquals(
            Poll.LATEST_AND_HISTORY,
            HistoryFetchPolicy.plan(
                trigger = Trigger.APP_OPEN,
                lastHistorySuccessAtMs = openedAt,
                nowMs = fiveMinutesLater,
                lastAnySuccessAtMs = openedAt
            )
        )
        assertTrue(
            HistoryFetchPolicy.shouldFetchHistory(
                Trigger.APP_OPEN,
                lastHistorySuccessAtMs = openedAt,
                nowMs = fiveMinutesLater + 60_000L
            )
        )
        assertFalse(
            HistoryFetchPolicy.shouldFetchHistory(
                Trigger.APP_OPEN,
                lastHistorySuccessAtMs = openedAt,
                nowMs = fiveMinutesLater - 1L
            )
        )
    }

    @Test
    fun pullToRefreshUsesTheSameFiveMinuteHistoryFloor() {
        assertTrue(
            HistoryFetchPolicy.shouldFetchHistory(
                Trigger.PULL_TO_REFRESH,
                lastHistorySuccessAtMs = null,
                nowMs = openedAt
            )
        )
        assertFalse(
            HistoryFetchPolicy.shouldFetchHistory(
                Trigger.PULL_TO_REFRESH,
                lastHistorySuccessAtMs = openedAt,
                nowMs = openedAt + 4 * 60 * 1000L
            )
        )
        assertEquals(
            Poll.LATEST_ONLY,
            HistoryFetchPolicy.plan(
                trigger = Trigger.PULL_TO_REFRESH,
                lastHistorySuccessAtMs = openedAt,
                nowMs = openedAt + 4 * 60 * 1000L,
                lastAnySuccessAtMs = openedAt
            )
        )
        assertTrue(
            HistoryFetchPolicy.shouldFetchHistory(
                Trigger.PULL_TO_REFRESH,
                lastHistorySuccessAtMs = openedAt,
                nowMs = openedAt + HistoryFetchPolicy.FOREGROUND_HISTORY_MIN_MS
            )
        )
    }

    @Test
    fun rapidResumeCoalescesButAWidgetSnapshotDoesNotBlockAppOpenHistory() {
        assertEquals(
            Poll.COALESCED,
            HistoryFetchPolicy.plan(
                trigger = Trigger.APP_OPEN,
                lastHistorySuccessAtMs = openedAt,
                nowMs = openedAt + 2_000L,
                lastAnySuccessAtMs = openedAt + 500L
            )
        )
        assertEquals(
            Poll.LATEST_AND_HISTORY,
            HistoryFetchPolicy.plan(
                trigger = Trigger.APP_OPEN,
                lastHistorySuccessAtMs = openedAt - HistoryFetchPolicy.FOREGROUND_HISTORY_MIN_MS,
                nowMs = openedAt,
                lastAnySuccessAtMs = openedAt - 1_000L
            )
        )
    }

    @Test
    fun backgroundHistoryIsAboutEveryTwoHoursNotEveryWidgetTick() {
        assertEquals(15L, HistoryFetchPolicy.BACKGROUND_PERIOD_MINUTES)
        assertTrue(HistoryFetchPolicy.BACKGROUND_PERIOD_MINUTES >= 15L)
        assertEquals(2 * 60 * 60 * 1000L, HistoryFetchPolicy.BACKGROUND_HISTORY_MS)
        assertEquals(5 * 60 * 1000L, HistoryFetchPolicy.FOREGROUND_HISTORY_MIN_MS)

        assertTrue(
            HistoryFetchPolicy.shouldFetchHistory(
                Trigger.BACKGROUND,
                lastHistorySuccessAtMs = null,
                nowMs = openedAt
            )
        )
        assertFalse(
            HistoryFetchPolicy.shouldFetchHistory(
                Trigger.BACKGROUND,
                lastHistorySuccessAtMs = openedAt,
                nowMs = openedAt + 4 * 60 * 1000L
            )
        )
        assertEquals(
            Poll.LATEST_ONLY,
            HistoryFetchPolicy.plan(
                trigger = Trigger.BACKGROUND,
                lastHistorySuccessAtMs = openedAt,
                nowMs = openedAt + 15 * 60 * 1000L,
                lastAnySuccessAtMs = openedAt
            )
        )
        val justUnderTwoHours = openedAt + HistoryFetchPolicy.BACKGROUND_HISTORY_MS - 1L
        assertFalse(
            HistoryFetchPolicy.shouldFetchHistory(
                Trigger.BACKGROUND,
                lastHistorySuccessAtMs = openedAt,
                nowMs = justUnderTwoHours
            )
        )
        assertEquals(
            Poll.LATEST_AND_HISTORY,
            HistoryFetchPolicy.plan(
                trigger = Trigger.BACKGROUND,
                lastHistorySuccessAtMs = openedAt,
                nowMs = openedAt + HistoryFetchPolicy.BACKGROUND_HISTORY_MS,
                lastAnySuccessAtMs = openedAt
            )
        )
    }

    @Test
    fun failedBackgroundAttemptDoesNotRetryEveryWidgetTickButAppOpenStillCan() {
        val attemptedAt = openedAt
        val fourMinutesLater = openedAt + 4 * 60 * 1000L
        assertFalse(
            HistoryFetchPolicy.shouldFetchHistory(
                trigger = Trigger.BACKGROUND,
                lastHistorySuccessAtMs = null,
                nowMs = fourMinutesLater,
                lastHistoryAttemptAtMs = attemptedAt
            )
        )
        assertTrue(
            HistoryFetchPolicy.shouldFetchHistory(
                trigger = Trigger.APP_OPEN,
                lastHistorySuccessAtMs = null,
                nowMs = fourMinutesLater,
                lastHistoryAttemptAtMs = attemptedAt
            )
        )
    }

    @Test
    fun missingWorkTriggerStaysOnTheBackgroundPolicy() {
        assertEquals(Trigger.BACKGROUND, HistoryFetchPolicy.Trigger.fromWire(null))
        assertEquals(Trigger.BACKGROUND, HistoryFetchPolicy.Trigger.fromWire("include_series"))
        assertEquals(Trigger.APP_OPEN, HistoryFetchPolicy.Trigger.fromWire("app_open"))
        assertEquals(Trigger.PULL_TO_REFRESH, HistoryFetchPolicy.Trigger.fromWire("pull_to_refresh"))
        assertEquals("background", Trigger.BACKGROUND.wire())
    }
}
