package com.example.eschedule.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.eschedule.EScheduleApp
import com.example.eschedule.data.ClassRepository
import com.example.eschedule.data.ApiKeyEntry
import com.example.eschedule.data.AppLog
import com.example.eschedule.data.ImportPlan
import com.example.eschedule.data.NotificationPrefs
import com.example.eschedule.data.ReminderRepository
import com.example.eschedule.data.SaveCheck
import com.example.eschedule.data.SettingsRepository
import com.example.eschedule.data.ThemeMode
import com.example.eschedule.data.ScannedClass
import com.example.eschedule.data.ScheduleScanner
import com.example.eschedule.data.minuteTicker
import com.example.eschedule.data.model.Reminder
import com.example.eschedule.data.model.ReminderStatus
import com.example.eschedule.data.model.SchoolClass
import com.example.eschedule.data.toSchoolClasses
import com.example.eschedule.ui.classes.ClassBlock
import com.example.eschedule.ui.components.ClassCardData
import com.example.eschedule.ui.components.ReminderItemData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

private fun todayTicker(): Flow<LocalDate> = minuteTicker().map { it.toLocalDate() }.distinctUntilChanged()

// ─── Home ────────────────────────────────────────────────────────────────────

data class HomeUiState(
    val loaded: Boolean = false,
    /** Any class exists at all (not just today). */
    val hasAnyClasses: Boolean = false,
    val currentClass: ClassCardData? = null,
    val upcomingClasses: List<ClassCardData> = emptyList(),
    val reminders: List<ReminderItemData> = emptyList(),
    /** Open reminders beyond the ones shown on Home. */
    val moreReminders: Int = 0,
) {
    /** Nothing to show anywhere: no classes and no open reminders. */
    val isBlank: Boolean get() = loaded && !hasAnyClasses && reminders.isEmpty()
}

