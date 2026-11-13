package com.zekkers.watthome.ui

import android.text.format.DateFormat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zekkers.watthome.data.GraphSeriesSelection
import com.zekkers.watthome.data.GraphSeriesStyle
import com.zekkers.watthome.data.HomeStatus
import com.zekkers.watthome.data.StatusFormatter
import com.zekkers.watthome.data.StatusUiState
import com.zekkers.watthome.data.TodayEnergyProbe
import com.zekkers.watthome.data.WidgetPlotLayout
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

private sealed interface EnergyTouch {
    data class Graph(val x: Float, val y: Float, val viewW: Float, val viewH: Float) : EnergyTouch
    data class Legend(val id: String) : EnergyTouch
}

@Composable
private fun TodayCurveCard(status: HomeStatus?, series: GraphSeriesSelection) {
    val density = LocalDensity.current.density
    val use24Hour = DateFormat.is24HourFormat(LocalContext.current)
    val (bitmapW, bitmapH) = TodayEnergyProbe.appBitmapSize(density)
    // First-paint snapshot has no series, so this is an empty grid until
    // the IO refresh fills traces in. Do not force a heavy overlay here.
    val curve = remember(status, series, bitmapW, bitmapH) {
        SparklineRenderer.renderToday(
            status = status,
            widthPx = bitmapW,
            heightPx = bitmapH,
            series = series,
            showLegend = false
        )
    }
    val hasCurve = StatusFormatter.hasVisibleTodayCurve(status, series)
    val selectionState = remember { mutableStateOf<EnergyTouch?>(null) }
    val tip = energyTip(selectionState.value, status, series, density, use24Hour)
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(WidgetPlotLayout.PlotAspect)
                    .pointerInput(status, series, density, use24Hour) {
                        detectTapGestures(
                            onTap = { offset ->
                                val viewW = size.width.toFloat()
                                val viewH = size.height.toFloat()
                                val next = TodayEnergyProbe.inspect(
                                    status, series, viewW, viewH, density,
                                    offset.x, offset.y, use24Hour
                                )
                                val currentTip = energyTip(
                                    selectionState.value, status, series, density, use24Hour
                                )
                                selectionState.value = when {
                                    next == null -> null
                                    currentTip != null && currentTip.samePoint(next) -> null
                                    else -> EnergyTouch.Graph(offset.x, offset.y, viewW, viewH)
                                }
                            },
                            onLongPress = { offset ->
                                val viewW = size.width.toFloat()
                                val viewH = size.height.toFloat()
                                val next = TodayEnergyProbe.inspect(
                                    status, series, viewW, viewH, density,
                                    offset.x, offset.y, use24Hour
                                )
                                if (next != null) {
                                    selectionState.value = EnergyTouch.Graph(offset.x, offset.y, viewW, viewH)
                                }
                            }
                        )
                    }
                    .pointerInput(status, series, density, use24Hour) {
                        detectHorizontalDragGestures(
                            onDragStart = { offset ->
                                selectGraphPoint(
                                    selectionState, status, series, density, use24Hour,
                                    offset, size.width.toFloat(), size.height.toFloat()
                                )
                            },
                            onHorizontalDrag = { change, _ ->
                                selectGraphPoint(
                                    selectionState, status, series, density, use24Hour,
                                    change.position, size.width.toFloat(), size.height.toFloat()
                                )
                            }
                        )
                    }
            ) {
                Image(
                    bitmap = curve.asImageBitmap(),
                    contentDescription = "Today’s energy. Tap or hold a point for the exact value.",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
                val graphTip = tip?.takeIf { it.fromGraph }
                if (graphTip != null) {
                    val focus = graphTip.readings.firstOrNull { it.focused }
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val x = graphTip.anchorX * size.width
                        val y = graphTip.anchorY * size.height
                        drawLine(
                            color = Color.White.copy(alpha = 0.8f),
                            start = Offset(x, 0f),
                            end = Offset(x, size.height),
                            strokeWidth = 2f
                        )
                        drawCircle(color = Color.White, radius = 7f, center = Offset(x, y))
                        drawCircle(
                            color = Color(focus?.color ?: GraphSeriesStyle.SOLAR),
                            radius = 4.5f,
                            center = Offset(x, y)
                        )
                    }
                }
            }
            if (tip?.fromGraph == true) {
                EnergyTooltipBubble(tip) { selectionState.value = null }
            } else {
                Spacer(Modifier.height(8.dp))
            }
            EnergyLegend(series) { id ->
                selectionState.value = if (selectionState.value == EnergyTouch.Legend(id)) {
                    null
                } else {
                    EnergyTouch.Legend(id)
                }
            }
            if (tip != null && !tip.fromGraph) {
                EnergyTooltipBubble(tip) { selectionState.value = null }
            }
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

