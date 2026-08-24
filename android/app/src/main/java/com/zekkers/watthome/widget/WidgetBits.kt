package com.zekkers.watthome.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxWidth
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.zekkers.watthome.data.HomeStatus
import com.zekkers.watthome.data.PowerUp
import com.zekkers.watthome.data.StatusFormatter

@Composable
internal fun SocToken(
    percent: Int?,
    numberSize: TextUnit,
    percentSize: TextUnit = 14.sp
) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            text = StatusFormatter.percentNumber(percent),
            style = TextStyle(
                color = ColorProvider(Color.White, Color.White),
                fontSize = numberSize,
                fontWeight = FontWeight.Bold
            ),
            maxLines = 1
        )
        if (percent != null) {
            Text(
                text = "%",
                style = TextStyle(
                    color = ColorProvider(Color.White, Color.White),
                    fontSize = percentSize,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1
            )
        }
    }
}

@Composable
internal fun PowerUpTimes(powerUp: PowerUp?, compact: Boolean = false) {
    if (compact) {
        Text(
            text = StatusFormatter.powerUpCompactHours(powerUp),
            style = TextStyle(
                color = ColorProvider(Cream, Cream),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            ),
            maxLines = 1
        )
        return
    }
    Column(horizontalAlignment = Alignment.End) {
        Text(
            text = StatusFormatter.powerUpStartLine(powerUp),
            style = TextStyle(
                color = ColorProvider(Cream, Cream),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            ),
            maxLines = 1
        )
        Text(
            text = StatusFormatter.powerUpEndLine(powerUp),
            style = TextStyle(
                color = ColorProvider(Cream, Cream),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            ),
            maxLines = 1
        )
        Text(
            text = if (StatusFormatter.hasPowerUp(powerUp)) "Power Up" else "session",
            style = TextStyle(color = ColorProvider(Mint, Mint), fontSize = 10.sp),
            maxLines = 1
        )
    }
}

@Composable
internal fun SessionHeader(status: HomeStatus?) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = GlanceModifier.defaultWeight()) {
            SocToken(status?.socPercent, numberSize = 26.sp, percentSize = 14.sp)
            Text(
                text = "battery",
                style = TextStyle(color = ColorProvider(Mint, Mint), fontSize = 10.sp),
                maxLines = 1
            )
        }
        PowerUpTimes(status?.nextPowerUp)
    }
}
