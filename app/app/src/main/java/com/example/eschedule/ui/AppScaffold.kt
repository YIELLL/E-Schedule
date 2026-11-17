package com.example.eschedule.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.eschedule.theme.AppTheme
import com.example.eschedule.data.AppLog
import com.example.eschedule.data.ImportPlan
import com.example.eschedule.data.SaveCheck
import com.example.eschedule.data.ScannedClass
import com.example.eschedule.data.scan.ScanException
import kotlinx.coroutines.CancellationException
import com.example.eschedule.data.model.SchoolClass
import com.example.eschedule.data.toDraft
import com.example.eschedule.data.toScannedRows
import java.time.LocalDate
import com.example.eschedule.ui.classes.ClassSheet
import com.example.eschedule.ui.classes.ClassesScreen
import com.example.eschedule.ui.classes.ViewClassSheet
import com.example.eschedule.ui.components.BottomNavBar
import com.example.eschedule.ui.components.TabRoute
import com.example.eschedule.ui.home.HomeScreen
import com.example.eschedule.ui.reminders.AddReminderSheet
import com.example.eschedule.ui.reminders.RemindersScreen
import com.example.eschedule.ui.reminders.ViewReminderSheet
import com.example.eschedule.ui.scan.ReviewScanScreen
import com.example.eschedule.ui.scan.ScanScreen
import com.example.eschedule.ui.settings.SettingsScreen
import kotlinx.coroutines.launch

/**
 * Root composable.
 * Hosts the bottom navigation bar and swaps tab content. Modal sheets, the Review Scan screen and
 * delete confirmations are layered on top. All data comes from the ViewModels (Room-backed).
 */
