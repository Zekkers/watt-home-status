package com.zekkers.watthome.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.zekkers.watthome.worker.StatusRefreshScheduler

abstract class WattWidgetReceiver : GlanceAppWidgetReceiver() {
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        StatusRefreshScheduler.enqueuePeriodic(context)
        StatusRefreshScheduler.enqueueNow(context)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        StatusRefreshScheduler.enqueuePeriodic(context)
        StatusRefreshScheduler.enqueueNow(context)
        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }
}

class BatteryWidgetReceiver : WattWidgetReceiver() {
    override val glanceAppWidget = BatteryWidget()
}

class BatterySessionWidgetReceiver : WattWidgetReceiver() {
    override val glanceAppWidget = BatterySessionWidget()
}

class GlanceTileWidgetReceiver : WattWidgetReceiver() {
    override val glanceAppWidget = GlanceTileWidget()
}

class StatusWidgetReceiver : WattWidgetReceiver() {
    override val glanceAppWidget = StatusWidget()
}

class StripWidgetReceiver : WattWidgetReceiver() {
    override val glanceAppWidget = StripWidget()
}
