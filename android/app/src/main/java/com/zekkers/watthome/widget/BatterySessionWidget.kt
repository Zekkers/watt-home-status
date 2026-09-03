package com.zekkers.watthome.widget

import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.appwidget.SizeMode
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.width
import com.zekkers.watthome.data.GraphSeriesSelection
import com.zekkers.watthome.data.HomeStatus
import com.zekkers.watthome.data.WidgetPlotLayout

class BatterySessionWidget : WattGlanceWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact
    override val cardPadding = 6.dp
    override val cardRadius = 16.dp

    @Composable
    override fun Content(status: HomeStatus?) {
        val size = LocalSize.current
        val density = Resources.getSystem().displayMetrics.density
        val series = GraphSeriesSelection.WIDGET_COMPACT
        val pane = WidgetPlotLayout.sessionPlot(size.width.value, size.height.value)
        val curve = SparklineRenderer.renderToday(
            status = status,
            widthPx = (pane.plotWidthDp * density).toInt().coerceAtLeast(72),
            heightPx = (pane.plotHeightDp * density).toInt().coerceAtLeast(28),
            fillSlot = true,
            series = series,
            showLegend = false
        )
        if (pane.besideHeader) {
            Row(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SessionHeader(
                    status = status,
                    contentPaddingDp = 12f,
                    availableWidthDp = pane.leftWidthDp,
                    showSolar = true,
                    modifier = GlanceModifier.defaultWeight()
                )
                Image(
                    provider = ImageProvider(curve),
                    contentDescription = "Today’s energy",
                    contentScale = ContentScale.FillBounds,
                    modifier = GlanceModifier.width(pane.plotWidthDp.dp).fillMaxHeight()
                )
            }
        } else {
            Column(modifier = GlanceModifier.fillMaxSize()) {
                SessionHeader(
                    status = status,
                    contentPaddingDp = 12f,
                    showSolar = true
                )
                Image(
                    provider = ImageProvider(curve),
                    contentDescription = "Today’s energy",
                    contentScale = ContentScale.FillBounds,
                    modifier = GlanceModifier.fillMaxWidth().defaultWeight()
                )
            }
        }
    }
}
