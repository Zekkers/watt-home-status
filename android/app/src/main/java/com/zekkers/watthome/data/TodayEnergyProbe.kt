package com.zekkers.watthome.data

import android.graphics.Paint
import java.time.Instant
import java.util.Locale
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.round

/**
 * Hit testing for the Today’s energy sparkline. Coordinates match
 * [com.zekkers.watthome.widget.SparklineRenderer] so a touch lands on the
 * same point the bitmap draws.
 */
object TodayEnergyProbe {
    const val MinutesInDay = 24 * 60.0
    const val SnapGapMinutes = 20.0
    const val SiblingWindowMinutes = 8.0
    private const val IdleWatts = 0.5

    data class CurveLayout(
        val chart: WidgetPlotLayout.FloatBox,
        val zeroLabelWidthPx: Float
    )

    data class BitmapFit(
        val left: Float,
        val top: Float,
        val scale: Float
    )

    data class EnergyTooltip(
        val timeText: String?,
        val fromGraph: Boolean,
        val focusId: String,
        val anchorX: Float,
        val anchorY: Float,
        val readings: List<EnergyTooltipReading>
    ) {
        fun samePoint(other: EnergyTooltip): Boolean =
            fromGraph && other.fromGraph && focusId == other.focusId && timeText == other.timeText
    }

    data class EnergyTooltipReading(
        val id: String,
        val label: String,
        val valueText: String,
        val unit: String,
        val meaning: String,
        val color: Long,
        val focused: Boolean
    )

    private data class PlottedPoint(
        val id: String,
        val label: String,
        val minutes: Double?,
        val x: Float,
        val y: Float,
        val value: Double,
        val unit: String,
        val meaning: String,
        val valueText: String,
        val color: Long,
        val timestamp: String?
    )

    fun appBitmapSize(density: Float): Pair<Int, Int> {
        val width = (320f * density).toInt().coerceAtLeast(180)
        val height = (110f * density).toInt().coerceAtLeast(72)
        return width to height
    }

    fun wattSamples(samples: List<BatterySample>, enabled: Boolean): List<BatterySample> {
        if (!enabled) return emptyList()
        return samples.filter { it.t != null && it.w != null }
    }

    fun socSamples(samples: List<BatterySample>, enabled: Boolean): List<BatterySample> {
        if (!enabled) return emptyList()
        return samples.filter { it.t != null && it.soc != null }
    }

    fun layout(
        bitmapW: Float,
        bitmapH: Float,
        fillSlot: Boolean,
        showLegend: Boolean,
        seriesAny: Boolean,
        hasPower: Boolean,
        zeroLabelWidthPx: Float? = null
    ): CurveLayout {
        val plot = WidgetPlotLayout.plotBounds(bitmapW, bitmapH, fillSlot)
        val legendH = WidgetPlotLayout.legendHeightPx(plot.height, showLegend, seriesAny)
        val chartHeight = (plot.height - legendH).coerceAtLeast(1f)
        val labelWidth = zeroLabelWidthPx ?: measuredZeroLabelWidth(chartHeight)
        val gutter = WidgetPlotLayout.zeroGutterPx(hasPower, labelWidth)
        return CurveLayout(
            chart = WidgetPlotLayout.chartBounds(plot, legendH, gutter),
            zeroLabelWidthPx = labelWidth
        )
    }

