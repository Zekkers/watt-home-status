package com.zekkers.watthome.widget

import android.content.Context
import android.content.Intent
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.zekkers.watthome.MainActivity
import com.zekkers.watthome.data.StatusRepository
import com.zekkers.watthome.worker.StatusRefreshScheduler

class WidgetTapAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val app = context.applicationContext
        val repository = StatusRepository.get(app)
        val previousSoc = repository.cachedStatus()?.socPercent
        runCatching {
            val status = repository.refresh(includeSeries = true)
            WidgetUpdater.updateAll(app)
            StatusRefreshScheduler.scheduleAfterSuccess(
                context = app,
                status = status,
                previousSocPercent = previousSoc,
                liveOk = repository.uiState.value.liveOk
            )
        }.onFailure {
            StatusRefreshScheduler.enqueueNow(app, includeSeries = true)
        }
        val open = Intent(app, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        runCatching { app.startActivity(open) }
    }
}
