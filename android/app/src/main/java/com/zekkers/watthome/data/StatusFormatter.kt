package com.zekkers.watthome.data

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

object StatusFormatter {
    val london: ZoneId = ZoneId.of("Europe/London")

    private val displayFormat: DateTimeFormatter =
        DateTimeFormatter.ofPattern("EEE d MMM yyyy, HH:mm", Locale.UK)
    private val clockFormat: DateTimeFormatter =
        DateTimeFormatter.ofPattern("HH:mm", Locale.UK)

    fun dash(value: String?): String = value?.takeIf { it.isNotBlank() } ?: "—"

    fun percent(value: Int?): String = value?.let { "$it%" } ?: "—"

    fun watts(value: Int?): String = value?.let { "$it W" } ?: "—"

    fun signedWatts(value: Double?): String {
        if (value == null) return "—"
        val rounded = if (abs(value) >= 10) value.toInt().toString() else String.format(Locale.UK, "%.0f", value)
        return "${if (value > 0) "+" else ""}$rounded W"
    }

    fun overnight(overnight: Overnight?): String {
        if (overnight == null) return "—"
        if (!overnight.label.isNullOrBlank() && overnight.start == null && overnight.end == null) {
            return overnight.label
        }
        val start = overnight.start?.takeIf { it.isNotBlank() }
        val end = overnight.end?.takeIf { it.isNotBlank() }
        val window = when {
            start != null && end != null -> "$start–$end"
            start != null -> start
            end != null -> end
            else -> overnight.label
        }
        val cap = overnight.capPercent?.let { "cap $it%" }
        return listOfNotNull(window, cap).joinToString(" · ").ifBlank { "—" }
    }

    fun powerUpWindow(powerUp: PowerUp?): String = powerUpWindowOrNull(powerUp) ?: "No Power Up"

    fun powerUpWindowOrNull(powerUp: PowerUp?): String? {
        if (powerUp == null) return null
        val from = displayClock(powerUp.from)
        val to = displayClock(powerUp.to)
        val window = when {
            from != null && to != null -> "$from–$to"
            from != null -> from
            to != null -> to
            else -> powerUp.label?.takeIf { it.isNotBlank() }
        }
        return window?.takeIf { it.isNotBlank() }
    }

    fun powerUpLine(powerUp: PowerUp?): String {
        val window = powerUpWindowOrNull(powerUp) ?: return "No Power Up"
        return "Power Up $window"
    }

    fun showPowerUpBadge(powerUp: PowerUp?, now: ZonedDateTime = ZonedDateTime.now(london)): Boolean {
        if (powerUp == null) return false
        if (powerUp.optedIn == true) return true
        return powerUpWindowOrNull(powerUp) != null && isPowerUpUpcoming(powerUp, now)
    }

    fun isPowerUpUpcoming(powerUp: PowerUp?, now: ZonedDateTime = ZonedDateTime.now(london)): Boolean {
        if (powerUp == null) return false
        val end = powerUpEnd(powerUp)
        return if (end != null) !end.isBefore(now) else powerUpWindowOrNull(powerUp) != null
    }

    fun savingsPounds(savings: Savings?): String? {
        val amount = savings?.lastGbp ?: return null
        return String.format(Locale.UK, "£%.2f", amount)
    }

    fun savingsLine(savings: Savings?): String? {
        val pounds = savingsPounds(savings) ?: return null
        val label = savings?.label?.takeIf { it.isNotBlank() }
        return if (label != null) "$pounds · $label" else pounds
    }

    fun weatherLabel(weather: WeatherTomorrow?): String =
        weather?.label?.takeIf { it.isNotBlank() }
            ?: weather?.code?.replace('_', ' ')?.replaceFirstChar { it.titlecase(Locale.UK) }
            ?: "—"

    fun lastAction(value: String?): String = dash(value)

    fun formatUpdated(raw: String?): String {
        if (raw.isNullOrBlank()) return "Unknown"
        val offsetDateTime = parseTimestamp(raw) ?: return raw
        return offsetDateTime.atZoneSameInstant(london).format(displayFormat) + " UK"
    }

    fun parseTimestamp(raw: String): OffsetDateTime? {
        runCatching { return OffsetDateTime.parse(raw) }
        runCatching { return Instant.parse(raw).atOffset(ZoneOffset.UTC) }
        return null
    }

    fun displayClock(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        parseTimestamp(raw)?.let { return it.atZoneSameInstant(london).format(clockFormat) }
        runCatching { return LocalTime.parse(raw.trim()).format(clockFormat) }
        val hm = Regex("""^(\d{1,2}):(\d{2})""").find(raw.trim()) ?: return raw.trim()
        val hour = hm.groupValues[1].toInt()
        val minute = hm.groupValues[2].toInt()
        return String.format(Locale.UK, "%02d:%02d", hour, minute)
    }

    private fun powerUpEnd(powerUp: PowerUp): ZonedDateTime? {
        val toClock = displayClock(powerUp.to) ?: return null
        val toTime = runCatching { LocalTime.parse(toClock) }.getOrNull() ?: return null
        val day = powerUp.date?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            ?: ZonedDateTime.now(london).toLocalDate()
        return ZonedDateTime.of(day, toTime, london)
    }
}
