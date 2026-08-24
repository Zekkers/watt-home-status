package com.zekkers.watthome.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.zekkers.watthome.data.BatterySample
import com.zekkers.watthome.data.HomeStatus
import com.zekkers.watthome.data.StatusFormatter
import kotlin.math.abs

object SparklineRenderer {
    private const val Background = 0xFF1B3A24.toInt()
    private const val Grid = 0x4481C784
    private const val Line = 0xFFE8F5E9.toInt()
    private const val Charge = 0xFF81C784.toInt()
    private const val Discharge = 0xFFF9A825.toInt()
    private const val MinutesInDay = 24 * 60.0
    /** Same plot shape as the 2×2 Glance tile (~260×90). */
    private const val PlotAspect = 260f / 90f

    fun renderToday(
        status: HomeStatus?,
        widthPx: Int,
        heightPx: Int,
        fillSlot: Boolean = false
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(widthPx.coerceAtLeast(8), heightPx.coerceAtLeast(8), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Background)
        val plot = if (fillSlot) {
            RectF(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat())
        } else {
            letterbox(bitmap.width.toFloat(), bitmap.height.toFloat(), PlotAspect)
        }
        if (status == null || widthPx < 8 || heightPx < 8) {
            drawGrid(canvas, plot.top, plot.width(), plot.height(), plot.left)
            return bitmap
        }
        val soc = status.socSeries.filter { it.t != null && it.soc != null }
        val watts = status.batteryWSeries.filter { it.t != null && it.w != null }
        val hasSoc = soc.size >= 2
        val hasWatts = watts.size >= 2
        if (!hasSoc && !hasWatts) {
            drawGrid(canvas, plot.top, plot.width(), plot.height(), plot.left)
            return bitmap
        }
        if (hasSoc && hasWatts) {
            val socBottom = plot.top + plot.height() * 0.82f
            val wattsTop = plot.top + plot.height() * 0.86f
            drawSoc(canvas, soc, plot.left, plot.top, socBottom, plot.width())
            drawSignedWatts(canvas, watts, plot.left, wattsTop, plot.bottom, plot.width())
            return bitmap
        }
        if (hasSoc) {
            drawSoc(canvas, soc, plot.left, plot.top, plot.bottom, plot.width())
            return bitmap
        }
        drawSignedWatts(canvas, watts, plot.left, plot.top, plot.bottom, plot.width())
        return bitmap
    }

    private fun letterbox(availW: Float, availH: Float, aspect: Float): RectF {
        var w = availW
        var h = w / aspect
        if (h > availH) {
            h = availH
            w = h * aspect
        }
        val left = (availW - w) / 2f
        val top = (availH - h) / 2f
        return RectF(left, top, left + w, top + h)
    }

    private fun drawSoc(
        canvas: Canvas,
        samples: List<BatterySample>,
        left: Float,
        top: Float,
        bottom: Float,
        width: Float
    ) {
        val height = (bottom - top).coerceAtLeast(1f)
        drawGrid(canvas, top, width, height, left)
        val pad = height * 0.14f
        val usable = (height - 2 * pad).coerceAtLeast(1f)
        val xs = xPositions(samples, left, width)
        val path = Path()
        samples.forEachIndexed { index, sample ->
            val soc = sample.soc!!.coerceIn(0.0, 100.0)
            val y = top + pad + ((100.0 - soc) / 100.0).toFloat() * usable
            if (index == 0) path.moveTo(xs[index], y) else path.lineTo(xs[index], y)
        }
        canvas.drawPath(path, linePaint(Line, 3.6f))
    }

    private fun drawSignedWatts(
        canvas: Canvas,
        samples: List<BatterySample>,
        left: Float,
        top: Float,
        bottom: Float,
        width: Float
    ) {
        val height = (bottom - top).coerceAtLeast(1f)
        drawGrid(canvas, top, width, height, left)
        val points = samples.map { it.w!! }
        val maxAbs = points.maxOf { abs(it) }.coerceAtLeast(1.0)
        val pad = height * 0.12f
        val midY = top + height / 2f
        val usable = ((height - 2 * pad) / 2f).coerceAtLeast(1f)
        val xs = xPositions(samples, left, width)
        fun y(value: Double): Float = midY - (value / maxAbs).toFloat() * usable
        val zeroPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Grid
            strokeWidth = 2f
        }
        canvas.drawLine(left, midY, left + width, midY, zeroPaint)
        val charge = linePaint(Charge, 2.6f)
        val discharge = linePaint(Discharge, 2.6f)
        for (i in 1 until samples.size) {
            val paint = if (points[i] >= 0 && points[i - 1] >= 0) charge else discharge
            canvas.drawLine(xs[i - 1], y(points[i - 1]), xs[i], y(points[i]), paint)
        }
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
