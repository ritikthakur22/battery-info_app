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
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crdy.powergyan.data.local.history.BatteryHistoryHelper
import com.crdy.powergyan.data.local.history.HistoryPoint
import com.crdy.powergyan.data.local.history.UsageTracker
import com.crdy.powergyan.domain.model.BatteryHealth
import com.crdy.powergyan.domain.model.BatterySnapshot
import com.crdy.powergyan.domain.model.BatteryStatus
import com.crdy.powergyan.domain.model.minutesTo
import com.crdy.powergyan.domain.model.watts
import java.util.Locale

@Composable
fun BatteryDetailsScreen(homeViewModel: HomeViewModel) {
    val context = LocalContext.current
    var lastChargeStarted by remember { mutableStateOf<Long?>(null) }
    var session by remember { mutableStateOf<List<HistoryPoint>>(emptyList()) }
    val batteryState by homeViewModel.batteryState.collectAsState()
    val usage = remember(batteryState) { UsageTracker(context).state() }

    // Session-only extremes, reset when the screen is recreated.
    var tempRange by remember { mutableStateOf<ClosedFloatingPointRange<Float>?>(null) }
    var currentRange by remember { mutableStateOf<IntRange?>(null) }

    LaunchedEffect(batteryState) {
        val snapshot = batteryState ?: return@LaunchedEffect
        val helper = BatteryHistoryHelper(context)
        helper.insertSnapshot(snapshot)
        lastChargeStarted = helper.getLastChargeStarted()
        // The latest charging session: charging rows since the most recent charge start.
        session = lastChargeStarted?.let { start -> helper.getHistory().filter { it.timestamp >= start && it.isCharging && it.powerMw != null } }.orEmpty()
        snapshot.temperatureC?.let { t ->
            tempRange = tempRange?.let { minOf(it.start, t)..maxOf(it.endInclusive, t) } ?: t..t
        }
        snapshot.currentUa?.let { ua ->
            val ma = ua / 1000
            currentRange = currentRange?.let { minOf(it.first, ma)..maxOf(it.last, ma) } ?: ma..ma
        }
    }

    val snapshot = batteryState ?: return LoadingContent()
    val locale = Locale.getDefault()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            BigBatteryHardwareCard(snapshot)
        }

        item {
            AnimatedHealthBox(snapshot.health ?: BatteryHealth.UNKNOWN)
        }

        if (session.size > 2) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Charging Speed Curve", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Power at each battery level in the last session", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(14.dp))
                        ChargingSpeedChart(session, Modifier.fillMaxWidth().height(140.dp))
                    }
                }
            }
        }

        item {
            val est = snapshot.estimatedCapacityMah
            val design = snapshot.designCapacityMah
            val wear = if (est != null && design != null) (est * 100 / design).coerceAtMost(100) else null
            MetricRow(
                Metric("Capacity Now", est?.let { "$it mAh" } ?: "—", if (est == null) "Shown above 50% charge" else "Measured charge counter"),
                Metric("Cell Health", wear?.let { "$it% left" } ?: "—", design?.let { "Original $it mAh" } ?: "Design capacity not exposed")
            )
        }

        if (usage.onMs + usage.offMs > 10 * 60_000) {
            item {
                MetricRow(
                    Metric("Screen On Drain", drainRate(usage.onDrop, usage.onMs), "${usage.onDrop}% in ${formatMinutes((usage.onMs / 60_000).toInt())}"),
                    Metric("Screen Off Drain", drainRate(usage.offDrop, usage.offMs), "${usage.offDrop}% in ${formatMinutes((usage.offMs / 60_000).toInt())}")
                )
            }
        }

        item {
            MetricRow(
                Metric("Current Flow", snapshot.currentUa?.let { "${it / 1000} mA" } ?: "—",
                    currentRange?.let { "Range ${it.first} to ${it.last} mA" }),
                Metric("Power Draw", snapshot.watts()?.let { String.format(locale, "%.2f W", it) } ?: "—",
                    if (snapshot.status == BatteryStatus.CHARGING) "Flowing into battery" else "System consumption")
            )
        }
        item {
            MetricRow(
                Metric("Temperature", snapshot.temperatureC?.let { String.format(locale, "%.1f °C", it) } ?: "—",
                    tempRange?.let { String.format(locale, "Range %.1f to %.1f °C", it.start, it.endInclusive) }),
                Metric("Cell Voltage", snapshot.voltageMv?.let { String.format(locale, "%.2f V", it / 1000f) } ?: "—", null)
            )
        }
        item {
            MetricRow(
                Metric("Charge Cycles", snapshot.cycleCount?.toString() ?: "—", "Hardware-logged lifetime cycles"),
                Metric("Battery Chemistry", snapshot.technology?.ifBlank { null } ?: "Li-ion", null)
            )
        }
        item {
            val toFull = snapshot.minutesTo(100)
            MetricRow(
                Metric("Time to 100%", toFull?.let { formatMinutes(it) } ?: "—", if (toFull == null) "Calculated while plugged in" else "Estimate at current rate"),
                Metric("Last Connected", lastChargeStarted?.let { elapsedLabel(System.currentTimeMillis() - it) } ?: "—", null)
            )
        }
    }
}

