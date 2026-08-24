package com.zekkers.watthome.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.zekkers.watthome.data.HomeStatus
import com.zekkers.watthome.data.StatusFormatter

class BatterySessionWidget : WattGlanceWidget() {
    @Composable
    override fun Content(status: HomeStatus?) {
        BatterySessionContent(status)
    }
}

@Composable
internal fun BatterySessionContent(status: HomeStatus?) {
    val weatherRes = WeatherIcons.drawableRes(status?.weatherTomorrow)
    Box(modifier = GlanceModifier.fillMaxSize()) {
        Row(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = StatusFormatter.percent(status?.socPercent),
                    style = TextStyle(
                        color = ColorProvider(Color.White, Color.White),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = "battery",
                    style = TextStyle(color = ColorProvider(Mint, Mint), fontSize = 10.sp)
                )
            }
            Column(
                modifier = GlanceModifier.defaultWeight(),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = StatusFormatter.powerUpWindow(status?.nextPowerUp),
                    style = TextStyle(
                        color = ColorProvider(Cream, Cream),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
                Spacer(GlanceModifier.height(2.dp))
                Text(
                    text = if (StatusFormatter.powerUpWindowOrNull(status?.nextPowerUp) == null) {
                        "session"
                    } else {
                        "Power Up"
                    },
                    style = TextStyle(color = ColorProvider(Mint, Mint), fontSize = 10.sp)
                )
            }
        }
        if (weatherRes != null) {
            Box(
                modifier = GlanceModifier.fillMaxWidth(),
                contentAlignment = Alignment.TopEnd
            ) {
                Image(
                    provider = ImageProvider(weatherRes),
                    contentDescription = StatusFormatter.weatherLabel(status?.weatherTomorrow),
                    modifier = GlanceModifier.size(16.dp)
                )
            }
        }
    }
}
