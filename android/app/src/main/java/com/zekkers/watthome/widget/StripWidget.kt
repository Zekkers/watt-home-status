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
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.zekkers.watthome.data.HomeStatus
import com.zekkers.watthome.data.StatusFormatter

class StripWidget : WattGlanceWidget() {
    @Composable
    override fun Content(status: HomeStatus?) {
        val weatherRes = WeatherIcons.drawableRes(status?.weatherTomorrow)
        val savings = StatusFormatter.savingsPounds(status?.lastSavings) ?: "—"
        Row(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = StatusFormatter.percent(status?.socPercent),
                style = TextStyle(
                    color = ColorProvider(Color.White, Color.White),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                ),
                modifier = GlanceModifier.defaultWeight()
            )
            Text(
                text = StatusFormatter.powerUpWindow(status?.nextPowerUp),
                style = TextStyle(
                    color = ColorProvider(Cream, Cream),
                    fontSize = 13.sp
                ),
                modifier = GlanceModifier.defaultWeight()
            )
            if (weatherRes != null) {
                Image(
                    provider = ImageProvider(weatherRes),
                    contentDescription = StatusFormatter.weatherLabel(status?.weatherTomorrow),
                    modifier = GlanceModifier.size(20.dp)
                )
            } else {
                Text(
                    text = "—",
                    style = TextStyle(color = ColorProvider(Mint, Mint), fontSize = 13.sp)
                )
            }
            Text(
                text = savings,
                style = TextStyle(
                    color = ColorProvider(Solar, Solar),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.End
                ),
                modifier = GlanceModifier.defaultWeight()
            )
        }
    }
}
