package com.zekkers.watthome.widget

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.zekkers.watthome.data.HomeStatus
import com.zekkers.watthome.data.StatusFormatter
import com.zekkers.watthome.data.StatusRepository
import com.zekkers.watthome.worker.StatusRefreshScheduler

class GlanceTileWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        StatusRefreshScheduler.enqueuePeriodic(context)
        val status = StatusRepository.get(context).cachedStatus()
        val density = context.resources.displayMetrics.density
        val sparkline = SparklineRenderer.render(
            series = status?.batteryWSeries.orEmpty(),
            widthPx = (240 * density).toInt().coerceAtLeast(160),
            heightPx = (56 * density).toInt().coerceAtLeast(40)
        )
        provideContent {
            WidgetCard {
                GlanceTileContent(status, sparkline)
            }
        }
    }
}

@Composable
private fun GlanceTileContent(status: HomeStatus?, sparkline: Bitmap?) {
    Column(modifier = GlanceModifier.fillMaxSize()) {
        BatterySessionContent(status)
        if (sparkline != null) {
            Spacer(GlanceModifier.height(8.dp))
            Image(
                provider = ImageProvider(sparkline),
                contentDescription = "Battery power",
                modifier = GlanceModifier.fillMaxWidth().height(44.dp)
            )
        }
        val savings = StatusFormatter.savingsLine(status?.savings)
        if (savings != null) {
            Spacer(GlanceModifier.height(6.dp))
            Text(
                text = savings,
                style = TextStyle(
                    color = ColorProvider(Solar, Solar),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            )
        } else if (status?.batteryW != null) {
            Spacer(GlanceModifier.height(6.dp))
            Text(
                text = StatusFormatter.signedWatts(status.batteryW),
                style = TextStyle(
                    color = ColorProvider(Color.White, Color.White),
                    fontSize = 12.sp
                )
            )
        }
    }
}
