package com.eschedule.app

import android.app.Application
import com.eschedule.app.data.AppContainer

class EScheduleApp : Application() {
    val container: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        container.startNotificationSync()
    }
}