    fun measuredZeroLabelWidth(chartHeightPx: Float): Float {
        val textPx = WidgetPlotLayout.zeroLabelTextPx(chartHeightPx)
        val measured = runCatching {
            Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = textPx }.measureText(WidgetPlotLayout.ZeroLabel)
        }.getOrDefault(0f)
        if (measured > 1f) return measured
        return WidgetTextMeasure.estimateWidthDp(WidgetPlotLayout.ZeroLabel, textPx, bold = false)
    }

    fun powerMaxAbs(
        solar: List<BatterySample>,
        battery: List<BatterySample>,
        house: List<BatterySample>,
        grid: List<BatterySample>
    ): Double {
        var max = 1.0
        fun take(samples: List<BatterySample>) {
            for (sample in samples) {
                val magnitude = abs(sample.w ?: continue)
                if (magnitude > max) max = magnitude
            }
        }
        take(solar)
        take(battery)
        take(house)
        take(grid)
        return max
    }

    fun xPositions(samples: List<BatterySample>, left: Float, width: Float): List<Float> {
        val minutes = samples.map { minutesFromMidnight(it.t) }
        if (minutes.all { it != null }) {
            return minutes.map { minute ->
                (left + (minute!! / MinutesInDay) * (width - 1)).toFloat()
            }
        }
        val last = samples.lastIndex.coerceAtLeast(1)
        return samples.indices.map { index -> left + index * (width - 1f) / last }
    }

    fun yForWatts(value: Double, top: Float, height: Float, maxAbs: Double): Float {
        val pad = height * 0.08f
        val midY = top + height / 2f
        val usable = ((height - 2 * pad) / 2f).coerceAtLeast(1f)
        val scale = if (maxAbs == 0.0) 0.0 else value / maxAbs
        return midY - scale.toFloat() * usable
    }

    fun yForSoc(soc: Double, top: Float, height: Float): Float {
        val pad = height * 0.08f
        val usable = (height - 2 * pad).coerceAtLeast(1f)
        val clamped = soc.coerceIn(0.0, 100.0)
        return top + pad + ((100.0 - clamped) / 100.0).toFloat() * usable
    }

    fun minutesFromMidnight(raw: String?): Double? {
        if (raw.isNullOrBlank()) return null
        val stamp = StatusFormatter.parseTimestamp(raw) ?: return null
        val local = stamp.atZoneSameInstant(StatusFormatter.london).toLocalTime()
        return local.hour * 60.0 + local.minute + local.second / 60.0
    }

    fun fitBitmap(bitmapW: Float, bitmapH: Float, viewW: Float, viewH: Float): BitmapFit {
        if (bitmapW <= 0f || bitmapH <= 0f || viewW <= 0f || viewH <= 0f) {
            return BitmapFit(0f, 0f, 1f)
        }
        val scale = min(viewW / bitmapW, viewH / bitmapH)
        val drawnW = bitmapW * scale
        val drawnH = bitmapH * scale
        return BitmapFit((viewW - drawnW) / 2f, (viewH - drawnH) / 2f, scale)
    }

    fun inspect(
        status: HomeStatus?,
        series: GraphSeriesSelection,
        viewWidthPx: Float,
        viewHeightPx: Float,
        density: Float,
        touchXPx: Float,
        touchYPx: Float,
        use24Hour: Boolean = true,
        showLegend: Boolean = false,
        fillSlot: Boolean = false,
        zeroLabelWidthPx: Float? = null
    ): EnergyTooltip? {
        if (status == null || viewWidthPx < 1f || viewHeightPx < 1f) return null
        val (bitmapW, bitmapH) = appBitmapSize(density)
        val solar = wattSamples(status.solarWSeries, series.solar)
        val battery = wattSamples(status.batteryWSeries, series.battery)
        val house = wattSamples(status.houseWSeries, series.house)
        val grid = wattSamples(status.gridWSeries, series.grid)
        val soc = socSamples(status.socSeries, series.soc)
        val hasPower = solar.size >= 2 || battery.size >= 2 || house.size >= 2 || grid.size >= 2
        val hasSoc = soc.size >= 2
        if (!hasPower && !hasSoc) return null
        val chart = layout(
            bitmapW = bitmapW.toFloat(),
            bitmapH = bitmapH.toFloat(),
            fillSlot = fillSlot,
            showLegend = showLegend,
            seriesAny = series.any(),
            hasPower = hasPower,
            zeroLabelWidthPx = zeroLabelWidthPx
        ).chart
        val points = plottedPoints(solar, battery, house, grid, soc, chart)
        if (points.isEmpty()) return null
        val fit = fitBitmap(bitmapW.toFloat(), bitmapH.toFloat(), viewWidthPx, viewHeightPx)
        val scale = fit.scale.coerceAtLeast(0.0001f)
        val bitmapX = (touchXPx - fit.left) / scale
        val bitmapY = (touchYPx - fit.top) / scale
        val slop = 24f / scale
        val inside = bitmapX >= chart.left - slop && bitmapX <= chart.right + slop &&
            bitmapY >= chart.top - slop && bitmapY <= chart.bottom + slop
        if (!inside) return null
        val gapPx = snapGapPx(points, chart.width)
        val candidates = points.filter { abs(it.x - bitmapX) <= gapPx }
        if (candidates.isEmpty()) return null
        val focus = candidates.minBy { point ->
            val dx = point.x - bitmapX
            val dy = point.y - bitmapY
            dx * dx + dy * dy
        }
        val moment = points.filter { sameMoment(it, focus, chart.width) }
            .groupBy { it.id }
            .map { (_, group) -> group.minBy { abs(it.x - focus.x) } }
        val order = listOf(
            GraphSeriesSelection.ID_SOLAR,
            GraphSeriesSelection.ID_BATTERY,
            GraphSeriesSelection.ID_HOUSE,
            GraphSeriesSelection.ID_GRID,
            GraphSeriesSelection.ID_SOC
        )
        val readings = moment
            .sortedWith(compareBy<PlottedPoint>({ if (it.id == focus.id) 0 else 1 }, { order.indexOf(it.id) }))
            .map { point ->
                EnergyTooltipReading(
                    id = point.id,
                    label = point.label,
                    valueText = point.valueText,
                    unit = point.unit,
                    meaning = point.meaning,
                    color = point.color,
                    focused = point.id == focus.id
                )
            }
        val anchorX = ((fit.left + focus.x * scale) / viewWidthPx).coerceIn(0f, 1f)
        val anchorY = ((fit.top + focus.y * scale) / viewHeightPx).coerceIn(0f, 1f)
        return EnergyTooltip(
            timeText = StatusFormatter.probeClock(focus.timestamp, use24Hour),
            fromGraph = true,
            focusId = focus.id,
            anchorX = anchorX,
            anchorY = anchorY,
            readings = readings
        )
    }

    fun latest(
        status: HomeStatus?,
        seriesId: String,
        use24Hour: Boolean = true
    ): EnergyTooltip {
        val spec = seriesSpec(seriesId)
        val sample = lastSample(status, seriesId)
        val reading = if (sample == null || spec == null) {
            EnergyTooltipReading(
                id = seriesId,
                label = spec?.label ?: seriesId,
                valueText = "—",
                unit = spec?.unit ?: "",
                meaning = neutralMeaning(seriesId),
                color = spec?.color ?: GraphSeriesStyle.SOLAR,
                focused = true
            )
        } else {
            val value = if (seriesId == GraphSeriesSelection.ID_SOC) sample.soc ?: 0.0 else sample.w ?: 0.0
            EnergyTooltipReading(
                id = seriesId,
                label = spec.label,
                valueText = numberText(value, spec.signed),
                unit = spec.unit,
                meaning = meaningFor(seriesId, value),
                color = colorFor(seriesId, value),
                focused = true
            )
        }
        return EnergyTooltip(
            timeText = sample?.t?.let { StatusFormatter.probeClock(it, use24Hour) },
            fromGraph = false,
            focusId = seriesId,
            anchorX = 0.5f,
            anchorY = 1f,
            readings = listOf(reading)
        )
    }

    private fun plottedPoints(
        solar: List<BatterySample>,
        battery: List<BatterySample>,
        house: List<BatterySample>,
        grid: List<BatterySample>,
        soc: List<BatterySample>,
        chart: WidgetPlotLayout.FloatBox
    ): List<PlottedPoint> {
        val points = mutableListOf<PlottedPoint>()
        val hasPower = solar.size >= 2 || battery.size >= 2 || house.size >= 2 || grid.size >= 2
        val maxAbs = if (hasPower) powerMaxAbs(solar, battery, house, grid) else 1.0
        fun addWatts(id: String, samples: List<BatterySample>) {
            if (samples.size < 2) return
            val spec = seriesSpec(id) ?: return
            val xs = xPositions(samples, chart.left, chart.width)
            samples.forEachIndexed { index, sample ->
                val value = sample.w ?: return@forEachIndexed
                points += PlottedPoint(
                    id = id,
                    label = spec.label,
                    minutes = minutesFromMidnight(sample.t),
                    x = xs[index],
                    y = yForWatts(value, chart.top, chart.height, maxAbs),
                    value = value,
                    unit = spec.unit,
                    meaning = meaningFor(id, value),
                    valueText = numberText(value, spec.signed),
                    color = colorFor(id, value),
                    timestamp = sample.t
                )
            }
        }
        addWatts(GraphSeriesSelection.ID_SOLAR, solar)
        addWatts(GraphSeriesSelection.ID_BATTERY, battery)
        addWatts(GraphSeriesSelection.ID_HOUSE, house)
        addWatts(GraphSeriesSelection.ID_GRID, grid)
        if (soc.size >= 2) {
            val spec = seriesSpec(GraphSeriesSelection.ID_SOC) ?: return points
            val xs = xPositions(soc, chart.left, chart.width)
            soc.forEachIndexed { index, sample ->
                val value = sample.soc ?: return@forEachIndexed
                points += PlottedPoint(
                    id = GraphSeriesSelection.ID_SOC,
                    label = spec.label,
                    minutes = minutesFromMidnight(sample.t),
                    x = xs[index],
                    y = yForSoc(value, chart.top, chart.height),
                    value = value,
                    unit = spec.unit,
                    meaning = meaningFor(GraphSeriesSelection.ID_SOC, value),
                    valueText = numberText(value, signed = false),
                    color = colorFor(GraphSeriesSelection.ID_SOC, value),
                    timestamp = sample.t
                )
            }
        }
        return points
    }

    private fun snapGapPx(points: List<PlottedPoint>, chartWidth: Float): Float {
        val timeBased = points.all { it.minutes != null }
        if (timeBased) {
            return ((SnapGapMinutes / MinutesInDay) * (chartWidth - 1f)).toFloat().coerceAtLeast(8f)
        }
        val xs = points.map { it.x }.sorted()
        val spacing = if (xs.size >= 2) (xs.last() - xs.first()) / (xs.size - 1) else chartWidth
        return (spacing / 2f).coerceAtLeast(24f)
    }

    private fun sameMoment(a: PlottedPoint, b: PlottedPoint, chartWidth: Float): Boolean {
        val aMinutes = a.minutes
        val bMinutes = b.minutes
        if (aMinutes != null && bMinutes != null) {
            return abs(aMinutes - bMinutes) <= SiblingWindowMinutes
        }
        val window = ((SiblingWindowMinutes / MinutesInDay) * (chartWidth - 1f)).toFloat().coerceAtLeast(4f)
        return abs(a.x - b.x) <= window
    }

    private fun lastSample(status: HomeStatus?, seriesId: String): BatterySample? {
        if (status == null) return null
        val samples = when (seriesId) {
            GraphSeriesSelection.ID_SOLAR -> wattSamples(status.solarWSeries, true)
            GraphSeriesSelection.ID_BATTERY -> wattSamples(status.batteryWSeries, true)
            GraphSeriesSelection.ID_HOUSE -> wattSamples(status.houseWSeries, true)
            GraphSeriesSelection.ID_GRID -> wattSamples(status.gridWSeries, true)
            GraphSeriesSelection.ID_SOC -> socSamples(status.socSeries, true)
            else -> emptyList()
        }
        if (samples.isEmpty()) return null
        // Rank by the full timestamp. Clock time alone treats 23:45 yesterday as
        // later than 00:20 today, including a live tip appended after midnight.
        return samples.withIndex().maxWith(
            compareBy<IndexedValue<BatterySample>> { indexed ->
                indexed.value.t?.let { StatusFormatter.parseTimestamp(it)?.toInstant() } ?: Instant.MIN
            }.thenBy { it.index }
        ).value
    }

    private data class SeriesSpec(
        val label: String,
        val unit: String,
        val signed: Boolean,
        val color: Long
    )

    private fun seriesSpec(id: String): SeriesSpec? = when (id) {
        GraphSeriesSelection.ID_SOLAR -> SeriesSpec("Solar", "W", false, GraphSeriesStyle.SOLAR)
        GraphSeriesSelection.ID_BATTERY -> SeriesSpec("Battery", "W", true, GraphSeriesStyle.BATTERY_CHARGE)
        GraphSeriesSelection.ID_HOUSE -> SeriesSpec("House", "W", false, GraphSeriesStyle.HOUSE)
        GraphSeriesSelection.ID_GRID -> SeriesSpec("Grid", "W", true, GraphSeriesStyle.GRID)
        GraphSeriesSelection.ID_SOC -> SeriesSpec("SOC", "%", false, GraphSeriesStyle.SOC_PLOT)
        else -> null
    }

    private fun colorFor(id: String, value: Double): Long = when {
        id == GraphSeriesSelection.ID_BATTERY && value < 0.0 -> GraphSeriesStyle.BATTERY_DISCHARGE
        id == GraphSeriesSelection.ID_SOC -> GraphSeriesStyle.SOC_PLOT
        else -> seriesSpec(id)?.color ?: GraphSeriesStyle.SOLAR
    }

    /**
     * Battery watts are the app sign: positive charges (green trace), negative discharges.
     * Grid watts are the inverter figure unchanged: positive imports, negative exports.
     */
    fun meaningFor(id: String, value: Double): String = when (id) {
        GraphSeriesSelection.ID_SOLAR -> "Power from the solar array"
        GraphSeriesSelection.ID_HOUSE -> "Power the house is using"
        GraphSeriesSelection.ID_BATTERY -> when {
            value > IdleWatts -> "Charging the battery"
            value < -IdleWatts -> "Discharging the battery"
            else -> "Battery idle"
        }
        GraphSeriesSelection.ID_GRID -> when {
            value > IdleWatts -> "Importing from the grid"
            value < -IdleWatts -> "Exporting to the grid"
            else -> "No grid flow"
        }
        GraphSeriesSelection.ID_SOC -> "How full the battery is"
        else -> neutralMeaning(id)
    }

    private fun neutralMeaning(id: String): String = when (id) {
        GraphSeriesSelection.ID_SOLAR -> "Power from the solar array"
        GraphSeriesSelection.ID_HOUSE -> "Power the house is using"
        GraphSeriesSelection.ID_BATTERY -> "Battery power. Positive is charging, negative is discharging"
        GraphSeriesSelection.ID_GRID -> "Grid power. Positive is import, negative is export"
        GraphSeriesSelection.ID_SOC -> "How full the battery is"
        else -> ""
    }

    fun numberText(value: Double, signed: Boolean): String {
        val nearest = round(value).toLong()
        val body = if (abs(value - nearest.toDouble()) < 0.05) {
            nearest.toString()
        } else {
            String.format(Locale.UK, "%.1f", value)
        }
        return if (signed && value > 0.0 && !body.startsWith("+")) "+$body" else body
    }
}
