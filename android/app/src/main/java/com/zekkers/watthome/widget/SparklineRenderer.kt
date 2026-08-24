package com.zekkers.watthome.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import com.zekkers.watthome.data.BatterySample
import com.zekkers.watthome.data.HomeStatus
import com.zekkers.watthome.data.StatusFormatter
import java.time.LocalTime
import kotlin.math.abs

object SparklineRenderer {
    private const val Background = 0xFF1B3A24.toInt()
    private const val Grid = 0x4481C784
    private const val Line = 0xFFE8F5E9.toInt()
    private const val Charge = 0xFF81C784.toInt()
    private const val Discharge = 0xFFF9A825.toInt()
    private const val MinutesInDay = 24 * 60.0

    fun renderToday(status: HomeStatus?, widthPx: Int, heightPx: Int): Bitmap {
        val empty = Bitmap.createBitmap(widthPx.coerceAtLeast(8), heightPx.coerceAtLeast(8), Bitmap.Config.ARGB_8888)
        if (status == null || widthPx < 8 || heightPx < 8) {
            drawEmpty(empty)
            return empty
        }
        val soc = status.socSeries.filter { it.soc != null }
        if (soc.size >= 2) {
            renderUnsigned(empty, soc) { it.soc!! }
            return empty
        }
        val watts = status.batteryWSeries.filter { it.w != null }
        if (watts.size >= 2) {
            renderSigned(empty, watts)
            return empty
        }
        drawEmpty(empty)
        return empty
    }

    private fun renderUnsigned(bitmap: Bitmap, samples: List<BatterySample>, value: (BatterySample) -> Double) {
        val canvas = Canvas(bitmap)
        canvas.drawColor(Background)
        drawGrid(canvas, bitmap.width, bitmap.height)
        val pad = bitmap.height * 0.16f
        val usable = (bitmap.height - 2 * pad).coerceAtLeast(1f)
        val xs = xPositions(samples, bitmap.width)
        val paint = linePaint(Line)
        val path = Path()
        samples.forEachIndexed { index, sample ->
            val y = pad + ((100.0 - value(sample).coerceIn(0.0, 100.0)) / 100.0).toFloat() * usable
            if (index == 0) path.moveTo(xs[index], y) else path.lineTo(xs[index], y)
        }
        canvas.drawPath(path, paint)
    }

    private fun renderSigned(bitmap: Bitmap, samples: List<BatterySample>) {
        val canvas = Canvas(bitmap)
        canvas.drawColor(Background)
        drawGrid(canvas, bitmap.width, bitmap.height)
        val points = samples.map { it.w!! }
        val maxAbs = points.maxOf { abs(it) }.coerceAtLeast(1.0)
        val pad = bitmap.height * 0.12f
        val midY = bitmap.height / 2f
        val usable = ((bitmap.height - 2 * pad) / 2f).coerceAtLeast(1f)
        val xs = xPositions(samples, bitmap.width)
        fun y(value: Double): Float = midY - (value / maxAbs).toFloat() * usable
        val zeroPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Grid
            strokeWidth = 1.5f
        }
        canvas.drawLine(0f, midY, bitmap.width.toFloat(), midY, zeroPaint)
        val charge = linePaint(Charge)
        val discharge = linePaint(Discharge)
        for (i in 1 until samples.size) {
            val paint = if (points[i] >= 0 && points[i - 1] >= 0) charge else discharge
            canvas.drawLine(xs[i - 1], y(points[i - 1]), xs[i], y(points[i]), paint)
        }
    }

    private fun xPositions(samples: List<BatterySample>, width: Int): List<Float> {
        val minutes = samples.map { minutesFromMidnight(it.t) }
        if (minutes.all { it != null }) {
            return minutes.map { ((it!! / MinutesInDay) * (width - 1)).toFloat() }
        }
        val last = samples.lastIndex.coerceAtLeast(1)
        return samples.indices.map { it * (width - 1).toFloat() / last }
    }

    private fun minutesFromMidnight(raw: String?): Double? {
        if (raw.isNullOrBlank()) return null
        StatusFormatter.parseTimestamp(raw)?.let { stamp ->
            val local = stamp.atZoneSameInstant(StatusFormatter.london).toLocalTime()
            return local.hour * 60.0 + local.minute + local.second / 60.0
        }
        return runCatching {
            val local = LocalTime.parse(raw.trim())
            local.hour * 60.0 + local.minute + local.second / 60.0
        }.getOrNull()
    }

    private fun drawEmpty(bitmap: Bitmap) {
        val canvas = Canvas(bitmap)
        canvas.drawColor(Background)
        drawGrid(canvas, bitmap.width, bitmap.height)
    }

    private fun drawGrid(canvas: Canvas, width: Int, height: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Grid
            strokeWidth = 1.2f
        }
        val step = height / 4f
        for (i in 1..3) {
            val y = i * step
            canvas.drawLine(0f, y, width.toFloat(), y, paint)
        }
    }

    private fun linePaint(color: Int): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        strokeWidth = 3.4f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
}
