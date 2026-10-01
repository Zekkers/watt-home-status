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
    val clock: PowerUpClock?,
    val optedIn: Boolean,
    val window: PowerUp
) {
    val shortLabel: String get() = kind.shortLabel
    val showTimes: Boolean get() = clock != null
    val cueLabel: String get() = if (clock == null) SessionLayout.UPCOMING_CUE_LABEL else shortLabel
}

/**
 * Today's Watt sessions as separate chips. Never concatenates last_action
 * or weather_tomorrow prose. Same London calendar-day + end+5min rule as
 * Power Up.
 */
object SessionLayout {
    const val UPCOMING_CUE_LABEL = "opted in"

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
        now: ZonedDateTime = ZonedDateTime.now(StatusFormatter.london),
        style: ClockStyle = ClockStyle.DEFAULT
    ): List<VisibleSession> {
        if (status == null) return emptyList()
        val seen = linkedSetOf<String>()
        val sessions = mutableListOf<VisibleSession>()
        for ((raw, fallback) in candidates(status)) {
            val window = raw ?: continue
            val clock = PowerUpLayout.clock(window, now, style) ?: continue
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

    /**
     * Widget chips: today's in-window sessions (with times) plus a compact
     * opted-in cue for a future booked Power Up / Happy Hour / Power Down.
     * No weekday or clock until the London calendar day.
     */
    fun widgetSessions(
        status: HomeStatus?,
        now: ZonedDateTime = ZonedDateTime.now(StatusFormatter.london),
        style: ClockStyle = ClockStyle.DEFAULT
    ): List<VisibleSession> {
        if (status == null) return emptyList()
        val seen = linkedSetOf<String>()
        val sessions = mutableListOf<VisibleSession>()
        for (session in visible(status, now, style)) {
            seen += windowKey(session.window, session.kind)
            sessions += session
        }
        for ((raw, fallback) in candidates(status)) {
            val window = raw ?: continue
            if (!StatusFormatter.isUpcomingOptedIn(window, now)) continue
            val kind = inferredKind(window, fallback)
            val key = windowKey(window, kind)
            if (!seen.add(key)) continue
            sessions += VisibleSession(
                kind = kind,
                clock = null,
                optedIn = true,
                window = window
            )
        }
        return sessions
    }

    private fun candidates(status: HomeStatus): List<Pair<PowerUp?, SessionKind>> = listOf(
        status.nextPowerUp to inferredKind(status.nextPowerUp, SessionKind.PowerUp),
        status.bookedPowerUp to inferredKind(status.bookedPowerUp, SessionKind.PowerUp),
        status.bookedHappyHour to inferredKind(status.bookedHappyHour, SessionKind.HappyHour),
        status.bookedPowerDown to inferredKind(status.bookedPowerDown, SessionKind.PowerDown)
    )

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
