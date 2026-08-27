package com.zekkers.watthome.data

import android.graphics.Paint
import android.graphics.Typeface
import android.text.TextPaint
import kotlin.math.max

object WidgetTextMeasure {
    fun estimateWidthDp(text: String, sp: Float, bold: Boolean = false): Float {
        val em = if (bold) 0.72f else 0.62f
        var units = 0f
        for (ch in text) {
            units += when (ch) {
                ' ' -> 0.33f
                '-' -> 0.40f
                '·' -> 0.50f
                '%' -> 0.85f
                'm' -> 0.90f
                'w', 'M', 'W' -> 0.95f
                'i', 'l', 't', 'f', 'j' -> 0.35f
                '1' -> 0.50f
                '\u2060', '\u200B', '\uFEFF' -> 0f
                else -> em
            }
        }
        return units * sp
    }

    fun widthDp(text: String, sp: Float, density: Float, bold: Boolean = false): Float {
        val estimated = estimateWidthDp(text, sp, bold)
        val painted = paintWidthDp(text, sp, density, bold)
        return if (painted != null) max(estimated, painted) else estimated
    }

    fun fits(text: String, sp: Float, availableDp: Float, density: Float, bold: Boolean = false): Boolean =
        widthDp(text, sp, density, bold) <= availableDp

    private fun paintWidthDp(text: String, sp: Float, density: Float, bold: Boolean): Float? {
        if (density <= 0f) return null
        return runCatching {
            val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                isFakeBoldText = bold
                textSize = sp * density
                typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            }
            val px = paint.measureText(text)
            if (px <= 1f) null else px / density
        }.getOrNull()
    }
}

data class PowerUpClock(val from: String, val to: String) {
    val oneLine: String get() = "$from - $to"
}

enum class PowerUpClockMode { Hidden, Stacked, OneLine }

object PowerUpLayout {
    fun clock(powerUp: PowerUp?): PowerUpClock? {
        val from = StatusFormatter.twelveHourClock(powerUp?.from) ?: return null
        val to = StatusFormatter.twelveHourClock(powerUp?.to) ?: return null
        return PowerUpClock(from, to)
    }

    fun oneByOne(powerUp: PowerUp?): PowerUpClockMode =
        if (clock(powerUp) == null) PowerUpClockMode.Hidden else PowerUpClockMode.Stacked

    fun twoByTwo(powerUp: PowerUp?): PowerUpClockMode = oneByOne(powerUp)

    fun twoByOne(
        powerUp: PowerUp?,
        availableTimeDp: Float = 0f,
        timeSp: Float = 13f,
        boltDp: Float = 0f,
        density: Float = 1f
    ): PowerUpClockMode {
        if (clock(powerUp) == null) return PowerUpClockMode.Hidden
        return PowerUpClockMode.Stacked
    }

    fun wide(
        powerUp: PowerUp?,
        availableDp: Float,
        timeSp: Float,
        density: Float
    ): PowerUpClockMode {
        val clock = clock(powerUp) ?: return PowerUpClockMode.Hidden
        val need = WidgetTextMeasure.widthDp(clock.oneLine, timeSp, density) * 1.3f
        return if (need <= availableDp) PowerUpClockMode.OneLine else PowerUpClockMode.Stacked
    }
}
