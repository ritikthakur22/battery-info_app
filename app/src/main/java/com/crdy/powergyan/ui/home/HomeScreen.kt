package com.crdy.powergyan.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crdy.powergyan.data.local.history.BatteryHistoryHelper
import com.crdy.powergyan.data.local.history.HistoryPoint
import com.crdy.powergyan.domain.model.BatterySnapshot
import com.crdy.powergyan.domain.model.BatteryStatus
import com.crdy.powergyan.domain.model.DisplaySettings
import com.crdy.powergyan.domain.model.PluggedState
import com.crdy.powergyan.domain.model.SmartChargeConfig
import com.crdy.powergyan.domain.model.minutesTo
import com.crdy.powergyan.domain.model.watts
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(viewModel: HomeViewModel, settings: DisplaySettings = DisplaySettings()) {
    val batteryState by viewModel.batteryState.collectAsState()
    val context = LocalContext.current
    var history by remember { mutableStateOf<List<HistoryPoint>>(emptyList()) }

    LaunchedEffect(batteryState) {
        batteryState?.let { snapshot ->
            val helper = BatteryHistoryHelper(context)
            helper.insertSnapshot(snapshot)
            history = helper.getHistory()
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        batteryState?.let { BatteryContent(it, settings, history) } ?: LoadingContent()
    }
}

@Composable
fun BatteryContent(snapshot: BatterySnapshot, settings: DisplaySettings, history: List<HistoryPoint>) {
    val config = settings.smartChargeConfig
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { BatteryHeroCard(snapshot, config, settings.textScale) }
        item { QuickMetricGrid(snapshot, config) }
        item { AdviceBanner(snapshot, config) }

        if (history.size > 1) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Charge History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                "Last 24 Hours",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                        Box(Modifier.fillMaxWidth().height(160.dp)) {
                            BatteryGraph(history = history, band = config.takeIf { it.enabled })
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Made with ",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    " • Updated ${DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(snapshot.timestamp))}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Rich elevated Hero card with animated pulse and live status badges */
@Composable
private fun BatteryHeroCard(snapshot: BatterySnapshot, config: SmartChargeConfig, textScale: Float) {
    val progress by animateFloatAsState((snapshot.percentage / 100f).coerceIn(0f, 1f), label = "battery progress")
    val charging = snapshot.status == BatteryStatus.CHARGING
    val pulse = if (charging) {
        rememberInfiniteTransition(label = "charging").animateFloat(
            0.82f, 1f, infiniteRepeatable(tween(1000), RepeatMode.Reverse), label = "pulse"
        ).value
    } else 1f
    val low = snapshot.percentage <= 20 && !charging
    val accent = if (low) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val bandColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
    val target = if (config.enabled) config.stopLimit else 100
    val etaMinutes = snapshot.minutesTo(target)
    val liveWatts = snapshot.watts()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Glowing Arc Gauge
            Box(
                modifier = Modifier.size(150.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                    val stroke = 12.dp.toPx()
                    drawArc(track, -90f, 360f, false, style = Stroke(stroke, cap = StrokeCap.Round))
                    if (config.enabled) {
                        drawArc(
                            bandColor,
                            -90f + config.resumeLimit * 3.6f,
                            (config.stopLimit - config.resumeLimit) * 3.6f,
                            false,
                            style = Stroke(stroke * 0.45f, cap = StrokeCap.Round)
                        )
                    }
                    drawArc(
                        accent.copy(alpha = pulse),
                        -90f,
                        360f * progress,
                        false,
                        style = Stroke(stroke, cap = StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "${snapshot.percentage}%",
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 36.sp * textScale,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        snapshot.status.label(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = accent
                    )
                }
            }

            Spacer(Modifier.width(18.dp))

            // Right: Detailed Telemetry Badges
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Connection pill
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(if (charging) accent else Color.Gray, CircleShape)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "${snapshot.plugged.label()} Power",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Live Wattage Pill
                if (charging && liveWatts != null && liveWatts >= 0.1f) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = String.format(Locale.getDefault(), "⚡ %.1fW Fast Charging", liveWatts),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Time to Full / Stop Limit
                if (charging && etaMinutes != null && etaMinutes > 0) {
                    Text(
                        text = "⏱ ~${formatMinutes(etaMinutes)} to $target%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                } else if (!charging) {
                    Text(
                        text = "Running purely on battery",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/** 2-Column Responsive Metric Grid with elevation and tactile cards */
@Composable
private fun QuickMetricGrid(snapshot: BatterySnapshot, config: SmartChargeConfig) {
    val target = if (config.enabled) config.stopLimit else 100
    val tempC = snapshot.temperatureC
    val tempColor = when {
        tempC == null -> MaterialTheme.colorScheme.onSurface
        tempC >= 42f -> MaterialTheme.colorScheme.error
        tempC >= 38f -> Color(0xFFF59E0B)
        else -> Color(0xFF10B981)
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricPillCard(
                title = "Temperature",
                value = tempC?.let { String.format(Locale.getDefault(), "%.1f °C", it) } ?: "—",
                accent = tempColor,
                modifier = Modifier.weight(1f)
            )
            MetricPillCard(
                title = "Live Voltage",
                value = snapshot.voltageMv?.let { String.format(Locale.getDefault(), "%.2f V", it / 1000f) } ?: "—",
                accent = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricPillCard(
                title = "Current Flow",
                value = snapshot.currentUa?.let { "${it / 1000} mA" } ?: "—",
                accent = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            MetricPillCard(
                title = if (config.enabled) "Target Limit" else "Target Full",
                value = "$target%",
                accent = if (config.enabled) Color(0xFF10B981) else MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MetricPillCard(title: String, value: String, accent: Color, modifier: Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = accent)
        }
    }
}

@Composable
private fun AdviceBanner(snapshot: BatterySnapshot, config: SmartChargeConfig) {
    val message = statusAdvice(snapshot, config)
    val isWarning = snapshot.percentage <= 20 && snapshot.status != BatteryStatus.CHARGING
    val containerColor = if (isWarning) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
    val contentColor = if (isWarning) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer

    Surface(
        color = containerColor,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (isWarning) Icons.Filled.Warning else Icons.Filled.Info,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private fun statusAdvice(snapshot: BatterySnapshot, config: SmartChargeConfig): String = when {
    snapshot.status == BatteryStatus.CHARGING && config.enabled ->
        "Smart Charge active: Capped at ${config.stopLimit}% to preserve hardware cell life."
    snapshot.status == BatteryStatus.CHARGING ->
        "Charging to 100%. Enable Smart Charge in Control to stop at a healthier battery level."
    snapshot.status == BatteryStatus.FULL -> "Fully charged. You can safely disconnect your charger."
    snapshot.plugged != PluggedState.NONE && config.enabled -> "Plugged in. Charging paused automatically by Smart Charge."
    snapshot.percentage <= 20 -> "Battery low. Connect your charger soon."
    else -> "Device discharging normally. System health optimal."
}

internal fun formatMinutes(minutes: Int) = if (minutes < 60) "$minutes min" else "${minutes / 60}h ${minutes % 60}m"

internal fun BatteryStatus.label() = when (this) {
    BatteryStatus.CHARGING -> "Charging"
    BatteryStatus.DISCHARGING -> "On Battery"
    BatteryStatus.FULL -> "Full"
    BatteryStatus.NOT_CHARGING -> "Not Charging"
    BatteryStatus.UNKNOWN -> "Monitoring"
}

private fun PluggedState.label() = when (this) {
    PluggedState.NONE -> "Battery"
    PluggedState.AC -> "AC Adapter"
    PluggedState.USB -> "USB Port"
    PluggedState.WIRELESS -> "Wireless Dock"
    PluggedState.UNKNOWN -> "External"
}

@Composable
fun LoadingContent() {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        Text("Reading battery telemetry…", style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun BatteryGraph(history: List<HistoryPoint>, band: SmartChargeConfig? = null) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
    val bandColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        reveal.animateTo(1f, animationSpec = tween(900, easing = FastOutSlowInEasing))
    }
    Canvas(modifier = Modifier.fillMaxSize().semantics { contentDescription = "Battery level over the last 24 hours" }) {
        val width = size.width
        val height = size.height
        fun yFor(level: Int) = height - level / 100f * height

        band?.let {
            drawRect(
                bandColor,
                Offset(0f, yFor(it.stopLimit)),
                androidx.compose.ui.geometry.Size(width, yFor(it.resumeLimit) - yFor(it.stopLimit))
            )
        }
        for (level in listOf(25, 50, 75)) {
            drawLine(gridColor, Offset(0f, yFor(level)), Offset(width, yFor(level)), 1.dp.toPx())
        }

        val minTime = history.first().timestamp
        val timeRange = (history.last().timestamp - minTime).coerceAtLeast(1000)
        val points = history.map { Offset((it.timestamp - minTime).toFloat() / timeRange * width, yFor(it.level)) }
        val visible = (points.size * reveal.value).toInt().coerceIn(1, points.size)

        // Gradient Area Fill under Curve
        val fillPath = Path()
        fillPath.moveTo(points[0].x, height)
        points.take(visible).forEach { fillPath.lineTo(it.x, it.y) }
        fillPath.lineTo(points[visible - 1].x, height)
        fillPath.close()

        drawPath(
            fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(primaryColor.copy(alpha = 0.28f), primaryColor.copy(alpha = 0.02f)),
                startY = 0f,
                endY = height
            )
        )

        val linePath = Path()
        points.take(visible).forEachIndexed { index, point ->
            if (index == 0) linePath.moveTo(point.x, point.y) else linePath.lineTo(point.x, point.y)
        }
        drawPath(linePath, primaryColor, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawCircle(primaryColor, radius = 4.5.dp.toPx(), center = points[visible - 1])
    }
}
