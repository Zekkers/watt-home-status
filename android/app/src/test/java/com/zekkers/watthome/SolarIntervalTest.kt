package com.zekkers.watthome

import com.zekkers.watthome.data.BatterySample
import com.zekkers.watthome.data.HomeStatus
import com.zekkers.watthome.data.HomeStatusJson
import com.zekkers.watthome.data.HomeStatusParser
import com.zekkers.watthome.data.RefreshCadence
import com.zekkers.watthome.data.SolarInterval
import com.zekkers.watthome.data.StatusFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.OffsetDateTime

class SolarIntervalTest {
    private val now = OffsetDateTime.parse("2026-08-24T12:15:00+01:00")

    @Test
    fun averagesArrayOneSamplesInPreviousFifteenMinuteWindow() {
        val samples = listOf(
            BatterySample(t = "2026-08-24T12:00:00+01:00", w = 400.0),
            BatterySample(t = "2026-08-24T12:05:00+01:00", w = 450.0),
            BatterySample(t = "2026-08-24T12:10:00+01:00", w = 380.0),
            BatterySample(t = "2026-08-24T12:15:00+01:00", w = 418.0)
        )
        val lastPoll = OffsetDateTime.parse("2026-08-24T12:00:00+01:00")
        val average = SolarInterval.averageInWindow(samples, lastPoll, now)
        assertEquals(412, average)
        assertEquals("412 W", StatusFormatter.watts(average))
    }

    @Test
    fun ninetySecondWindowIgnoresOlderFifteenMinuteSamples() {
        val samples = listOf(
            BatterySample(t = "2026-08-24T12:00:00+01:00", w = 900.0),
            BatterySample(t = "2026-08-24T12:13:30+01:00", w = 400.0),
            BatterySample(t = "2026-08-24T12:15:00+01:00", w = 424.0)
        )
        val lastPoll = now.minusSeconds(RefreshCadence.FOLLOW_UP_SECONDS)
        val average = SolarInterval.averageInWindow(samples, lastPoll, now)
        assertEquals(412, average)
    }

    @Test
    fun nightZeroStaysZeroWatts() {
        val samples = listOf(
            BatterySample(t = "2026-08-24T12:00:00+01:00", w = 0.0),
            BatterySample(t = "2026-08-24T12:15:00+01:00", w = 0.0)
        )
        val lastPoll = OffsetDateTime.parse("2026-08-24T12:00:00+01:00")
        assertEquals(0, SolarInterval.averageInWindow(samples, lastPoll, now))
        assertEquals("0 W", StatusFormatter.watts(0))
    }

    @Test
    fun firstPollUsesIdleLookbackThenCurrentWattsIfEmpty() {
        val idleStart = SolarInterval.windowStart(null, now)
        assertEquals(now.minusMinutes(RefreshCadence.IDLE_PERIOD_MINUTES), idleStart)
        val fromPoints = listOf(
            BatterySample(t = "2026-08-24T12:05:00+01:00", w = 400.0),
            BatterySample(t = "2026-08-24T12:10:00+01:00", w = 424.0)
        )
        assertEquals(412, SolarInterval.averageInWindow(fromPoints, null, now))
        assertNull(SolarInterval.averageInWindow(emptyList(), null, now))
    }

    @Test
    fun longGapClampsToTwentyMinutesNotADayAverage() {
        val lastPoll = now.minusHours(8)
        val start = SolarInterval.windowStart(lastPoll, now)
        assertEquals(now.minusMinutes(SolarInterval.MAX_LOOKBACK_MINUTES), start)
        val samples = listOf(
            BatterySample(t = "2026-08-24T04:00:00+01:00", w = 2000.0),
            BatterySample(t = "2026-08-24T12:00:00+01:00", w = 400.0),
            BatterySample(t = "2026-08-24T12:15:00+01:00", w = 424.0)
        )
        assertEquals(412, SolarInterval.averageInWindow(samples, lastPoll, now))
    }

    @Test
    fun finishBuffersCurrentWattsOnFirstPoll() {
        val status = HomeStatus(
            updated = "2026-08-24T12:15:00+01:00",
            socPercent = 49,
            solarW = 256
        )
        val finished = SolarInterval.finish(status, previous = null, now = now)
        assertEquals(256, finished.solarIntervalAvgW)
        assertEquals(1, finished.solarWSeries.size)
        assertEquals(256.0, finished.solarWSeries.single().w!!, 0.01)
        assertEquals("2026-08-24T12:15:00+01:00", finished.lastWidgetPollAt)
        assertEquals("256 W", StatusFormatter.solarIntervalWatts(finished))
    }

    @Test
    fun finishMeansPreviousPollAndCurrentWhenNoDataPoints() {
        val previous = HomeStatus(
            updated = "2026-08-24T12:13:30+01:00",
            solarW = 400,
            solarWSeries = listOf(BatterySample(t = "2026-08-24T12:13:30+01:00", w = 400.0)),
            lastWidgetPollAt = "2026-08-24T12:13:30+01:00"
        )
        val current = HomeStatus(
            updated = "2026-08-24T12:15:00+01:00",
            solarW = 424
        )
        val finished = SolarInterval.finish(current, previous, now)
        assertEquals(412, finished.solarIntervalAvgW)
        assertEquals(2, finished.solarWSeries.size)
    }

    @Test
    fun retainDropsSamplesOlderThanTwentyMinutes() {
        val kept = SolarInterval.retainRecent(
            listOf(
                BatterySample(t = "2026-08-24T11:50:00+01:00", w = 10.0),
                BatterySample(t = "2026-08-24T12:00:00+01:00", w = 400.0),
                BatterySample(t = "2026-08-24T12:15:00+01:00", w = 424.0)
            ),
            now
        )
        assertEquals(2, kept.size)
        assertEquals(400.0, kept.first().w!!, 0.01)
    }

    @Test
    fun cacheRoundTripKeepsIntervalAverage() {
        val status = HomeStatus(
            updated = "2026-08-24T12:15:00+01:00",
            socPercent = 49,
            solarW = 424,
            solarIntervalAvgW = 412,
            solarWSeries = listOf(
                BatterySample(t = "2026-08-24T12:00:00+01:00", w = 400.0),
                BatterySample(t = "2026-08-24T12:15:00+01:00", w = 424.0)
            ),
            lastWidgetPollAt = "2026-08-24T12:15:00+01:00"
        )
        val encoded = HomeStatusJson.encode(status)
        assertTrue(encoded.contains("solar_interval_avg_w"))
        assertTrue(encoded.contains("solar_w_series"))
        assertFalse(encoded.contains("Bearer"))
        val parsed = HomeStatusParser.parse(encoded)
        assertEquals(412, parsed.solarIntervalAvgW)
        assertEquals(2, parsed.solarWSeries.size)
        assertEquals("2026-08-24T12:15:00+01:00", parsed.lastWidgetPollAt)
        assertEquals("412 W", StatusFormatter.solarIntervalWatts(parsed))
    }
}
