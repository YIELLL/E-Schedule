package com.eschedule.app.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.eschedule.app.EScheduleApp
import com.eschedule.app.MainActivity
import com.eschedule.app.R
import com.eschedule.app.data.model.SchoolClass
import com.eschedule.app.ui.display
import com.eschedule.app.ui.relativeDay
import com.eschedule.app.ui.timeRange
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Builds and pushes the content of the home-screen widget. */
object WidgetUpdater {

    private const val DEFAULT_ROWS = 3
    private const val MAX_ROWS = 6
    private const val REFRESH_REQUEST_CODE = 7001
    private val headerDate = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.US)

    suspend fun update(context: Context) {
        val app = context.applicationContext
        val manager = AppWidgetManager.getInstance(app)
        val todayIds = manager.getAppWidgetIds(ComponentName(app, TodayWidgetProvider::class.java))

        if (todayIds.isEmpty()) {
            cancelRefresh(app)
            return
        }

        val classes = (app as EScheduleApp).container.classRepository.classes.first()
        val now = LocalDateTime.now()

        todayIds.forEach { id ->
            val rows = rowsFor(manager.getAppWidgetOptions(id).getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0))
            manager.updateAppWidget(id, todayViews(app, classes, now, rows))
        }
        scheduleRefresh(app, classes, now)
    }

    // ── Today ────────────────────────────────────────────────────────────────

    /** How many class rows fit in a widget of [minHeightDp] (header and "+N more" line reserved). */
    private fun rowsFor(minHeightDp: Int): Int =
        if (minHeightDp <= 0) DEFAULT_ROWS else ((minHeightDp - 66) / 34).coerceIn(1, MAX_ROWS)

    private fun todayViews(context: Context, classes: List<SchoolClass>, now: LocalDateTime, maxRows: Int): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_today)
        val today = now.toLocalDate()
        val time = now.toLocalTime()

        views.setTextViewText(R.id.today_title, "Today")
        views.setTextViewText(R.id.today_date, today.format(headerDate))
        views.removeAllViews(R.id.today_rows)

        val allToday = classes.filter { it.meetsOn(today) }
        val remaining = allToday.filter { it.endTime > time }.sortedBy { it.startTime }

        if (remaining.isEmpty()) {
            views.setViewVisibility(R.id.today_rows, android.view.View.GONE)
            views.setViewVisibility(R.id.today_more, android.view.View.GONE)
            views.setViewVisibility(R.id.today_empty, android.view.View.VISIBLE)
            views.setTextViewText(
                R.id.today_empty,
                if (allToday.isEmpty()) "No classes today" else "You're all done for today",
            )
        } else {
            views.setViewVisibility(R.id.today_rows, android.view.View.VISIBLE)
            views.setViewVisibility(R.id.today_empty, android.view.View.GONE)
            remaining.take(maxRows).forEach { cls ->
                views.addView(R.id.today_rows, rowViews(context, cls, live = cls.startTime <= time))
            }
            val hidden = remaining.size - maxRows
            if (hidden > 0) {
                views.setTextViewText(R.id.today_more, "+$hidden more")
                views.setViewVisibility(R.id.today_more, android.view.View.VISIBLE)
            } else {
                views.setViewVisibility(R.id.today_more, android.view.View.GONE)
            }
        }
        views.setOnClickPendingIntent(R.id.today_root, openAppIntent(context))
        return views
    }

    private fun rowViews(context: Context, cls: SchoolClass, live: Boolean): RemoteViews {
        val row = RemoteViews(context.packageName, R.layout.widget_today_row)
        row.setTextViewText(R.id.row_time, cls.startTime.display())
        row.setTextViewText(R.id.row_title, cls.title)
        row.setTextViewText(R.id.row_room, cls.location.ifBlank { "No room" })
        if (live) {
            row.setInt(R.id.row_root, "setBackgroundResource", R.drawable.widget_row_now)
            val accent = ContextCompat.getColor(context, R.color.widget_accent_text)
            row.setTextColor(R.id.row_time, accent)
            row.setTextColor(R.id.row_title, accent)
        }
        return row
    }

    // ── Shared ───────────────────────────────────────────────────────────────

    private fun openAppIntent(context: Context): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun refreshIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        REFRESH_REQUEST_CODE,
        Intent(context, WidgetRefreshReceiver::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun cancelRefresh(context: Context) {
        context.getSystemService(AlarmManager::class.java)?.cancel(refreshIntent(context))
    }

    /** Wakes the widgets at the next moment their content changes: a class starting or ending, or midnight. */
    private fun scheduleRefresh(context: Context, classes: List<SchoolClass>, now: LocalDateTime) {
        val today = now.toLocalDate()
        val time = now.toLocalTime()
        val boundaries = classes.filter { it.meetsOn(today) }
            .flatMap { listOf(it.startTime, it.endTime) }
            .filter { it > time }
            .map { today.atTime(it) }
        val midnight = today.plusDays(1).atStartOfDay()
        val next = (boundaries + midnight).min()
        val triggerAt = next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() + 1_000L

        context.getSystemService(AlarmManager::class.java)
            ?.setAndAllowWhileIdle(AlarmManager.RTC, triggerAt, refreshIntent(context))
    }
}