class HomeViewModel(
    classes: ClassRepository,
    private val reminders: ReminderRepository,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        classes.classes,
        reminders.active,
        minuteTicker(),
    ) { allClasses, openReminders, now ->
        val today = now.toLocalDate()
        val time = now.toLocalTime()
        val todays = allClasses.filter { it.meetsOn(today) }.sortedBy { it.startTime }
        // Upcoming = what is still left today; once the day's classes are done the list is empty.
        HomeUiState(
            loaded = true,
            hasAnyClasses = allClasses.isNotEmpty(),
            currentClass = todays
                .firstOrNull { !time.isBefore(it.startTime) && time.isBefore(it.endTime) }
                ?.toCard(),
            upcomingClasses = todays.filter { it.startTime.isAfter(time) }.map { it.toCard() },
            reminders = openReminders.take(HOME_REMINDER_LIMIT).map { it.toActiveItem(today) },
            moreReminders = (openReminders.size - HOME_REMINDER_LIMIT).coerceAtLeast(0),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun setStatus(id: Long, status: ReminderStatus) {
        viewModelScope.launch { reminders.setStatus(id, status) }
    }

    private companion object {
        const val HOME_REMINDER_LIMIT = 5
    }
}

// ─── Classes ─────────────────────────────────────────────────────────────────

class ClassesViewModel(private val repository: ClassRepository) : ViewModel() {

    /** Blocks for the current Monday–Sunday week (classes outside their date range are hidden). */
    val weekBlocks: StateFlow<List<ClassBlock>> = combine(repository.classes, todayTicker()) { classes, today ->
        val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        (0 until 7).flatMap { dayIndex ->
            val date = monday.plusDays(dayIndex.toLong())
            classes.filter { it.runsDuringWeek(monday, monday.plusDays(6)) && date.dayOfWeek in it.days }.map { cls ->
                val start = cls.startTime.hour + cls.startTime.minute / 60f
                val end = cls.endTime.hour + cls.endTime.minute / 60f
                ClassBlock(
                    id = "${cls.id}_$dayIndex",
                    classId = cls.id,
                    subjectCode = cls.code,
                    dayIndex = dayIndex,
                    startHour = start,
                    durationHours = end - start,
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** True once at least one class exists (so the empty state can tell "none yet" from "none this week"). */
    val hasClasses: StateFlow<Boolean> = repository.classes
        .map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun observe(id: Long): Flow<SchoolClass?> = repository.observe(id)

    fun save(cls: SchoolClass) {
        viewModelScope.launch { repository.save(cls) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { repository.delete(id) }
    }

    /** Checks a class before saving: fine, an exact duplicate (refuse) or a clash (ask to replace). */
    suspend fun check(cls: SchoolClass): SaveCheck = repository.check(cls)

    /** Saves [cls] after removing the classes it replaces. */
    fun replaceAndSave(cls: SchoolClass, replaceIds: Collection<Long>) {
        viewModelScope.launch { repository.replaceAndSave(cls, replaceIds) }
    }

    /** Works out what saving the reviewed scan rows would do, without changing anything. */
    suspend fun planImport(items: List<ScannedClass>): ImportPlan =
        repository.plan(items.toSchoolClasses(LocalDate.now()))

    /** Applies a plan from [planImport]; returns how many classes were added. */
    suspend fun applyImport(plan: ImportPlan, replaceConflicts: Boolean): Int =
        repository.applyImport(plan, replaceConflicts)
}

// ─── Reminders ───────────────────────────────────────────────────────────────

enum class ReminderFilter(val label: String) {
    All("All"), Today("Today"), Week("Week"), Month("Month"), Completed("Completed"),
}

data class RemindersUiState(
    val loaded: Boolean = false,
    /** Any reminder exists at all, ignoring the filter. */
    val hasAny: Boolean = false,
    val filter: ReminderFilter = ReminderFilter.All,
    val active: List<ReminderItemData> = emptyList(),
    val completed: List<ReminderItemData> = emptyList(),
) {
    val showActive: Boolean get() = filter != ReminderFilter.Completed
    val showCompleted: Boolean get() = filter == ReminderFilter.All || filter == ReminderFilter.Completed
}

class RemindersViewModel(private val repository: ReminderRepository) : ViewModel() {

    private val filter = MutableStateFlow(ReminderFilter.All)

    val uiState: StateFlow<RemindersUiState> = combine(
        repository.active,
        repository.completed,
        filter,
        todayTicker(),
    ) { active, completed, filter, today ->
        val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val weekEnd = weekStart.plusDays(6)
        val visible = when (filter) {
            ReminderFilter.All -> active
            ReminderFilter.Today -> active.filter { it.date == today }
            ReminderFilter.Week -> active.filter { !it.date.isBefore(weekStart) && !it.date.isAfter(weekEnd) }
            ReminderFilter.Month -> active.filter { it.date.year == today.year && it.date.month == today.month }
            ReminderFilter.Completed -> emptyList()
        }
        RemindersUiState(
            loaded = true,
            hasAny = active.isNotEmpty() || completed.isNotEmpty(),
            filter = filter,
            active = visible.map { it.toActiveItem(today) },
            completed = completed.map { it.toCompletedItem() },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RemindersUiState())

    fun setFilter(value: ReminderFilter) {
        filter.value = value
    }

    fun observe(id: Long): Flow<Reminder?> = repository.observe(id)

    fun setStatus(id: Long, status: ReminderStatus) {
        viewModelScope.launch { repository.setStatus(id, status) }
    }

    fun save(reminder: Reminder) {
        viewModelScope.launch { repository.save(reminder) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { repository.delete(id) }
    }
}

// ─── Scan ────────────────────────────────────────────────────────────────────

class ScanViewModel(private val scanner: ScheduleScanner) : ViewModel() {
    suspend fun scan(image: ByteArray, rotationDegrees: Int? = null): List<ScannedClass> =
        scanner.scan(image, rotationDegrees)
}

// ─── Settings ────────────────────────────────────────────────────────────────

class SettingsViewModel(
    private val settings: SettingsRepository,
    private val resetEverything: suspend () -> Unit,
) : ViewModel() {
    val notifications: StateFlow<NotificationPrefs> = settings.notifications
    val apiKeys: StateFlow<List<ApiKeyEntry>> = settings.apiKeys
    val activeApiKeyId: StateFlow<String?> = settings.activeApiKeyId
    val themeMode: StateFlow<ThemeMode> = settings.themeMode
    val logLines: StateFlow<List<String>> = AppLog.lines

    fun updateNotifications(transform: (NotificationPrefs) -> NotificationPrefs) = settings.updateNotifications(transform)
    fun saveApiKey(id: String?, label: String, value: String) = settings.saveApiKey(id, label, value)
    fun deleteApiKey(id: String) = settings.deleteApiKey(id)
    fun setActiveApiKey(id: String) = settings.setActiveApiKey(id)
    fun setThemeMode(mode: ThemeMode) = settings.setThemeMode(mode)
    fun clearLog() = AppLog.clear()

    /** Deletes all data and settings; [onDone] runs once it has finished. */
    fun resetApp(onDone: () -> Unit) {
        viewModelScope.launch {
            resetEverything()
            onDone()
        }
    }
}

// ─── Factory ─────────────────────────────────────────────────────────────────

object AppViewModels {
    private fun CreationExtras.container() = (this[APPLICATION_KEY] as EScheduleApp).container

    val Factory = viewModelFactory {
        initializer { container().let { HomeViewModel(it.classRepository, it.reminderRepository) } }
        initializer { ClassesViewModel(container().classRepository) }
        initializer { RemindersViewModel(container().reminderRepository) }
        initializer { ScanViewModel(container().scanner) }
        initializer { container().let { c -> SettingsViewModel(c.settings) { c.resetAll() } } }
    }
}
