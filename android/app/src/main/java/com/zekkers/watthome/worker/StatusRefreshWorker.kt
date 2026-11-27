package com.zekkers.watthome.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.zekkers.watthome.data.HistoryFetchPolicy
import com.zekkers.watthome.data.RefreshErrors
import com.zekkers.watthome.data.StatusRepository
import com.zekkers.watthome.widget.WidgetUpdater
import kotlinx.coroutines.CancellationException

class StatusRefreshWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val repository = StatusRepository.get(applicationContext)
        repository.loadCached()
        val trigger = HistoryFetchPolicy.Trigger.fromWire(
            inputData.getString(HistoryFetchPolicy.KEY_TRIGGER)
        )
        return try {
            repository.refresh(trigger)
            WidgetUpdater.updateAll(applicationContext)
            Result.success()
        } catch (error: CancellationException) {
            if (RefreshErrors.isStructuredCancellation(error)) {
                if (isStopped) Result.success() else throw error
            } else {
                WidgetUpdater.updateAll(applicationContext)
                Result.retry()
            }
        } catch (_: Exception) {
            WidgetUpdater.updateAll(applicationContext)
            Result.retry()
        }
    }
}
