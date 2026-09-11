package com.zekkers.watthome.data

import java.time.ZonedDateTime

enum class SessionKind {
    PowerUp,
    HappyHour,
    PowerDown;

    val shortLabel: String
        get() = when (this) {
            PowerUp -> "Power Up"
            HappyHour -> "Happy Hour"
            PowerDown -> "Power Down"
        }

    val isFreeElectric: Boolean get() = this != PowerDown
}

data class VisibleSession(
    val kind: SessionKind,
    val clock: PowerUpClock,
    val optedIn: Boolean,
    val window: PowerUp
) {
    val shortLabel: String get() = kind.shortLabel
}

/**
 * Today's Watt sessions as separate chips. Never concatenates last_action
 * or weather_tomorrow prose. Same London calendar-day + end+5min rule as
 * Power Up.
 */
object SessionLayout {
    fun bookedWindows(status: HomeStatus?): List<PowerUp> {
        if (status == null) return emptyList()
        return listOfNotNull(
            status.nextPowerUp,
            status.bookedPowerUp,
            status.bookedHappyHour,
            status.bookedPowerDown
        )
    }

    fun visible(
        status: HomeStatus?,
        now: ZonedDateTime = ZonedDateTime.now(StatusFormatter.london)
    ): List<VisibleSession> {
        if (status == null) return emptyList()
        val seen = linkedSetOf<String>()
        val sessions = mutableListOf<VisibleSession>()
        val candidates = listOf(
            status.nextPowerUp to inferredKind(status.nextPowerUp, SessionKind.PowerUp),
            status.bookedPowerUp to inferredKind(status.bookedPowerUp, SessionKind.PowerUp),
            status.bookedHappyHour to inferredKind(status.bookedHappyHour, SessionKind.HappyHour),
            status.bookedPowerDown to inferredKind(status.bookedPowerDown, SessionKind.PowerDown)
        )
        for ((raw, fallback) in candidates) {
            val window = raw ?: continue
            val clock = PowerUpLayout.clock(window, now) ?: continue
            val kind = inferredKind(window, fallback)
            val key = windowKey(window, kind)
            if (!seen.add(key)) continue
            sessions += VisibleSession(
                kind = kind,
                clock = clock,
                optedIn = StatusFormatter.optedInPowerUp(window, now),
                window = window
            )
        }
        return sessions.sortedBy { StatusFormatter.parseLocalTime(it.window.from) }
    }

    fun kind(window: PowerUp?, fallback: SessionKind = SessionKind.PowerUp): SessionKind =
        inferredKind(window, fallback)

    fun usesBolt(kind: SessionKind): Boolean = kind.isFreeElectric

    private fun inferredKind(window: PowerUp?, fallback: SessionKind): SessionKind {
        if (window == null) return fallback
        val kind = window.kind?.lowercase()?.replace(' ', '_')
        val label = window.label?.lowercase().orEmpty()
        return when {
            kind == "power_down" || label.contains("power down") -> SessionKind.PowerDown
            kind == "weekend_happy_hour" ||
                kind == "happy_hour" ||
                label.contains("happy hour") -> SessionKind.HappyHour
            kind == "power_up" || label.contains("power up") -> SessionKind.PowerUp
            else -> fallback
        }
    }

    private fun windowKey(window: PowerUp?, kind: SessionKind): String {
        val date = window?.date.orEmpty()
        val from = StatusFormatter.displayClock(window?.from).orEmpty()
        val to = StatusFormatter.displayClock(window?.to).orEmpty()
        return "$kind|$date|$from|$to"
    }
}
