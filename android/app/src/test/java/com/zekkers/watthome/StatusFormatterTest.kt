package com.zekkers.watthome

import com.zekkers.watthome.data.HomeStatusParser
import com.zekkers.watthome.data.PowerUp
import com.zekkers.watthome.data.StatusFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime

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
    fun parsesExampleJsonAndNullPowerUp() {
        val status = HomeStatusParser.parse(sample)
        assertEquals(64, status.socPercent)
        assertEquals(980, status.solarW)
        assertEquals(55, status.target1600Percent)
        assertEquals("02:00", status.overnight?.start)
        assertEquals("16:00-19:00", status.peakWindow)
        assertNull(status.nextPowerUp)
        assertEquals("No Power Up", StatusFormatter.powerUpWindow(status.nextPowerUp))
        assertFalse(StatusFormatter.showPowerUpBadge(status.nextPowerUp))
        assertEquals("02:00–03:00 · cap 30%", StatusFormatter.overnight(status.overnight))
        assertEquals("64%", StatusFormatter.percent(status.socPercent))
        assertEquals("980 W", StatusFormatter.watts(status.solarW))
        assertTrue(status.batteryWSeries.isEmpty())
        assertNull(status.savings)
        assertNull(status.weatherTomorrow)
    }

    @Test
    fun parsesPowerUpObjectAndNewFields() {
        val status = HomeStatusParser.parse(
            """
            {
              "soc_percent": 71,
              "target_1600": 60,
              "overnight_slot": "02:00-03:00",
              "next_power_up": {"from":"13:00","to":"15:00","date":"2026-08-25","opted_in":true},
              "weather_tomorrow": {"code":"partly_cloudy","label":"Partly cloudy"},
              "battery_w": -320,
              "battery_w_series": [
                {"t":"2026-08-24T12:00:00+01:00","w":200},
                {"t":"2026-08-24T12:15:00+01:00","w":-50}
              ],
              "savings": {"last_gbp":4.2,"label":"21 Aug session"}
            }
            """.trimIndent()
        )
        assertEquals(71, status.socPercent)
        assertEquals(60, status.target1600Percent)
        assertEquals("02:00-03:00", status.overnight?.label)
        assertEquals("13:00", status.nextPowerUp?.from)
        assertEquals(true, status.nextPowerUp?.optedIn)
        assertEquals("13:00–15:00", StatusFormatter.powerUpWindow(status.nextPowerUp))
        assertTrue(StatusFormatter.showPowerUpBadge(status.nextPowerUp))
        assertEquals("Partly cloudy", status.weatherTomorrow?.label)
        assertEquals(-320.0, status.batteryW)
        assertEquals(2, status.batteryWSeries.size)
        assertEquals("£4.20", StatusFormatter.savingsPounds(status.savings))
        assertEquals("£4.20 · 21 Aug session", StatusFormatter.savingsLine(status.savings))
        assertEquals("-320 W", StatusFormatter.signedWatts(status.batteryW))
    }

    @Test
    fun parsesPowerUpStringAndHidesBadgeWhenNull() {
        val status = HomeStatusParser.parse("""{"next_power_up":"13:00-15:00"}""")
        assertEquals("13:00-15:00", StatusFormatter.powerUpWindow(status.nextPowerUp))
        assertNull(HomeStatusParser.parse("""{"next_power_up":null}""").nextPowerUp)
    }

    @Test
    fun hidesBadgeWhenNotOptedInAndWindowEnded() {
        val yesterday = LocalDate.of(2020, 1, 1)
        val powerUp = PowerUp(from = "13:00", to = "15:00", date = yesterday.toString(), optedIn = false)
        val now = ZonedDateTime.of(2026, 8, 24, 16, 0, 0, 0, StatusFormatter.london)
        assertFalse(StatusFormatter.showPowerUpBadge(powerUp, now))
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
    fun ignoresUnknownFields() {
        val status = HomeStatusParser.parse("""{"soc_percent":10,"extra_future":{"ok":true}}""")
        assertEquals(10, status.socPercent)
    }
}
