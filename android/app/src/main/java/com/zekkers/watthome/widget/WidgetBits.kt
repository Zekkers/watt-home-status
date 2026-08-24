package com.zekkers.watthome.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.zekkers.watthome.R
import com.zekkers.watthome.data.HomeStatus
import com.zekkers.watthome.data.PowerUp
import com.zekkers.watthome.data.StatusFormatter

@Composable
internal fun SocToken(
    percent: Int?,
    numberSize: TextUnit
) {
    Text(
        text = StatusFormatter.percent(percent),
        style = TextStyle(
            color = ColorProvider(Color.White, Color.White),
            fontSize = numberSize,
            fontWeight = FontWeight.Bold
        ),
        maxLines = 1
    )
}

@Composable
internal fun PowerUpTimes(powerUp: PowerUp?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = StatusFormatter.powerUpSpokenWindow(powerUp),
            style = TextStyle(
                color = ColorProvider(Cream, Cream),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            ),
            maxLines = 1
        )
        if (StatusFormatter.optedInPowerUp(powerUp)) {
            Image(
                provider = ImageProvider(R.drawable.ic_power_up_badge),
                contentDescription = "Power Up",
                modifier = GlanceModifier.padding(start = 4.dp).size(12.dp)
            )
        }
    }
}

@Composable
internal fun SessionHeader(status: HomeStatus?) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = GlanceModifier.defaultWeight()) {
            SocToken(status?.socPercent, numberSize = 26.sp)
            Text(
                text = "battery",
                style = TextStyle(color = ColorProvider(Mint, Mint), fontSize = 10.sp),
                maxLines = 1
            )
        }
        PowerUpTimes(status?.nextPowerUp)
    }
}
