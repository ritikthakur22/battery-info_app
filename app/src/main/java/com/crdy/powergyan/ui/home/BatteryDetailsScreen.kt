package com.crdy.powergyan.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.crdy.powergyan.data.local.history.BatteryHistoryHelper
import com.crdy.powergyan.data.local.history.HistoryPoint
import com.crdy.powergyan.data.local.history.UsageTracker
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.crdy.powergyan.domain.model.BatteryHealth
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
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { HealthSummary(snapshot.health ?: BatteryHealth.UNKNOWN) }

        if (session.size > 2) {
            item {
                Surface(color = MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.medium) {
                    Column(Modifier.padding(14.dp)) {
                        Text("Charging speed", style = MaterialTheme.typography.titleMedium)
                        Text("Power at each battery level in the last session", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(12.dp))
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
                Metric("Capacity now", est?.let { "$it mAh" } ?: "—", if (est == null) "Shown above 50% charge" else "Estimated from the charge counter"),
                Metric("Wear", wear?.let { "$it% left" } ?: "—", design?.let { "Of $it mAh when new" } ?: "Design capacity not reported")
            )
        }

        if (usage.onMs + usage.offMs > 10 * 60_000) {
            item {
                MetricRow(
                    Metric("Screen on", drainRate(usage.onDrop, usage.onMs), "${usage.onDrop}% in ${formatMinutes((usage.onMs / 60_000).toInt())}"),
                    Metric("Screen off", drainRate(usage.offDrop, usage.offMs), "${usage.offDrop}% in ${formatMinutes((usage.offMs / 60_000).toInt())}")
                )
            }
        }

        item {
            MetricRow(
                Metric("Current", snapshot.currentUa?.let { "${it / 1000} mA" } ?: "Not reported",
                    currentRange?.let { "Range ${it.first} to ${it.last} mA" }),
                Metric("Power", snapshot.watts()?.let { String.format(locale, "%.2f W", it) } ?: "Not reported",
                    if (snapshot.status == BatteryStatus.CHARGING) "Into the battery" else "Being used")
            )
        }
        item {
            MetricRow(
                Metric("Temperature", snapshot.temperatureC?.let { String.format(locale, "%.1f °C", it) } ?: "Not reported",
                    tempRange?.let { String.format(locale, "Range %.1f to %.1f °C", it.start, it.endInclusive) }),
                Metric("Voltage", snapshot.voltageMv?.let { String.format(locale, "%.2f V", it / 1000f) } ?: "Not reported", null)
            )
        }
        item {
            MetricRow(
                Metric("Charge cycles", snapshot.cycleCount?.toString() ?: "Not reported", "As counted by the battery"),
                Metric("Technology", snapshot.technology?.ifBlank { null } ?: "Not reported", null)
            )
        }
        item {
            val toFull = snapshot.minutesTo(100)
            MetricRow(
                Metric("Time to full", toFull?.let { formatMinutes(it) } ?: "—", if (toFull == null) "Shown while charging" else "Estimate at current speed"),
                Metric("Last plugged in", lastChargeStarted?.let { elapsedLabel(System.currentTimeMillis() - it) } ?: "—", null)
            )
        }
    }
}

private fun drainRate(drop: Int, ms: Long): String =
    if (ms < 5 * 60_000) "—" else String.format(Locale.getDefault(), "%.1f%%/h", drop * 3_600_000f / ms)

/** Power (y) against battery level (x): the shape shows where the phone slows charging down. */
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
            drawPath(path, line, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
            Text("$minLevel%", style = MaterialTheme.typography.labelMedium, color = label, modifier = Modifier.weight(1f))
            Text(String.format(Locale.getDefault(), "Peak %.1f W", peakW), style = MaterialTheme.typography.labelMedium, color = label, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            Text("$maxLevel%", style = MaterialTheme.typography.labelMedium, color = label, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
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
    Surface(modifier = modifier, color = MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.medium) {
        Column(Modifier.padding(14.dp)) {
            Text(metric.title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            Text(metric.value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            metric.note?.let {
                Spacer(Modifier.height(2.dp))
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun HealthSummary(health: BatteryHealth) {
    // Android only reports coarse health states; say what it means rather than grade the battery.
    val (title, detail, warn) = when (health) {
        BatteryHealth.GOOD -> Triple("Good", "Android reports no battery problems.", false)
        BatteryHealth.OVERHEAT -> Triple("Overheating", "Unplug and let the phone cool down.", true)
        BatteryHealth.COLD -> Triple("Too cold", "Charging may be slow until the phone warms up.", true)
        BatteryHealth.OVER_VOLTAGE -> Triple("Over voltage", "Try a different charger or cable.", true)
        BatteryHealth.DEAD, BatteryHealth.UNKNOWN_FAILURE -> Triple("Failing", "The battery reports a fault. Consider a service check.", true)
        BatteryHealth.UNKNOWN -> Triple("Not reported", "This phone doesn't share battery health.", false)
    }
    val container = if (warn) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
    val content = if (warn) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
    Surface(color = container, contentColor = content, shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Text("Battery health", style = MaterialTheme.typography.labelLarge)
            Text(title, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(4.dp))
            Text(detail, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
