package com.example.eschedule.data

import android.content.Context
import com.example.eschedule.data.local.AppDatabase
import com.example.eschedule.data.scan.GeminiClient
import com.example.eschedule.data.scan.GeminiScheduleScanner
import com.example.eschedule.data.scan.KeyHealthStore
import com.example.eschedule.data.scan.KeyRotator
import com.example.eschedule.notify.NotificationScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

/**
 * Manual dependency container: one database and one repository of each kind for the whole app.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext
    private val database = AppDatabase.create(appContext)

    val classRepository = ClassRepository(database.classDao())
    val reminderRepository = ReminderRepository(database.reminderDao())
    val settings = SettingsRepository(appContext)
    val scanner: ScheduleScanner = GeminiScheduleScanner(KeyRotator(settings, KeyHealthStore(appContext)), GeminiClient())

    // ── Class / reminder alerts ──────────────────────────────────────────────
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val notificationScheduler = NotificationScheduler(appContext)
    private val resyncRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /**
     * Keeps the system alarms in step with the app: whenever the classes, the open reminders or the
     * notification settings change — or [requestNotificationResync] is called — the alarms are
     * planned again. Started once, from the Application.
     */
    @OptIn(FlowPreview::class)
    fun startNotificationSync() {
        appScope.launch {
            combine(
                classRepository.classes,
                reminderRepository.active,
                settings.notifications,
                resyncRequests.onStart { emit(Unit) },
            ) { classes, reminders, prefs, _ -> Triple(classes, reminders, prefs) }
                .debounce(300)
                .collect { (classes, reminders, prefs) ->
                    runCatching { notificationScheduler.reschedule(classes, reminders, prefs) }
                        .onFailure { AppLog.e("Notify", "Planning alarms failed", it) }
                }
        }
    }

    /** Plans the alarms again (after an alarm fired, a reboot, or a clock change). */
    fun requestNotificationResync() {
        resyncRequests.tryEmit(Unit)
    }

    /**
     * Wipes everything the app stores: all classes and reminders, saved settings and the error log.
     * The UI updates by itself because it observes the database and the settings.
     */
    suspend fun resetAll() {
        withContext(Dispatchers.IO) { database.clearAllTables() }
        settings.reset()
        AppLog.clear()
    }
}

/** Emits the current time now and then at the start of every minute (drives "now"-based UI). */
fun minuteTicker(): Flow<LocalDateTime> = flow {
    while (true) {
        val now = LocalDateTime.now()
        emit(now)
        delay((60 - now.second) * 1000L - now.nano / 1_000_000)
    }
}
