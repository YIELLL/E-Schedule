package com.example.eschedule.data

import android.content.Context
import com.example.eschedule.data.local.AppDatabase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.LocalDateTime

/**
 * Manual dependency container: one database and one repository of each kind for the whole app.
 * Swap [scanner] for the real recognition backend when it is ready.
 */
class AppContainer(context: Context) {

    private val database = AppDatabase.create(context)

    val classRepository = ClassRepository(database.classDao())
    val reminderRepository = ReminderRepository(database.reminderDao())
    val scanner: ScheduleScanner = DemoScheduleScanner()
}

/** Emits the current time now and then at the start of every minute (drives "now"-based UI). */
fun minuteTicker(): Flow<LocalDateTime> = flow {
    while (true) {
        val now = LocalDateTime.now()
        emit(now)
        delay((60 - now.second) * 1000L - now.nano / 1_000_000)
    }
}
