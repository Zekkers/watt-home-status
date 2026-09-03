package com.zekkers.watthome.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.zekkers.watthome.data.GraphSeriesSelection
import com.zekkers.watthome.data.GraphSeriesStyle
import com.zekkers.watthome.data.HomeStatus
import com.zekkers.watthome.data.StatusFormatter
import com.zekkers.watthome.data.StatusUiState
import com.zekkers.watthome.widget.SparklineRenderer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusScreen(
    state: StatusUiState,
    series: GraphSeriesSelection,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val status = state.status
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Watt Home") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
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
            if (!state.hasToken) {
                Text(
                    text = "Add GivEnergy token for live battery",
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            } else if (state.liveOk) {
                Text(
                    text = "Live house battery",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (state.error != null) {
                Text(
                    text = state.error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            } else if (state.showingLastKnown && state.status != null) {
                Text(
                    text = "Showing last known status",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
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
                    if (status?.houseW != null) {
                        Text(
                            text = "House ${StatusFormatter.watts(status.houseW)}",
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }
                }
            }

            TodayCurveCard(status, series)

            StatusRow("Overnight slot", StatusFormatter.overnight(status?.overnight))
            StatusRow("16:00 target", StatusFormatter.percent(status?.target1600Percent))
            StatusRow("Peak window", StatusFormatter.dash(status?.peakWindow))
            StatusRow("Next Power Up", StatusFormatter.powerUpSpokenWindow(status?.nextPowerUp))
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
                text = if (state.hasToken) {
                    "Live battery from this phone’s token. Public extras still come from the family status file."
                } else {
                    "Public status only until a GivEnergy token is saved. No tracking."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun TodayCurveCard(status: HomeStatus?, series: GraphSeriesSelection) {
    val density = LocalDensity.current.density
    val curve = remember(status, series, density) {
        SparklineRenderer.renderToday(
            status = status,
            widthPx = (320 * density).toInt().coerceAtLeast(180),
            heightPx = (110 * density).toInt().coerceAtLeast(72),
            series = series,
            showLegend = false
        )
    }
    val hasCurve = StatusFormatter.hasVisibleTodayCurve(status, series)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(
                text = "Today’s energy",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(Modifier.height(8.dp))
            Image(
                bitmap = curve.asImageBitmap(),
                contentDescription = "Today’s energy",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(260f / 90f),
                contentScale = ContentScale.Fit
            )
            Spacer(Modifier.height(8.dp))
            EnergyLegend(series)
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EnergyLegend(series: GraphSeriesSelection) {
    val items = buildList {
        if (series.solar) add("Solar" to Color(GraphSeriesStyle.SOLAR))
        if (series.battery) add("Battery" to Color(GraphSeriesStyle.BATTERY_UI))
        if (series.house) add("House" to Color(GraphSeriesStyle.HOUSE))
        if (series.grid) add("Grid" to Color(GraphSeriesStyle.GRID))
        if (series.soc) add("SOC" to Color(GraphSeriesStyle.SOC_UI))
    }
    if (items.isEmpty()) return
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items.forEach { (label, color) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(color, CircleShape)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
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
