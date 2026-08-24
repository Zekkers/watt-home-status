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
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.zekkers.watthome.R
import com.zekkers.watthome.data.HomeStatus
import com.zekkers.watthome.data.PowerUpClock
import com.zekkers.watthome.data.PowerUpClockMode
import com.zekkers.watthome.data.PowerUpLayout
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
internal fun PowerUpTimeLine(
    text: String,
    fontSize: TextUnit
) {
    Text(
        text = text,
        style = TextStyle(
            color = ColorProvider(Cream, Cream),
            fontSize = fontSize,
            fontWeight = FontWeight.Medium
        ),
        maxLines = 1
    )
}

@Composable
internal fun PowerUpBolt() {
    Image(
        provider = ImageProvider(R.drawable.ic_power_up_badge),
        contentDescription = "Power Up",
        modifier = GlanceModifier
            .padding(top = 8.dp, end = 8.dp)
            .size(PowerUpBoltSize)
    )
}

@Composable
internal fun PowerUpClockBlock(
    clock: PowerUpClock?,
    mode: PowerUpClockMode,
    fontSize: TextUnit,
    showBolt: Boolean,
    modifier: GlanceModifier = GlanceModifier
) {
    if (mode == PowerUpClockMode.Hidden || clock == null) return
    Row(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalAlignment = Alignment.Top
    ) {
        if (mode == PowerUpClockMode.OneLine) {
            PowerUpTimeLine(clock.oneLine, fontSize)
        } else {
            Column(horizontalAlignment = Alignment.End) {
                PowerUpTimeLine(clock.from, fontSize)
                PowerUpTimeLine(clock.to, fontSize)
            }
        }
        if (showBolt) {
            Image(
                provider = ImageProvider(R.drawable.ic_power_up_badge),
                contentDescription = "Power Up",
                modifier = GlanceModifier.padding(start = 6.dp).size(PowerUpBoltSize)
            )
        }
    }
}

@Composable
internal fun BatterySocStack(
    status: HomeStatus?,
    modifier: GlanceModifier = GlanceModifier,
    socSize: TextUnit = 32.sp,
    timeSize: TextUnit = 10.sp,
    fillBoltCorner: Boolean = false
) {
    val clock = PowerUpLayout.clock(status?.nextPowerUp)
    val showBolt = clock != null && StatusFormatter.optedInPowerUp(status?.nextPowerUp)
    Box(modifier = modifier) {
        if (fillBoltCorner) {
            Column(
                verticalAlignment = Alignment.Top,
                horizontalAlignment = Alignment.Start
            ) {
                SocToken(percent = status?.socPercent, numberSize = socSize)
                if (clock != null) {
                    PowerUpTimeLine(clock.from, timeSize)
                    PowerUpTimeLine(clock.to, timeSize)
                }
            }
            if (showBolt) {
                Box(
                    modifier = GlanceModifier.fillMaxSize(),
                    contentAlignment = Alignment.TopEnd
                ) {
                    PowerUpBolt()
                }
            }
        } else {
            Row(verticalAlignment = Alignment.Top) {
                Column(
                    verticalAlignment = Alignment.Top,
                    horizontalAlignment = Alignment.Start
                ) {
                    SocToken(percent = status?.socPercent, numberSize = socSize)
                    if (clock != null) {
                        PowerUpTimeLine(clock.from, timeSize)
                        PowerUpTimeLine(clock.to, timeSize)
                    }
                }
                if (showBolt) {
                    PowerUpBolt()
                }
            }
        }
    }
}

@Composable
internal fun SessionHeader(
    status: HomeStatus?,
    clockMode: PowerUpClockMode
) {
    val clock = PowerUpLayout.clock(status?.nextPowerUp)
    val showBolt = clock != null && StatusFormatter.optedInPowerUp(status?.nextPowerUp)
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            SocToken(status?.socPercent, numberSize = 26.sp)
            Text(
                text = "battery",
                style = TextStyle(color = ColorProvider(Mint, Mint), fontSize = 10.sp),
                maxLines = 1
            )
        }
        PowerUpClockBlock(
            clock = clock,
            mode = clockMode,
            fontSize = 13.sp,
            showBolt = showBolt,
            modifier = GlanceModifier.defaultWeight()
        )
    }
}
