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
import com.example.eschedule.data.ScannedClass
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
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val homeState by homeVm.uiState.collectAsStateWithLifecycle()
    val weekBlocks by classesVm.weekBlocks.collectAsStateWithLifecycle()
    val hasClasses by classesVm.hasClasses.collectAsStateWithLifecycle()
    val remindersState by remindersVm.uiState.collectAsStateWithLifecycle()

    var currentTab by remember { mutableStateOf<TabRoute>(TabRoute.Home) }
    var captureRequested by remember { mutableStateOf(false) }

    // Scan flow state
    var isScanning by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var reviewItems by remember { mutableStateOf<List<ScannedClass>?>(null) }

    // Sheet / dialog state — null = closed
    var showAddReminder by remember { mutableStateOf(false) }
    var viewReminderId by remember { mutableStateOf<Long?>(null) }
    var editReminderId by remember { mutableStateOf<Long?>(null) }
    var deleteReminderId by remember { mutableStateOf<Long?>(null) }

    var showAddClass by remember { mutableStateOf(false) }
    var viewClassId by remember { mutableStateOf<Long?>(null) }
    var editClassId by remember { mutableStateOf<Long?>(null) }
    var deleteClassId by remember { mutableStateOf<Long?>(null) }

    fun toast(message: String) = Toast.makeText(context, message, Toast.LENGTH_SHORT).show()

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color(0xFFF7F7F7),
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
                        onImageCaptured = { jpeg ->
                            scope.launch {
                                isScanning = true
                                runCatching { scanVm.scan(jpeg) }
                                    .onSuccess { reviewItems = it }
                                    .onFailure { toast("Couldn't read the schedule. Please try again.") }
                                isScanning = false
                            }
                        },
                    )
                    TabRoute.Reminders -> RemindersScreen(
                        state = remindersState,
                        onFilterChange = remindersVm::setFilter,
                        onStatusChange = remindersVm::setStatus,
                        onReminderMoreClick = { viewReminderId = it },
                        onAddReminder = { showAddReminder = true },
                    )
                    TabRoute.Settings -> SettingsScreen(
                        onItemClick = { /* future: navigate to sub-settings */ },
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
                        runCatching { classesVm.importScanned(shown) }
                            .onSuccess { count ->
                                reviewItems = null
                                currentTab = TabRoute.Classes
                                toast(if (count == 1) "Added 1 class" else "Added $count classes")
                            }
                            .onFailure { toast("Couldn't save your classes. Please try again.") }
                        isSaving = false
                    }
                },
                onRemove = { removed -> reviewItems = shown.filterNot { it.id == removed.id } },
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
            onSave = {
                classesVm.save(it)
                showAddClass = false
            },
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
                    onSave = { updated ->
                        classesVm.save(updated)
                        editClassId = null
                    },
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
}

@Composable
private fun ConfirmDeleteDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
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
