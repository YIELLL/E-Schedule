package com.example.eschedule.data

import com.example.eschedule.data.local.ClassDao
import com.example.eschedule.data.local.ReminderDao
import com.example.eschedule.data.model.Reminder
import com.example.eschedule.data.model.ReminderStatus
import com.example.eschedule.data.model.SchoolClass
import com.example.eschedule.data.model.toEntity
import com.example.eschedule.data.model.toModel
import kotlinx.coroutines.flow.Flow
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
