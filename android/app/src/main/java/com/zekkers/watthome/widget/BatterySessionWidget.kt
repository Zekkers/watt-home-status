package com.zekkers.watthome.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.size
import com.zekkers.watthome.data.HomeStatus
import com.zekkers.watthome.data.StatusFormatter

class BatterySessionWidget : WattGlanceWidget() {
    override val cardPadding = 8.dp

    @Composable
    override fun Content(status: HomeStatus?) {
        val weatherRes = WeatherIcons.drawableRes(status?.weatherTomorrow)
        Box(modifier = GlanceModifier.fillMaxSize()) {
            SessionHeader(status)
            if (weatherRes != null) {
                Box(
                    modifier = GlanceModifier.fillMaxWidth(),
                    contentAlignment = Alignment.TopEnd
                ) {
                    Image(
                        provider = ImageProvider(weatherRes),
                        contentDescription = StatusFormatter.weatherLabel(status?.weatherTomorrow),
                        modifier = GlanceModifier.size(14.dp)
                    )
                }
            }
        }
    }
}
