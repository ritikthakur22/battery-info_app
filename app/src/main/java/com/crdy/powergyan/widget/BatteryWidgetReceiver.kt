package com.crdy.powergyan.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/** Refreshed by BatteryMonitorService on each history change, and by the system every 30 minutes. */
class BatteryWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BatteryGlanceWidget()
}
