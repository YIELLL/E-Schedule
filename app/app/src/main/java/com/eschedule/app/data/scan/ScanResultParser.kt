package com.eschedule.app.data.scan

import com.eschedule.app.data.ScanDay
import com.eschedule.app.data.ScannedClass
import com.eschedule.app.data.parseTimeRange
import com.eschedule.app.data.scanTimeFormat
import org.json.JSONObject
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

/**
 * Turns the model's transcription (`{"rows":[{subject_code, description, from, to, day, room}]}`)
 * into [ScannedClass] rows. The model only copies what is printed; the conversions happen here:
 * "TueThu" becomes two days, "01:00 PM" becomes a time, "L4." becomes the room "L4".
 */
object ScanResultParser {

    fun parse(json: String): List<ScannedClass> {
        val rows = runCatching { JSONObject(json).getJSONArray("rows") }.getOrElse {
            throw GeminiFailure(FailureKind.BadResponse, 200, "The rows were missing from the reply", cause = it)
        }

        val result = mutableListOf<ScannedClass>()
        for (i in 0 until rows.length()) {
            val row = rows.optJSONObject(i)
            val parsed = row?.let { toClasses(it) }
            if (!parsed.isNullOrEmpty()) result += parsed
        }

        return result
            .distinctBy { listOf(it.title, it.day, it.timeRange, it.location) }
            .sortedWith(compareBy({ it.day.ordinal }, { parseTimeRange(it.timeRange)?.first }))
    }

    private fun toClasses(row: JSONObject): List<ScannedClass> {
        val code = row.text("subject_code")
        val start = parseTime(row.text("from")) ?: return emptyList()
        val end = parseTime(row.text("to")) ?: return emptyList()
        if (code.isBlank() || !end.isAfter(start)) return emptyList()

        val days = parseDays(row.text("day"))
        if (days.isEmpty()) return emptyList()

        val description = row.text("description")
        val title = if (description.isBlank()) code else "$code - $description"
        val range = "${start.format(scanTimeFormat)} - ${end.format(scanTimeFormat)}"
        val room = row.text("room").trim(' ', '.', ',', ';', ':', '-')

        return days.map { day ->
            ScannedClass(
                id = UUID.randomUUID().toString(),
                title = title,
                timeRange = range,
                location = room,
                day = day,
            )
        }
    }

    private fun JSONObject.text(name: String): String =
        if (isNull(name)) "" else optString(name).trim()

    // ── Times ────────────────────────────────────────────────────────────────

    private val twelveHour = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
    private val twentyFourHour = DateTimeFormatter.ofPattern("H:mm", Locale.US)

    /** "01:00 PM", "1:00pm", "1 PM", "13:00" → time; null when it isn't one. */
    internal fun parseTime(raw: String): LocalTime? {
        var s = raw.trim().uppercase(Locale.US).replace(".", "")
        if (s.isEmpty()) return null
        val meridiem = Regex("""\s*([AP]M)$""").find(s)
        if (meridiem != null) {
            s = s.removeSuffix(meridiem.value).trim()
            if (':' !in s) s += ":00"
            return runCatching { LocalTime.parse("$s ${meridiem.groupValues[1]}", twelveHour) }.getOrNull()
        }
        if (':' !in s) return null
        return runCatching { LocalTime.parse(s, twentyFourHour) }.getOrNull()
    }

    // ── Days ─────────────────────────────────────────────────────────────────

    /** Longest first so "thu" wins over "t" and "th" over "t". */
    private val dayTokens: List<Pair<String, ScanDay>> = listOf(
        "monday" to ScanDay.Monday, "mon" to ScanDay.Monday, "m" to ScanDay.Monday,
        "tuesday" to ScanDay.Tuesday, "tues" to ScanDay.Tuesday, "tue" to ScanDay.Tuesday, "t" to ScanDay.Tuesday,
        "wednesday" to ScanDay.Wednesday, "wed" to ScanDay.Wednesday, "w" to ScanDay.Wednesday,
        "thursday" to ScanDay.Thursday, "thurs" to ScanDay.Thursday, "thur" to ScanDay.Thursday,
        "thu" to ScanDay.Thursday, "th" to ScanDay.Thursday,
        "friday" to ScanDay.Friday, "fri" to ScanDay.Friday, "f" to ScanDay.Friday,
        "saturday" to ScanDay.Saturday, "sat" to ScanDay.Saturday, "sa" to ScanDay.Saturday, "s" to ScanDay.Saturday,
        "sunday" to ScanDay.Sunday, "sun" to ScanDay.Sunday, "su" to ScanDay.Sunday,
    ).sortedByDescending { it.first.length }

    /** "TueThu" → Tue, Thu; "MWF" → Mon, Wed, Fri; "TTh" → Tue, Thu; anything unknown is skipped. */
    internal fun parseDays(raw: String): List<ScanDay> {
        val letters = raw.lowercase(Locale.US).filter { it in 'a'..'z' }
        val found = linkedSetOf<ScanDay>()
        var i = 0
        while (i < letters.length) {
            val match = dayTokens.firstOrNull { letters.startsWith(it.first, i) }
            if (match != null) {
                found += match.second
                i += match.first.length
            } else {
                i++
            }
        }
        return found.toList()
    }
}
