package com.zekkers.watthome.widget

import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
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
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.zekkers.watthome.R
import com.zekkers.watthome.data.HomeStatus
import com.zekkers.watthome.data.PowerUpClock
import com.zekkers.watthome.data.PowerUpClockMode
import com.zekkers.watthome.data.PowerUpLayout
import com.zekkers.watthome.data.SocLayout
import com.zekkers.watthome.data.SocTokenSpec
import com.zekkers.watthome.data.StatusFormatter

@Composable
internal fun SocToken(
    percent: Int?,
    numberSize: TextUnit,
    text: String = StatusFormatter.percent(percent)
) {
    Text(
        text = text,
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
internal fun FittedSocToken(spec: SocTokenSpec) {
    SocToken(percent = null, numberSize = spec.sizeSp.sp, text = spec.text)
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
internal fun PowerUpBolt(
    size: Dp = PowerUpBoltSize,
    startPad: Dp = 6.dp
) {
    Image(
        provider = ImageProvider(R.drawable.ic_power_up_badge),
        contentDescription = "Power Up",
        modifier = GlanceModifier.padding(start = startPad).size(size)
    )
}

@Composable
internal fun PowerUpClockBlock(
    clock: PowerUpClock?,
    mode: PowerUpClockMode,
    fontSize: TextUnit,
    showBolt: Boolean,
    modifier: GlanceModifier = GlanceModifier,
    alignEnd: Boolean = true,
    boltSize: Dp = PowerUpBoltSize,
    boltPad: Dp = 6.dp
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
            PowerUpBolt(size = boltSize, startPad = boltPad)
        }
    }
}

@Composable
internal fun SolarWattsLine(
    status: HomeStatus?,
    fontSize: TextUnit = 10.sp
) {
    val watts = status?.solarW ?: return
    Text(
        text = StatusFormatter.watts(watts),
        style = TextStyle(
            color = ColorProvider(Solar, Solar),
            fontSize = fontSize,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        ),
        maxLines = 1
    )
}

@Composable
internal fun BatterySocStack(
    status: HomeStatus?,
    modifier: GlanceModifier = GlanceModifier,
    socSize: TextUnit = 26.sp,
    timeSize: TextUnit = 10.sp,
    contentPaddingDp: Float = 4f
) {
    val clock = PowerUpLayout.clock(status?.nextPowerUp)
    val showBolt = clock != null && StatusFormatter.optedInPowerUp(status?.nextPowerUp)
    val density = Resources.getSystem().displayMetrics.density
    val innerWidth = LocalSize.current.width.value - contentPaddingDp
    val soc = SocLayout.token(
        percent = status?.socPercent,
        availableDp = innerWidth,
        density = density,
        preferredSp = socSize.value
    )
    Column(
        modifier = modifier,
        verticalAlignment = Alignment.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FittedSocToken(soc)
        SolarWattsLine(status, timeSize)
        if (clock != null) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = GlanceModifier.defaultWeight(),
                    horizontalAlignment = Alignment.Start
                ) {
                    PowerUpTimeLine(clock.from, timeSize)
                    PowerUpTimeLine(clock.to, timeSize)
                }
                if (showBolt) {
                    PowerUpBolt(size = CompactHeaderBoltSize, startPad = 2.dp)
                }
            }
        }
    }
}

@Composable
internal fun SessionHeader(
    status: HomeStatus?,
    contentPaddingDp: Float = 12f,
    availableWidthDp: Float? = null,
    showSolar: Boolean = false,
    modifier: GlanceModifier = GlanceModifier.fillMaxWidth()
) {
    val clock = PowerUpLayout.clock(status?.nextPowerUp)
    val showBolt = clock != null && StatusFormatter.optedInPowerUp(status?.nextPowerUp)
    val density = Resources.getSystem().displayMetrics.density
    val innerWidth = availableWidthDp ?: (LocalSize.current.width.value - contentPaddingDp)
    val soc = SocLayout.token(
        percent = status?.socPercent,
        availableDp = SocLayout.headerSocBudget(innerWidth, clock, showBolt, density),
        density = density
    )
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = GlanceModifier.defaultWeight()) {
            FittedSocToken(soc)
            if (showSolar) {
                SolarWattsLine(status, 10.sp)
            } else {
                Text(
                    text = "battery",
                    style = TextStyle(color = ColorProvider(Mint, Mint), fontSize = 10.sp),
                    maxLines = 1
                )
            }
        }
        if (clock != null) {
            Row(
                modifier = GlanceModifier.wrapContentSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    PowerUpTimeLine(clock.from, SocLayout.HeaderTimeSp.sp)
                    PowerUpTimeLine(clock.to, SocLayout.HeaderTimeSp.sp)
                }
                if (showBolt) {
                    PowerUpBolt(size = CompactHeaderBoltSize, startPad = 2.dp)
                }
            }
        }
    }
}
