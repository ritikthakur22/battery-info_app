package com.crdy.powergyan.ui.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.crdy.powergyan.data.local.history.BatteryHistoryHelper
import com.crdy.powergyan.data.local.history.HistoryPoint
import com.crdy.powergyan.domain.model.BatteryHealth
import com.crdy.powergyan.domain.model.BatterySnapshot
import com.crdy.powergyan.domain.model.BatteryStatus
import com.crdy.powergyan.monetization.TestNativeAdCard
import kotlinx.coroutines.launch

@Composable
fun BatteryDetailsScreen(showAds: Boolean = false, homeViewModel: HomeViewModel) {
    val context = LocalContext.current
    var history by remember { mutableStateOf<List<HistoryPoint>>(emptyList()) }
    var lastChargeStarted by remember { mutableStateOf<Long?>(null) }
    val batteryState by homeViewModel.batteryState.collectAsState()
    
    var minTemp by remember { mutableStateOf(batteryState?.temperatureC ?: 0f) }
    var maxTemp by remember { mutableStateOf(batteryState?.temperatureC ?: 0f) }
    var minCurrent by remember { mutableStateOf(batteryState?.currentUa ?: 0) }
    var maxCurrent by remember { mutableStateOf(batteryState?.currentUa ?: 0) }

    LaunchedEffect(batteryState) {
        batteryState?.let { snapshot ->
            val helper = BatteryHistoryHelper(context)
            helper.insertSnapshot(snapshot)
            history = helper.getHistory()
            lastChargeStarted = helper.getLastChargeStarted()
        }
        batteryState?.temperatureC?.let {
            if (minTemp == 0f || it < minTemp) minTemp = it
            if (it > maxTemp) maxTemp = it
        }
        batteryState?.currentUa?.let {
            if (minCurrent == 0 || it < minCurrent) minCurrent = it
            if (it > maxCurrent) maxCurrent = it
        }
    }

    LaunchedEffect(Unit) {
        val helper = BatteryHistoryHelper(context)
        history = helper.getHistory()
        lastChargeStarted = helper.getLastChargeStarted()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Battery Details", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
        
        batteryState?.let { snapshot ->
            item { 
                BigBatteryIcon(snapshot)
            }
            
            item {
                AnimatedHealthBox(health = snapshot.health ?: BatteryHealth.UNKNOWN)
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val currentMa = (snapshot.currentUa ?: 0) / 1000f
                    val voltageV = (snapshot.voltageMv ?: 0) / 1000f
                    val watts = currentMa * voltageV / 1000f
                    
                    MetricCard(
                        title = "Current (mA)", 
                        value = "${currentMa} mA", 
                        subtext = "Min: ${minCurrent / 1000} | Max: ${maxCurrent / 1000}",
                        icon = Icons.Filled.Check,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Power (W)", 
                        value = String.format("%.2f W", Math.abs(watts)), 
                        subtext = if (snapshot.status == BatteryStatus.CHARGING) "Charging" else "Discharging",
                        icon = Icons.Filled.Check,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricCard(
                        title = "Temperature", 
                        value = "${snapshot.temperatureC ?: "--"} °C", 
                        subtext = "Min: $minTemp | Max: $maxTemp",
                        icon = Icons.Filled.Info,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Voltage", 
                        value = "${snapshot.voltageMv ?: "--"} mV", 
                        subtext = "Current Voltage",
                        icon = Icons.Filled.Check,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            item {
                MetricCard(
                    title = "Last Charged Time",
                    value = lastChargeStarted?.let { elapsedLabel(System.currentTimeMillis() - it) } ?: "Not enough data to show",
                    subtext = "Since charging started",
                    icon = Icons.Filled.DateRange,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        
        item {
            Card(
                modifier = Modifier.fillMaxWidth().height(250.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Charge/Discharge Curve", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(16.dp))
                    if (history.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Not enough data yet. Leave the app running!")
                        }
                    } else {
                        BatteryGraph(history)
                    }
                }
            }
        }
        
        if (showAds) {
            item {
                TestNativeAdCard(Modifier.fillMaxWidth())
            }
        }
    }
}

private fun elapsedLabel(elapsedMs: Long): String {
    val minutes = elapsedMs.coerceAtLeast(0L) / 60_000L
    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "$minutes min ago"
        else -> "${minutes / 60} hr ago"
    }
}

@Composable
fun BigBatteryIcon(snapshot: BatterySnapshot) {
    val progress by animateFloatAsState((snapshot.percentage / 100f).coerceIn(0f, 1f), label = "battery fill")
    val accentColor = MaterialTheme.colorScheme.primary
    
    Box(modifier = Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(100.dp, 160.dp)) {
            val stroke = 8.dp.toPx()
            val w = size.width
            val h = size.height
            val capHeight = h * 0.08f
            val capWidth = w * 0.4f
            
            // Draw Cap
            drawRoundRect(
                color = Color.Gray,
                topLeft = androidx.compose.ui.geometry.Offset((w - capWidth) / 2, 0f),
                size = androidx.compose.ui.geometry.Size(capWidth, capHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
            )
            
            // Draw Body Outline
            drawRoundRect(
                color = Color.Gray,
                topLeft = androidx.compose.ui.geometry.Offset(0f, capHeight),
                size = androidx.compose.ui.geometry.Size(w, h - capHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()),
                style = Stroke(stroke)
            )
            
            // Draw Fill
            val fillHeight = (h - capHeight - stroke * 2) * progress
            val topOffset = h - stroke - fillHeight
            drawRoundRect(
                color = accentColor,
                topLeft = androidx.compose.ui.geometry.Offset(stroke, topOffset),
                size = androidx.compose.ui.geometry.Size(w - stroke * 2, fillHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
            )
        }
        Text(
            text = "${snapshot.percentage}%",
            color = if (snapshot.percentage > 40) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun MetricCard(title: String, value: String, subtext: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(2.dp))
            Text(subtext, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun AnimatedHealthBox(health: BatteryHealth) {
    val infiniteTransition = rememberInfiniteTransition()
    val colorPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val (baseColor, icon, text) = when (health) {
        BatteryHealth.GOOD -> Triple(Color(0xFF4CAF50), Icons.Filled.Info, "Excellent Health")
        BatteryHealth.OVERHEAT -> Triple(Color(0xFFF44336), Icons.Filled.Warning, "Overheating!")
        BatteryHealth.COLD -> Triple(Color(0xFF2196F3), Icons.Filled.Warning, "Too Cold")
        else -> Triple(Color(0xFFFF9800), Icons.Filled.Warning, "Needs Attention")
    }
    
    val animatedColor = baseColor.copy(alpha = 0.7f + (0.3f * colorPhase))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(animatedColor)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Battery Health", color = Color.White, style = MaterialTheme.typography.labelLarge)
                Text(text, color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun BatteryGraph(history: List<HistoryPoint>) {
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(history) {
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
