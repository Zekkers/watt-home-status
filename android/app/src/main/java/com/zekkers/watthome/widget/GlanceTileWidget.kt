package com.zekkers.watthome.widget

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
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
import androidx.glance.layout.ContentScale
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
        val curve = SparklineRenderer.renderToday(
            status = status,
            widthPx = (260 * density).toInt().coerceAtLeast(180),
            heightPx = (90 * density).toInt().coerceAtLeast(64)
        )
        provideContent {
            WidgetCard {
                GlanceTileContent(status, curve)
            }
        }
    }
}

@Composable
private fun GlanceTileContent(status: HomeStatus?, curve: Bitmap) {
    val hasCurve = StatusFormatter.hasTodayCurve(status)
    val savings = StatusFormatter.savingsWidgetLine(status?.lastSavings)
    Column(modifier = GlanceModifier.fillMaxSize()) {
        SessionHeader(status)
        Spacer(GlanceModifier.height(8.dp))
        Image(
            provider = ImageProvider(curve),
            contentDescription = "Today’s battery",
            contentScale = ContentScale.Fit,
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
        if (savings != null) {
            Spacer(GlanceModifier.height(4.dp))
            Text(
                text = savings,
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
