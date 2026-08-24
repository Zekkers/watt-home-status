package com.zekkers.watthome.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.zekkers.watthome.data.HomeStatus
import com.zekkers.watthome.data.StatusFormatter

class StatusWidget : WattGlanceWidget() {
    @Composable
    override fun Content(status: HomeStatus?) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            Text(
                text = "Watt Home",
                style = TextStyle(color = ColorProvider(Leaf, Leaf), fontSize = 12.sp)
            )
            Spacer(GlanceModifier.height(8.dp))
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                Column(modifier = GlanceModifier.fillMaxWidth()) {
                    Row(modifier = GlanceModifier.fillMaxWidth()) {
                        Text(
                            text = StatusFormatter.percent(status?.socPercent),
                            style = TextStyle(
                                color = ColorProvider(Color.White, Color.White),
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = GlanceModifier.defaultWeight()
                        )
                        Text(
                            text = StatusFormatter.watts(status?.solarW),
                            style = TextStyle(
                                color = ColorProvider(Solar, Solar),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                    Spacer(GlanceModifier.height(2.dp))
                    Row(modifier = GlanceModifier.fillMaxWidth()) {
                        Text(
                            text = "battery",
                            style = TextStyle(color = ColorProvider(Mint, Mint), fontSize = 11.sp),
                            modifier = GlanceModifier.defaultWeight()
                        )
                        Text(
                            text = "solar",
                            style = TextStyle(color = ColorProvider(SolarSoft, SolarSoft), fontSize = 11.sp)
                        )
                    }
                }
            }
            Spacer(GlanceModifier.height(10.dp))
            Text(
                text = "Overnight ${StatusFormatter.overnight(status?.overnight)}",
                style = TextStyle(color = ColorProvider(Cream, Cream), fontSize = 13.sp)
            )
            Spacer(GlanceModifier.height(2.dp))
            Text(
                text = "16:00 target ${StatusFormatter.percent(status?.target1600Percent)}  ·  Peak ${StatusFormatter.dash(status?.peakWindow)}",
                style = TextStyle(color = ColorProvider(Cream, Cream), fontSize = 12.sp)
            )
            Spacer(GlanceModifier.height(2.dp))
            Text(
                text = StatusFormatter.powerUpLine(status?.nextPowerUp),
                style = TextStyle(color = ColorProvider(Cream, Cream), fontSize = 12.sp)
            )
            Spacer(GlanceModifier.height(2.dp))
            Text(
                text = "Last ${StatusFormatter.lastAction(status?.lastAction)}",
                style = TextStyle(color = ColorProvider(Cream, Cream), fontSize = 12.sp)
            )
            Spacer(GlanceModifier.height(8.dp))
            Text(
                text = StatusFormatter.formatUpdated(status?.updated),
                style = TextStyle(color = ColorProvider(Mint, Mint), fontSize = 11.sp)
            )
        }
    }
}
