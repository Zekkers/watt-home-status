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
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.wrapContentSize
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
        modifier = GlanceModifier.wrapContentSize(),
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
        modifier = GlanceModifier.wrapContentSize(),
        style = TextStyle(
            color = ColorProvider(Cream, Cream),
            fontSize = fontSize,
            fontWeight = FontWeight.Medium
        ),
        maxLines = 1
    )
}

@Composable
internal fun PowerUpClockBlock(
    clock: PowerUpClock?,
    mode: PowerUpClockMode,
    fontSize: TextUnit,
    showBolt: Boolean,
    modifier: GlanceModifier = GlanceModifier,
    alignEnd: Boolean = true
) {
    if (mode == PowerUpClockMode.Hidden || clock == null) return
    Row(
        modifier = modifier,
        horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (mode == PowerUpClockMode.OneLine) {
            PowerUpTimeLine(clock.oneLine, fontSize)
        } else {
            Column(
                horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start
            ) {
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
    socSize: TextUnit = 26.sp,
    timeSize: TextUnit = 10.sp
) {
    val clock = PowerUpLayout.clock(status?.nextPowerUp)
    val showBolt = clock != null && StatusFormatter.optedInPowerUp(status?.nextPowerUp)
    Column(
        modifier = modifier,
        verticalAlignment = Alignment.Top,
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SocToken(percent = status?.socPercent, numberSize = socSize)
            Spacer(GlanceModifier.defaultWeight())
            if (showBolt) {
                Image(
                    provider = ImageProvider(R.drawable.ic_power_up_badge),
                    contentDescription = "Power Up",
                    modifier = GlanceModifier.size(PowerUpBoltSize)
                )
            }
        }
        if (clock != null) {
            PowerUpTimeLine(clock.from, timeSize)
            PowerUpTimeLine(clock.to, timeSize)
        }
    }
}

@Composable
internal fun SessionHeader(status: HomeStatus?) {
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
        Spacer(GlanceModifier.defaultWeight())
        if (clock != null) {
            Column(horizontalAlignment = Alignment.End) {
                PowerUpTimeLine(clock.from, 13.sp)
                PowerUpTimeLine(clock.to, 13.sp)
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
