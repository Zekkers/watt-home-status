package com.zekkers.watthome.data

import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

object StatusFormatter {
    val london: ZoneId = ZoneId.of("Europe/London")

    private val displayFormat: DateTimeFormatter =
        DateTimeFormatter.ofPattern("EEE d MMM yyyy, HH:mm", Locale.UK)

    fun dash(value: String?): String = value?.takeIf { it.isNotBlank() } ?: "—"

    fun percent(value: Int?): String = value?.let { "$it%" } ?: "—"

    fun watts(value: Int?): String = value?.let { "$it W" } ?: "—"

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

    fun nextPowerUp(value: String?): String =
        value?.takeIf { it.isNotBlank() } ?: "None scheduled"

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
}
