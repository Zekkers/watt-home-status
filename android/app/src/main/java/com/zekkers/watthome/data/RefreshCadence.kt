package com.zekkers.watthome.data

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import kotlin.math.abs

/**
 * Adaptive widget poll: 15 min when the pack is quiet, ~90s while it is moving.
 * WorkManager periodic work cannot go below 15 minutes; one-shot follow-ups fill the gap.
 */
object RefreshCadence {
    const val MOVING_WATTS = 500.0
    const val FOLLOW_UP_SECONDS = 90L
    const val IDLE_PERIOD_MINUTES = 15L
    const val KEY_INCLUDE_SERIES = "include_series"

    fun needsFastPoll(
        status: HomeStatus,
        previousSocPercent: Int? = null,
        now: ZonedDateTime = ZonedDateTime.now(StatusFormatter.london)
    ): Boolean {
        if (abs(status.batteryW ?: 0.0) > MOVING_WATTS) return true
        if (isPowerUpLive(status.nextPowerUp, now)) return true
        val soc = status.socPercent
        if (soc != null && previousSocPercent != null && soc != previousSocPercent) return true
        return false
    }

    fun isPowerUpLive(
        powerUp: PowerUp?,
        now: ZonedDateTime = ZonedDateTime.now(StatusFormatter.london)
    ): Boolean {
        if (powerUp == null || powerUp.optedIn != true) return false
        val from = parseClock(powerUp.from) ?: return false
        val to = parseClock(powerUp.to) ?: return false
        val today = now.toLocalDate()
        val date = parseDate(powerUp.date) ?: today
        if (date != today) return false
        val time = now.toLocalTime()
        return if (to.isAfter(from) || to == from) {
            !time.isBefore(from) && time.isBefore(to)
        } else {
            !time.isBefore(from) || time.isBefore(to)
        }
    }

    private fun parseClock(raw: String?): LocalTime? {
        val clock = StatusFormatter.displayClock(raw) ?: return null
        return runCatching { LocalTime.parse(clock) }.getOrNull()
    }

    private fun parseDate(raw: String?): LocalDate? {
        if (raw.isNullOrBlank()) return null
        return runCatching { LocalDate.parse(raw.trim()) }.getOrNull()
    }
}
