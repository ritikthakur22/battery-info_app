package com.crdy.batterygyan.widget

import android.content.Context
import android.os.BatteryManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.crdy.batterygyan.MainActivity

class BatteryGlanceWidget : GlanceAppWidget() {

    companion object {
        private val SMALL_SQUARE = DpSize(100.dp, 100.dp)
        private val HORIZONTAL_RECTANGLE = DpSize(200.dp, 100.dp)
        private val LARGE_SQUARE = DpSize(200.dp, 200.dp)
    }

    override val sizeMode = SizeMode.Responsive(
        setOf(SMALL_SQUARE, HORIZONTAL_RECTANGLE, LARGE_SQUARE)
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                WidgetContent()
            }
        }
    }
}

@Composable
fun WidgetContent() {
    val size = LocalSize.current
    val context = LocalContext.current
    
    // Quick local read for the widget without background coroutine polling
    val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    val percentage = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    val isCharging = batteryManager.isCharging

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.surface)
            .clickable(actionStartActivity(android.content.Intent(context, MainActivity::class.java).apply { flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK }))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (size.width < 150.dp) {
            // Small Widget
            Text(
                text = "$percentage%",
                style = TextStyle(
                    color = GlanceTheme.colors.primary,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        } else if (size.height < 150.dp) {
            // Medium Horizontal Widget
            Text(
                text = "$percentage%",
                style = TextStyle(
                    color = GlanceTheme.colors.primary,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = if (isCharging) "Charging" else "Normal",
                style = TextStyle(
                    color = GlanceTheme.colors.secondary,
                    fontSize = 16.sp
                )
            )
        } else {
            // Large Widget
            Text(
                text = "$percentage%",
                style = TextStyle(
                    color = GlanceTheme.colors.primary,
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = if (isCharging) "Charging" else "Discharging",
                style = TextStyle(
                    color = GlanceTheme.colors.secondary,
                    fontSize = 20.sp
                )
            )
        }
    }
}
