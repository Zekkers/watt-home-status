package com.zekkers.watthome.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import com.zekkers.watthome.data.HomeStatus

class BatterySessionWidget : WattGlanceWidget() {
    override val cardPadding = 6.dp
    override val cardRadius = 16.dp

    @Composable
    override fun Content(status: HomeStatus?) {
        Box(
            modifier = GlanceModifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            SessionHeader(status)
        }
    }
}
