package com.zekkers.watthome.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.SizeMode
import androidx.glance.layout.fillMaxSize
import com.zekkers.watthome.data.HomeStatus

class BatteryWidget : WattGlanceWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact
    override val cardPadding = 4.dp
    override val cardRadius = 14.dp

    @Composable
    override fun Content(status: HomeStatus?) {
        BatterySocStack(
            status = status,
            modifier = GlanceModifier.fillMaxSize(),
            fillBoltCorner = true
        )
    }
}
