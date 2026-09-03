package com.zekkers.watthome.data

/**
 * Widget sparkline panes. The 4×2 overview plots on the right at the pane’s
 * real size (no 260×90 letterbox). Compact tiles keep a 0W gutter whenever
 * power traces are drawn.
 */
object WidgetPlotLayout {
    const val ZeroLabel = "0W"
    const val PlotAspect = 260f / 90f

    const val OverviewPadDp = 24f
    const val SessionPadDp = 12f
    const val GlancePadDp = 16f

    const val OverviewLeftShare = 0.55f
    const val OverviewLeftMinDp = 140f
    const val OverviewLeftMaxDp = 220f
    const val OverviewLeftFloorDp = 80f

    const val MinPlotWidthDp = 72f
    const val MinPlotHeightDp = 24f
    const val SessionHeaderDp = 34f
    const val SessionBesideMaxHeightDp = 56f
    const val GlanceHeaderDp = 38f
    const val GlanceGapDp = 6f
    const val GlanceLineDp = 16f

    const val ZeroLabelPadPx = 6f

    data class FloatBox(
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float
    ) {
        val width: Float get() = right - left
        val height: Float get() = bottom - top
        val midY: Float get() = top + height / 2f
    }

    data class SplitPane(
        val leftWidthDp: Float,
        val plotWidthDp: Float,
        val plotHeightDp: Float,
        val besideHeader: Boolean
    )

    fun letterbox(availW: Float, availH: Float, aspect: Float = PlotAspect): FloatBox {
        var w = availW
        var h = w / aspect
        if (h > availH) {
            h = availH
            w = h * aspect
        }
        val left = (availW - w) / 2f
        val top = (availH - h) / 2f
        return FloatBox(left, top, left + w, top + h)
    }

    fun plotBounds(bitmapW: Float, bitmapH: Float, fillSlot: Boolean): FloatBox =
        if (fillSlot) {
            FloatBox(0f, 0f, bitmapW, bitmapH)
        } else {
            letterbox(bitmapW, bitmapH)
        }

    fun legendHeightPx(plotHeightPx: Float, showLegend: Boolean, seriesAny: Boolean): Float =
        if (showLegend && seriesAny) (plotHeightPx * 0.18f).coerceAtLeast(12f) else 0f

    fun zeroGutterPx(hasPower: Boolean, labelWidthPx: Float, padPx: Float = ZeroLabelPadPx): Float =
        if (!hasPower || labelWidthPx <= 0f) 0f else labelWidthPx + padPx

    fun zeroLabelTextPx(chartHeightPx: Float): Float =
        (chartHeightPx * 0.16f).coerceIn(8f, 14f)

    fun chartBounds(plot: FloatBox, legendHeightPx: Float, zeroGutterPx: Float): FloatBox =
        FloatBox(
            left = plot.left + zeroGutterPx,
            top = plot.top + legendHeightPx,
            right = plot.right,
            bottom = plot.bottom
        )

    fun overviewSplit(widgetWidthDp: Float, widgetHeightDp: Float): SplitPane {
        val innerW = (widgetWidthDp - OverviewPadDp).coerceAtLeast(120f)
        val innerH = (widgetHeightDp - OverviewPadDp).coerceAtLeast(64f)
        var left = (innerW * OverviewLeftShare).coerceIn(OverviewLeftMinDp, OverviewLeftMaxDp)
        var plot = innerW - left
        if (plot < MinPlotWidthDp) {
            plot = MinPlotWidthDp.coerceAtMost(innerW * 0.5f)
            left = innerW - plot
        }
        return SplitPane(
            leftWidthDp = left.coerceAtLeast(OverviewLeftFloorDp),
            plotWidthDp = plot.coerceAtLeast(64f),
            plotHeightDp = innerH,
            besideHeader = true
        )
    }

    fun sessionPlot(widgetWidthDp: Float, widgetHeightDp: Float): SplitPane {
        val innerW = (widgetWidthDp - SessionPadDp).coerceAtLeast(80f)
        val innerH = (widgetHeightDp - SessionPadDp).coerceAtLeast(28f)
        val below = innerH - SessionHeaderDp
        return if (below >= MinPlotHeightDp && innerH >= SessionBesideMaxHeightDp) {
            SplitPane(
                leftWidthDp = innerW,
                plotWidthDp = innerW,
                plotHeightDp = below,
                besideHeader = false
            )
        } else {
            val plotW = (innerW * 0.38f).coerceAtLeast(MinPlotWidthDp).coerceAtMost(innerW * 0.45f)
            SplitPane(
                leftWidthDp = (innerW - plotW).coerceAtLeast(72f),
                plotWidthDp = plotW,
                plotHeightDp = innerH,
                besideHeader = true
            )
        }
    }

    fun glanceBottom(widgetWidthDp: Float, widgetHeightDp: Float, extraLines: Int): SplitPane {
        val innerW = (widgetWidthDp - GlancePadDp).coerceAtLeast(80f)
        val innerH = (widgetHeightDp - GlancePadDp).coerceAtLeast(48f)
        val used = GlanceHeaderDp + GlanceGapDp + extraLines * GlanceLineDp
        return SplitPane(
            leftWidthDp = innerW,
            plotWidthDp = innerW,
            plotHeightDp = (innerH - used).coerceAtLeast(MinPlotHeightDp),
            besideHeader = false
        )
    }
}