/** Animated battery hardware silhouette with dynamic liquid fill */
@Composable
private fun BigBatteryHardwareCard(snapshot: BatterySnapshot) {
    val progress by animateFloatAsState((snapshot.percentage / 100f).coerceIn(0f, 1f), label = "battery fill")
    val accent = if (snapshot.percentage <= 20) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            // Animated Hardware Battery Silhouette
            Box(
                modifier = Modifier.size(90.dp, 130.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = 6.dp.toPx()
                    val w = size.width
                    val h = size.height
                    val capHeight = h * 0.08f
                    val capWidth = w * 0.38f

                    // Battery Terminal Cap
                    drawRoundRect(
                        color = Color.Gray.copy(alpha = 0.5f),
                        topLeft = Offset((w - capWidth) / 2, 0f),
                        size = androidx.compose.ui.geometry.Size(capWidth, capHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                    )

                    // Battery Shell Outline
                    drawRoundRect(
                        color = Color.Gray.copy(alpha = 0.4f),
                        topLeft = Offset(0f, capHeight),
                        size = androidx.compose.ui.geometry.Size(w, h - capHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(10.dp.toPx()),
                        style = Stroke(stroke)
                    )

                    // Liquid Fill with smooth corners
                    val fillHeight = (h - capHeight - stroke * 2) * progress
                    val topOffset = h - stroke - fillHeight
                    drawRoundRect(
                        color = accent,
                        topLeft = Offset(stroke, topOffset),
                        size = androidx.compose.ui.geometry.Size(w - stroke * 2, fillHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx())
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "${snapshot.percentage}%",
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 44.sp, fontWeight = FontWeight.Bold),
                    color = accent
                )
                Text(
                    snapshot.status.label(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "Capacity: ${snapshot.designCapacityMah ?: 5000} mAh",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Rich ambient pulse health card */
@Composable
private fun AnimatedHealthBox(health: BatteryHealth) {
    val infiniteTransition = rememberInfiniteTransition(label = "health transition")
    val colorPhase by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "phase"
    )

    val (baseColor, icon, title, subtitle) = when (health) {
        BatteryHealth.GOOD -> Quadruple(Color(0xFF10B981), Icons.Filled.Check, "Optimal Battery Health", "Android reports healthy internal cell resistance.")
        BatteryHealth.OVERHEAT -> Quadruple(Color(0xFFEF4444), Icons.Filled.Warning, "Battery Overheating!", "Unplug and allow the device to cool down.")
        BatteryHealth.COLD -> Quadruple(Color(0xFF3B82F6), Icons.Filled.Info, "Battery Cold", "Charging speeds are reduced to protect cells.")
        BatteryHealth.OVER_VOLTAGE -> Quadruple(Color(0xFFF59E0B), Icons.Filled.Warning, "Overvoltage Detected", "Try a certified charging cable or brick.")
        BatteryHealth.DEAD, BatteryHealth.UNKNOWN_FAILURE -> Quadruple(Color(0xFFEF4444), Icons.Filled.Warning, "Cell Failure Reported", "Consider having your hardware inspected.")
        BatteryHealth.UNKNOWN -> Quadruple(Color(0xFF6B7280), Icons.Filled.Info, "Standard Health", "System monitoring operational.")
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(baseColor.copy(alpha = 0.16f * colorPhase))
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(baseColor.copy(alpha = 0.25f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = baseColor, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(title, color = baseColor, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(2.dp))
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

private fun drainRate(drop: Int, ms: Long): String =
    if (ms < 5 * 60_000) "—" else String.format(Locale.getDefault(), "%.1f%%/h", drop * 3_600_000f / ms)

@Composable
private fun ChargingSpeedChart(points: List<HistoryPoint>, modifier: Modifier) {
    val line = MaterialTheme.colorScheme.primary
    val grid = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val label = MaterialTheme.colorScheme.onSurfaceVariant
    val peakW = (points.maxOf { it.powerMw ?: 0 } / 1000f).coerceAtLeast(1f)
    val minLevel = points.minOf { it.level }
    val maxLevel = points.maxOf { it.level }.coerceAtLeast(minLevel + 1)
    Column(modifier.semantics { contentDescription = String.format(Locale.getDefault(), "Charging power peaked at %.1f watts", peakW) }) {
        Canvas(Modifier.fillMaxWidth().weight(1f)) {
            for (f in listOf(0.25f, 0.5f, 0.75f)) drawLine(grid, Offset(0f, size.height * f), Offset(size.width, size.height * f), 1.dp.toPx())
            val path = Path()
            points.sortedBy { it.level }.forEachIndexed { i, p ->
                val x = (p.level - minLevel).toFloat() / (maxLevel - minLevel) * size.width
                val y = size.height - (p.powerMw ?: 0) / 1000f / peakW * size.height
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, line, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
            Text("$minLevel%", style = MaterialTheme.typography.labelSmall, color = label, modifier = Modifier.weight(1f))
            Text(String.format(Locale.getDefault(), "Peak %.1f W", peakW), style = MaterialTheme.typography.labelSmall, color = label, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            Text("$maxLevel%", style = MaterialTheme.typography.labelSmall, color = label, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
        }
    }
}

private fun elapsedLabel(elapsedMs: Long): String {
    val minutes = elapsedMs.coerceAtLeast(0L) / 60_000L
    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "$minutes min ago"
        minutes < 48 * 60 -> "${minutes / 60} h ago"
        else -> "${minutes / (24 * 60)} days ago"
    }
}

private data class Metric(val title: String, val value: String, val note: String?)

@Composable
private fun MetricRow(left: Metric, right: Metric) {
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        MetricCard(left, Modifier.weight(1f).fillMaxHeight())
        MetricCard(right, Modifier.weight(1f).fillMaxHeight())
    }
}

@Composable
private fun MetricCard(metric: Metric, modifier: Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(metric.title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(metric.value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            metric.note?.let {
                Spacer(Modifier.height(2.dp))
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
            }
        }
    }
}
