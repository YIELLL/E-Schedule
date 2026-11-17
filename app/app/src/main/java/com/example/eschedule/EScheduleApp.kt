package com.example.eschedule

import android.app.Application
import com.example.eschedule.data.AppContainer
import com.example.eschedule.data.AppLog

class EScheduleApp : Application() {
    val container: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        AppLog.init(this)
        container.startNotificationSync()
    }
}
