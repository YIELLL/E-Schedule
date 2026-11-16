package com.example.eschedule.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A recurring class. Times are minutes after midnight, dates are `LocalDate.toEpochDay()`,
 * and [daysMask] has one bit per weekday: Monday = bit 0 … Sunday = bit 6.
 */
@Entity(tableName = "classes")
data class ClassEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val startMinute: Int,
    val endMinute: Int,
    val startEpochDay: Long,
    val endEpochDay: Long,
    val daysMask: Int,
    val location: String,
    val description: String,
)

/** A to-do with a due date and an optional time of day ([timeMinute] = null → all day). */
@Entity(
    tableName = "reminders",
    indices = [Index("completed"), Index("dateEpochDay")],
)
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val dateEpochDay: Long,
    val timeMinute: Int?,
    val location: String,
    val notes: String,
    /** Finished (done or won't do). */
    val completed: Boolean = false,
    /** Only meaningful when [completed]: dropped rather than done. */
    val wontDo: Boolean = false,
    val completedAtMillis: Long? = null,
)
