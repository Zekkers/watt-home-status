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
import com.zekkers.watthome.data.HistoryFetchPolicy
import java.util.concurrent.TimeUnit

object StatusRefreshScheduler {
    private const val PERIODIC_WORK = "watt-home-status-periodic"
    private const val ONCE_WORK = "watt-home-status-once"

    /** Pre-1.2.23 one-shot ~90s follow-up. Cancelled so an upgrade cannot keep polling. */
    private const val LEGACY_FAST_POLL_WORK = "watt-home-status-followup"

    private val networkConstraints: Constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    fun enqueuePeriodic(context: Context) {
        cancelLegacyFastPoll(context)
        val request = PeriodicWorkRequestBuilder<StatusRefreshWorker>(
            HistoryFetchPolicy.BACKGROUND_PERIOD_MINUTES,
            TimeUnit.MINUTES
        )
            .setInputData(backgroundTrigger())
            .setConstraints(networkConstraints)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    /** Widget add, widget update, or process start. Not an app open. */
    fun enqueueNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<StatusRefreshWorker>()
            .setInputData(backgroundTrigger())
            .setConstraints(networkConstraints)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            ONCE_WORK,
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    private fun backgroundTrigger() = workDataOf(
        HistoryFetchPolicy.KEY_TRIGGER to HistoryFetchPolicy.Trigger.BACKGROUND.wire()
    )

    private fun cancelLegacyFastPoll(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(LEGACY_FAST_POLL_WORK)
    }
}
