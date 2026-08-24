package com.zekkers.watthome.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import com.zekkers.watthome.data.HomeStatus
import kotlin.math.abs

object SparklineRenderer {
    private const val Background = 0xFF1B3A24.toInt()
    private const val Grid = 0x4481C784
    private const val Line = 0xFFE8F5E9.toInt()
    private const val Charge = 0xFF81C784.toInt()
    private const val Discharge = 0xFFF9A825.toInt()

    fun renderToday(status: HomeStatus?, widthPx: Int, heightPx: Int): Bitmap {
        val empty = Bitmap.createBitmap(widthPx.coerceAtLeast(8), heightPx.coerceAtLeast(8), Bitmap.Config.ARGB_8888)
        if (status == null || widthPx < 8 || heightPx < 8) {
            drawEmpty(empty)
            return empty
        }
        val soc = status.socSeries.mapNotNull { it.soc }
        if (soc.size >= 2) {
            renderUnsigned(empty, soc, minY = 0.0, maxY = 100.0)
            return empty
        }
        val watts = status.batteryWSeries.mapNotNull { it.w }
        if (watts.size >= 2) {
            renderSigned(empty, watts)
            return empty
        }
        drawEmpty(empty)
        return empty
    }

    private fun renderUnsigned(bitmap: Bitmap, points: List<Double>, minY: Double, maxY: Double) {
        val canvas = Canvas(bitmap)
        canvas.drawColor(Background)
        drawGrid(canvas, bitmap.width, bitmap.height)
        val pad = bitmap.height * 0.16f
        val usable = (bitmap.height - 2 * pad).coerceAtLeast(1f)
        val span = (maxY - minY).coerceAtLeast(1.0)
        val dx = (bitmap.width - 1).toFloat() / points.lastIndex
        val paint = linePaint(Line)
        val path = Path()
        points.forEachIndexed { index, value ->
            val x = index * dx
            val y = pad + ((maxY - value) / span).toFloat() * usable
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        canvas.drawPath(path, paint)
    }

    private fun renderSigned(bitmap: Bitmap, points: List<Double>) {
        val canvas = Canvas(bitmap)
        canvas.drawColor(Background)
        drawGrid(canvas, bitmap.width, bitmap.height)
        val maxAbs = points.maxOf { abs(it) }.coerceAtLeast(1.0)
        val pad = bitmap.height * 0.12f
        val midY = bitmap.height / 2f
        val usable = ((bitmap.height - 2 * pad) / 2f).coerceAtLeast(1f)
        val dx = (bitmap.width - 1).toFloat() / points.lastIndex
        fun y(value: Double): Float = midY - (value / maxAbs).toFloat() * usable
        val zeroPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Grid
            strokeWidth = 1.5f
        }
        canvas.drawLine(0f, midY, bitmap.width.toFloat(), midY, zeroPaint)
        val charge = linePaint(Charge)
        val discharge = linePaint(Discharge)
        for (i in 1 until points.size) {
            val paint = if (points[i] >= 0 && points[i - 1] >= 0) charge else discharge
            canvas.drawLine((i - 1) * dx, y(points[i - 1]), i * dx, y(points[i]), paint)
        }
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
