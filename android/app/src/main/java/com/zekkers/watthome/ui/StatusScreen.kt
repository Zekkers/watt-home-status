package com.zekkers.watthome.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.zekkers.watthome.data.HomeStatus
import com.zekkers.watthome.data.StatusFormatter
import com.zekkers.watthome.data.StatusUiState
import com.zekkers.watthome.widget.SparklineRenderer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusScreen(
    state: StatusUiState,
    onRefresh: () -> Unit
) {
    val status = state.status
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Watt Home") },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.error != null) {
                Text(
                    text = state.error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Battery",
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.labelLarge
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = StatusFormatter.percent(status?.socPercent),
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.displayLarge
                        )
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .padding(start = 16.dp, bottom = 12.dp)
                                    .height(22.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Solar ${StatusFormatter.watts(status?.solarW)}",
                        color = MaterialTheme.colorScheme.secondary,
                        style = MaterialTheme.typography.headlineMedium
                    )
                }
            }

            TodayCurveCard(status)

            StatusRow("Overnight slot", StatusFormatter.overnight(status?.overnight))
            StatusRow("16:00 target", StatusFormatter.percent(status?.target1600Percent))
            StatusRow("Peak window", StatusFormatter.dash(status?.peakWindow))
            StatusRow("Next Power Up", StatusFormatter.powerUpWindow(status?.nextPowerUp))
            if (status?.batteryW != null) {
                StatusRow("Battery power", StatusFormatter.signedWatts(status.batteryW))
            }
            if (status?.weatherTomorrow != null) {
                StatusRow("Tomorrow", StatusFormatter.weatherLabel(status.weatherTomorrow))
            }
            StatusFormatter.savingsDetailLine(status?.lastSavings)?.let { StatusRow("Power Up results", it) }
            StatusRow("Last action", StatusFormatter.lastAction(status?.lastAction))
            StatusRow("Updated", StatusFormatter.formatUpdated(status?.updated))

            Text(
                text = "Public status only. No login, no tracking.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun TodayCurveCard(status: HomeStatus?) {
    val density = LocalDensity.current.density
    val curve = remember(status, density) {
        SparklineRenderer.renderToday(
            status = status,
            widthPx = (320 * density).toInt().coerceAtLeast(180),
            heightPx = (110 * density).toInt().coerceAtLeast(72)
        )
    }
    val hasCurve = StatusFormatter.hasTodayCurve(status)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(
                text = "Today’s battery",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(Modifier.height(8.dp))
            Image(
                bitmap = curve.asImageBitmap(),
                contentDescription = "Today’s battery",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp),
                contentScale = ContentScale.FillBounds
            )
            if (!hasCurve) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "waiting for today’s curve",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun StatusRow(label: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
