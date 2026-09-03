package com.zekkers.watthome

import android.app.Application
import com.zekkers.watthome.data.StatusRepository
import com.zekkers.watthome.worker.StatusRefreshScheduler

class WattHomeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        StatusRepository.get(this)
        StatusRefreshScheduler.enqueuePeriodic(this)
        StatusRefreshScheduler.enqueueNow(this)
    }
}
