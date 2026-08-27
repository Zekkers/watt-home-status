package com.zekkers.watthome.widget

import android.content.Context
import androidx.glance.appwidget.updateAll

object WidgetUpdater {
    suspend fun updateAll(context: Context) {
        BatteryWidget().updateAll(context)
        BatterySessionWidget().updateAll(context)
        GlanceTileWidget().updateAll(context)
        StatusWidget().updateAll(context)
        StripWidget().updateAll(context)
    }
}
