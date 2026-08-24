package com.zekkers.watthome

import android.app.Application
import com.zekkers.watthome.worker.StatusRefreshScheduler

class WattHomeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        StatusRefreshScheduler.enqueuePeriodic(this)
        StatusRefreshScheduler.enqueueNow(this)
    }
}
