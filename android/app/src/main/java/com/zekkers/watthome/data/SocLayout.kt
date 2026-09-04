package com.zekkers.watthome.data

/**
 * Glance Text with maxLines=1 ellipsizes instead of shrinking. The 1×1 tile
 * has room for two-digit SOC at 26sp (`88%`) but not three digits (`100%`).
 */
data class SocTokenSpec(
    val text: String,
    val sizeSp: Float
)

object SocLayout {
    const val PreferredSp = 26f
    const val MinSp = 16f
    const val FitSlackDp = 2f

    /** Typical 1×1 inner width on a phone launcher after 2dp card padding. */
    const val OneByOneInnerDp = 66f

    /** 2×1 / 2×2 header: stacked times stay wrap-content; bolt is reserved. */
    const val HeaderTimeSp = 13f
    const val HeaderBoltDp = 16f
    const val HeaderBoltPadDp = 2f

    /** ~2 launcher cells minus compact card padding. */
    const val CompactHeaderInnerDp = 142f

    fun token(
        percent: Int?,
        availableDp: Float,
        density: Float,
        preferredSp: Float = PreferredSp,
        minSp: Float = MinSp
    ): SocTokenSpec {
        val budget = (availableDp - FitSlackDp).coerceAtLeast(0f)
        val withPct = StatusFormatter.percent(percent)
        val fitted = sizeSp(withPct, budget, density, preferredSp, minSp)
        if (percent != 100 || WidgetTextMeasure.fits(withPct, fitted, budget, density, bold = true)) {
            return SocTokenSpec(withPct, fitted)
        }
        val bare = StatusFormatter.percentNumber(percent)
        return SocTokenSpec(bare, sizeSp(bare, budget, density, preferredSp, minSp))
    }

    fun sizeSp(
        text: String,
        availableDp: Float,
        density: Float,
        preferredSp: Float,
        minSp: Float = MinSp
    ): Float {
        if (availableDp <= 0f) return preferredSp
        var sp = preferredSp
        while (sp > minSp && !WidgetTextMeasure.fits(text, sp, availableDp, density, bold = true)) {
            sp -= 0.5f
        }
        return sp.coerceAtLeast(minSp)
    }

    fun headerTrailingDp(
        clock: PowerUpClock?,
        showBolt: Boolean,
        density: Float,
        timeSp: Float = HeaderTimeSp,
        boltDp: Float = HeaderBoltDp,
        boltPadDp: Float = HeaderBoltPadDp
    ): Float {
        if (clock == null) return 0f
        val time = maxOf(
            WidgetTextMeasure.widthDp(clock.from, timeSp, density),
            WidgetTextMeasure.widthDp(clock.to, timeSp, density)
        )
        val bolt = if (showBolt) boltDp + boltPadDp else 0f
        return time + bolt
    }

    fun headerSocBudget(
        innerWidthDp: Float,
        clock: PowerUpClock?,
        showBolt: Boolean,
        density: Float
    ): Float = (innerWidthDp - headerTrailingDp(clock, showBolt, density)).coerceAtLeast(0f)

    fun headerFits(
        percent: Int?,
        innerWidthDp: Float,
        clock: PowerUpClock?,
        showBolt: Boolean,
        density: Float,
        preferredSp: Float = PreferredSp
    ): Boolean {
        val token = token(percent, headerSocBudget(innerWidthDp, clock, showBolt, density), density, preferredSp)
        val used = WidgetTextMeasure.widthDp(token.text, token.sizeSp, density, bold = true) +
            headerTrailingDp(clock, showBolt, density)
        return used <= innerWidthDp && !token.text.contains('…') && token.text.startsWith(percent?.toString() ?: "—")
    }
}
