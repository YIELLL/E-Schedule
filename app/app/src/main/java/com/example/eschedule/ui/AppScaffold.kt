package com.example.eschedule.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.eschedule.data.FakeScheduleScanRepository
import com.example.eschedule.data.ScannedClass
import com.example.eschedule.data.ScheduleScanRepository
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
 * Hosts the bottom navigation bar and swaps tab content.
 * Modal sheets and the Review Scan screen are layered on top of the tab content.
 *
 * @param scanRepository backend seam for the scan → review → save flow. Defaults to a fake;
 *        pass the real implementation here once the backend is ready.
 */
@Composable
fun AppScaffold(
    scanRepository: ScheduleScanRepository = remember { FakeScheduleScanRepository() },
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var currentTab by remember { mutableStateOf<TabRoute>(TabRoute.Home) }
    var captureRequested by remember { mutableStateOf(false) }

    // Scan flow state
    var isScanning by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var reviewItems by remember { mutableStateOf<List<ScannedClass>?>(null) }

    // Sheet state holders — null = closed
    var showAddReminder by remember { mutableStateOf(false) }
    var viewReminderId by remember { mutableStateOf<String?>(null) }
    var editReminderId by remember { mutableStateOf<String?>(null) }

    var showAddClass by remember { mutableStateOf(false) }
    var viewClassId by remember { mutableStateOf<String?>(null) }
    var editClassId by remember { mutableStateOf<String?>(null) }

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
            Box(modifier = Modifier.fillMaxSize()) {
                when (currentTab) {
                    TabRoute.Home -> HomeScreen(
                        onClassClick = { viewClassId = it },
                        onReminderMoreClick = { viewReminderId = it },
                        onReminderEditClick = { editReminderId = it },
                    )
                    TabRoute.Classes -> ClassesScreen(
                        onClassClick = { viewClassId = it },
                        onAddClass = { showAddClass = true },
                    )
                    TabRoute.Scan -> ScanScreen(
                        captureRequested = captureRequested,
                        onCaptureHandled = { captureRequested = false },
                        onImageCaptured = { jpeg ->
                            scope.launch {
                                isScanning = true
                                runCatching { scanRepository.scan(jpeg) }
                                    .onSuccess { reviewItems = it }
                                    .onFailure {
                                        Toast.makeText(
                                            context,
                                            "Couldn't read the schedule. Please try again.",
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                    }
                                isScanning = false
                            }
                        },
                    )
                    TabRoute.Reminders -> RemindersScreen(
                        onAddReminder = { showAddReminder = true },
                        onReminderMoreClick = { viewReminderId = it },
                        onReminderEditClick = { editReminderId = it },
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
                        runCatching { scanRepository.save(shown) }
                            .onSuccess {
                                reviewItems = null
                                currentTab = TabRoute.Classes
                            }
                            .onFailure {
                                Toast.makeText(
                                    context,
                                    "Couldn't save your classes. Please try again.",
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
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
            onDismiss = { showAddReminder = false },
            onSave = { _ -> showAddReminder = false },
        )
    }
    viewReminderId?.let { id ->
        ViewReminderSheet(
            reminderId = id,
            onDismiss = { viewReminderId = null },
            onEdit = {
                editReminderId = id
                viewReminderId = null
            },
            onDelete = { viewReminderId = null },
        )
    }
    editReminderId?.let { id ->
        AddReminderSheet(
            sheetTitle = "Edit Reminder",
            initialTitle = "Pag laba", // Will be pulled from repo later
            onDismiss = { editReminderId = null },
            onSave = { _ -> editReminderId = null },
        )
    }

    // ─── Class sheets ─────────────────────────────────────────────────────────────
    if (showAddClass) {
        ClassSheet(
            sheetTitle = "Add Class",
            onDismiss = { showAddClass = false },
            onSave = { _, _ -> showAddClass = false },
        )
    }
    viewClassId?.let { id ->
        ViewClassSheet(
            classId = id,
            onDismiss = { viewClassId = null },
            onEdit = {
                editClassId = id
                viewClassId = null
            },
            onDelete = { viewClassId = null },
        )
    }
    editClassId?.let { id ->
        ClassSheet(
            sheetTitle = "Edit Class",
            initialTitle = "IT102 - Mobile Application Development",
            initialSelectedDays = setOf(1, 3), // Tue & Thu
            onDismiss = { editClassId = null },
            onSave = { _, _ -> editClassId = null },
        )
    }
}
