package com.zekkers.watthome.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.zekkers.watthome.data.GraphSeriesSelection
import com.zekkers.watthome.data.GraphSeriesStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TokenScreen(
    hasToken: Boolean,
    message: String?,
    error: String?,
    canSkip: Boolean,
    series: GraphSeriesSelection,
    onSeriesChange: (GraphSeriesSelection) -> Unit,
    onSave: (String) -> Unit,
    onTest: (String) -> Unit,
    onRemove: () -> Unit,
    onSkip: () -> Unit,
    onBack: (() -> Unit)?
) {
    var token by remember { mutableStateOf("") }
    if (onBack != null) {
        BackHandler(onBack = onBack)
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Options") },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Navigate up"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Paste a GivEnergy API Bearer token for live house battery.",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "Create one in Account Settings → Manage API Tokens on givenergy.cloud. Same app on both phones; paste the token once on each.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f)
            )
            OutlinedTextField(
                value = token,
                onValueChange = { token = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("GivEnergy API token") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onSave(token) }) { Text("Save") }
                OutlinedButton(onClick = { onTest(token) }) { Text("Test") }
                if (hasToken) {
                    OutlinedButton(onClick = onRemove) { Text("Remove") }
                }
            }
            if (canSkip) {
                TextButton(onClick = onSkip) {
                    Text("Use public status for now")
                }
            }
            val note = error ?: message
            if (note != null) {
                Text(
                    text = note,
                    color = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (hasToken) "A token is saved on this phone." else "No token on this phone yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Today’s energy",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Tick the traces on today’s graph. Solar and battery start on.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f)
            )
            SeriesTick(
                label = "Solar",
                checked = series.solar,
                color = Color(GraphSeriesStyle.SOLAR),
                onCheckedChange = { onSeriesChange(series.copy(solar = it)) }
            )
            SeriesTick(
                label = "Battery",
                checked = series.battery,
                color = Color(GraphSeriesStyle.BATTERY_UI),
                onCheckedChange = { onSeriesChange(series.copy(battery = it)) }
            )
            SeriesTick(
                label = "House",
                checked = series.house,
                color = Color(GraphSeriesStyle.HOUSE),
                onCheckedChange = { onSeriesChange(series.copy(house = it)) }
            )
            SeriesTick(
                label = "Grid",
                checked = series.grid,
                color = Color(GraphSeriesStyle.GRID),
                onCheckedChange = { onSeriesChange(series.copy(grid = it)) }
            )
            SeriesTick(
                label = "SOC",
                checked = series.soc,
                color = Color(GraphSeriesStyle.SOC_UI),
                onCheckedChange = { onSeriesChange(series.copy(soc = it)) }
            )
        }
    }
}

@Composable
private fun SeriesTick(
    label: String,
    checked: Boolean,
    color: Color,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = color,
                uncheckedColor = color.copy(alpha = 0.7f)
            )
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
