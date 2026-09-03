package com.zekkers.watthome.widget

import android.content.Context
import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.zekkers.watthome.data.GraphSeriesSelection
import com.zekkers.watthome.data.HomeStatus
import com.zekkers.watthome.data.StatusFormatter
import com.zekkers.watthome.data.StatusRepository
import com.zekkers.watthome.data.WidgetPlotLayout
import com.zekkers.watthome.data.WidgetTextMeasure
import com.zekkers.watthome.worker.StatusRefreshScheduler

class GlanceTileWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        StatusRefreshScheduler.enqueuePeriodic(context)
        val status = StatusRepository.get(context).cachedStatus()
        provideContent {
            WidgetCard(radius = 16.dp, padding = 8.dp) {
                GlanceTileContent(status, GraphSeriesSelection.WIDGET_COMPACT)
            }
        }
    }
}

@Composable
private fun GlanceTileContent(status: HomeStatus?, series: GraphSeriesSelection) {
    val hasCurve = StatusFormatter.hasVisibleTodayCurve(status, series)
    val savings = StatusFormatter.savingsWidgetLine(status?.lastSavings)
    val density = Resources.getSystem().displayMetrics.density
    val size = LocalSize.current
    val innerWidth = size.width.value - 16f
    val savingsLine = savings?.takeIf { WidgetTextMeasure.fits(it, 11f, innerWidth, density) }
    val extraLines = (if (!hasCurve) 1 else 0) + (if (savingsLine != null) 1 else 0)
    val pane = WidgetPlotLayout.glanceBottom(size.width.value, size.height.value, extraLines)
    val curve = SparklineRenderer.renderToday(
        status = status,
        widthPx = (pane.plotWidthDp * density).toInt().coerceAtLeast(80),
        heightPx = (pane.plotHeightDp * density).toInt().coerceAtLeast(36),
        fillSlot = true,
        series = series,
        showLegend = false
    )
    Column(
        modifier = GlanceModifier.fillMaxSize(),
        verticalAlignment = Alignment.Top,
        horizontalAlignment = Alignment.Start
    ) {
        SessionHeader(status, contentPaddingDp = 16f)
        Spacer(GlanceModifier.height(6.dp))
        Image(
            provider = ImageProvider(curve),
            contentDescription = "Today’s energy",
            contentScale = ContentScale.FillBounds,
            modifier = GlanceModifier.fillMaxWidth().defaultWeight()
        )
        if (!hasCurve) {
            Spacer(GlanceModifier.height(4.dp))
            Text(
                text = "waiting for today’s curve",
                style = TextStyle(color = ColorProvider(Mint, Mint), fontSize = 11.sp),
                maxLines = 1
            )
        }
        if (savingsLine != null) {
            Spacer(GlanceModifier.height(4.dp))
            Text(
                text = savingsLine,
                style = TextStyle(
                    color = ColorProvider(Solar, Solar),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 1
            )
        }
    }
}
