package com.zekkers.watthome

import com.zekkers.watthome.data.BatterySample
import com.zekkers.watthome.data.HomeStatus
import com.zekkers.watthome.data.HomeStatusJson
import com.zekkers.watthome.data.HomeStatusParser
import com.zekkers.watthome.data.LiveStatus
import com.zekkers.watthome.data.StatusFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.ZonedDateTime

/** The status.json shape the routines write today (16 Nov 2026). */
class StatusFeedShapeTest {
    private val feed = """
        {
          "updated_at": "2026-11-16T14:11:59+00:00",
          "timezone": "Europe/London",
          "soc_percent": 97,
          "battery_w": -263,
          "target_1600_percent": 75,
          "overnight": {"from": "02:40", "to": "05:00", "percent_limit": 88, "kw": 6, "cleared": false, "kwh": 11.3, "mins": 140, "path": "cold_sunny_cap88"},
          "next_power_up": null,
          "last_action": "14:11 Mon 16 Nov check: no write",
          "updated": "2026-11-16T14:11:59+00:00",
          "peak_window": {"from": "14:22", "to": "16:00", "target_percent": 75, "applied": true, "reason": "started_now_high_draw"},
          "weather_tomorrow": {"code": "cloud", "label": "Cloudy"},
          "peak_topup": {"from": "14:22", "to": "16:00", "percent_limit": 75, "kw": 6}
        }
    """.trimIndent()

    private fun at(text: String) = ZonedDateTime.parse("$text[Europe/London]")

    @Test
    fun overnightSlotCardIsFilledFromTheCurrentShape() {
        val status = HomeStatusParser.parse(feed)
        val text = StatusFormatter.overnight(status.overnight)
        assertNotEquals("—", text)
        assertTrue(text.isNotBlank())
        assertEquals("02:40–05:00 · 88% · 6 kW", text)
    }

    @Test
    fun overnightHandlesFractionalKwClearedAndEmpty() {
        val half = HomeStatusParser.parse("""{"overnight":{"from":"03:00","to":"05:00","percent_limit":55,"kw":5.5}}""")
        assertEquals("03:00–05:00 · 55% · 5.5 kW", StatusFormatter.overnight(half.overnight))
        val cleared = HomeStatusParser.parse("""{"overnight":{"from":"02:40","to":"05:00","cleared":true}}""")
        assertEquals("None set", StatusFormatter.overnight(cleared.overnight))
        assertNull(HomeStatusParser.parse("""{"overnight":{}}""").overnight)
        assertNull(HomeStatusParser.parse("""{"overnight":null}""").overnight)
    }

    @Test
    fun peakWindowShowsTodayUntilItEnds() {
        val status = HomeStatusParser.parse(feed)
        assertEquals("2026-11-16", status.peakWindow?.date)
        assertEquals("14:22–16:00 · 75%", StatusFormatter.peakWindow(status.peakWindow, at("2026-11-16T14:30:00Z")))
        assertEquals("14:22–16:00 · 75%", StatusFormatter.peakWindow(status.peakWindow, at("2026-11-16T09:00:00Z")))
        assertEquals("—", StatusFormatter.peakWindow(status.peakWindow, at("2026-11-16T16:00:00Z")))
        assertEquals("—", StatusFormatter.peakWindow(status.peakWindow, at("2026-11-17T14:30:00Z")))
    }

    @Test
    fun peakWindowPrefersItsOwnDate() {
        val status = HomeStatusParser.parse(
            """{"updated_at":"2026-11-16T14:11:59+00:00","peak_window":{"from":"14:22","to":"16:00","target_percent":75,"date":"2026-11-12"}}"""
        )
        assertEquals("—", StatusFormatter.peakWindow(status.peakWindow, at("2026-11-16T14:30:00Z")))
    }

    @Test
    fun nextPowerUpNullIsADash() {
        val status = HomeStatusParser.parse(feed)
        assertNull(status.nextPowerUp)
        assertEquals("—", StatusFormatter.powerUpSpokenWindow(status.nextPowerUp))
    }

    @Test
    fun cachedJsonRoundTripKeepsPlanFields() {
        val status = HomeStatusParser.parse(feed)
        val again = HomeStatusParser.parse(HomeStatusJson.encode(status))
        assertEquals(status.overnight, again.overnight)
        assertEquals(status.peakWindow, again.peakWindow)
        assertEquals("02:40–05:00 · 88% · 6 kW", StatusFormatter.overnight(again.overnight))
    }

    @Test
    fun publicExtrasFillACachedSnapshotWithoutTouchingLiveNumbers() {
        val cached = HomeStatus(
            updated = "2026-11-16T14:10:00Z",
            socPercent = 96,
            batteryW = -331.0,
            socSeries = listOf(BatterySample(t = "2026-11-16T14:00:00Z", soc = 95.0))
        )
        val merged = LiveStatus.withPublicExtras(cached, HomeStatusParser.parse(feed))
        assertEquals(96, merged.socPercent)
        assertEquals(-331.0, merged.batteryW)
        assertEquals(cached.socSeries, merged.socSeries)
        assertEquals("02:40–05:00 · 88% · 6 kW", StatusFormatter.overnight(merged.overnight))
        assertEquals(75, merged.peakWindow?.targetPercent)
        assertEquals("Cloudy", merged.weatherTomorrow?.label)
    }

    @Test
    fun foregroundFloorStillReadsTheStatusFile() {
        val repo = File("src/main/java/com/zekkers/watthome/data/StatusRepository.kt").readText()
        assertTrue(repo.contains("return@withLock refreshPublicExtras(current.status, current)"))
        assertTrue(!repo.contains("return@withLock current.status\n"))
    }

    @Test
    fun repoStatusJsonOvernightRendersWhenPresent() {
        val file = File("../../status.json")
        if (!file.isFile) return
        val status = HomeStatusParser.parse(file.readText())
        if (status.overnight != null && status.overnight?.cleared != true) {
            assertNotEquals("—", StatusFormatter.overnight(status.overnight))
        }
    }
}
