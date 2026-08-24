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
import com.zekkers.watthome.R
import com.zekkers.watthome.data.HomeStatus
import com.zekkers.watthome.data.StatusFormatter

class BatteryWidget : WattGlanceWidget() {
    override val cardPadding = 4.dp

    @Composable
    override fun Content(status: HomeStatus?) {
        val showBadge = StatusFormatter.optedInPowerUp(status?.nextPowerUp)
        Box(modifier = GlanceModifier.fillMaxSize()) {
            Box(
                modifier = GlanceModifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                SocToken(
                    percent = status?.socPercent,
                    numberSize = 28.sp,
                    percentSize = 16.sp
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
                        modifier = GlanceModifier.size(14.dp)
                    )
                }
            }
        }
    }
}
