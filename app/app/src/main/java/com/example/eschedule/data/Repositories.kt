package com.example.eschedule.data

import com.example.eschedule.data.local.ClassDao
import com.example.eschedule.data.local.ReminderDao
import com.example.eschedule.data.model.Reminder
import com.example.eschedule.data.model.ReminderStatus
import com.example.eschedule.data.model.SchoolClass
import com.example.eschedule.data.model.toEntity
import com.example.eschedule.data.model.toModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Instant

class ClassRepository(private val dao: ClassDao) {

    val classes: Flow<List<SchoolClass>> = dao.observeAll().map { list -> list.map { it.toModel() } }

    fun observe(id: Long): Flow<SchoolClass?> = dao.observeById(id).map { it?.toModel() }

    /** Inserts when [cls] is new (id = 0), otherwise updates it. Returns the row id. */
    suspend fun save(cls: SchoolClass): Long =
        if (cls.id == 0L) dao.insert(cls.toEntity())
        else dao.update(cls.toEntity()).let { cls.id }

    suspend fun saveAll(classes: List<SchoolClass>) = dao.insertAll(classes.map { it.toEntity() })

    /** Checks [candidate] against everything saved: fine, a duplicate (refuse) or a conflict (ask). */
    suspend fun check(candidate: SchoolClass): SaveCheck = checkAgainst(candidate, classes.first())

    /** Deletes the classes in [replaceIds] and saves [cls] in their place. */
    suspend fun replaceAndSave(cls: SchoolClass, replaceIds: Collection<Long>) {
        replaceIds.forEach { dao.deleteById(it) }
        save(cls)
    }

    suspend fun plan(incoming: List<SchoolClass>): ImportPlan = planImport(incoming, classes.first())

    /**
     * Applies an [ImportPlan]: the clean classes are always added; the conflicting ones are added only
     * when [replaceConflicts] is true, in which case the classes they overlap are deleted first.
     * Returns how many classes were added.
     */
    suspend fun applyImport(plan: ImportPlan, replaceConflicts: Boolean): Int {
        val toSave = if (replaceConflicts) {
            plan.conflicts.flatMap { it.existing }.map { it.id }.distinct().forEach { dao.deleteById(it) }
            plan.toAdd + plan.conflicts.map { it.incoming }
        } else {
            plan.toAdd
        }
        saveAll(toSave)
        return toSave.size
    }

    suspend fun delete(id: Long) = dao.deleteById(id)
}

class ReminderRepository(private val dao: ReminderDao) {

    val active: Flow<List<Reminder>> = dao.observeActive().map { list -> list.map { it.toModel() } }
    val completed: Flow<List<Reminder>> = dao.observeCompleted().map { list -> list.map { it.toModel() } }

    fun observe(id: Long): Flow<Reminder?> = dao.observeById(id).map { it?.toModel() }

    suspend fun save(reminder: Reminder): Long =
        if (reminder.id == 0L) dao.insert(reminder.toEntity())
        else dao.update(reminder.toEntity()).let { reminder.id }

    suspend fun setStatus(id: Long, status: ReminderStatus) = dao.setStatus(
        id = id,
        completed = status != ReminderStatus.Open,
        wontDo = status == ReminderStatus.WontDo,
        completedAtMillis = if (status == ReminderStatus.Open) null else Instant.now().toEpochMilli(),
    )

    suspend fun delete(id: Long) = dao.deleteById(id)
}
