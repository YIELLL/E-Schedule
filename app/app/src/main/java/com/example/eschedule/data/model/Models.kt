package com.example.eschedule.data.model

import com.example.eschedule.data.local.ClassEntity
import com.example.eschedule.data.local.ReminderEntity
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/** A class that repeats on [days] between [startDate] and [endDate] (inclusive). */
data class SchoolClass(
    val id: Long = 0,
    val title: String,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val days: Set<DayOfWeek>,
    val location: String = "",
    val description: String = "",
) {
    /** "IT102" from "IT102 - Mobile Application Development". */
    val code: String get() = title.substringBefore(" - ").trim().ifEmpty { title }

    fun meetsOn(date: LocalDate): Boolean =
        date.dayOfWeek in days && !date.isBefore(startDate) && !date.isAfter(endDate)
}

/** Where a reminder stands: still open, finished, or dropped ("won't do"). */
enum class ReminderStatus { Open, Done, WontDo }

/** A to-do due on [date], optionally at [time]. */
data class Reminder(
    val id: Long = 0,
    val title: String,
    val date: LocalDate,
    val time: LocalTime? = null,
    val location: String = "",
    val notes: String = "",
    val completed: Boolean = false,
    val wontDo: Boolean = false,
    val completedAt: Instant? = null,
) {
    val status: ReminderStatus
        get() = when {
            !completed -> ReminderStatus.Open
            wontDo -> ReminderStatus.WontDo
            else -> ReminderStatus.Done
        }
}

// ── Entity ↔ model mapping ─────────────────────────────────────────────────────

private fun Set<DayOfWeek>.toMask(): Int = fold(0) { acc, d -> acc or (1 shl (d.value - 1)) }
private fun Int.toDays(): Set<DayOfWeek> = DayOfWeek.entries.filter { this and (1 shl (it.value - 1)) != 0 }.toSet()
private fun LocalTime.toMinute(): Int = hour * 60 + minute
private fun Int.toTime(): LocalTime = LocalTime.of(this / 60, this % 60)

fun ClassEntity.toModel() = SchoolClass(
    id = id,
    title = title,
    startTime = startMinute.toTime(),
    endTime = endMinute.toTime(),
    startDate = LocalDate.ofEpochDay(startEpochDay),
    endDate = LocalDate.ofEpochDay(endEpochDay),
    days = daysMask.toDays(),
    location = location,
    description = description,
)

fun SchoolClass.toEntity() = ClassEntity(
    id = id,
    title = title.trim(),
    startMinute = startTime.toMinute(),
    endMinute = endTime.toMinute(),
    startEpochDay = startDate.toEpochDay(),
    endEpochDay = endDate.toEpochDay(),
    daysMask = days.toMask(),
    location = location.trim(),
    description = description.trim(),
)

fun ReminderEntity.toModel() = Reminder(
    id = id,
    title = title,
    date = LocalDate.ofEpochDay(dateEpochDay),
    time = timeMinute?.toTime(),
    location = location,
    notes = notes,
    completed = completed,
    wontDo = wontDo,
    completedAt = completedAtMillis?.let(Instant::ofEpochMilli),
)

fun Reminder.toEntity() = ReminderEntity(
    id = id,
    title = title.trim(),
    dateEpochDay = date.toEpochDay(),
    timeMinute = time?.toMinute(),
    location = location.trim(),
    notes = notes.trim(),
    completed = completed,
    wontDo = wontDo,
    completedAtMillis = completedAt?.toEpochMilli(),
)
