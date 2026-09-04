package com.zekkers.watthome

import com.zekkers.watthome.data.BatterySample
import com.zekkers.watthome.data.GraphSeriesSelection
import com.zekkers.watthome.data.HomeStatus
import com.zekkers.watthome.data.StatusFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GraphSeriesSelectionTest {
    @Test
    fun defaultsAreSolarAndBatteryOnly() {
        val defaults = GraphSeriesSelection.DEFAULT
        assertTrue(defaults.solar)
        assertTrue(defaults.battery)
        assertFalse(defaults.house)
        assertFalse(defaults.grid)
        assertFalse(defaults.soc)
        assertEquals("solar,battery", defaults.encode())
        assertEquals(defaults, GraphSeriesSelection.decode(null))
        assertEquals(defaults, GraphSeriesSelection.WIDGET_COMPACT)
    }

    @Test
    fun encodeDecodeRoundTripAndEmptyMeansAllOff() {
        val allOn = GraphSeriesSelection(
            solar = true,
            battery = true,
            house = true,
            grid = true,
            soc = true
        )
        assertEquals(allOn, GraphSeriesSelection.decode(allOn.encode()))
        val restored = GraphSeriesSelection.decode("soc, house, GRID")
        assertFalse(restored.solar)
        assertFalse(restored.battery)
        assertTrue(restored.house)
        assertTrue(restored.grid)
        assertTrue(restored.soc)
        val allOff = GraphSeriesSelection.decode("")
        assertFalse(allOff.any())
        assertEquals("", allOff.encode())
    }

    @Test
    fun visibleCurveRespectsPersistedTicks() {
        val solarOnly = HomeStatus(
            solarWSeries = listOf(
                BatterySample(t = "2026-08-26T10:00:00+01:00", w = 200.0),
                BatterySample(t = "2026-08-26T11:00:00+01:00", w = 400.0)
            ),
            socSeries = listOf(
                BatterySample(t = "2026-08-26T10:00:00+01:00", soc = 40.0),
                BatterySample(t = "2026-08-26T11:00:00+01:00", soc = 50.0)
            )
        )
        assertTrue(StatusFormatter.hasTodayCurve(solarOnly))
        assertTrue(StatusFormatter.hasVisibleTodayCurve(solarOnly, GraphSeriesSelection.DEFAULT))
        assertFalse(
            StatusFormatter.hasVisibleTodayCurve(
                solarOnly,
                GraphSeriesSelection(solar = false, battery = true, soc = false)
            )
        )
        assertTrue(
            StatusFormatter.hasVisibleTodayCurve(
                solarOnly,
                GraphSeriesSelection(solar = false, battery = false, soc = true)
            )
        )
    }
}
