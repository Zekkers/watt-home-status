package com.zekkers.watthome.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.zekkers.watthome.data.BatterySample
import com.zekkers.watthome.data.GraphSeriesSelection
import com.zekkers.watthome.data.GraphSeriesStyle
import com.zekkers.watthome.data.HomeStatus
import com.zekkers.watthome.data.StatusFormatter
import com.zekkers.watthome.data.WidgetPlotLayout
import kotlin.math.abs

object SparklineRenderer {
    private const val Background = 0xFF1B3A24.toInt()
    private const val Grid = 0x4481C784
    private const val ZeroLabelColor = 0xFFE8F5E9.toInt()
    private const val MinutesInDay = 24 * 60.0

    fun renderToday(
        status: HomeStatus?,
        widthPx: Int,
        heightPx: Int,
        fillSlot: Boolean = false,
        series: GraphSeriesSelection = GraphSeriesSelection.DEFAULT,
        showLegend: Boolean = !fillSlot
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(widthPx.coerceAtLeast(8), heightPx.coerceAtLeast(8), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Background)
        val plotBox = WidgetPlotLayout.plotBounds(
            bitmap.width.toFloat(),
            bitmap.height.toFloat(),
            fillSlot
        )
        val plot = plotBox.toRect()
        if (status == null || widthPx < 8 || heightPx < 8) {
            drawGrid(canvas, plot.top, plot.width(), plot.height(), plot.left)
            return bitmap
        }
        val solar = wattSamples(status.solarWSeries, series.solar)
        val battery = wattSamples(status.batteryWSeries, series.battery)
        val house = wattSamples(status.houseWSeries, series.house)
        val grid = wattSamples(status.gridWSeries, series.grid)
        val soc = if (series.soc) {
            status.socSeries.filter { it.t != null && it.soc != null }
        } else {
            emptyList()
        }
        val hasPower = solar.size >= 2 || battery.size >= 2 || house.size >= 2 || grid.size >= 2
        val hasSoc = soc.size >= 2
        val legendH = WidgetPlotLayout.legendHeightPx(plotBox.height, showLegend, series.any())
        val labelPaint = zeroLabelPaint(plotBox.height - legendH)
        val gutter = WidgetPlotLayout.zeroGutterPx(hasPower, labelPaint.measureText(WidgetPlotLayout.ZeroLabel))
        val chartBox = WidgetPlotLayout.chartBounds(plotBox, legendH, gutter)
        val chart = chartBox.toRect()
        if (!hasPower && !hasSoc) {
            drawGrid(canvas, chart.top, chart.width(), chart.height(), chart.left)
            if (legendH > 0f) drawLegend(canvas, plot, series, legendH)
            return bitmap
        }
        drawGrid(canvas, chart.top, chart.width(), chart.height(), chart.left)
        if (hasPower) {
            val values = buildList {
                addAll(solar.map { it.w!! })
                addAll(battery.map { it.w!! })
                addAll(house.map { it.w!! })
                addAll(grid.map { it.w!! })
            }
            val maxAbs = values.maxOf { abs(it) }.coerceAtLeast(1.0)
            drawZeroLine(canvas, chart)
            drawZeroLabel(canvas, plot.left, chartBox.midY, labelPaint)
            if (solar.size >= 2) drawPowerLine(canvas, solar, chart, maxAbs, GraphSeriesStyle.SOLAR.toInt())
            if (house.size >= 2) drawPowerLine(canvas, house, chart, maxAbs, GraphSeriesStyle.HOUSE.toInt())
            if (grid.size >= 2) drawPowerLine(canvas, grid, chart, maxAbs, GraphSeriesStyle.GRID.toInt())
            if (battery.size >= 2) drawSignedBattery(canvas, battery, chart, maxAbs)
        }
        if (hasSoc) {
            drawSocOverlay(canvas, soc, chart)
        }
        if (legendH > 0f) drawLegend(canvas, plot, series, legendH)
        return bitmap
    }

    private fun wattSamples(samples: List<BatterySample>, enabled: Boolean): List<BatterySample> {
        if (!enabled) return emptyList()
        return samples.filter { it.t != null && it.w != null }
    }

    private fun WidgetPlotLayout.FloatBox.toRect(): RectF = RectF(left, top, right, bottom)

