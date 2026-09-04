package com.zekkers.watthome.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.zekkers.watthome.data.RefreshCadence
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
        val previousSoc = repository.uiState.value.status?.socPercent
        val includeSeries = inputData.getBoolean(RefreshCadence.KEY_INCLUDE_SERIES, true)
        return try {
            val status = repository.refresh(includeSeries = includeSeries)
            WidgetUpdater.updateAll(applicationContext)
            StatusRefreshScheduler.scheduleAfterSuccess(
                context = applicationContext,
                status = status,
                previousSocPercent = previousSoc,
                liveOk = repository.uiState.value.liveOk
            )
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
