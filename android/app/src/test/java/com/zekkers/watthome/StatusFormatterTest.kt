package com.zekkers.watthome

import com.zekkers.watthome.data.HomeStatusParser
import com.zekkers.watthome.data.StatusFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneOffset

class StatusFormatterTest {
    private val sample = """
        {
          "updated": "2026-08-24T15:34:00+01:00",
          "soc_percent": 64,
          "solar_w": 980,
          "target_1600_percent": 55,
          "overnight": {"start": "02:00", "end": "03:00", "cap_percent": 30},
          "peak_window": "16:00-19:00",
          "next_power_up": null,
          "last_action": "status.json seeded"
        }
    """.trimIndent()

    @Test
    fun parsesLiveFeedKeysAndNullPowerUp() {
        val status = HomeStatusParser.parse(sample)
        assertEquals(64, status.socPercent)
        assertEquals(980, status.solarW)
        assertEquals(55, status.target1600Percent)
        assertEquals("02:00", status.overnight?.start)
        assertEquals(30, status.overnight?.capPercent)
        assertEquals("16:00-19:00", status.peakWindow)
        assertNull(status.nextPowerUp)
        assertEquals("No Power Up", StatusFormatter.powerUpWindow(status.nextPowerUp))
        assertFalse(StatusFormatter.hasPowerUp(status.nextPowerUp))
        assertEquals("02:00–03:00 · cap 30%", StatusFormatter.overnight(status.overnight))
        assertEquals("64\u2060%", StatusFormatter.percent(status.socPercent))
        assertEquals("64", StatusFormatter.percentNumber(status.socPercent))
        assertEquals("980 W", StatusFormatter.watts(status.solarW))
        assertTrue(status.batteryWSeries.isEmpty())
        assertNull(status.lastSavings)
        assertNull(status.weatherTomorrow)
    }

    @Test
    fun ignoresTarget1600AndOvernightSlotAliases() {
        val status = HomeStatusParser.parse(
            """
            {
              "soc_percent": 10,
              "target_1600": 99,
              "overnight_slot": "02:00-03:00",
              "overnight": {"start": "02:00", "end": "03:00", "cap_percent": 30}
            }
            """.trimIndent()
        )
        assertNull(status.target1600Percent)
        assertEquals("02:00", status.overnight?.start)
        assertNull(HomeStatusParser.parse("""{"overnight_slot":"02:00-03:00"}""").overnight)
    }

    @Test
    fun parsesPowerUpObjectStringAndLastSavingsBatch() {
        val status = HomeStatusParser.parse(
            """
            {
              "soc_percent": 71,
              "target_1600_percent": 60,
              "overnight": {"start": "02:00", "end": "03:00", "cap_percent": 30},
              "next_power_up": {"from":"12:00","to":"14:00","date":"2026-08-25","opted_in":true},
              "weather_tomorrow": {"code":"partly_cloudy","label":"Partly cloudy"},
              "battery_w": -320,
              "battery_w_series": [
                {"t":"2026-08-24T12:00:00+01:00","w":200},
                {"t":"2026-08-24T12:15:00+01:00","w":-50}
              ],
              "last_savings": {
                "gbp": 36.95,
                "kwh_extra": null,
                "percent_extra": null,
                "kind": "power_up",
                "window_label": "21 Jul–1 Aug 2026 (9 sessions)",
                "source": "results_email",
                "at": "2026-08-01T12:00:00+01:00"
              }
            }
            """.trimIndent()
        )
        assertEquals(71, status.socPercent)
        assertEquals(60, status.target1600Percent)
        assertEquals("12:00", status.nextPowerUp?.from)
        assertEquals("12:00–14:00", StatusFormatter.powerUpWindow(status.nextPowerUp))
        assertEquals("12–14", StatusFormatter.powerUpCompactHours(status.nextPowerUp))
        assertEquals("12:00", StatusFormatter.powerUpStartLine(status.nextPowerUp))
        assertEquals("14:00", StatusFormatter.powerUpEndLine(status.nextPowerUp))
        assertEquals("Power Up 12–14", StatusFormatter.powerUpLine(status.nextPowerUp))
        assertTrue(StatusFormatter.hasPowerUp(status.nextPowerUp))
        assertEquals("Partly cloudy", status.weatherTomorrow?.label)
        assertEquals(-320.0, status.batteryW)
        assertEquals(2, status.batteryWSeries.size)
        assertTrue(StatusFormatter.hasTodayCurve(status))
        assertEquals(36.95, status.lastSavings?.gbp)
        assertEquals("£36.95", StatusFormatter.savingsPounds(status.lastSavings))
        assertEquals("£36.95 · 9 sessions", StatusFormatter.savingsBatchLine(status.lastSavings))
        assertEquals(
            "£36.95 · 21 Jul–1 Aug 2026 (9 sessions)",
            StatusFormatter.savingsDetailLine(status.lastSavings)
        )
        assertEquals("-320 W", StatusFormatter.signedWatts(status.batteryW))
    }

