package com.zekkers.watthome

import android.app.Application
import com.zekkers.watthome.worker.StatusRefreshScheduler

class WattHomeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // No StatusRepository / EncryptedPrefs / network here — first frame
        // hydrates the compact SharedPreferences snapshot on its own.
        StatusRefreshScheduler.enqueuePeriodic(this)
        StatusRefreshScheduler.enqueueNow(this)
    }
}
