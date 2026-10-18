package com.crdy.powergyan.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
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
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item { BatteryGauge(snapshot, config, settings.textScale) }
        item { Readings(snapshot, config) }
        item {
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.large) {
                Text(
                    statusAdvice(snapshot, config),
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
        if (history.size > 1) {
            item {
                Text("Last 24 hours", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                Box(Modifier.fillMaxWidth().height(160.dp)) {
                    BatteryGraph(history = history, band = config.takeIf { it.enabled })
                }
            }
        }
        item {
            Text(
                "Updated ${DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(snapshot.timestamp))}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** The ring shows the charge; when Smart Charge is on, the resume–stop band is drawn on the track. */
@Composable
private fun BatteryGauge(snapshot: BatterySnapshot, config: SmartChargeConfig, textScale: Float) {
    val progress by animateFloatAsState((snapshot.percentage / 100f).coerceIn(0f, 1f), label = "battery progress")
    val charging = snapshot.status == BatteryStatus.CHARGING
    val pulse = if (charging) {
        rememberInfiniteTransition(label = "charging").animateFloat(
            0.7f, 1f, infiniteRepeatable(tween(1200), RepeatMode.Reverse), label = "pulse"
        ).value
    } else 1f
    val low = snapshot.percentage <= 20 && !charging
    val accent = if (low) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.onSurface.copy(alpha = .08f)
    val bandColor = MaterialTheme.colorScheme.primary.copy(alpha = .28f)
    val bandText = if (config.enabled) ", kept between ${config.resumeLimit} and ${config.stopLimit} percent" else ""

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .semantics { contentDescription = "Battery ${snapshot.percentage} percent, ${snapshot.status.label()}$bandText" },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(240.dp)) {
            val stroke = 14.dp.toPx()
            val inset = stroke * 1.4f
            val ringSize = androidx.compose.ui.geometry.Size(size.width - inset * 2, size.height - inset * 2)
            val topLeft = Offset(inset, inset)
            drawArc(track, -90f, 360f, false, topLeft, ringSize, style = Stroke(stroke))
            if (config.enabled) {
                // The band sits outside the fill ring so both stay readable.
                val bandInset = inset - stroke * 0.95f
                drawArc(
                    bandColor, -90f + config.resumeLimit * 3.6f, (config.stopLimit - config.resumeLimit) * 3.6f, false,
                    Offset(bandInset, bandInset),
                    androidx.compose.ui.geometry.Size(size.width - bandInset * 2, size.height - bandInset * 2),
                    style = Stroke(stroke * 0.35f, cap = StrokeCap.Round)
                )
            }
            drawArc(accent.copy(alpha = pulse), -90f, 360f * progress, false, topLeft, ringSize, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "${snapshot.percentage}%",
                style = MaterialTheme.typography.displayLarge.let { it.copy(fontSize = it.fontSize * textScale) },
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(snapshot.status.label(), style = MaterialTheme.typography.labelLarge, color = accent)
        }
    }
}

/** A quiet row of the numbers people check while charging. */
@Composable
private fun Readings(snapshot: BatterySnapshot, config: SmartChargeConfig) {
    val target = if (config.enabled) config.stopLimit else 100
    val items = buildList {
        add("Source" to snapshot.plugged.label())
        snapshot.watts()?.takeIf { it >= 0.1f }?.let { add("Power" to String.format(Locale.getDefault(), "%.1f W", it)) }
        snapshot.temperatureC?.let { add("Temp" to String.format(Locale.getDefault(), "%.1f °C", it)) }
        snapshot.minutesTo(target)?.let { add("To $target%" to formatMinutes(it)) }
    }
    Column {
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Row(Modifier.fillMaxWidth().padding(vertical = 14.dp)) {
            items.forEach { (label, value) ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(value, style = MaterialTheme.typography.titleMedium)
                    Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
    }
}

private fun statusAdvice(snapshot: BatterySnapshot, config: SmartChargeConfig): String = when {
    snapshot.status == BatteryStatus.CHARGING && config.enabled ->
        "Charging stops at ${config.stopLimit}% and starts again at ${config.resumeLimit}%."
    snapshot.status == BatteryStatus.CHARGING ->
        "Charging to 100%. Turn on Smart Charge in Control to stop at a level that's easier on the battery."
    snapshot.status == BatteryStatus.FULL -> "Fully charged. You can unplug."
    snapshot.plugged != PluggedState.NONE && config.enabled -> "Plugged in. Charging is paused to protect the battery."
    snapshot.percentage <= 20 -> "Battery is low. Plug in soon."
    else -> "Running on battery."
}

internal fun formatMinutes(minutes: Int) = if (minutes < 60) "$minutes min" else "${minutes / 60} h ${minutes % 60} min"

internal fun BatteryStatus.label() = when (this) {
    BatteryStatus.CHARGING -> "Charging"
    BatteryStatus.DISCHARGING -> "On battery"
    BatteryStatus.FULL -> "Full"
    BatteryStatus.NOT_CHARGING -> "Not charging"
    BatteryStatus.UNKNOWN -> "Unknown"
}

private fun PluggedState.label() = when (this) {
    PluggedState.NONE -> "Battery"
    PluggedState.AC -> "Charger"
    PluggedState.USB -> "USB"
    PluggedState.WIRELESS -> "Wireless"
    PluggedState.UNKNOWN -> "Unknown"
}

@Composable
fun LoadingContent() {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text("Reading battery…", style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun BatteryGraph(history: List<HistoryPoint>, band: SmartChargeConfig? = null) {
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val bandColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        reveal.animateTo(1f, animationSpec = tween(900, easing = FastOutSlowInEasing))
    }
    Canvas(modifier = Modifier.fillMaxSize().semantics { contentDescription = "Battery level over the last 24 hours" }) {
        val width = size.width
        val height = size.height
        fun yFor(level: Int) = height - level / 100f * height

        band?.let {
            drawRect(bandColor, Offset(0f, yFor(it.stopLimit)), androidx.compose.ui.geometry.Size(width, yFor(it.resumeLimit) - yFor(it.stopLimit)))
        }
        for (level in listOf(25, 50, 75)) {
            drawLine(gridColor, Offset(0f, yFor(level)), Offset(width, yFor(level)), 1.dp.toPx())
        }

        val minTime = history.first().timestamp
        val timeRange = (history.last().timestamp - minTime).coerceAtLeast(1000)
        val points = history.map { Offset((it.timestamp - minTime).toFloat() / timeRange * width, yFor(it.level)) }
        val visible = (points.size * reveal.value).toInt().coerceIn(1, points.size)
        val path = Path()
        points.take(visible).forEachIndexed { index, point ->
            if (index == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
        }
        drawPath(path, lineColor, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawCircle(lineColor, radius = 4.dp.toPx(), center = points[visible - 1])
    }
}
