package com.zekkers.watthome.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import com.zekkers.watthome.data.BatterySample
import kotlin.math.abs

object SparklineRenderer {
    fun render(series: List<BatterySample>, widthPx: Int, heightPx: Int): Bitmap? {
        val points = series.mapNotNull { it.w }
        if (points.size < 2 || widthPx < 8 || heightPx < 8) return null

        val maxAbs = points.maxOf { abs(it) }.coerceAtLeast(1.0)
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val pad = heightPx * 0.12f
        val midY = heightPx / 2f
        val usable = ((heightPx - 2 * pad) / 2f).coerceAtLeast(1f)
        val dx = (widthPx - 1).toFloat() / (points.lastIndex)

        val zeroPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x66A5D6A7
            strokeWidth = 1.5f
        }
        canvas.drawLine(0f, midY, widthPx.toFloat(), midY, zeroPaint)

        val chargePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF81C784.toInt()
            strokeWidth = 3.2f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        val dischargePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFF9A825.toInt()
            strokeWidth = 3.2f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        fun y(value: Double): Float = midY - (value / maxAbs).toFloat() * usable

        val fillPath = Path()
        fillPath.moveTo(0f, midY)
        points.forEachIndexed { index, value ->
            val x = index * dx
            if (index == 0) fillPath.lineTo(x, y(value)) else fillPath.lineTo(x, y(value))
        }
        fillPath.lineTo((points.lastIndex) * dx, midY)
        fillPath.close()
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x334CAF50
            style = Paint.Style.FILL
        }
        canvas.drawPath(fillPath, fillPaint)

        for (i in 1 until points.size) {
            val paint = if (points[i] >= 0 && points[i - 1] >= 0) chargePaint else dischargePaint
            canvas.drawLine(
                (i - 1) * dx,
                y(points[i - 1]),
                i * dx,
                y(points[i]),
                paint
            )
        }
        return bitmap
    }
}
