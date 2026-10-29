package com.zekkers.watthome

import com.zekkers.watthome.data.BatterySample
import com.zekkers.watthome.data.GraphSeriesSelection
import com.zekkers.watthome.data.HomeStatus
import com.zekkers.watthome.data.StatusFormatter
import com.zekkers.watthome.data.TodayEnergyProbe
import com.zekkers.watthome.data.WidgetPlotLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class TodayEnergyProbeTest {
    private val noon = "2026-08-26T12:00:00+01:00"
    private val quarter = "2026-08-26T12:15:00+01:00"
    private val afternoonUtc = "2026-08-26T14:30:00Z"

    @Test
    fun probeClockIs24HourUnlessThePhoneSettingSaysOtherwise() {
        assertEquals("15:30", StatusFormatter.probeClock(afternoonUtc))
        assertEquals("15:30", StatusFormatter.probeClock(afternoonUtc, use24Hour = true))
        assertEquals("3:30pm", StatusFormatter.probeClock(afternoonUtc, use24Hour = false))
        assertEquals("12:00am", StatusFormatter.probeClock("2026-01-15T00:00:00Z", use24Hour = false))
        assertEquals("00:00", StatusFormatter.probeClock("2026-01-15T00:00:00Z", use24Hour = true))
    }

    @Test
    fun chartLeavesRoomForTheZeroLabelOnlyWhenPowerTracesExist() {
        val plot = WidgetPlotLayout.plotBounds(320f, 110f, fillSlot = false)
        val power = TodayEnergyProbe.layout(
            bitmapW = 320f,
            bitmapH = 110f,
            fillSlot = false,
            showLegend = false,
            seriesAny = true,
            hasPower = true,
            zeroLabelWidthPx = 20f
        )
        val socOnly = TodayEnergyProbe.layout(
            bitmapW = 320f,
            bitmapH = 110f,
            fillSlot = false,
            showLegend = false,
            seriesAny = true,
            hasPower = false,
            zeroLabelWidthPx = 20f
        )
        assertEquals(plot.left + 26f, power.chart.left, 0.01f)
        assertEquals(plot.left, socOnly.chart.left, 0.01f)
        assertEquals(plot.right, power.chart.right, 0.01f)
    }

    @Test
    fun xPositionUsesLondonMinutesAcrossTheDay() {
        val chartLeft = 27.111f
        val chartWidth = 291.666f
        val samples = listOf(BatterySample(t = noon, w = 420.0))
        val x = TodayEnergyProbe.xPositions(samples, chartLeft, chartWidth).single()
        assertEquals(172.444f, x, 0.05f)
        assertEquals(930.0, TodayEnergyProbe.minutesFromMidnight(afternoonUtc)!!, 0.01)
    }

    @Test
    fun tapOnSolarPointShowsExactWattsUnitMeaningAnd24HourTime() {
        val status = day(
            solar = listOf(sample(noon, 420.0), sample(quarter, 419.6))
        )
        val tip = touch(status, GraphSeriesSelection.DEFAULT, noon, 420.0)
        assertNotNull(tip)
        assertEquals("12:00", tip!!.timeText)
        assertTrue(tip.fromGraph)
        val solar = tip.readings.first { it.id == GraphSeriesSelection.ID_SOLAR }
        assertEquals("420", solar.valueText)
        assertEquals("419.6", TodayEnergyProbe.numberText(419.6, signed = false))
        assertEquals("W", solar.unit)
        assertEquals("Power from the solar array", solar.meaning)
        assertTrue(solar.focused)
    }

    @Test
    fun twelveHourSettingKeepsTheMinute() {
        val status = day(solar = listOf(sample(afternoonUtc, 10.0), sample(quarter, 12.0)))
        val series = GraphSeriesSelection(solar = true, battery = false)
        val tip = touch(status, series, afternoonUtc, 10.0, use24Hour = false)
        assertEquals("3:30pm", tip!!.timeText)
        assertEquals("10", tip.readings.single().valueText)
    }

    @Test
    fun nearestLineIsFocusedAndTheOtherSeriesAtThatTimeIsIncluded() {
        val status = day(
            solar = listOf(sample(noon, 420.0), sample(quarter, 400.0)),
            battery = listOf(sample(noon, -400.0), sample(quarter, -380.0))
        )
        val solarTip = touch(status, GraphSeriesSelection.DEFAULT, noon, 420.0)
        assertEquals(GraphSeriesSelection.ID_SOLAR, solarTip!!.focusId)
        assertEquals(GraphSeriesSelection.ID_SOLAR, solarTip.readings.first().id)
        val battery = solarTip.readings.first { it.id == GraphSeriesSelection.ID_BATTERY }
        assertEquals("-400", battery.valueText)
        assertEquals("W", battery.unit)
        assertEquals("Discharging the battery", battery.meaning)

        val charge = touch(
            day(battery = listOf(sample(noon, 800.0), sample(quarter, 700.0))),
            GraphSeriesSelection(solar = false, battery = true),
            noon,
            800.0
        )
        val charging = charge!!.readings.single()
        assertEquals("+800", charging.valueText)
        assertEquals("Charging the battery", charging.meaning)
    }

    @Test
    fun gridAndSocDescribeImportExportAndStateOfCharge() {
        val status = day(
            grid = listOf(sample(noon, 200.0), sample(quarter, -14.0)),
            soc = listOf(
                BatterySample(t = noon, soc = 63.4),
                BatterySample(t = quarter, soc = 64.0)
            )
        )
        val series = GraphSeriesSelection(solar = false, battery = false, grid = true, soc = true)
        val imported = touch(status, series, noon, 200.0, seriesId = GraphSeriesSelection.ID_GRID)
        val grid = imported!!.readings.first { it.id == GraphSeriesSelection.ID_GRID }
        assertEquals("+200", grid.valueText)
        assertEquals("W", grid.unit)
        assertEquals("Importing from the grid", grid.meaning)
        val soc = imported.readings.first { it.id == GraphSeriesSelection.ID_SOC }
        assertEquals("63.4", soc.valueText)
        assertEquals("%", soc.unit)
        assertEquals("How full the battery is", soc.meaning)

        val exported = touch(status, series, quarter, -14.0, seriesId = GraphSeriesSelection.ID_GRID)
        assertEquals("Exporting to the grid", exported!!.readings.first { it.id == GraphSeriesSelection.ID_GRID }.meaning)
    }

    @Test
    fun aTouchInTheEmptyEveningDoesNotInventAPoint() {
        val status = day(solar = listOf(sample(noon, 420.0), sample(quarter, 400.0)))
        val (x, y) = pointOnChart(status, GraphSeriesSelection.DEFAULT, "2026-08-26T23:00:00+01:00", 0.0)
        val tip = TodayEnergyProbe.inspect(
            status = status,
            series = GraphSeriesSelection.DEFAULT,
            viewWidthPx = 320f,
            viewHeightPx = 110f,
            density = 1f,
            touchXPx = x,
            touchYPx = y,
            zeroLabelWidthPx = 20f
        )
        assertNull(tip)
    }

    @Test
    fun disabledSeriesStayOutOfTheTooltip() {
        val status = day(
            solar = listOf(sample(noon, 420.0), sample(quarter, 400.0)),
            battery = listOf(sample(noon, -50.0), sample(quarter, -40.0))
        )
        val tip = touch(
            status,
            GraphSeriesSelection(solar = true, battery = false),
            noon,
            420.0
        )
        assertEquals(listOf(GraphSeriesSelection.ID_SOLAR), tip!!.readings.map { it.id })
    }

    @Test
    fun legendTapUsesTheLatestReadingNotTheSeriesTick() {
        val status = day(
            solar = listOf(sample(quarter, 390.0), sample(noon, 420.0))
        )
        val tip = TodayEnergyProbe.latest(status, GraphSeriesSelection.ID_SOLAR)
        assertEquals("12:15", tip.timeText)
        assertEquals("390", tip.readings.single().valueText)
        assertEquals("W", tip.readings.single().unit)
        assertEquals("Power from the solar array", tip.readings.single().meaning)
        val missing = TodayEnergyProbe.latest(status, GraphSeriesSelection.ID_HOUSE)
        assertNull(missing.timeText)
        assertEquals("—", missing.readings.single().valueText)
        assertEquals("Power the house is using", missing.readings.single().meaning)
    }

    @Test
    fun letterboxedViewStillHitsTheSameSample() {
        val status = day(solar = listOf(sample(noon, 420.0), sample(quarter, 400.0)))
        val (bitmapX, bitmapY) = pointOnChart(status, GraphSeriesSelection.DEFAULT, noon, 420.0)
        val fit = TodayEnergyProbe.fitBitmap(320f, 110f, 260f, 90f)
        val tip = TodayEnergyProbe.inspect(
            status = status,
            series = GraphSeriesSelection.DEFAULT,
            viewWidthPx = 260f,
            viewHeightPx = 90f,
            density = 1f,
            touchXPx = fit.left + bitmapX * fit.scale,
            touchYPx = fit.top + bitmapY * fit.scale,
            zeroLabelWidthPx = 20f
        )
        assertEquals("420", tip!!.readings.single().valueText)
        assertEquals("12:00", tip.timeText)
    }

    private fun touch(
        status: HomeStatus,
        series: GraphSeriesSelection,
        timestamp: String,
        watts: Double,
        use24Hour: Boolean = true,
        seriesId: String = GraphSeriesSelection.ID_SOLAR
    ): TodayEnergyProbe.EnergyTooltip? {
        val (x, y) = pointOnChart(status, series, timestamp, watts, seriesId)
        return TodayEnergyProbe.inspect(
            status = status,
            series = series,
            viewWidthPx = 320f,
            viewHeightPx = 110f,
            density = 1f,
            touchXPx = x,
            touchYPx = y,
            use24Hour = use24Hour,
            zeroLabelWidthPx = 20f
        )
    }

    private fun pointOnChart(
        status: HomeStatus,
        series: GraphSeriesSelection,
        timestamp: String,
        watts: Double,
        seriesId: String = GraphSeriesSelection.ID_SOLAR
    ): Pair<Float, Float> {
        val solar = TodayEnergyProbe.wattSamples(status.solarWSeries, series.solar)
        val battery = TodayEnergyProbe.wattSamples(status.batteryWSeries, series.battery)
        val house = TodayEnergyProbe.wattSamples(status.houseWSeries, series.house)
        val grid = TodayEnergyProbe.wattSamples(status.gridWSeries, series.grid)
        val hasPower = solar.size >= 2 || battery.size >= 2 || house.size >= 2 || grid.size >= 2
        val chart = TodayEnergyProbe.layout(
            bitmapW = 320f,
            bitmapH = 110f,
            fillSlot = false,
            showLegend = false,
            seriesAny = series.any(),
            hasPower = hasPower,
            zeroLabelWidthPx = 20f
        ).chart
        val samples = when (seriesId) {
            GraphSeriesSelection.ID_BATTERY -> battery
            GraphSeriesSelection.ID_HOUSE -> house
            GraphSeriesSelection.ID_GRID -> grid
            GraphSeriesSelection.ID_SOC -> TodayEnergyProbe.socSamples(status.socSeries, series.soc)
            else -> solar
        }
        val xs = TodayEnergyProbe.xPositions(samples, chart.left, chart.width)
        val index = samples.indexOfFirst { it.t == timestamp }
        val x = if (index >= 0) {
            xs[index]
        } else {
            val minutes = TodayEnergyProbe.minutesFromMidnight(timestamp) ?: 0.0
            (chart.left + (minutes / TodayEnergyProbe.MinutesInDay) * (chart.width - 1f)).toFloat()
        }
        val y = if (seriesId == GraphSeriesSelection.ID_SOC) {
            TodayEnergyProbe.yForSoc(samples.getOrNull(index)?.soc ?: 0.0, chart.top, chart.height)
        } else if (!hasPower) {
            chart.midY
        } else {
            TodayEnergyProbe.yForWatts(
                watts,
                chart.top,
                chart.height,
                TodayEnergyProbe.powerMaxAbs(solar, battery, house, grid)
            )
        }
        assertTrue(abs(x) < 10_000f)
        return x to y
    }

    private fun day(
        solar: List<BatterySample> = emptyList(),
        battery: List<BatterySample> = emptyList(),
        house: List<BatterySample> = emptyList(),
        grid: List<BatterySample> = emptyList(),
        soc: List<BatterySample> = emptyList()
    ) = HomeStatus(
        solarWSeries = solar,
        batteryWSeries = battery,
        houseWSeries = house,
        gridWSeries = grid,
        socSeries = soc
    )

    private fun sample(t: String, w: Double) = BatterySample(t = t, w = w)
}