    @Test
    fun parsesPowerUpHumanStringAsFromTo() {
        val status = HomeStatusParser.parse("""{"next_power_up":"12:00–14:00 Tue 25 Aug"}""")
        assertEquals("12:00", status.nextPowerUp?.from)
        assertEquals("14:00", status.nextPowerUp?.to)
        assertEquals("12:00–14:00", StatusFormatter.powerUpWindow(status.nextPowerUp))
        assertTrue(StatusFormatter.hasPowerUp(status.nextPowerUp))
        assertNull(HomeStatusParser.parse("""{"next_power_up":null}""").nextPowerUp)
    }

    @Test
    fun doesNotInventPerSessionPounds() {
        val status = HomeStatusParser.parse(
            """{"last_savings":{"gbp":36.95,"window_label":"21 Jul–1 Aug 2026 (9 sessions)"}}"""
        )
        val line = StatusFormatter.savingsBatchLine(status.lastSavings)
        assertEquals("£36.95 · 9 sessions", line)
        assertFalse(line!!.contains("£4.10"))
        assertFalse(line.contains("£4.11"))
    }

    @Test
    fun formatsUpdatedInLondon() {
        val formatted = StatusFormatter.formatUpdated("2026-08-24T15:34:00+01:00")
        assertEquals("Mon 24 Aug 2026, 15:34 UK", formatted)

        val bstFromUtc = StatusFormatter.formatUpdated("2026-08-24T14:34:00Z")
        assertEquals("Mon 24 Aug 2026, 15:34 UK", bstFromUtc)

        val winter = StatusFormatter.formatUpdated("2026-01-15T15:34:00Z")
        assertEquals("Thu 15 Jan 2026, 15:34 UK", winter)
    }

    @Test
    fun parseTimestampKeepsOffset() {
        val parsed = StatusFormatter.parseTimestamp("2026-08-24T15:34:00+01:00")
        assertEquals(ZoneOffset.ofHours(1), parsed?.offset)
    }

    @Test
    fun displayClockKeepsUkWallClockAndConvertsIso() {
        assertEquals("13:00", StatusFormatter.displayClock("13:00"))
        assertEquals("15:34", StatusFormatter.displayClock("2026-08-24T14:34:00Z"))
    }

    @Test
    fun compactHoursNeverSplitColonZeroZero() {
        val noon = HomeStatusParser.parse("""{"next_power_up":{"from":"12:00","to":"14:00"}}""")
        val peak = HomeStatusParser.parse("""{"next_power_up":{"from":"16:00","to":"19:00"}}""")
        assertEquals("12–14", StatusFormatter.powerUpCompactHours(noon.nextPowerUp))
        assertEquals("16–19", StatusFormatter.powerUpCompactHours(peak.nextPowerUp))
        assertEquals("12:00", StatusFormatter.powerUpStartLine(noon.nextPowerUp))
        assertEquals("14:00", StatusFormatter.powerUpEndLine(noon.nextPowerUp))
        assertEquals("16:00", StatusFormatter.powerUpStartLine(peak.nextPowerUp))
        assertEquals("19:00", StatusFormatter.powerUpEndLine(peak.nextPowerUp))
    }

    @Test
    fun parsesHistoryArraysForTodayCurve() {
        val status = HomeStatusParser.parse(
            """
            {
              "soc_series": [
                {"t":"2026-08-24T10:00:00+01:00","soc":40},
                {"t":"2026-08-24T12:00:00+01:00","soc":55}
              ],
              "history": [
                {"t":"2026-08-24T10:00:00+01:00","battery_w":200},
                {"t":"2026-08-24T12:00:00+01:00","w":-80}
              ],
              "samples": [
                {"time":"2026-08-24T11:00:00+01:00","soc_percent":48}
              ]
            }
            """.trimIndent()
        )
        assertTrue(status.socSeries.size >= 2)
        assertTrue(status.batteryWSeries.size >= 2)
        assertTrue(StatusFormatter.hasTodayCurve(status))
        val live = HomeStatusParser.parse("""{"soc_percent":63,"battery_w":1020}""")
        assertFalse(StatusFormatter.hasTodayCurve(live))

        val numbers = HomeStatusParser.parse("""{"soc_series":[40,55,63,70]}""")
        assertEquals(4, numbers.socSeries.size)
        assertTrue(StatusFormatter.hasTodayCurve(numbers))
    }

    @Test
    fun ignoresUnknownFields() {
        val status = HomeStatusParser.parse("""{"soc_percent":10,"extra_future":{"ok":true}}""")
        assertEquals(10, status.socPercent)
    }
}
