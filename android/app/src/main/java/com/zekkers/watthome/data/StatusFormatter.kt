package com.zekkers.watthome.data

import java.time.Instant
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

object StatusFormatter {
    val london: ZoneId = ZoneId.of("Europe/London")

    private val displayFormat: DateTimeFormatter =
        DateTimeFormatter.ofPattern("EEE d MMM yyyy, HH:mm", Locale.UK)
    private val clockFormat: DateTimeFormatter =
        DateTimeFormatter.ofPattern("HH:mm", Locale.UK)
    private val sessionCount = Regex("""\((\d+)\s+sessions?\)""", RegexOption.IGNORE_CASE)

    fun dash(value: String?): String = value?.takeIf { it.isNotBlank() } ?: "—"

    fun percent(value: Int?): String = value?.let { "$it\u2060%" } ?: "—"

    fun percentNumber(value: Int?): String = value?.toString() ?: "—"

    fun watts(value: Int?): String = value?.let { "$it W" } ?: "—"

    fun signedWatts(value: Double?): String {
        if (value == null) return "—"
        val rounded = if (abs(value) >= 10) value.toInt().toString() else String.format(Locale.UK, "%.0f", value)
        return "${if (value > 0) "+" else ""}$rounded W"
    }

    fun overnight(overnight: Overnight?): String {
        if (overnight == null) return "—"
        val start = overnight.start?.takeIf { it.isNotBlank() }
        val end = overnight.end?.takeIf { it.isNotBlank() }
        val window = when {
            start != null && end != null -> "$start–$end"
            start != null -> start
            end != null -> end
            else -> null
        }
        val cap = overnight.capPercent?.let { "cap $it%" }
        return listOfNotNull(window, cap).joinToString(" · ").ifBlank { "—" }
    }

    fun powerUpWindow(powerUp: PowerUp?): String = powerUpWindowOrNull(powerUp) ?: "No Power Up"

    fun powerUpWindowOrNull(powerUp: PowerUp?): String? {
        if (powerUp == null) return null
        val from = displayClock(powerUp.from)
        val to = displayClock(powerUp.to)
        return when {
            from != null && to != null -> "$from–$to"
            from != null -> from
            to != null -> to
            else -> powerUp.label?.takeIf { it.isNotBlank() }
        }
    }

    fun powerUpLine(powerUp: PowerUp?): String {
        if (powerUp == null) return "No Power Up"
        val hours = powerUpCompactHours(powerUp)
        return if (hours == "No Power Up") "No Power Up" else "Power Up $hours"
    }

    fun powerUpStartLine(powerUp: PowerUp?): String {
        if (powerUp == null) return "No"
        return displayClock(powerUp.from) ?: "Power Up"
    }

    fun powerUpEndLine(powerUp: PowerUp?): String {
        if (powerUp == null) return "Power Up"
        return displayClock(powerUp.to) ?: "set"
    }

    fun powerUpCompactHours(powerUp: PowerUp?): String {
        if (powerUp == null) return "No Power Up"
        val fromHour = displayClock(powerUp.from)?.substringBefore(":")
        val toHour = displayClock(powerUp.to)?.substringBefore(":")
        return if (fromHour != null && toHour != null) "$fromHour–$toHour" else "No Power Up"
    }

    fun hasTodayCurve(status: HomeStatus?): Boolean {
        if (status == null) return false
        return status.socSeries.count { it.soc != null } >= 2 ||
            status.batteryWSeries.count { it.w != null } >= 2
    }

    fun hasPowerUp(powerUp: PowerUp?): Boolean = powerUp != null

    fun optedInPowerUp(powerUp: PowerUp?): Boolean = powerUp?.optedIn == true

    fun savingsPounds(savings: LastSavings?): String? {
        val amount = savings?.gbp ?: return null
        return String.format(Locale.UK, "£%.2f", amount)
    }

    fun savingsBatchLine(savings: LastSavings?): String? {
        val pounds = savingsPounds(savings) ?: return null
        val sessions = sessionCountLabel(savings?.windowLabel)
        return if (sessions != null) "$pounds · $sessions" else pounds
    }

    fun savingsDetailLine(savings: LastSavings?): String? {
        val pounds = savingsPounds(savings) ?: return null
        val window = savings?.windowLabel?.takeIf { it.isNotBlank() }
        val sessions = sessionCountLabel(savings?.windowLabel)
        return when {
            window != null -> "$pounds · $window"
            sessions != null -> "$pounds · $sessions"
            else -> pounds
        }
    }

    fun sessionCountLabel(windowLabel: String?): String? {
        val count = sessionCount.find(windowLabel.orEmpty())?.groupValues?.get(1)?.toIntOrNull()
            ?: return null
        return if (count == 1) "1 session" else "$count sessions"
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
}
