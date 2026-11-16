package com.example.eschedule.ui

import com.example.eschedule.data.model.Reminder
import com.example.eschedule.data.model.ReminderStatus
import com.example.eschedule.data.model.SchoolClass
import com.example.eschedule.ui.components.ClassCardData
import com.example.eschedule.ui.components.ReminderItemData
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

// Display formatting shared by screens and sheets. Kept in one place so every surface
// writes times and dates the same way ("8:00 AM", "August 1, 2026", "Sep 21", "10/18/24").

private val timeFormat = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
private val longDateFormat = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.US)
private val shortDateFormat = DateTimeFormatter.ofPattern("MMM d", Locale.US)
private val shortDateYearFormat = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US)
private val numericDateFormat = DateTimeFormatter.ofPattern("M/d/yy", Locale.US)
private val weekdayDateFormat = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.US)

fun LocalTime.display(): String = format(timeFormat)
fun LocalDate.displayLong(): String = format(longDateFormat)

fun timeRange(start: LocalTime, end: LocalTime) = "${start.display()} - ${end.display()}"

/** "Aug 1 – Dec 17, 2026" (years shown on both ends only when they differ). */
fun dateRange(start: LocalDate, end: LocalDate): String =
    if (start.year == end.year) "${start.format(shortDateFormat)} – ${end.format(shortDateYearFormat)}"
    else "${start.format(shortDateYearFormat)} – ${end.format(shortDateYearFormat)}"

/** "Repeats every Tuesday & Thursday" / "Repeats every weekday" / "Repeats every day". */
fun repeatsLabel(days: Set<DayOfWeek>): String {
    val weekdays = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
    return when {
        days.size == 7 -> "Repeats every day"
        days == weekdays -> "Repeats every weekday"
        days.isEmpty() -> "Does not repeat"
        else -> {
            val names = days.sorted().map { it.getDisplayName(TextStyle.FULL, Locale.US) }
            "Repeats every " + if (names.size == 1) names[0]
            else names.dropLast(1).joinToString(", ") + " & " + names.last()
        }
    }
}

/** Relative day: "Today" / "Tomorrow" / "Yesterday" / "Mon, Nov 17" (+ year when not this year). */
fun relativeDay(date: LocalDate, today: LocalDate): String = when (date) {
    today -> "Today"
    today.plusDays(1) -> "Tomorrow"
    today.minusDays(1) -> "Yesterday"
    else -> if (date.year == today.year) date.format(weekdayDateFormat) else date.format(shortDateYearFormat)
}

/** Right-hand label on a reminder row: the time for today, "Tomorrow", or a short date. */
fun Reminder.dueLabel(today: LocalDate): String = when {
    date == today -> time?.display() ?: "Today"
    date == today.plusDays(1) -> "Tomorrow"
    date.year == today.year -> date.format(shortDateFormat)
    else -> date.format(numericDateFormat)
}

/** Completed rows show when the reminder was finished (falls back to its due date). */
fun Reminder.completedLabel(): String =
    (completedAt?.atZone(ZoneId.systemDefault())?.toLocalDate() ?: date).format(numericDateFormat)

fun SchoolClass.toCard() = ClassCardData(
    id = id,
    title = title,
    timeRange = timeRange(startTime, endTime),
    location = location.ifBlank { "No room set" },
)

fun Reminder.toActiveItem(today: LocalDate) = ReminderItemData(
    id = id,
    title = title,
    dueLabel = dueLabel(today),
    status = ReminderStatus.Open,
    isOverdue = date.isBefore(today),
)

fun Reminder.toCompletedItem() = ReminderItemData(
    id = id,
    title = title,
    dueLabel = completedLabel(),
    status = if (wontDo) ReminderStatus.WontDo else ReminderStatus.Done,
)
