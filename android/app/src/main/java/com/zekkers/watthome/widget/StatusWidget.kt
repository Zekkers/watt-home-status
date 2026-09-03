package com.zekkers.watthome.widget

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
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
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.width
import androidx.glance.layout.wrapContentSize
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.zekkers.watthome.data.GraphSeriesPrefs
import com.zekkers.watthome.data.GraphSeriesSelection
import com.zekkers.watthome.data.HomeStatus
import com.zekkers.watthome.data.PowerUpClock
import com.zekkers.watthome.data.PowerUpClockMode
import com.zekkers.watthome.data.PowerUpLayout
import com.zekkers.watthome.data.StatusFormatter
import com.zekkers.watthome.data.StatusRepository
import com.zekkers.watthome.data.WidgetPlotLayout
import com.zekkers.watthome.worker.StatusRefreshScheduler

class StatusWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        StatusRefreshScheduler.enqueuePeriodic(context)
        val status = StatusRepository.get(context).cachedStatus()
        val series = GraphSeriesPrefs.read(context)
        provideContent {
            WidgetCard {
                OverviewContent(status, series)
            }
        }
    }
}

@Composable
private fun OverviewContent(status: HomeStatus?, series: GraphSeriesSelection) {
    val hasCurve = StatusFormatter.hasVisibleTodayCurve(status, series)
    val density = Resources.getSystem().displayMetrics.density
    val size = LocalSize.current
    val pane = WidgetPlotLayout.overviewSplit(size.width.value, size.height.value)
    val curve = SparklineRenderer.renderToday(
        status = status,
        widthPx = (pane.plotWidthDp * density).toInt().coerceAtLeast(80),
        heightPx = (pane.plotHeightDp * density).toInt().coerceAtLeast(64),
        fillSlot = true,
        series = series,
        showLegend = true
    )
    val clock = PowerUpLayout.clock(status?.nextPowerUp)
    val showBolt = StatusFormatter.optedInPowerUp(status?.nextPowerUp)
    val clockMode = PowerUpLayout.wide(
        powerUp = status?.nextPowerUp,
        availableDp = pane.leftWidthDp,
        timeSp = 12f,
        density = density,
        showBolt = showBolt
    )
    Row(
        modifier = GlanceModifier.fillMaxSize(),
        verticalAlignment = Alignment.Top,
        horizontalAlignment = Alignment.Start
    ) {
        OverviewNumbers(
            status = status,
            clock = clock,
            clockMode = clockMode,
            showBolt = showBolt,
            hasCurve = hasCurve,
            modifier = GlanceModifier.width(pane.leftWidthDp.dp).fillMaxHeight()
        )
        OverviewPlot(
            curve = curve,
            modifier = GlanceModifier.width(pane.plotWidthDp.dp).fillMaxHeight()
        )
    }
}

@Composable
private fun OverviewNumbers(
    status: HomeStatus?,
    clock: PowerUpClock?,
    clockMode: PowerUpClockMode,
    showBolt: Boolean,
    hasCurve: Boolean,
    modifier: GlanceModifier
) {
    Column(
        modifier = modifier,
        verticalAlignment = Alignment.Top,
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "Watt Home",
            style = TextStyle(color = ColorProvider(Leaf, Leaf), fontSize = 12.sp),
            maxLines = 1
        )
        Spacer(GlanceModifier.height(6.dp))
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom
        ) {
            SocToken(status?.socPercent, numberSize = 28.sp)
            Spacer(GlanceModifier.defaultWeight())
            Text(
                text = StatusFormatter.watts(status?.solarW),
                style = TextStyle(
                    color = ColorProvider(Solar, Solar),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1
            )
        }
        Spacer(GlanceModifier.height(6.dp))
        Text(
            text = "Overnight ${StatusFormatter.overnight(status?.overnight)}",
            style = TextStyle(color = ColorProvider(Cream, Cream), fontSize = 12.sp),
            maxLines = 1
        )
        Text(
            text = "16:00 ${StatusFormatter.percent(status?.target1600Percent)}  ·  Peak ${StatusFormatter.dash(status?.peakWindow)}",
            style = TextStyle(color = ColorProvider(Cream, Cream), fontSize = 12.sp),
            maxLines = 1
        )
        PowerUpClockBlock(
            clock = clock,
            mode = clockMode,
            fontSize = 12.sp,
            showBolt = showBolt,
            modifier = GlanceModifier.wrapContentSize(),
            alignEnd = false
        )
        Spacer(GlanceModifier.height(6.dp))
        if (!hasCurve) {
            Text(
                text = "waiting for today’s curve",
                style = TextStyle(color = ColorProvider(Mint, Mint), fontSize = 11.sp),
                maxLines = 1
            )
        }
        StatusFormatter.savingsBatchLine(status?.lastSavings)?.let { savings ->
            Text(
                text = savings,
                style = TextStyle(
                    color = ColorProvider(Solar, Solar),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 1
            )
        }
        Text(
            text = StatusFormatter.formatUpdated(status?.updated),
            style = TextStyle(color = ColorProvider(Mint, Mint), fontSize = 11.sp),
            maxLines = 1
        )
    }
}

@Composable
private fun OverviewPlot(curve: Bitmap, modifier: GlanceModifier) {
    Image(
        provider = ImageProvider(curve),
        contentDescription = "Today’s energy",
        contentScale = ContentScale.FillBounds,
        modifier = modifier
    )
}
