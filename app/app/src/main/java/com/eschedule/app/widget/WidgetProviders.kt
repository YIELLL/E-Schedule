package com.eschedule.app.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.Context
import android.os.Bundle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** Redraws every widget off the main thread, keeping the broadcast alive until it is done. */
internal fun BroadcastReceiver.refreshAsync(context: Context) {
    val pending = goAsync()
    val appContext = context.applicationContext
    CoroutineScope(Dispatchers.Default).launch {
        try {
            withTimeoutOrNull(8_000) { WidgetUpdater.update(appContext) }
        } catch (_: Throwable) {
            // A failed redraw just leaves the previous content; the next change retries.
        } finally {
            pending.finish()
        }
    }
}

abstract class BaseWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) =
        refreshAsync(context)

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) = refreshAsync(context)

    override fun onDeleted(context: Context, appWidgetIds: IntArray) = refreshAsync(context)

    override fun onDisabled(context: Context) = refreshAsync(context)
}

class TodayWidgetProvider : BaseWidgetProvider()

/** Fires at the next class start / end (or midnight) so the widgets move on by themselves. */
class WidgetRefreshReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: android.content.Intent) = refreshAsync(context)
}