@Composable
fun AppScaffold(
    homeVm: HomeViewModel = viewModel(factory = AppViewModels.Factory),
    classesVm: ClassesViewModel = viewModel(factory = AppViewModels.Factory),
    remindersVm: RemindersViewModel = viewModel(factory = AppViewModels.Factory),
    scanVm: ScanViewModel = viewModel(factory = AppViewModels.Factory),
    settingsVm: SettingsViewModel = viewModel(factory = AppViewModels.Factory),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val homeState by homeVm.uiState.collectAsStateWithLifecycle()
    val weekBlocks by classesVm.weekBlocks.collectAsStateWithLifecycle()
    val hasClasses by classesVm.hasClasses.collectAsStateWithLifecycle()
    val remindersState by remindersVm.uiState.collectAsStateWithLifecycle()
    val notificationPrefs by settingsVm.notifications.collectAsStateWithLifecycle()
    val apiKeys by settingsVm.apiKeys.collectAsStateWithLifecycle()
    val activeApiKeyId by settingsVm.activeApiKeyId.collectAsStateWithLifecycle()
    val themeMode by settingsVm.themeMode.collectAsStateWithLifecycle()
    val logLines by settingsVm.logLines.collectAsStateWithLifecycle()

    var currentTab by remember { mutableStateOf<TabRoute>(TabRoute.Home) }
    var captureRequested by remember { mutableStateOf(false) }

    // Scan flow state
    var isScanning by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var reviewItems by remember { mutableStateOf<List<ScannedClass>?>(null) }
    var scanFailure by remember { mutableStateOf<ScanFailure?>(null) }

    // Sheet / dialog state — null = closed
    var showAddReminder by remember { mutableStateOf(false) }
    var viewReminderId by remember { mutableStateOf<Long?>(null) }
    var editReminderId by remember { mutableStateOf<Long?>(null) }
    var deleteReminderId by remember { mutableStateOf<Long?>(null) }

    var showAddClass by remember { mutableStateOf(false) }
    var viewClassId by remember { mutableStateOf<Long?>(null) }
    var editClassId by remember { mutableStateOf<Long?>(null) }
    var deleteClassId by remember { mutableStateOf<Long?>(null) }

    // Review Scan: a scanned row opened in the same View / Edit sheets used elsewhere
    var reviewViewId by remember { mutableStateOf<String?>(null) }
    var reviewEditId by remember { mutableStateOf<String?>(null) }
    val today = remember { LocalDate.now() }

    // Pending decisions: a class that clashes with existing ones, and a scan import that does
    var pendingClassSave by remember { mutableStateOf<PendingClassSave?>(null) }
    var pendingImport by remember { mutableStateOf<ImportPlan?>(null) }

    fun toast(message: String) = Toast.makeText(context, message, Toast.LENGTH_SHORT).show()

    /** Reads a captured photo; on failure shows the reason with a retry. */
    fun runScan(image: ByteArray, rotation: Int?) {
        scope.launch {
            isScanning = true
            runCatching { scanVm.scan(image, rotation) }
                .onSuccess { reviewItems = it }
                .onFailure {
                    if (it is CancellationException) throw it
                    val message = (it as? ScanException)?.userMessage ?: "Couldn't read the schedule. Please try again."
                    if (it !is ScanException) AppLog.e("Scan", "Reading the captured schedule failed", it)
                    scanFailure = ScanFailure(message, image, rotation, canRetry = (it as? ScanException)?.kind.let { kind ->
                        kind != ScanException.Kind.NoKey && kind != ScanException.Kind.BadImage
                    })
                }
            isScanning = false
        }
    }

    /** Saves [cls] unless it duplicates or clashes with an existing class (then it refuses / asks). */
    fun saveClass(cls: SchoolClass, onDone: () -> Unit) {
        scope.launch {
            when (val result = classesVm.check(cls)) {
                SaveCheck.Ok -> {
                    classesVm.save(cls)
                    onDone()
                }
                is SaveCheck.Duplicate ->
                    toast("\"${result.existing.title}\" is already in your schedule at that time.")
                is SaveCheck.Conflict -> pendingClassSave = PendingClassSave(cls, result.existing, onDone)
            }
        }
    }

    fun finishImport(plan: ImportPlan, replaceConflicts: Boolean) {
        scope.launch {
            isSaving = true
            runCatching { classesVm.applyImport(plan, replaceConflicts) }
                .onSuccess { count ->
                    reviewItems = null
                    currentTab = TabRoute.Classes
                    val skipped = plan.duplicates
                    toast(
                        buildString {
                            append(if (count == 1) "Added 1 class" else "Added $count classes")
                            if (skipped > 0) append(" \u00B7 skipped $skipped duplicate" + if (skipped == 1) "" else "s")
                        }
                    )
                }
                .onFailure {
                    AppLog.e("Import", "Saving scanned classes failed", it)
                    toast("Couldn't save your classes. Please try again.")
                }
            isSaving = false
        }
    }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = AppTheme.colors.background,
            contentColor = AppTheme.colors.text,
            bottomBar = {
                BottomNavBar(
                    currentTab = currentTab,
                    onTabSelected = { currentTab = it },
                    onScanClick = {
                        if (currentTab == TabRoute.Scan) captureRequested = true
                        else currentTab = TabRoute.Scan
                    },
                )
            },
        ) { innerPadding ->
            // Screens handle their own insets (titles sit under the status bar, lists clear the
            // floating nav bar), so Scaffold's innerPadding is intentionally not applied.
            @Suppress("UNUSED_EXPRESSION") innerPadding
            Crossfade(
                targetState = currentTab,
                animationSpec = tween(durationMillis = 220),
                modifier = Modifier.fillMaxSize(),
                label = "tabContent",
            ) { tab ->
                when (tab) {
                    TabRoute.Home -> HomeScreen(
                        state = homeState,
                        onClassClick = { viewClassId = it },
                        onReminderMoreClick = { viewReminderId = it },
                        onReminderStatusChange = homeVm::setStatus,
                        onSeeAllReminders = { currentTab = TabRoute.Reminders },
                        onAddClass = { showAddClass = true },
                        onScan = { currentTab = TabRoute.Scan },
                    )
                    TabRoute.Classes -> ClassesScreen(
                        blocks = weekBlocks,
                        hasAnyClasses = hasClasses,
                        onClassClick = { viewClassId = it },
                        onAddClass = { showAddClass = true },
                    )
                    TabRoute.Scan -> ScanScreen(
                        captureRequested = captureRequested,
                        onCaptureHandled = { captureRequested = false },
                        onImageCaptured = { jpeg, rotation -> runScan(jpeg, rotation) },
                    )
                    TabRoute.Reminders -> RemindersScreen(
                        state = remindersState,
                        onFilterChange = remindersVm::setFilter,
                        onStatusChange = remindersVm::setStatus,
                        onReminderMoreClick = { viewReminderId = it },
                        onAddReminder = { showAddReminder = true },
                    )
                    TabRoute.Settings -> SettingsScreen(
                        notifications = notificationPrefs,
                        onNotificationsChange = settingsVm::updateNotifications,
                        apiKeys = apiKeys,
                        activeApiKeyId = activeApiKeyId,
                        onSaveApiKey = settingsVm::saveApiKey,
                        onDeleteApiKey = settingsVm::deleteApiKey,
                        onSetActiveApiKey = settingsVm::setActiveApiKey,
                        themeMode = themeMode,
                        onThemeModeChange = settingsVm::setThemeMode,
                        logLines = logLines,
                        onClearLog = settingsVm::clearLog,
                        onResetApp = { settingsVm.resetApp { toast("App reset") } },
                    )
                }
            }
        }

        // ─── Review Scan (full screen, above the nav bar) ────────────────────────────
        AnimatedVisibility(
            visible = reviewItems != null,
            enter = fadeIn() + slideInVertically { it / 12 },
            exit = fadeOut() + slideOutVertically { it / 12 },
        ) {
            // Keep the last list while the exit animation plays.
            var shown by remember { mutableStateOf(reviewItems.orEmpty()) }
            reviewItems?.let { shown = it }

            BackHandler { reviewItems = null }
            ReviewScanScreen(
                items = shown,
                isSaving = isSaving,
                onClose = { reviewItems = null },
                onSave = {
                    scope.launch {
                        isSaving = true
                        runCatching { classesVm.planImport(shown) }
                            .onSuccess { plan ->
                                when {
                                    plan.conflicts.isNotEmpty() -> pendingImport = plan      // ask first
                                    plan.toAdd.isEmpty() -> {
                                        reviewItems = null
                                        toast("Those classes are already in your schedule.")
                                    }
                                    else -> finishImport(plan, replaceConflicts = false)
                                }
                            }
                            .onFailure {
                                AppLog.e("Import", "Checking scanned classes failed", it)
                                toast("Couldn't save your classes. Please try again.")
                            }
                        isSaving = false
                    }
                },
                onMore = { reviewViewId = it.id },
            )
        }

        // ─── Busy overlay while the photo is being recognised ────────────────────────
        if (isScanning) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x66000000))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},      // swallow touches while scanning
                    ),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        }
    }

    // ─── Reminder sheets ─────────────────────────────────────────────────────────
    if (showAddReminder) {
        AddReminderSheet(
            initial = null,
            onDismiss = { showAddReminder = false },
            onSave = {
                remindersVm.save(it)
                showAddReminder = false
            },
        )
    }
    viewReminderId?.let { id ->
        key(id) {
            val reminder by remember(id) { remindersVm.observe(id) }.collectAsStateWithLifecycle(null)
            reminder?.let {
                ViewReminderSheet(
                    reminder = it,
                    onDismiss = { viewReminderId = null },
                    onEdit = {
                        editReminderId = id
                        viewReminderId = null
                    },
                    onDelete = { deleteReminderId = id },
                )
            }
        }
    }
    editReminderId?.let { id ->
        key(id) {
            val reminder by remember(id) { remindersVm.observe(id) }.collectAsStateWithLifecycle(null)
            reminder?.let {
                AddReminderSheet(
                    sheetTitle = "Edit Reminder",
                    initial = it,
                    onDismiss = { editReminderId = null },
                    onSave = { updated ->
                        remindersVm.save(updated)
                        editReminderId = null
                    },
                )
            }
        }
    }
    deleteReminderId?.let { id ->
        ConfirmDeleteDialog(
            title = "Delete reminder?",
            message = "This reminder will be removed permanently.",
            onConfirm = {
                remindersVm.delete(id)
                deleteReminderId = null
                viewReminderId = null
            },
            onDismiss = { deleteReminderId = null },
        )
    }

    // ─── Class sheets ─────────────────────────────────────────────────────────────
    if (showAddClass) {
        ClassSheet(
            sheetTitle = "Add Class",
            initial = null,
            onDismiss = { showAddClass = false },
            onSave = { saveClass(it) { showAddClass = false } },
        )
    }
    viewClassId?.let { id ->
        key(id) {
            val cls by remember(id) { classesVm.observe(id) }.collectAsStateWithLifecycle(null)
            cls?.let {
                ViewClassSheet(
                    cls = it,
                    onDismiss = { viewClassId = null },
                    onEdit = {
                        editClassId = id
                        viewClassId = null
                    },
                    onDelete = { deleteClassId = id },
                )
            }
        }
    }
    editClassId?.let { id ->
        key(id) {
            val cls by remember(id) { classesVm.observe(id) }.collectAsStateWithLifecycle(null)
            cls?.let {
                ClassSheet(
                    sheetTitle = "Edit Class",
                    initial = it,
                    onDismiss = { editClassId = null },
                    onSave = { updated -> saveClass(updated) { editClassId = null } },
                )
            }
        }
    }
    deleteClassId?.let { id ->
        ConfirmDeleteDialog(
            title = "Delete class?",
            message = "It will be removed from every day it repeats on.",
            onConfirm = {
                classesVm.delete(id)
                deleteClassId = null
                viewClassId = null
            },
            onDismiss = { deleteClassId = null },
        )
    }

    // ─── Review Scan: scanned row opened like any class ──────────────────────────
    reviewViewId?.let { id ->
        val row = reviewItems?.firstOrNull { it.id == id }
        if (row != null) {
            key(id) {
                ViewClassSheet(
                    cls = row.toDraft(today),
                    onDismiss = { reviewViewId = null },
                    onEdit = {
                        reviewEditId = id
                        reviewViewId = null
                    },
                    onDelete = {
                        reviewItems = reviewItems?.filterNot { it.id == id }
                        reviewViewId = null
                    },
                )
            }
        }
    }
    reviewEditId?.let { id ->
        val row = reviewItems?.firstOrNull { it.id == id }
        if (row != null) {
            key(id) {
                ClassSheet(
                    sheetTitle = "Edit Class",
                    initial = row.toDraft(today),
                    onDismiss = { reviewEditId = null },
                    onSave = { updated ->
                        // A row is one weekday; the edit may now cover several.
                        reviewItems = reviewItems?.flatMap { if (it.id == id) updated.toScannedRows() else listOf(it) }
                        reviewEditId = null
                    },
                )
            }
        }
    }

    // ─── The photo couldn't be read: say why, offer to try again ─────────────────
    scanFailure?.let { failure ->
        AlertDialog(
            onDismissRequest = { scanFailure = null },
            containerColor = AppTheme.colors.surface,
            title = { Text("Couldn't read the schedule") },
            text = { Text(failure.message) },
            confirmButton = {
                if (failure.canRetry) {
                    TextButton(
                        onClick = {
                            scanFailure = null
                            runScan(failure.image, failure.rotation)
                        },
                    ) { Text("Try again") }
                } else {
                    TextButton(onClick = { scanFailure = null }) { Text("OK") }
                }
            },
            dismissButton = if (failure.canRetry) {
                { TextButton(onClick = { scanFailure = null }) { Text("Cancel") } }
            } else null,
        )
    }

    // ─── A class that clashes with existing ones: replace, or go back and change it ───
    pendingClassSave?.let { pending ->
        val plural = pending.conflicts.size > 1
        AlertDialog(
            onDismissRequest = { pendingClassSave = null },
            containerColor = AppTheme.colors.surface,
            title = { Text("Schedule conflict") },
            text = {
                Text(
                    "This overlaps with:\n" +
                        pending.conflicts.joinToString("\n") {
                            "\u2022 ${it.title} (${shortDays(it.days)}, ${timeRange(it.startTime, it.endTime)})"
                        } +
                        "\n\nReplace ${if (plural) "them" else "it"} with the new class?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        classesVm.replaceAndSave(pending.cls, pending.conflicts.map { it.id })
                        pendingClassSave = null
                        pending.onDone()
                    },
                ) { Text("Replace", color = Color(0xFFD93025)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingClassSave = null }) { Text("Cancel") }
            },
        )
    }

    // ─── A scan whose classes clash with existing ones ────────────────────────────
    pendingImport?.let { plan ->
        val clashing = plan.conflicts.flatMap { it.existing }.distinctBy { it.id }
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            containerColor = AppTheme.colors.surface,
            title = { Text("Schedule conflicts") },
            text = {
                Text(
                    "${plan.conflicts.size} scanned ${if (plan.conflicts.size == 1) "class overlaps" else "classes overlap"} " +
                        "with classes you already have:\n" +
                        clashing.take(4).joinToString("\n") {
                            "\u2022 ${it.title} (${shortDays(it.days)}, ${timeRange(it.startTime, it.endTime)})"
                        } +
                        (if (clashing.size > 4) "\n\u2022 \u2026and ${clashing.size - 4} more" else "") +
                        "\n\nReplace the existing ones, or keep them and skip the overlapping scanned classes?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingImport = null
                        finishImport(plan, replaceConflicts = true)
                    },
                ) { Text("Replace existing", color = Color(0xFFD93025)) }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        pendingImport = null
                        if (plan.toAdd.isEmpty()) {
                            reviewItems = null
                            toast("Nothing new to add.")
                        } else {
                            finishImport(plan, replaceConflicts = false)
                        }
                    },
                ) { Text("Keep existing") }
            },
        )
    }
}

/** A class waiting on the user's answer to "replace the classes it overlaps?". */
private class PendingClassSave(
    val cls: SchoolClass,
    val conflicts: List<SchoolClass>,
    val onDone: () -> Unit,
)

@Composable
private fun ConfirmDeleteDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppTheme.colors.surface,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Delete", color = Color(0xFFD93025)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

/** A scan that failed: what to tell the user, and the photo to retry with. */
private class ScanFailure(
    val message: String,
    val image: ByteArray,
    val rotation: Int?,
    val canRetry: Boolean,
)
