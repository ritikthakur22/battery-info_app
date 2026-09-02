package com.crdy.powergyan.ui.home

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crdy.powergyan.domain.model.BatteryHealth
import com.crdy.powergyan.domain.model.BatterySnapshot
import com.crdy.powergyan.domain.model.BatteryStatus
import com.crdy.powergyan.domain.model.DisplaySettings

import androidx.compose.ui.platform.LocalContext
import com.crdy.powergyan.data.local.history.BatteryHistoryHelper
import com.crdy.powergyan.data.local.history.HistoryPoint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import kotlin.math.roundToInt

import com.crdy.powergyan.monetization.TestAdBanner
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(viewModel: HomeViewModel, settings: DisplaySettings = DisplaySettings(), showAds: Boolean = false) {
    val batteryState by viewModel.batteryState.collectAsState()
    val context = LocalContext.current
    var history by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<List<HistoryPoint>>(emptyList()) }
    
    androidx.compose.runtime.LaunchedEffect(batteryState) {
        batteryState?.let { snapshot ->
            val helper = BatteryHistoryHelper(context)
            helper.insertSnapshot(snapshot)
            history = helper.getHistory()
        }
    }
    
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        batteryState?.let { BatteryContent(it, settings, showAds, history = history, onRefresh = { viewModel.refresh() }, config = settings.smartChargeConfig) } ?: LoadingContent()
    }
}

@Composable
fun BatteryContent(snapshot: BatterySnapshot, settings: DisplaySettings, showAds: Boolean = false, history: List<HistoryPoint> = emptyList(), onRefresh: () -> Unit, config: com.crdy.powergyan.domain.model.SmartChargeConfig? = null) {
    val scale = settings.textScale
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("POWER GYAN", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Text("Your power, at a glance", style = MaterialTheme.typography.headlineSmall.copy(fontSize = 25.sp * scale), fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Refresh Data", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
        item { BatteryHero(snapshot, scale, config) }
        
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
        if (history.size > 1) {
            item {
                Text("Charge Curve", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), 
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth().height(180.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        BatteryGraph(history = history)
                    }
                }
            }
        }
        
        item {
            val footerText = "Made with ❤️"
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Updated ${DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(snapshot.timestamp))}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = .6f))
                Text(footerText, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(8.dp))
        }
        if (showAds) item { TestAdBanner(Modifier.fillMaxWidth().padding(bottom = 6.dp)) }
    }
}

@Composable
private fun BatteryHero(snapshot: BatterySnapshot, scale: Float, config: com.crdy.powergyan.domain.model.SmartChargeConfig?) {
    val progress by animateFloatAsState((snapshot.percentage / 100f).coerceIn(0f, 1f), label = "battery progress")
    val infinite = rememberInfiniteTransition(label = "charging pulse")
    val pulse by infinite.animateFloat(0.88f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "pulse")
    val accent = if (snapshot.percentage <= 20) MaterialTheme.colorScheme.error
        else MaterialTheme.colorScheme.primary
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
                Text(if (snapshot.status == BatteryStatus.CHARGING) "⚡ Charging" else "● ${snapshot.status.toLabel()}", style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp * scale), fontWeight = FontWeight.Bold, color = accent)
                Text("${snapshot.plugged.toLabel()} connection", style = MaterialTheme.typography.bodyMedium)
                
                // Add Wattage Pill
                if (snapshot.currentUa != null && snapshot.voltageMv != null && snapshot.currentUa != 0) {
                    val cMa = Math.abs(snapshot.currentUa) / 1000f
                    val vV = snapshot.voltageMv / 1000f
                    val watts = (cMa * vV) / 1000f
                    if (watts > 0.5f) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = String.format("⚡ %.1fW Power", watts),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                
                if (snapshot.status == BatteryStatus.CHARGING && snapshot.currentUa != null && snapshot.chargeCounterUah != null && snapshot.currentUa != 0) {
                    val currentMa = Math.abs(snapshot.currentUa) / 1000
                    val capacityMah = snapshot.chargeCounterUah / 1000
                    val totalMah = if (snapshot.percentage > 0) (capacityMah * 100) / snapshot.percentage else 0
                    val target = if (config?.enabled == true) config.stopLimit else 100
                    if (target > snapshot.percentage && currentMa > 0 && totalMah > 0) {
                        val missingMah = totalMah * (target - snapshot.percentage) / 100
                        val hoursLeft = missingMah.toFloat() / currentMa.toFloat()
                        val minsLeft = (hoursLeft * 60).toInt()
                        if (minsLeft in 1..600) {
                            Text("~${minsLeft} mins to ${target}%", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

private fun BatteryStatus.toLabel() = name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }
private fun com.crdy.powergyan.domain.model.PluggedState.toLabel() = name.lowercase().replaceFirstChar { it.uppercase() }

@Composable
fun LoadingContent() {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text("Reading battery…", style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
fun BatteryGraph(history: List<HistoryPoint>) {
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val reveal = androidx.compose.runtime.remember { Animatable(0f) }
    androidx.compose.runtime.LaunchedEffect(history) {
        reveal.snapTo(0f)
        reveal.animateTo(1f, animationSpec = tween(1400, easing = FastOutSlowInEasing))
    }
    val glowAlpha = 0.18f + (0.08f * reveal.value)
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        val minTime = history.first().timestamp
        val maxTime = history.last().timestamp.coerceAtLeast(minTime + 1000)
        val timeRange = maxTime - minTime

        val points = history.map { point ->
            val x = ((point.timestamp - minTime).toFloat() / timeRange) * width
            val y = height - ((point.level.toFloat() / 100f) * height)
            androidx.compose.ui.geometry.Offset(x, y)
        }
        for (gridIndex in 1..4) {
            val fraction = gridIndex / 4f
            val y = height - (height * fraction)
            drawLine(gridColor, androidx.compose.ui.geometry.Offset(0f, y), androidx.compose.ui.geometry.Offset(width, y), 1.dp.toPx())
        }
        val path = Path()
        val visibleCount = (points.size * reveal.value).coerceAtLeast(1f).toInt().coerceAtMost(points.size)
        points.take(visibleCount).forEachIndexed { index, point ->
            if (index == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
        }
        drawPath(path = path, color = lineColor.copy(alpha = glowAlpha), style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(path = path, color = lineColor, style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        points.lastOrNull()?.let { point ->
            drawCircle(lineColor, radius = 7.dp.toPx(), center = point)
        }
    }
}
