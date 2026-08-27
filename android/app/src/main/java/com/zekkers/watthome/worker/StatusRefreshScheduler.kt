package com.zekkers.watthome.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.zekkers.watthome.data.HomeStatus
import com.zekkers.watthome.data.RefreshCadence
import java.util.concurrent.TimeUnit

object StatusRefreshScheduler {
    private const val PERIODIC_WORK = "watt-home-status-periodic"
    private const val ONCE_WORK = "watt-home-status-once"
    private const val FOLLOW_UP_WORK = "watt-home-status-followup"

    private val networkConstraints: Constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    fun enqueuePeriodic(context: Context) {
        val request = PeriodicWorkRequestBuilder<StatusRefreshWorker>(
            RefreshCadence.IDLE_PERIOD_MINUTES,
            TimeUnit.MINUTES
        )
            .setInputData(workDataOf(RefreshCadence.KEY_INCLUDE_SERIES to true))
            .setConstraints(networkConstraints)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun enqueueNow(context: Context, includeSeries: Boolean = true) {
        val request = OneTimeWorkRequestBuilder<StatusRefreshWorker>()
            .setInputData(workDataOf(RefreshCadence.KEY_INCLUDE_SERIES to includeSeries))
            .setConstraints(networkConstraints)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            ONCE_WORK,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun scheduleAfterSuccess(
        context: Context,
        status: HomeStatus,
        previousSocPercent: Int?,
        liveOk: Boolean
    ) {
        if (liveOk && RefreshCadence.needsFastPoll(status, previousSocPercent)) {
            enqueueFollowUp(context)
        } else {
            cancelFollowUp(context)
        }
    }

    /**
     * One-shot follow-up ~90s later. Not expedited: WorkManager forbids
     * setExpedited + setInitialDelay, and expedited work needs a foreground
     * notification on API 26–30. Delayed one-time work is enough while charging.
     */
    fun enqueueFollowUp(context: Context) {
        val request = OneTimeWorkRequestBuilder<StatusRefreshWorker>()
            .setInitialDelay(RefreshCadence.FOLLOW_UP_SECONDS, TimeUnit.SECONDS)
            .setInputData(workDataOf(RefreshCadence.KEY_INCLUDE_SERIES to false))
            .setConstraints(networkConstraints)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            FOLLOW_UP_WORK,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancelFollowUp(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(FOLLOW_UP_WORK)
    }
}
