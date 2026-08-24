package com.zekkers.watthome.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.color.ColorProvider
import androidx.compose.ui.graphics.Color
import com.zekkers.watthome.R
import com.zekkers.watthome.data.HomeStatus
import com.zekkers.watthome.data.StatusFormatter

class BatteryWidget : WattGlanceWidget() {
    @Composable
    override fun Content(status: HomeStatus?) {
        val showBadge = StatusFormatter.showPowerUpBadge(status?.nextPowerUp)
        Box(modifier = GlanceModifier.fillMaxSize()) {
            Box(
                modifier = GlanceModifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = StatusFormatter.percent(status?.socPercent),
                    style = TextStyle(
                        color = ColorProvider(Color.White, Color.White),
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            if (showBadge) {
                Box(
                    modifier = GlanceModifier.fillMaxSize(),
                    contentAlignment = Alignment.TopEnd
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_power_up_badge),
                        contentDescription = "Power Up",
                        modifier = GlanceModifier.size(16.dp)
                    )
                }
            }
        }
    }
}
