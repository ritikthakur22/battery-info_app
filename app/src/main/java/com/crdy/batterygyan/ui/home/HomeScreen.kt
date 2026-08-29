package com.crdy.batterygyan.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crdy.batterygyan.domain.model.BatterySnapshot
import com.crdy.batterygyan.domain.model.BatteryStatus

import com.crdy.batterygyan.domain.model.DisplaySettings

@Composable
fun HomeScreen(viewModel: HomeViewModel, settings: DisplaySettings = DisplaySettings()) {
    val batteryState by viewModel.batteryState.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        batteryState?.let { snapshot ->
            BatteryContent(snapshot, settings)
        } ?: LoadingContent()
    }
}

@Composable
fun BatteryContent(snapshot: BatterySnapshot, settings: DisplaySettings) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "BATTERY",
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp * settings.textScale),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "${snapshot.percentage}%",
            fontSize = 120.sp * settings.textScale,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        val statusText = when (snapshot.status) {
            BatteryStatus.CHARGING -> "Charging"
            BatteryStatus.DISCHARGING -> "Discharging"
            BatteryStatus.FULL -> "Full"
            BatteryStatus.NOT_CHARGING -> "Not Charging"
            BatteryStatus.UNKNOWN -> "Status Unknown"
        }

        Text(
            text = statusText,
            style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp * settings.textScale),
            color = MaterialTheme.colorScheme.secondary
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (settings.secondaryInfoEnabled) {
            snapshot.temperatureC?.let {
                Text(
                    text = "Temp: ${it}°C",
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp * settings.textScale)
                )
            }
            Text(
                text = "Health: ${snapshot.health}",
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp * settings.textScale)
            )
        }
    }
}

@Composable
fun LoadingContent() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Reading battery...", style = MaterialTheme.typography.titleLarge)
    }
}