private fun selectGraphPoint(
    selectionState: MutableState<EnergyTouch?>,
    status: HomeStatus?,
    series: GraphSeriesSelection,
    density: Float,
    use24Hour: Boolean,
    offset: Offset,
    viewW: Float,
    viewH: Float
) {
    val next = TodayEnergyProbe.inspect(
        status, series, viewW, viewH, density, offset.x, offset.y, use24Hour
    )
    if (next != null) {
        selectionState.value = EnergyTouch.Graph(offset.x, offset.y, viewW, viewH)
    }
}

private fun energyTip(
    selection: EnergyTouch?,
    status: HomeStatus?,
    series: GraphSeriesSelection,
    density: Float,
    use24Hour: Boolean
): TodayEnergyProbe.EnergyTooltip? = when (selection) {
    is EnergyTouch.Graph -> TodayEnergyProbe.inspect(
        status, series, selection.viewW, selection.viewH, density,
        selection.x, selection.y, use24Hour
    )
    is EnergyTouch.Legend -> TodayEnergyProbe.latest(status, selection.id, use24Hour)
    null -> null
}

@Composable
private fun EnergyTooltipBubble(
    tip: TodayEnergyProbe.EnergyTooltip,
    onDismiss: () -> Unit
) {
    val spoken = buildString {
        val clock = tip.timeText
        if (clock != null) {
            append(if (tip.fromGraph) clock else "Latest $clock")
            append(". ")
        }
        tip.readings.forEach { reading ->
            append(reading.label)
            append(' ')
            append(reading.valueText)
            if (reading.unit.isNotEmpty()) {
                append(' ')
                append(reading.unit)
            }
            append(". ")
            append(reading.meaning)
            append(". ")
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .background(MaterialTheme.colorScheme.inverseSurface, RoundedCornerShape(12.dp))
            .clickable(onClick = onDismiss)
            .semantics { contentDescription = spoken }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val clock = tip.timeText
        if (clock != null) {
            Text(
                text = if (tip.fromGraph) clock else "Latest $clock",
                color = MaterialTheme.colorScheme.inverseOnSurface,
                style = MaterialTheme.typography.labelLarge
            )
        }
        tip.readings.forEach { reading ->
            val dot = if (reading.id == GraphSeriesSelection.ID_SOC) {
                Color(GraphSeriesStyle.SOC_UI)
            } else {
                Color(reading.color)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .border(1.dp, MaterialTheme.colorScheme.inverseOnSurface, CircleShape)
                        .padding(1.dp)
                        .background(dot, CircleShape)
                )
                Column(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .weight(1f)
                ) {
                    Text(
                        text = reading.label,
                        color = MaterialTheme.colorScheme.inverseOnSurface,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (reading.focused) FontWeight.SemiBold else FontWeight.Normal
                    )
                    Text(
                        text = reading.meaning,
                        color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.75f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Text(
                    text = reading.valueText,
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (reading.unit.isNotEmpty()) {
                    Text(
                        text = reading.unit,
                        modifier = Modifier.padding(start = 4.dp),
                        color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

private data class LegendChip(val id: String, val label: String, val color: Color)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EnergyLegend(series: GraphSeriesSelection, onSelect: (String) -> Unit) {
    val items = buildList {
        if (series.solar) add(LegendChip(GraphSeriesSelection.ID_SOLAR, "Solar", Color(GraphSeriesStyle.SOLAR)))
        if (series.battery) add(LegendChip(GraphSeriesSelection.ID_BATTERY, "Battery", Color(GraphSeriesStyle.BATTERY_UI)))
        if (series.house) add(LegendChip(GraphSeriesSelection.ID_HOUSE, "House", Color(GraphSeriesStyle.HOUSE)))
        if (series.grid) add(LegendChip(GraphSeriesSelection.ID_GRID, "Grid", Color(GraphSeriesStyle.GRID)))
        if (series.soc) add(LegendChip(GraphSeriesSelection.ID_SOC, "SOC", Color(GraphSeriesStyle.SOC_UI)))
    }
    if (items.isEmpty()) return
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items.forEach { item ->
            Row(
                modifier = Modifier.clickable(
                    interactionSource = null,
                    indication = null,
                    role = Role.Button,
                    onClick = { onSelect(item.id) }
                ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(item.color, CircleShape)
                )
                Text(
                    text = item.label,
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
