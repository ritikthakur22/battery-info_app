package com.crdy.powergyan.widget

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.os.BatteryManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.crdy.powergyan.MainActivity
import com.crdy.powergyan.data.local.history.BatteryHistoryHelper
import com.crdy.powergyan.data.local.history.HistoryPoint

class BatteryGlanceWidget : GlanceAppWidget() {

    companion object {
        private val SMALL = DpSize(110.dp, 110.dp)
        private val WIDE = DpSize(250.dp, 110.dp)
        private val LARGE = DpSize(250.dp, 200.dp)
    }

    override val sizeMode = SizeMode.Responsive(setOf(SMALL, WIDE, LARGE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme { WidgetContent() }
        }
    }
}

/** Reads fresh values on every recomposition; Glance composes off the main thread, so the DB read is fine here. */
@Composable
private fun WidgetContent() {
    val size = LocalSize.current
    val context = LocalContext.current
    val batteryManager = context.getSystemService(BatteryManager::class.java)
    val percentage = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    val status = if (batteryManager.isCharging) "Charging" else "On battery"
    val history = if (size.width >= 250.dp) runCatching { BatteryHistoryHelper(context).getHistoryBlocking() }.getOrDefault(emptyList()) else emptyList()

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.surface)
            .cornerRadius(20.dp)
            .clickable(actionStartActivity(Intent(context, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = if (size.width < 250.dp) Alignment.CenterHorizontally else Alignment.Start
    ) {
        if (size.width < 250.dp) {
            Reading(percentage, status, 36)
        } else if (size.height < 200.dp) {
            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Reading(percentage, status, 36)
                Spacer(GlanceModifier.width(12.dp))
                Sparkline(history, GlanceModifier.defaultWeight().height(64.dp))
            }
        } else {
            Reading(percentage, status, 48)
            Spacer(GlanceModifier.height(10.dp))
            Sparkline(history, GlanceModifier.fillMaxWidth().defaultWeight())
            Text("Last 24 hours", style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp))
        }
    }
}

@Composable
private fun Reading(percentage: Int, status: String, sizeSp: Int) {
    Column {
        Text("$percentage%", style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = sizeSp.sp, fontWeight = FontWeight.Medium))
        Text(status, style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 13.sp))
    }
}

@Composable
private fun Sparkline(history: List<HistoryPoint>, modifier: GlanceModifier) {
    if (history.size < 2) {
        Text("History appears after a few changes", modifier = modifier, style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp))
        return
    }
    Image(ImageProvider(sparklineBitmap(history)), contentDescription = "Battery level over the last 24 hours", modifier = modifier, contentScale = ContentScale.FillBounds)
}

private fun sparklineBitmap(history: List<HistoryPoint>, width: Int = 480, height: Int = 160): Bitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val pad = 6f
    val minTime = history.first().timestamp
    val span = (history.last().timestamp - minTime).coerceAtLeast(1000).toFloat()
    val path = Path()
    history.forEachIndexed { i, p ->
        val x = pad + (p.timestamp - minTime) / span * (width - 2 * pad)
        val y = pad + (1 - p.level / 100f) * (height - 2 * pad)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    // Mid-tone accent that stays legible on both light and dark launcher themes.
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF4C7BEA.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 5f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    canvas.drawPath(path, paint)
    return bitmap
}
