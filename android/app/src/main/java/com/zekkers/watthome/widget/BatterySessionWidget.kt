package com.zekkers.watthome.widget

import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.appwidget.SizeMode
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import com.zekkers.watthome.data.HomeStatus
import com.zekkers.watthome.data.PowerUpLayout
import com.zekkers.watthome.data.StatusFormatter
import com.zekkers.watthome.data.WidgetTextMeasure

class BatterySessionWidget : WattGlanceWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact
    override val cardPadding = 6.dp
    override val cardRadius = 16.dp

    @Composable
    override fun Content(status: HomeStatus?) {
        val density = Resources.getSystem().displayMetrics.density
        val innerWidth = LocalSize.current.width.value - cardPadding.value * 2
        val socWidth = WidgetTextMeasure.widthDp(
            text = StatusFormatter.percent(status?.socPercent ?: 100),
            sp = 26f,
            density = density,
            bold = true
        )
        val availableTime = (innerWidth - socWidth - 8f).coerceAtLeast(0f)
        val clockMode = PowerUpLayout.twoByOne(
            powerUp = status?.nextPowerUp,
            availableTimeDp = availableTime,
            timeSp = 13f,
            boltDp = PowerUpBoltSize.value + 6f,
            density = density
        )
        Box(
            modifier = GlanceModifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            SessionHeader(status, clockMode)
        }
    }
}
