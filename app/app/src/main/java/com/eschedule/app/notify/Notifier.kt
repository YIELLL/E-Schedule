package com.eschedule.app.notify

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.eschedule.app.MainActivity
import com.eschedule.app.R

/** Builds and posts the class / reminder notifications. */
object Notifier {

    enum class Kind(val id: String, val channelName: String, val channelDescription: String) {
        Class("class", "Class reminders", "A heads-up before a class starts"),
        Reminder("reminder", "Reminder alerts", "When one of your reminders is due"),
    }

    /** Notifications are allowed: the runtime permission (Android 13+) and the system switch. */
    fun canPost(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return false
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    /**
     * Channel settings can't be changed once created, so the sound / vibrate choice is part of the
     * channel id: each combination gets its own channel and the right one is picked per alert.
     */
    private fun channelFor(context: Context, kind: Kind, sound: Boolean, vibrate: Boolean): String {
        val id = "${kind.id}_${if (sound) "sound" else "silent"}_${if (vibrate) "vibrate" else "still"}"
        if (Build.VERSION.SDK_INT >= 26) {
            val manager = context.getSystemService(NotificationManager::class.java)
            if (manager.getNotificationChannel(id) == null) {
                val details = buildList {
                    add(if (sound) "sound" else "silent")
                    add(if (vibrate) "vibrate" else "no vibration")
                }.joinToString(", ")
                val channel = NotificationChannel(id, "${kind.channelName} ($details)", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = kind.channelDescription
                    if (!sound) setSound(null, null)
                    enableVibration(vibrate)
                }
                manager.createNotificationChannel(channel)
            }
        }
        return id
    }

    /** Posts a notification; returns false when notifications aren't allowed. */
    @SuppressLint("MissingPermission")
    fun show(
        context: Context,
        notificationId: Int,
        kind: Kind,
        title: String,
        text: String,
        sound: Boolean,
        vibrate: Boolean,
    ): Boolean {
        if (!canPost(context)) return false

        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val builder = NotificationCompat.Builder(context, channelFor(context, kind, sound, vibrate))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(if (kind == Kind.Class) NotificationCompat.CATEGORY_EVENT else NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openApp)

        if (Build.VERSION.SDK_INT < 26) {
            // Before channels existed, sound / vibration are per notification.
            var defaults = 0
            if (sound) defaults = defaults or NotificationCompat.DEFAULT_SOUND
            if (vibrate) defaults = defaults or NotificationCompat.DEFAULT_VIBRATE
            builder.setDefaults(defaults)
        }

        NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        return true
    }
}
