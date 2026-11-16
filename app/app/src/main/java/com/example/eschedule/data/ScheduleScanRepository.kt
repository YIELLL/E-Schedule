package com.example.eschedule.data

import com.example.eschedule.data.model.SchoolClass
import kotlinx.coroutines.delay
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Day a scanned class meets on (the Review Scan screen groups by this). */
enum class ScanDay(val label: String, val dayOfWeek: DayOfWeek) {
    Monday("Monday", DayOfWeek.MONDAY),
    Tuesday("Tuesday", DayOfWeek.TUESDAY),
    Wednesday("Wednesday", DayOfWeek.WEDNESDAY),
    Thursday("Thursday", DayOfWeek.THURSDAY),
    Friday("Friday", DayOfWeek.FRIDAY),
    Saturday("Saturday", DayOfWeek.SATURDAY),
    Sunday("Sunday", DayOfWeek.SUNDAY),
}

/** One class row recognised from a photographed schedule. */
data class ScannedClass(
    val id: String,
    val title: String,
    /** e.g. "8:00 AM - 9:00 AM" */
    val timeRange: String,
    val location: String,
    val day: ScanDay,
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

/**
 * Turns reviewed scan rows into classes: rows with the same title, time and room become one class
 * that repeats on all of their days. Rows whose time can't be parsed are skipped.
 * New classes run for one semester (~4 months) from [today]; the user can adjust them afterwards.
 */
fun List<ScannedClass>.toSchoolClasses(today: LocalDate): List<SchoolClass> =
    groupBy { Triple(it.title.trim(), it.timeRange.trim(), it.location.trim()) }
        .mapNotNull { (key, rows) ->
            val (title, range, location) = key
            val parts = range.split("-", "–").map { it.trim().uppercase(Locale.US) }
            if (parts.size != 2) return@mapNotNull null
            val start = runCatching { LocalTime.parse(parts[0], scanTimeFormat) }.getOrNull() ?: return@mapNotNull null
            val end = runCatching { LocalTime.parse(parts[1], scanTimeFormat) }.getOrNull() ?: return@mapNotNull null
            if (!end.isAfter(start)) return@mapNotNull null
            SchoolClass(
                title = title,
                startTime = start,
                endTime = end,
                startDate = today,
                endDate = today.plusMonths(4),
                days = rows.map { it.day.dayOfWeek }.toSet(),
                location = location,
            )
        }
