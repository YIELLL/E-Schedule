package com.example.eschedule.data

import com.example.eschedule.data.model.SchoolClass
import kotlinx.coroutines.delay
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

/** Day a scanned class meets on (the Review Scan screen groups by this). */
enum class ScanDay(val label: String, val dayOfWeek: DayOfWeek) {
    Monday("Monday", DayOfWeek.MONDAY),
    Tuesday("Tuesday", DayOfWeek.TUESDAY),
    Wednesday("Wednesday", DayOfWeek.WEDNESDAY),
    Thursday("Thursday", DayOfWeek.THURSDAY),
    Friday("Friday", DayOfWeek.FRIDAY),
    Saturday("Saturday", DayOfWeek.SATURDAY),
    Sunday("Sunday", DayOfWeek.SUNDAY);

    companion object {
        fun of(day: DayOfWeek): ScanDay = entries.first { it.dayOfWeek == day }
    }
}

/**
 * One class row recognised from a photographed schedule (a single day).
 * The optional fields are filled in when the user edits the row on the Review Scan screen.
 */
data class ScannedClass(
    val id: String,
    val title: String,
    /** e.g. "8:00 AM - 9:00 AM" */
    val timeRange: String,
    val location: String,
    val day: ScanDay,
    val description: String = "",
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
)

/**
 * Backend seam for reading a photographed schedule.
 *
 * Implement this with the real recognition service (on-device OCR or a server) and hand it to
 * `AppContainer`. Saving the reviewed result is done locally through [ClassRepository].
 */
interface ScheduleScanner {
    /**
     * Sends the captured photo ([jpeg] bytes) for recognition and returns the classes found.
     * Throw any exception on failure — the UI shows a retry message.
     */
    suspend fun scan(jpeg: ByteArray): List<ScannedClass>
}

/**
 * Stand-in until the recognition backend exists: ignores the photo and returns the sample
 * schedule from the design so the scan → review → save flow can be exercised end to end.
 */
class DemoScheduleScanner : ScheduleScanner {
    override suspend fun scan(jpeg: ByteArray): List<ScannedClass> {
        delay(800) // pretend to upload + parse
        return listOf(
            ScannedClass("m1", "SIA101 - System Integration & Architecture", "8:00 AM - 9:00 AM", "CSFLD22", ScanDay.Monday),
            ScannedClass("m2", "IPT101 - Integrative Programming & Technologies 1", "1:00 PM - 2:00 PM", "CSFLD24", ScanDay.Monday),
            ScannedClass("m3", "ITS101 - IT Service Management", "3:00 PM - 4:00 PM", "CSFLD22", ScanDay.Monday),
            ScannedClass("m4", "NET102 - Networking 2", "4:00 PM - 7:00 PM", "L4", ScanDay.Monday),
            ScannedClass("t1", "IT102 - Mobile Application Development", "8:00 AM - 9:00 AM", "CSFLD22", ScanDay.Tuesday),
            ScannedClass("t2", "IM101 - Fundamentals of Database", "10:00 AM - 12:00 PM", "CSFLD7", ScanDay.Tuesday),
            ScannedClass("t3", "NET102 - Networking 2", "2:00 PM - 3:00 PM", "CSFLD7", ScanDay.Tuesday),
            ScannedClass("w1", "SIA101 - System Integration & Architecture", "8:00 AM - 9:00 AM", "CSFLD22", ScanDay.Wednesday),
            ScannedClass("w2", "GE ELEC 1 - Environmental Science", "9:00 AM - 12:00 PM", "CSFLD22", ScanDay.Wednesday),
        )
    }
}

private val scanTimeFormat = DateTimeFormatter.ofPattern("h:mm a", Locale.US)

/** "8:00 AM - 9:00 AM" → start and end, or null when it can't be read or the end isn't after the start. */
fun parseTimeRange(range: String): Pair<LocalTime, LocalTime>? {
    val parts = range.split("-", "–").map { it.trim().uppercase(Locale.US) }
    if (parts.size != 2) return null
    val start = runCatching { LocalTime.parse(parts[0], scanTimeFormat) }.getOrNull() ?: return null
    val end = runCatching { LocalTime.parse(parts[1], scanTimeFormat) }.getOrNull() ?: return null
    return if (end.isAfter(start)) start to end else null
}

/** A scanned row as an editable class (used by the View / Edit sheets on the Review Scan screen). */
fun ScannedClass.toDraft(today: LocalDate): SchoolClass {
    val (start, end) = parseTimeRange(timeRange) ?: (LocalTime.of(8, 0) to LocalTime.of(9, 0))
    return SchoolClass(
        title = title,
        startTime = start,
        endTime = end,
        startDate = startDate ?: today,
        endDate = endDate ?: today.plusMonths(4),
        days = setOf(day.dayOfWeek),
        location = location,
        description = description,
    )
}

/** An edited draft back into scan rows — one per weekday it repeats on. */
fun SchoolClass.toScannedRows(): List<ScannedClass> = days.sorted().map { weekday ->
    ScannedClass(
        id = UUID.randomUUID().toString(),
        title = title.trim(),
        timeRange = "${startTime.format(scanTimeFormat)} - ${endTime.format(scanTimeFormat)}",
        location = location.trim(),
        day = ScanDay.of(weekday),
        description = description.trim(),
        startDate = startDate,
        endDate = endDate,
    )
}

private data class ScanGroupKey(
    val title: String,
    val range: String,
    val location: String,
    val description: String,
    val startDate: LocalDate?,
    val endDate: LocalDate?,
)

/**
 * Turns reviewed scan rows into classes: rows with the same title, time, room and dates become one
 * class that repeats on all of their days. Rows whose time can't be parsed are skipped.
 * Classes without explicit dates run for one semester (~4 months) from [today].
 */
fun List<ScannedClass>.toSchoolClasses(today: LocalDate): List<SchoolClass> =
    groupBy {
        ScanGroupKey(it.title.trim(), it.timeRange.trim(), it.location.trim(), it.description.trim(), it.startDate, it.endDate)
    }.mapNotNull { (key, rows) ->
        val (start, end) = parseTimeRange(key.range) ?: return@mapNotNull null
        SchoolClass(
            title = key.title,
            startTime = start,
            endTime = end,
            startDate = key.startDate ?: today,
            endDate = key.endDate ?: today.plusMonths(4),
            days = rows.map { it.day.dayOfWeek }.toSet(),
            location = key.location,
            description = key.description,
        )
    }
