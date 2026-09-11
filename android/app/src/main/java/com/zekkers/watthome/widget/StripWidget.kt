package com.zekkers.watthome.widget

import androidx.compose.runtime.Composable
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
import com.zekkers.watthome.data.SessionLayout
import com.zekkers.watthome.data.StatusFormatter

class StripWidget : WattGlanceWidget() {
    @Composable
    override fun Content(status: HomeStatus?) {
        val weatherRes = WeatherIcons.drawableRes(status?.weatherTomorrow)
        val savings = StatusFormatter.savingsPounds(status?.lastSavings) ?: "—"
        val sessions = SessionLayout.visible(status)
        Row(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SocToken(status?.socPercent, numberSize = 20.sp)
            if (sessions.isEmpty()) {
                Text(
                    text = "—",
                    style = TextStyle(
                        color = ColorProvider(Cream, Cream),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    ),
                    maxLines = 1,
                    modifier = GlanceModifier.defaultWeight()
                )
            } else {
                sessions.forEach { session ->
                    SessionChip(
                        session = session,
                        fontSize = 12.sp,
                        oneLine = true,
                        alignEnd = false
                    )
                }
            }
            if (weatherRes != null) {
                Image(
                    provider = ImageProvider(weatherRes),
                    contentDescription = status?.weatherTomorrow?.code ?: "weather",
                    modifier = GlanceModifier.size(18.dp)
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
                maxLines = 1,
                modifier = GlanceModifier.defaultWeight()
            )
        }
    }
}
