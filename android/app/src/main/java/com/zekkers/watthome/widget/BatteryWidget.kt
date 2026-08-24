package com.zekkers.watthome.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.appwidget.SizeMode
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.zekkers.watthome.R
import com.zekkers.watthome.data.HomeStatus
import com.zekkers.watthome.data.StatusFormatter

class BatteryWidget : WattGlanceWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact
    override val cardPadding = 4.dp
    override val cardRadius = 14.dp

    @Composable
    override fun Content(status: HomeStatus?) {
        val powerUp = status?.nextPowerUp
        val from = StatusFormatter.twelveHourClock(powerUp?.from)
        val to = StatusFormatter.twelveHourClock(powerUp?.to)
        val showWindow = from != null && to != null
        val showBadge = StatusFormatter.optedInPowerUp(powerUp)

        Box(modifier = GlanceModifier.fillMaxSize()) {
            if (!showWindow) {
                Box(
                    modifier = GlanceModifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    SocToken(percent = status?.socPercent, numberSize = 32.sp)
                }
            } else {
                Column(
                    modifier = GlanceModifier.fillMaxSize(),
                    verticalAlignment = Alignment.Top,
                    horizontalAlignment = Alignment.Start
                ) {
                    SocToken(percent = status?.socPercent, numberSize = 32.sp)
                    Text(
                        text = "$from - $to",
                        style = TextStyle(
                            color = ColorProvider(Cream, Cream),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        maxLines = 1
                    )
                }
            }
            if (showBadge) {
                Box(
                    modifier = GlanceModifier.fillMaxSize(),
                    contentAlignment = Alignment.TopEnd
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_power_up_badge),
                        contentDescription = "Power Up",
                        modifier = GlanceModifier
                            .padding(top = 8.dp, end = 8.dp)
                            .size(PowerUpBoltSize)
                    )
                }
            }
        }
    }
}
