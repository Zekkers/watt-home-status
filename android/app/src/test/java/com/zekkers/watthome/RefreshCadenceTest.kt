package com.zekkers.watthome

import com.zekkers.watthome.data.HomeStatus
import com.zekkers.watthome.data.HomeStatusParser
import com.zekkers.watthome.data.PowerUp
import com.zekkers.watthome.data.RefreshCadence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZonedDateTime

class RefreshCadenceTest {
    private val londonNoon = ZonedDateTime.parse("2026-08-27T12:30:00+01:00[Europe/London]")
    private val londonMorning = ZonedDateTime.parse("2026-08-27T08:00:00+01:00[Europe/London]")
    private val londonTwoPm = ZonedDateTime.parse("2026-08-27T14:00:00+01:00[Europe/London]")

    @Test
    fun idleWhenQuietAndNoPowerUp() {
        val status = HomeStatus(socPercent = 40, batteryW = -62.0)
        assertFalse(RefreshCadence.needsFastPoll(status, previousSocPercent = 40, now = londonMorning))
    }

    @Test
    fun fastWhenChargingHard() {
        val status = HomeStatus(socPercent = 20, batteryW = 6000.0)
        assertTrue(RefreshCadence.needsFastPoll(status, previousSocPercent = 20, now = londonMorning))
    }

    @Test
    fun fastWhenDischargingHard() {
        val status = HomeStatus(socPercent = 80, batteryW = -1800.0)
        assertTrue(RefreshCadence.needsFastPoll(status, previousSocPercent = 80, now = londonMorning))
    }

    @Test
    fun idleAtExactlyFiveHundredWatts() {
        val status = HomeStatus(socPercent = 40, batteryW = 500.0)
        assertFalse(RefreshCadence.needsFastPoll(status, previousSocPercent = 40, now = londonMorning))
        assertTrue(RefreshCadence.needsFastPoll(status.copy(batteryW = 501.0), 40, londonMorning))
    }

    @Test
    fun fastDuringLivePowerUpWindow() {
        val status = HomeStatus(
            socPercent = 12,
            batteryW = -80.0,
            nextPowerUp = PowerUp(from = "12:00", to = "14:00", date = "2026-08-27", optedIn = true)
        )
        assertTrue(RefreshCadence.needsFastPoll(status, previousSocPercent = 12, now = londonNoon))
        assertFalse(RefreshCadence.needsFastPoll(status, previousSocPercent = 12, now = londonMorning))
        assertFalse(RefreshCadence.needsFastPoll(status, previousSocPercent = 12, now = londonTwoPm))
    }

    @Test
    fun skippedPowerUpDoesNotForceFastPoll() {
        val status = HomeStatus(
            socPercent = 40,
            batteryW = 0.0,
            nextPowerUp = PowerUp(from = "12:00", to = "14:00", date = "2026-08-27", optedIn = false)
        )
        assertFalse(RefreshCadence.needsFastPoll(status, previousSocPercent = 40, now = londonNoon))
    }

    @Test
    fun powerUpOnAnotherDayIsIdle() {
        val status = HomeStatus(
            socPercent = 40,
            batteryW = 0.0,
            nextPowerUp = PowerUp(from = "12:00", to = "14:00", date = "2026-08-28", optedIn = true)
        )
        assertFalse(RefreshCadence.isPowerUpLive(status.nextPowerUp, londonNoon))
    }

    @Test
    fun fastWhenSocMoved() {
        val status = HomeStatus(socPercent = 45, batteryW = 120.0)
        assertTrue(RefreshCadence.needsFastPoll(status, previousSocPercent = 40, now = londonMorning))
        assertFalse(RefreshCadence.needsFastPoll(status, previousSocPercent = 45, now = londonMorning))
    }

    @Test
    fun parsesPowerUpFromLiveStatusJson() {
        val status = HomeStatusParser.parse(
            """
            {
              "soc_percent": 12,
              "battery_w": -62,
              "next_power_up": {"from":"12:00","to":"14:00","date":"2026-08-27","opted_in":true}
            }
            """.trimIndent()
        )
        assertTrue(RefreshCadence.isPowerUpLive(status.nextPowerUp, londonNoon))
        assertFalse(RefreshCadence.needsFastPoll(status, 12, londonMorning))
    }

    @Test
    fun followUpDelayIsBetweenOneAndTwoMinutes() {
        assertEquals(90L, RefreshCadence.FOLLOW_UP_SECONDS)
        assertTrue(RefreshCadence.FOLLOW_UP_SECONDS in 60L..120L)
        assertEquals(15L, RefreshCadence.IDLE_PERIOD_MINUTES)
        assertEquals(500.0, RefreshCadence.MOVING_WATTS, 0.0)
    }
}
