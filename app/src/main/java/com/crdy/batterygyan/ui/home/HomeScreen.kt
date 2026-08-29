package com.crdy.batterygyan.ui.home

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crdy.batterygyan.domain.model.BatteryHealth
import com.crdy.batterygyan.domain.model.BatterySnapshot
import com.crdy.batterygyan.domain.model.BatteryStatus
import com.crdy.batterygyan.domain.model.DisplaySettings
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(viewModel: HomeViewModel, settings: DisplaySettings = DisplaySettings()) {
    val batteryState by viewModel.batteryState.collectAsState()
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        batteryState?.let { BatteryContent(it, settings) } ?: LoadingContent()
    }
}

@Composable
fun BatteryContent(snapshot: BatterySnapshot, settings: DisplaySettings) {
    val scale = settings.textScale
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("BATTERY GYAN", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Text("Your power, at a glance", style = MaterialTheme.typography.headlineSmall.copy(fontSize = 25.sp * scale), fontWeight = FontWeight.Bold)
                }
                Text("LIVE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
        }
        item { BatteryHero(snapshot, scale) }
        item { Text("Battery details", style = MaterialTheme.typography.titleLarge.copy(fontSize = 21.sp * scale), fontWeight = FontWeight.Bold) }
        items(detailRows(snapshot)) { row -> DetailCard(row, scale) }
        item {
            val message = when (snapshot.status) {
                BatteryStatus.CHARGING -> "Charging safely • Keep airflow around your device"
                BatteryStatus.FULL -> "Fully charged • Ready to go"
                BatteryStatus.DISCHARGING -> if (snapshot.percentage <= 20) "Low battery • Consider charging soon" else "Running normally • No action needed"
                else -> "Battery state is being monitored"
            }
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Text(message, modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp * scale), fontWeight = FontWeight.Medium)
            }
        }
        item {
            Text("Updated ${DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(snapshot.timestamp))}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = .6f), modifier = Modifier.padding(bottom = 8.dp))
        }
    }
}

private data class DetailRow(val label: String, val value: String, val symbol: String)

private fun detailRows(snapshot: BatterySnapshot): List<DetailRow> {
    val estimated = snapshot.chargeCounterUah?.let { counter ->
        if (snapshot.percentage > 0) "~${counter / 1000 * 100 / snapshot.percentage} mAh" else "Available"
    } ?: "Unavailable"
    return listOf(
        DetailRow("Health", snapshot.health.toLabel(), "♥"),
        DetailRow("Temperature", snapshot.temperatureC?.let { String.format(Locale.getDefault(), "%.1f °C", it) } ?: "Unavailable", "°"),
        DetailRow("Voltage", snapshot.voltageMv?.let { "%.2f V".format(Locale.getDefault(), it / 1000f) } ?: "Unavailable", "↯"),
        DetailRow("Current", snapshot.currentUa?.let { "%.0f mA".format(Locale.getDefault(), it / 1000f) } ?: "Unavailable", "⇄"),
        DetailRow("Estimated capacity", estimated, "▣"),
        DetailRow("Energy", snapshot.energyNwh?.let { "%.2f Wh".format(Locale.getDefault(), it / 1_000_000_000f) } ?: "Unavailable", "◈"),
        DetailRow("Technology", snapshot.technology ?: "Unavailable", "◉")
    )
}

private fun BatteryHealth?.toLabel() = when (this) {
    BatteryHealth.GOOD -> "Good"
    BatteryHealth.COLD -> "Cold"
    BatteryHealth.DEAD -> "Dead"
    BatteryHealth.OVERHEAT -> "Overheating"
    BatteryHealth.OVER_VOLTAGE -> "Over-voltage"
    BatteryHealth.UNKNOWN_FAILURE -> "Unknown failure"
    else -> "Unknown"
}

@Composable
private fun BatteryHero(snapshot: BatterySnapshot, scale: Float) {
    val progress by animateFloatAsState((snapshot.percentage / 100f).coerceIn(0f, 1f), label = "battery progress")
    val infinite = rememberInfiniteTransition(label = "charging pulse")
    val pulse by infinite.animateFloat(0.88f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "pulse")
    val accent = when {
        snapshot.status == BatteryStatus.CHARGING -> androidx.compose.ui.graphics.Color(0xFF00A889)
        snapshot.percentage <= 20 -> androidx.compose.ui.graphics.Color(0xFFE79A17)
        else -> MaterialTheme.colorScheme.primary
    }
    val track = MaterialTheme.colorScheme.onSurface.copy(alpha = .12f)
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(156.dp), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = 13.dp.toPx()
                    drawArc(track, -90f, 360f, false, style = Stroke(stroke, cap = StrokeCap.Round))
                    drawArc(accent.copy(alpha = if (snapshot.status == BatteryStatus.CHARGING) pulse else 1f), -90f, 360f * progress, false, style = Stroke(stroke, cap = StrokeCap.Round))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${snapshot.percentage}%", fontSize = 42.sp * scale, fontWeight = FontWeight.Bold, color = accent)
                    Text(snapshot.status.toLabel(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .7f))
                }
            }
            Spacer(Modifier.size(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(if (snapshot.status == BatteryStatus.CHARGING) "⚡ Charging" else "● ${snapshot.status.toLabel()}", style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp * scale), fontWeight = FontWeight.Bold)
                Text("${snapshot.plugged.toLabel()} connection", style = MaterialTheme.typography.bodyMedium)
                Text("Offline • event-driven", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

private fun BatteryStatus.toLabel() = name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }
private fun com.crdy.batterygyan.domain.model.PluggedState.toLabel() = name.lowercase().replaceFirstChar { it.uppercase() }

@Composable
private fun DetailCard(row: DetailRow, scale: Float) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(row.symbol, fontSize = 22.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(34.dp))
            Text(row.label, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp * scale), modifier = Modifier.weight(1f))
            Text(row.value, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp * scale), fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun LoadingContent() {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text("Reading battery…", style = MaterialTheme.typography.titleLarge)
    }
}
