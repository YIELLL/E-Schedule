package com.eschedule.app.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.eschedule.app.data.NotificationPrefs
import com.eschedule.app.data.model.Reminder
import com.eschedule.app.data.model.SchoolClass
import com.eschedule.app.ui.display
import com.eschedule.app.ui.relativeDay
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Turns classes, reminders and the notification settings into system alarms.
 *
 * It is a pure "plan and set" step: [reschedule] cancels what it planned last time and registers the
 * next alarm for every class (its next meeting) and every open reminder. It is re-run whenever the
 * data or the settings change, after an alarm fires, and when the app starts (which also covers
 * reboots, app updates and clock changes).
 */
class NotificationScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val store = context.getSharedPreferences("scheduled_alarms", Context.MODE_PRIVATE)

    private data class Planned(
        val kind: Notifier.Kind,
        val id: Long,
        val triggerAt: LocalDateTime,
        val title: String,
        val text: String,
        val sound: Boolean,
        val vibrate: Boolean,
    ) {
        val key: String get() = "${kind.id}:$id"
    }

    fun reschedule(
        classes: List<SchoolClass>,
        reminders: List<Reminder>,
        prefs: NotificationPrefs,
        now: LocalDateTime = LocalDateTime.now(),
    ) {
        cancelPrevious()

        val planned = buildList {
            if (prefs.classAlerts) {
                classes.forEach { cls -> nextClassAlarm(cls, prefs, now)?.let { add(it) } }
            }
            if (prefs.reminderAlerts) {
                reminders.forEach { reminder -> reminderAlarm(reminder, prefs, now)?.let { add(it) } }
            }
        }

        planned.forEach { schedule(it) }
        store.edit().putStringSet(KEY_PLANNED, planned.map { it.key }.toSet()).apply()
    }

    // ── Planning ──────────────────────────────────────────────────────────────

    /** The next meeting (within two weeks) whose alert time is still in the future. */
    private fun nextClassAlarm(cls: SchoolClass, prefs: NotificationPrefs, now: LocalDateTime): Planned? {
        val lead = prefs.classLeadMinutes.toLong()
        for (offset in 0L..14L) {
            val date = now.toLocalDate().plusDays(offset)
            if (!cls.meetsOn(date)) continue
            val trigger = LocalDateTime.of(date, cls.startTime).minusMinutes(lead)
            if (!trigger.isAfter(now)) continue
            val text = buildString {
                append(if (lead > 0) "Starts in $lead min" else "Starting now")
                append(" · ${cls.startTime.display()}")
                if (cls.location.isNotBlank()) append(" · ${cls.location}")
            }
            return Planned(Notifier.Kind.Class, cls.id, trigger, cls.title, text, prefs.sound, prefs.vibrate)
        }
        return null
    }

    /** All-day reminders alert at 8:00 AM on their day. */
    private fun reminderAlarm(reminder: Reminder, prefs: NotificationPrefs, now: LocalDateTime): Planned? {
        if (reminder.completed) return null
        val lead = prefs.reminderLeadMinutes.toLong()
        val due = LocalDateTime.of(reminder.date, reminder.time ?: LocalTime.of(8, 0))
        val trigger = due.minusMinutes(lead)
        if (!trigger.isAfter(now)) return null

        val text = if (lead == 0L) {
            "Due now"
        } else {
            val day = relativeDay(reminder.date, trigger.toLocalDate())
            "Due $day" + (reminder.time?.let { ", ${it.display()}" } ?: "")
        }
        return Planned(Notifier.Kind.Reminder, reminder.id, trigger, reminder.title, text, prefs.sound, prefs.vibrate)
    }

    // ── Alarm plumbing ────────────────────────────────────────────────────────

    private fun intentFor(kind: Notifier.Kind, id: Long): Intent =
        Intent(context, AlarmReceiver::class.java)
            .setAction(AlarmReceiver.ACTION)
            .setData(Uri.parse("eschedule://alarm/${kind.id}/$id"))   // one PendingIntent per item

    private fun schedule(planned: Planned) {
        val intent = intentFor(planned.kind, planned.id).apply {
            putExtra(AlarmReceiver.EXTRA_KIND, planned.kind.id)
            putExtra(AlarmReceiver.EXTRA_ID, planned.id)
            putExtra(AlarmReceiver.EXTRA_TITLE, planned.title)
            putExtra(AlarmReceiver.EXTRA_TEXT, planned.text)
            putExtra(AlarmReceiver.EXTRA_SOUND, planned.sound)
            putExtra(AlarmReceiver.EXTRA_VIBRATE, planned.vibrate)
        }
        val pending = PendingIntent.getBroadcast(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val triggerMillis = planned.triggerAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val exactAllowed = Build.VERSION.SDK_INT < 31 || alarmManager.canScheduleExactAlarms()
        val exactResult = if (exactAllowed) {
            runCatching { alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pending) }
        } else {
            null
        }
        if (exactResult == null || exactResult.isFailure) {
            // Exact alarms not permitted: the system may deliver this a few minutes late.
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pending)
        }
    }

    private fun cancelPrevious() {
        store.getStringSet(KEY_PLANNED, emptySet()).orEmpty().forEach { key ->
            val parts = key.split(":")
            val kind = Notifier.Kind.entries.firstOrNull { it.id == parts.getOrNull(0) } ?: return@forEach
            val id = parts.getOrNull(1)?.toLongOrNull() ?: return@forEach
            val existing = PendingIntent.getBroadcast(
                context, 0, intentFor(kind, id), PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
            )
            if (existing != null) {
                alarmManager.cancel(existing)
                existing.cancel()
            }
        }
        store.edit().remove(KEY_PLANNED).apply()
    }

    private companion object {
        const val KEY_PLANNED = "planned"
    }
}
