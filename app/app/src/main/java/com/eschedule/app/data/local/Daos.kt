package com.eschedule.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ClassDao {

    @Query("SELECT * FROM classes ORDER BY startMinute, title")
    fun observeAll(): Flow<List<ClassEntity>>

    @Query("SELECT * FROM classes WHERE id = :id")
    fun observeById(id: Long): Flow<ClassEntity?>

    @Insert
    suspend fun insert(entity: ClassEntity): Long

    @Insert
    suspend fun insertAll(entities: List<ClassEntity>)

    @Update
    suspend fun update(entity: ClassEntity)

    @Query("DELETE FROM classes WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface ReminderDao {

    /** Open reminders: soonest first; within a day, timed ones by time and all-day ones last. */
    @Query(
        """
        SELECT * FROM reminders WHERE completed = 0
        ORDER BY dateEpochDay,
                 CASE WHEN timeMinute IS NULL THEN 1 ELSE 0 END,
                 timeMinute,
                 id
        """
    )
    fun observeActive(): Flow<List<ReminderEntity>>

    /** Finished reminders, most recently completed first. */
    @Query("SELECT * FROM reminders WHERE completed = 1 ORDER BY completedAtMillis DESC, id DESC")
    fun observeCompleted(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE id = :id")
    fun observeById(id: Long): Flow<ReminderEntity?>

    @Insert
    suspend fun insert(entity: ReminderEntity): Long

    @Update
    suspend fun update(entity: ReminderEntity)

    @Query(
        "UPDATE reminders SET completed = :completed, wontDo = :wontDo, completedAtMillis = :completedAtMillis WHERE id = :id"
    )
    suspend fun setStatus(id: Long, completed: Boolean, wontDo: Boolean, completedAtMillis: Long?)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteById(id: Long)
}
