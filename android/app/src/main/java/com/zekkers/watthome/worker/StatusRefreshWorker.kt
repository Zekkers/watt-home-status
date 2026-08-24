package com.zekkers.watthome.worker

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.zekkers.watthome.data.StatusRepository
import com.zekkers.watthome.widget.StatusWidget

class StatusRefreshWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val repository = StatusRepository.get(applicationContext)
        repository.loadCached()
        return try {
            repository.refresh()
            StatusWidget().updateAll(applicationContext)
            Result.success()
        } catch (_: Exception) {
            StatusWidget().updateAll(applicationContext)
            Result.retry()
        }
    }
}
