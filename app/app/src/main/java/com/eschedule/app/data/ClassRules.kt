package com.eschedule.app.data

import com.eschedule.app.data.model.SchoolClass

// ─────────────────────────────────────────────────────────────────────────────
// Scheduling rules: when are two classes in conflict, and when is one a duplicate?
// Pure functions over the model, so they can be unit-tested and reused by every save path.
// ─────────────────────────────────────────────────────────────────────────────

private fun SchoolClass.overlapsInTime(other: SchoolClass) =
    startTime.isBefore(other.endTime) && other.startTime.isBefore(endTime)

private fun SchoolClass.overlapsInDates(other: SchoolClass) =
    !startDate.isAfter(other.endDate) && !other.startDate.isAfter(endDate)

/** Two different classes clash when they share a weekday, their date ranges touch and their times overlap. */
fun SchoolClass.conflictsWith(other: SchoolClass): Boolean =
    id != other.id &&
        days.any { it in other.days } &&
        overlapsInDates(other) &&
        overlapsInTime(other)

/** A clash that is also the same class entered twice: same title, same time slot, same days. */
fun SchoolClass.isDuplicateOf(other: SchoolClass): Boolean =
    conflictsWith(other) &&
        title.trim().equals(other.title.trim(), ignoreCase = true) &&
        startTime == other.startTime &&
        endTime == other.endTime &&
        days == other.days

/** Outcome of checking a class before it is saved. */
sealed interface SaveCheck {
    data object Ok : SaveCheck

    /** Exactly the same class already exists — refuse to add it again. */
    data class Duplicate(val existing: SchoolClass) : SaveCheck

    /** Overlaps these existing classes — ask the user whether to replace them. */
    data class Conflict(val existing: List<SchoolClass>) : SaveCheck
}

fun checkAgainst(candidate: SchoolClass, existing: List<SchoolClass>): SaveCheck {
    val clashes = existing.filter { candidate.conflictsWith(it) }
    clashes.firstOrNull { candidate.isDuplicateOf(it) }?.let { return SaveCheck.Duplicate(it) }
    return if (clashes.isEmpty()) SaveCheck.Ok else SaveCheck.Conflict(clashes)
}

// ── Importing a scanned schedule ───────────────────────────────────────────────

/** A scanned class that overlaps classes the user already has. */
data class ImportConflict(val incoming: SchoolClass, val existing: List<SchoolClass>)

/**
 * What importing a batch would do: [toAdd] are clean, [duplicates] were already present and will be
 * skipped, and [conflicts] need the user's decision (replace the existing ones, or skip the new ones).
 */
data class ImportPlan(
    val toAdd: List<SchoolClass>,
    val duplicates: Int,
    val conflicts: List<ImportConflict>,
)

fun planImport(incoming: List<SchoolClass>, existing: List<SchoolClass>): ImportPlan {
    var duplicates = 0
    val clean = mutableListOf<SchoolClass>()
    val conflicts = mutableListOf<ImportConflict>()
    incoming.forEach { candidate ->
        when (val result = checkAgainst(candidate, existing)) {
            SaveCheck.Ok -> clean += candidate
            is SaveCheck.Duplicate -> duplicates++
            is SaveCheck.Conflict -> conflicts += ImportConflict(candidate, result.existing)
        }
    }
    return ImportPlan(clean, duplicates, conflicts)
}
