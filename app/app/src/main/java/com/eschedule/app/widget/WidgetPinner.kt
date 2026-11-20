package com.eschedule.app.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.os.Build

/** Asks the launcher to place one of the app's widgets on the home screen (Android 8+). */
object WidgetPinner {

    enum class Result { Requested, Unsupported }

    fun pinToday(context: Context): Result = pin(context, TodayWidgetProvider::class.java)

    private fun pin(context: Context, provider: Class<*>): Result {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return Result.Unsupported
        val manager = AppWidgetManager.getInstance(context)
        if (!manager.isRequestPinAppWidgetSupported) return Result.Unsupported
        val accepted = manager.requestPinAppWidget(ComponentName(context, provider), null, null)
        return if (accepted) Result.Requested else Result.Unsupported
    }
}