    private fun zeroLabelPaint(chartHeightPx: Float): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ZeroLabelColor
        textSize = WidgetPlotLayout.zeroLabelTextPx(chartHeightPx)
    }

    private fun drawZeroLabel(canvas: Canvas, left: Float, zeroY: Float, paint: Paint) {
        val baseline = zeroY - (paint.fontMetrics.ascent + paint.fontMetrics.descent) / 2f
        canvas.drawText(WidgetPlotLayout.ZeroLabel, left + 1f, baseline, paint)
    }

    private fun drawSocOverlay(canvas: Canvas, samples: List<BatterySample>, chart: RectF) {
        val pad = chart.height() * 0.08f
        val usable = (chart.height() - 2 * pad).coerceAtLeast(1f)
        val xs = xPositions(samples, chart.left, chart.width())
        val path = Path()
        samples.forEachIndexed { index, sample ->
            val soc = sample.soc!!.coerceIn(0.0, 100.0)
            val y = chart.top + pad + ((100.0 - soc) / 100.0).toFloat() * usable
            if (index == 0) path.moveTo(xs[index], y) else path.lineTo(xs[index], y)
        }
        canvas.drawPath(path, linePaint(GraphSeriesStyle.SOC_PLOT.toInt(), 2.8f))
    }

    private fun drawPowerLine(
        canvas: Canvas,
        samples: List<BatterySample>,
        chart: RectF,
        maxAbs: Double,
        color: Int
    ) {
        val xs = xPositions(samples, chart.left, chart.width())
        val path = Path()
        samples.forEachIndexed { index, sample ->
            val y = yForWatts(sample.w!!, chart, maxAbs)
            if (index == 0) path.moveTo(xs[index], y) else path.lineTo(xs[index], y)
        }
        canvas.drawPath(path, linePaint(color, 2.5f))
    }

    private fun drawSignedBattery(
        canvas: Canvas,
        samples: List<BatterySample>,
        chart: RectF,
        maxAbs: Double
    ) {
        val xs = xPositions(samples, chart.left, chart.width())
        val points = samples.map { it.w!! }
        val charge = linePaint(GraphSeriesStyle.BATTERY_CHARGE.toInt(), 2.6f)
        val discharge = linePaint(GraphSeriesStyle.BATTERY_DISCHARGE.toInt(), 2.6f)
        for (i in 1 until samples.size) {
            val paint = if (points[i] >= 0 && points[i - 1] >= 0) charge else discharge
            canvas.drawLine(
                xs[i - 1],
                yForWatts(points[i - 1], chart, maxAbs),
                xs[i],
                yForWatts(points[i], chart, maxAbs),
                paint
            )
        }
    }

    private fun yForWatts(value: Double, chart: RectF, maxAbs: Double): Float {
        val pad = chart.height() * 0.08f
        val midY = chart.top + chart.height() / 2f
        val usable = ((chart.height() - 2 * pad) / 2f).coerceAtLeast(1f)
        return midY - (value / maxAbs).toFloat() * usable
    }

    private fun drawZeroLine(canvas: Canvas, chart: RectF) {
        val midY = chart.top + chart.height() / 2f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Grid
            strokeWidth = 2f
        }
        canvas.drawLine(chart.left, midY, chart.right, midY, paint)
    }

    private fun xPositions(samples: List<BatterySample>, left: Float, width: Float): List<Float> {
        val minutes = samples.map { minutesFromMidnight(it.t) }
        if (minutes.all { it != null }) {
            return minutes.map { (left + (it!! / MinutesInDay) * (width - 1)).toFloat() }
        }
        val last = samples.lastIndex.coerceAtLeast(1)
        return samples.indices.map { left + it * (width - 1) / last }
    }

    private fun minutesFromMidnight(raw: String?): Double? {
        if (raw.isNullOrBlank()) return null
        val stamp = StatusFormatter.parseTimestamp(raw) ?: return null
        val local = stamp.atZoneSameInstant(StatusFormatter.london).toLocalTime()
        return local.hour * 60.0 + local.minute + local.second / 60.0
    }

    private fun drawLegend(
        canvas: Canvas,
        plot: RectF,
        series: GraphSeriesSelection,
        height: Float
    ) {
        val items = buildList {
            if (series.solar) add(LegendItem("Solar", GraphSeriesStyle.SOLAR.toInt()))
            if (series.battery) add(LegendItem("Battery", GraphSeriesStyle.BATTERY_CHARGE.toInt()))
            if (series.house) add(LegendItem("House", GraphSeriesStyle.HOUSE.toInt()))
            if (series.grid) add(LegendItem("Grid", GraphSeriesStyle.GRID.toInt()))
            if (series.soc) add(LegendItem("SOC", GraphSeriesStyle.SOC_PLOT.toInt()))
        }
        if (items.isEmpty()) return
        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xDDE8F5E9.toInt()
            textSize = (height * 0.55f).coerceIn(9f, 14f)
        }
        val swatch = Paint(Paint.ANTI_ALIAS_FLAG)
        val gap = 10f
        val dot = (text.textSize * 0.45f).coerceAtLeast(3f)
        var x = plot.left + 2f
        val baseline = plot.top + height * 0.72f
        val cy = baseline - text.textSize * 0.32f
        items.forEach { item ->
            swatch.color = item.color
            canvas.drawCircle(x + dot, cy, dot, swatch)
            x += dot * 2f + 4f
            canvas.drawText(item.label, x, baseline, text)
            x += text.measureText(item.label) + gap
        }
    }

    private data class LegendItem(val label: String, val color: Int)

    private fun drawGrid(
        canvas: Canvas,
        top: Float,
        width: Float,
        height: Float,
        left: Float = 0f
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Grid
            strokeWidth = 1.2f
        }
        val step = height / 4f
        for (i in 1..3) {
            val y = top + i * step
            canvas.drawLine(left, y, left + width, y, paint)
        }
    }

    private fun linePaint(color: Int, stroke: Float): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        strokeWidth = stroke
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
}
