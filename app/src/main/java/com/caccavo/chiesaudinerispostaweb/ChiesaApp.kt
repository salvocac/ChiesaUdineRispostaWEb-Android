package com.caccavo.chiesaudinerispostaweb

import android.app.Application
import com.caccavo.chiesaudinerispostaweb.notifications.DailyVerseNotificationScheduler
import com.caccavo.chiesaudinerispostaweb.review.CurrentActivityTracker

class ChiesaApp : Application() {

    override fun onCreate() {
        super.onCreate()
        CurrentActivityTracker.install(this)
        DailyVerseNotificationScheduler.createNotificationChannel(this)
    }
}
