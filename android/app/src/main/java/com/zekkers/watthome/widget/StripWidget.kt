package com.zekkers.watthome.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
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
        val savings = StatusFormatter.savingsPounds(status?.lastSavings) ?: "—"
        val sessions = SessionLayout.visible(status)
        val overnight = StatusFormatter.overnightChipLine(status?.overnight)
        Row(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SocToken(status?.socPercent, numberSize = 20.sp)
            if (sessions.isNotEmpty()) {
                sessions.forEach { session ->
                    SessionChip(
                        session = session,
                        fontSize = 12.sp,
                        oneLine = true,
                        alignEnd = false
                    )
                }
            } else if (overnight != null) {
                OvernightChip(status?.overnight, 12.sp, alignEnd = false)
            }
            WeatherCodeChip(status?.weatherTomorrow, 12.sp, showCode = false, alignEnd = false)
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
