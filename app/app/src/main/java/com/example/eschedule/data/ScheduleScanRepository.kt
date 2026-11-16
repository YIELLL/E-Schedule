package com.example.eschedule.data

import kotlinx.coroutines.delay

/** Day a scanned class meets on (the Review Scan screen groups by this). */
enum class ScanDay(val label: String) {
    Monday("Monday"),
    Tuesday("Tuesday"),
    Wednesday("Wednesday"),
    Thursday("Thursday"),
    Friday("Friday"),
    Saturday("Saturday"),
    Sunday("Sunday"),
}

/** One class row recognised from a photographed schedule. */
data class ScannedClass(
    val id: String,
    val title: String,
    val timeRange: String,
    val location: String,
    val day: ScanDay,
)

/**
 * Backend seam for the scan → review → save flow.
 *
 * The UI only talks to this interface. When the backend is ready, implement it (e.g. with
 * Retrofit/Ktor) and pass the implementation to `AppScaffold(scanRepository = …)`.
 */
interface ScheduleScanRepository {

    /**
     * Sends the captured photo ([jpeg] bytes) for recognition and returns the classes found.
     * Throw any exception on failure — the UI shows a retry message.
     */
    suspend fun scan(jpeg: ByteArray): List<ScannedClass>

    /** Persists the classes the user kept on the Review Scan screen. */
    suspend fun save(classes: List<ScannedClass>)
}

/** Stand-in used until the backend exists: returns the sample schedule from the design. */
class FakeScheduleScanRepository : ScheduleScanRepository {

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

    override suspend fun save(classes: List<ScannedClass>) {
        delay(300)
    }
}
