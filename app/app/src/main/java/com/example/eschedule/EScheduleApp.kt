package com.example.eschedule

import android.app.Application
import com.example.eschedule.data.AppContainer

class EScheduleApp : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
