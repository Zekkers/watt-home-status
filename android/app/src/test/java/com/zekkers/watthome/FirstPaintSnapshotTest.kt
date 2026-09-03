package com.zekkers.watthome

import com.zekkers.watthome.data.BatterySample
import com.zekkers.watthome.data.FirstPaintSnapshot
import com.zekkers.watthome.data.HomeStatus
import com.zekkers.watthome.data.HomeStatusParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FirstPaintSnapshotTest {
    @Test
    fun firstPaintKeepsSocSolarHouseAndUpdatedWithoutSeries() {
        val status = HomeStatus(
            updated = "2026-09-03T15:14:27+01:00",
            socPercent = 100,
            solarW = 1851,
            houseW = 406,
            batteryW = 47.0,
            socSeries = listOf(
                BatterySample(t = "2026-09-03T14:00:00+01:00", soc = 90.0),
                BatterySample(t = "2026-09-03T15:00:00+01:00", soc = 100.0)
            )
        )
        val raw = FirstPaintSnapshot.encode(status)
        assertFalse(raw.contains("soc_series"))
        assertFalse(raw.contains("battery_w_series"))
        val painted = FirstPaintSnapshot.decode(raw)
        assertEquals(100, painted.socPercent)
        assertEquals(1851, painted.solarW)
        assertEquals(406, painted.houseW)
        assertEquals(47.0, painted.batteryW)
        assertEquals("2026-09-03T15:14:27+01:00", painted.updated)
        assertTrue(painted.socSeries.isEmpty())
        assertTrue(painted.batteryWSeries.isEmpty())
        assertTrue(painted.solarWSeries.isEmpty())
        assertTrue(painted.houseWSeries.isEmpty())
        assertTrue(painted.gridWSeries.isEmpty())
    }

    @Test
    fun publicFeedHouseWattsRoundTripThroughFullParser() {
        val status = HomeStatusParser.parse(
            """{"updated":"2026-09-03T15:14:27+01:00","soc_percent":100,"solar_w":1851,"house_w":406}"""
        )
        assertEquals(406, status.houseW)
        val compact = FirstPaintSnapshot.decode(FirstPaintSnapshot.encode(status))
        assertEquals(100, compact.socPercent)
        assertEquals(1851, compact.solarW)
        assertEquals(406, compact.houseW)
    }
}
