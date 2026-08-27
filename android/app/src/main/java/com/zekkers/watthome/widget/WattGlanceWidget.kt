package com.zekkers.watthome.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.Alignment
import com.zekkers.watthome.data.HomeStatus
import com.zekkers.watthome.data.StatusRepository
import com.zekkers.watthome.worker.StatusRefreshScheduler

internal val ForestBg = Color(0xFF1B3A24)
internal val Leaf = Color(0xFFC8E6C9)
internal val Mint = Color(0xFFA5D6A7)
internal val Cream = Color(0xFFE8F5E9)
internal val Solar = Color(0xFFF9A825)
internal val SolarSoft = Color(0xFFFFE082)
internal val PowerUpBoltSize = 24.dp

abstract class WattGlanceWidget : GlanceAppWidget() {
    protected open val cardPadding: Dp = 12.dp
    protected open val cardRadius: Dp = 20.dp

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        StatusRefreshScheduler.enqueuePeriodic(context)
        val status = StatusRepository.get(context).cachedStatus()
        provideContent {
            WidgetCard(radius = cardRadius, padding = cardPadding) {
                Content(status)
            }
        }
    }

    @Composable
    protected abstract fun Content(status: HomeStatus?)
}

@Composable
internal fun WidgetCard(
    radius: Dp = 20.dp,
    padding: Dp = 12.dp,
    content: @Composable () -> Unit
) {
    val onTap = actionRunCallback<WidgetTapAction>()
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(radius)
            .background(ForestBg)
            .clickable(onTap)
            .padding(padding),
        contentAlignment = Alignment.TopStart
    ) {
        content()
    }
}
