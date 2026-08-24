package com.zekkers.watthome.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.zekkers.watthome.worker.StatusRefreshScheduler

class StatusWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = StatusWidget()

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
