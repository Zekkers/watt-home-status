package com.zekkers.watthome

import com.zekkers.watthome.data.HomeStatusParser
import com.zekkers.watthome.data.PowerUp
import com.zekkers.watthome.data.PowerUpClockMode
import com.zekkers.watthome.data.PowerUpLayout
import com.zekkers.watthome.data.StatusFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZonedDateTime

class PowerUpExpiryTest {
    private val window = PowerUp(from = "12:30", to = "14:30", date = "2026-09-03", optedIn = true)
    private val upcoming = ZonedDateTime.parse("2026-09-03T10:00:00+01:00[Europe/London]")
    private val inProgress = ZonedDateTime.parse("2026-09-03T13:12:00+01:00[Europe/London]")
    private val justEnded = ZonedDateTime.parse("2026-09-03T14:30:00+01:00[Europe/London]")
    private val fourMinutesAfter = ZonedDateTime.parse("2026-09-03T14:34:00+01:00[Europe/London]")
    private val fiveMinutesAfter = ZonedDateTime.parse("2026-09-03T14:35:00+01:00[Europe/London]")
    private val evening = ZonedDateTime.parse("2026-09-03T22:12:00+01:00[Europe/London]")
    private val noDate = PowerUp(from = "12:30", to = "14:30", optedIn = true)
    private val tomorrow = window.copy(date = "2026-09-04")

    @Test
    fun upcomingWindowStillShowsTimesAndBolt() {
        assertVisible(window, upcoming)
    }

    @Test
    fun inProgressWindowStillShowsTimesAndBolt() {
        assertVisible(window, inProgress)
        assertVisible(window, justEnded)
        assertVisible(window, fourMinutesAfter)
    }

    @Test
    fun fiveMinutesAfterEndHidesTimesBoltAndSpokenWindow() {
        assertHidden(window, fiveMinutesAfter)
        assertHidden(window, evening)
    }

    @Test
    fun nextDayDateStillInTheFutureShows() {
        assertVisible(tomorrow, evening)
        assertVisible(tomorrow, fiveMinutesAfter)
    }

    @Test
    fun missingDateHidesOnceTodaysToPlusGraceHasPassed() {
        assertVisible(noDate, inProgress)
        assertHidden(noDate, fiveMinutesAfter)
        assertHidden(noDate, evening)
    }

    @Test
    fun skippedWindowHidesTimesOnceExpired() {
        val skipped = window.copy(optedIn = false)
        assertNotNull(PowerUpLayout.clock(skipped, inProgress))
        assertFalse(StatusFormatter.optedInPowerUp(skipped, inProgress))
        assertHidden(skipped, fiveMinutesAfter)
    }

    @Test
    fun everyWidgetSizeHidesExpiredWindow() {
        assertEquals(PowerUpClockMode.Hidden, PowerUpLayout.oneByOne(window, fiveMinutesAfter))
        assertEquals(PowerUpClockMode.Hidden, PowerUpLayout.twoByTwo(window, fiveMinutesAfter))
        assertEquals(PowerUpClockMode.Hidden, PowerUpLayout.twoByOne(window, now = fiveMinutesAfter))
        assertEquals(
            PowerUpClockMode.Hidden,
            PowerUpLayout.wide(window, availableDp = 200f, timeSp = 12f, density = 1f, now = fiveMinutesAfter)
        )
        assertEquals(PowerUpClockMode.Stacked, PowerUpLayout.oneByOne(window, inProgress))
        assertEquals(PowerUpClockMode.Stacked, PowerUpLayout.twoByTwo(window, upcoming))
        assertEquals(PowerUpClockMode.Stacked, PowerUpLayout.twoByOne(window, now = upcoming))
        assertEquals(
            PowerUpClockMode.OneLine,
            PowerUpLayout.wide(window, availableDp = 200f, timeSp = 12f, density = 1f, now = inProgress)
        )
    }

    @Test
    fun liveFeedShapeExpiresWithoutWaitingForALaterStatusFile() {
        val stale = HomeStatusParser.parse(
            """{"next_power_up":{"from":"12:30","to":"14:30","date":"2026-09-03","opted_in":true}}"""
        ).nextPowerUp
        assertHidden(stale, evening)
        assertVisible(stale, upcoming)
    }

    private fun assertVisible(powerUp: PowerUp?, now: ZonedDateTime) {
        val clock = PowerUpLayout.clock(powerUp, now)
        assertNotNull(clock)
        assertEquals("12:30pm", clock!!.from)
        assertEquals("2:30pm", clock.to)
        assertEquals("12:30pm - 2:30pm", StatusFormatter.powerUpSpokenWindow(powerUp, now))
        assertEquals("12:30pm - 2:30pm", StatusFormatter.powerUpSpokenWindowOrNull(powerUp, now))
        assertEquals(powerUp?.optedIn == true, StatusFormatter.optedInPowerUp(powerUp, now))
        assertTrue(StatusFormatter.isPowerUpCurrent(powerUp, now))
    }

    private fun assertHidden(powerUp: PowerUp?, now: ZonedDateTime) {
        assertNull(PowerUpLayout.clock(powerUp, now))
        assertFalse(StatusFormatter.optedInPowerUp(powerUp, now))
        assertFalse(StatusFormatter.isPowerUpCurrent(powerUp, now))
        assertNull(StatusFormatter.powerUpSpokenWindowOrNull(powerUp, now))
        assertEquals("—", StatusFormatter.powerUpSpokenWindow(powerUp, now))
        assertEquals("—", StatusFormatter.powerUpCompactHours(powerUp, now))
        assertEquals("No Power Up", StatusFormatter.powerUpWindow(powerUp, now))
    }
}
