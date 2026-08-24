package com.zekkers.watthome

import com.zekkers.watthome.data.HomeStatus
import com.zekkers.watthome.data.StatusFormatter
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.ZoneOffset

class StatusFormatterTest {
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

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
        val status = json.decodeFromString(HomeStatus.serializer(), sample)
        assertEquals(64, status.socPercent)
        assertEquals(980, status.solarW)
        assertEquals(55, status.target1600Percent)
        assertEquals("02:00", status.overnight?.start)
        assertEquals("16:00-19:00", status.peakWindow)
        assertNull(status.nextPowerUp)
        assertEquals("None scheduled", StatusFormatter.nextPowerUp(status.nextPowerUp))
        assertEquals("02:00–03:00 · cap 30%", StatusFormatter.overnight(status.overnight))
        assertEquals("64%", StatusFormatter.percent(status.socPercent))
        assertEquals("980 W", StatusFormatter.watts(status.solarW))
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
}
