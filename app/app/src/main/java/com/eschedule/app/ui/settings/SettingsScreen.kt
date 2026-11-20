package com.eschedule.app.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eschedule.app.theme.AppTheme
import com.eschedule.app.R
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.eschedule.app.data.ApiKeyEntry
import com.eschedule.app.data.NotificationPrefs
import com.eschedule.app.notify.Notifier
import com.eschedule.app.data.ThemeMode
import com.eschedule.app.theme.Spacing
import com.eschedule.app.ui.components.CardRadius
import com.eschedule.app.ui.components.EmptyAction
import com.eschedule.app.ui.components.EmptyArt
import com.eschedule.app.ui.components.EmptyCard
import com.eschedule.app.ui.components.EmptyState
import com.eschedule.app.ui.components.FigmaTextMuted
import com.eschedule.app.ui.components.FilterChip
import com.eschedule.app.ui.components.PageTitle
import com.eschedule.app.ui.components.ScreenMargin
import com.eschedule.app.ui.components.appShadow

// ─────────────────────────────────────────────────────────────────────────────
// Settings
//   Root list → sub-pages (kept in local state; the system back gesture returns to the list).
//   The info pages (Widgets, About, Privacy) live in InfoPages.kt.
// ─────────────────────────────────────────────────────────────────────────────

private enum class SettingsPage(val title: String) {
    Root(""),
    Widgets("Widgets"),
    Notifications("Notification & Reminder"),
    Theme("Theme"),
    AboutUs("About Us"),
    Privacy("Privacy Policy"),
    ApiKey("API Keys"),

    /** Not a page: opens the "reset app" confirmation. */
    ResetApp("Reset app"),
}

private data class SettingsItem(
    val label: String,
    val iconRes: Int,
    val page: SettingsPage,
    val trailing: String? = null,
    /** False for icons with their own colour (the red reset icon). */
    val tintable: Boolean = true,
)

@Composable
fun SettingsScreen(
    notifications: NotificationPrefs,
    onNotificationsChange: ((NotificationPrefs) -> NotificationPrefs) -> Unit,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    apiKeys: List<ApiKeyEntry>,
    activeApiKeyId: String?,
    onSaveApiKey: (id: String?, label: String, value: String) -> Unit,
    onDeleteApiKey: (String) -> Unit,
    onSetActiveApiKey: (String) -> Unit,
    onResetApp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var page by remember { mutableStateOf(SettingsPage.Root) }
    var showResetConfirm by remember { mutableStateOf(false) }
    BackHandler(enabled = page != SettingsPage.Root) { page = SettingsPage.Root }

    Crossfade(
        targetState = page,
        animationSpec = tween(200),
        modifier = modifier.fillMaxSize(),
        label = "settingsPage",
    ) { current ->
        when (current) {
            SettingsPage.Root -> SettingsRoot(
                themeLabel = themeMode.label,
                keyCount = apiKeys.size,
                onOpen = { target ->
                    if (target == SettingsPage.ResetApp) showResetConfirm = true else page = target
                },
            )
            SettingsPage.Notifications -> SubPage(current.title, onBack = { page = SettingsPage.Root }) {
                NotificationsContent(notifications, onNotificationsChange)
            }
            SettingsPage.Theme -> SubPage(current.title, onBack = { page = SettingsPage.Root }) {
                ThemeContent(themeMode, onThemeModeChange)
            }
            SettingsPage.ApiKey -> SubPage(current.title, onBack = { page = SettingsPage.Root }) {
                ApiKeysContent(apiKeys, activeApiKeyId, onSaveApiKey, onDeleteApiKey, onSetActiveApiKey)
            }
            SettingsPage.Widgets -> SubPage(current.title, onBack = { page = SettingsPage.Root }) {
                WidgetsContent()
            }
            SettingsPage.AboutUs -> SubPage(current.title, onBack = { page = SettingsPage.Root }) {
                AboutContent()
            }
            SettingsPage.Privacy -> SubPage(current.title, onBack = { page = SettingsPage.Root }) {
                PrivacyContent()
            }
            // "Reset app" is not a page (it opens a dialog); nothing to draw.
            SettingsPage.ResetApp -> Unit
        }
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            containerColor = AppTheme.colors.surface,
            title = { Text("Reset app?") },
            text = {
                Text(
                    "This permanently deletes all your classes and reminders, resets every setting " +
                        "(including your API keys). It can't be undone."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetConfirm = false
                        page = SettingsPage.Root
                        onResetApp()
                    },
                ) { Text("Reset everything", color = Color(0xFFD93025)) }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) { Text("Cancel") }
            },
        )
    }
}

// ── Root list ─────────────────────────────────────────────────────────────────

@Composable
private fun SettingsRoot(
    themeLabel: String,
    keyCount: Int,
    onOpen: (SettingsPage) -> Unit,
) {
    val general = listOf(
        SettingsItem("Widgets", R.drawable.ic_settings_widgets, SettingsPage.Widgets),
        SettingsItem("Notification & Reminder", R.drawable.ic_settings_notifications, SettingsPage.Notifications),
        SettingsItem("Theme", R.drawable.ic_settings_theme, SettingsPage.Theme, themeLabel),
    )
    val about = listOf(
        SettingsItem("About Us", R.drawable.ic_settings_follow, SettingsPage.AboutUs),
        SettingsItem("Privacy Policy", R.drawable.ic_settings_privacy, SettingsPage.Privacy),
    )
    val developer = listOf(
        SettingsItem("API keys", R.drawable.ic_settings_key, SettingsPage.ApiKey, if (keyCount == 0) "None saved" else "$keyCount saved"),
        SettingsItem("Reset app", R.drawable.ic_settings_reset, SettingsPage.ResetApp, tintable = false),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        PageTitle(title = "Settings")

        SettingsGroup("General", general, onOpen)
        Spacer(Modifier.height(Spacing.lg))
        SettingsGroup("About", about, onOpen)
        Spacer(Modifier.height(Spacing.lg))
        SettingsGroup("Developer", developer, onOpen)

        // Clear the floating nav bar
        Spacer(Modifier.height(140.dp))
    }
}

@Composable
private fun SettingsGroup(
    label: String,
    items: List<SettingsItem>,
    onOpen: (SettingsPage) -> Unit,
) {
    Text(
        text = label,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.Bold,
        color = AppTheme.colors.groupLabel,
        modifier = Modifier.padding(horizontal = ScreenMargin),
    )
    Spacer(Modifier.height(Spacing.sm))
    Column(
        modifier = Modifier
            .padding(horizontal = ScreenMargin)
            .fillMaxWidth()
            .appShadow(cornerRadius = CardRadius)
            .background(AppTheme.colors.surface, RoundedCornerShape(CardRadius))
            .padding(vertical = Spacing.xs),
    ) {
        items.forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onOpen(item.page) },
                    )
                    .padding(horizontal = Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painter = painterResource(item.iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    colorFilter = if (item.tintable) AppTheme.colors.blueIconFilter else null,
                )
                Spacer(Modifier.width(16.dp))
                Text(
                    text = item.label,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Normal,
                    color = AppTheme.colors.text,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
                if (item.trailing != null) {
                    Text(
                        text = item.trailing,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        color = FigmaTextMuted,
                    )
                }
            }
        }
    }
}

// ── Sub-page frame ────────────────────────────────────────────────────────────

@Composable
private fun SubPage(
    title: String,
    onBack: () -> Unit,
    scrolls: Boolean = true,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .statusBarsPadding()
                // Same vertical rhythm as PageTitle (24dp top, text 30dp tall, 16dp below): this row is
                // 40dp tall because of the back button, so it starts 5dp higher and ends 5dp lower.
                .padding(start = Spacing.sm, end = ScreenMargin, top = Spacing.lg - 5.dp, bottom = Spacing.md - 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onBack,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                val arrowColor = AppTheme.colors.title
                Canvas(Modifier.size(20.dp)) {
                    val w = 2.5.dp.toPx()
                    drawLine(arrowColor, Offset(size.width * 0.65f, size.height * 0.15f), Offset(size.width * 0.3f, size.height * 0.5f), w, StrokeCap.Round)
                    drawLine(arrowColor, Offset(size.width * 0.3f, size.height * 0.5f), Offset(size.width * 0.65f, size.height * 0.85f), w, StrokeCap.Round)
                }
            }
            Text(
                text = title,
                fontSize = 20.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.Bold,
                color = AppTheme.colors.title,
                modifier = Modifier.padding(start = Spacing.xs),
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .then(if (scrolls) Modifier.verticalScroll(rememberScrollState()) else Modifier),
        ) {
            content()
        }
    }
}

// ── Theme ─────────────────────────────────────────────────────────────────────

@Composable
private fun ThemeContent(
    mode: ThemeMode,
    onChange: (ThemeMode) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = ScreenMargin)) {
        GroupLabel("Appearance")
        SettingsCard {
            ThemeMode.entries.forEach { option ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onChange(option) },
                        )
                        .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = option.label, fontSize = 12.sp, lineHeight = 18.sp, color = AppTheme.colors.text)
                        Text(
                            text = when (option) {
                                ThemeMode.System -> "Match your phone's setting"
                                ThemeMode.Light -> "Always use the light theme"
                                ThemeMode.Dark -> "Always use the dark theme"
                            },
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            color = FigmaTextMuted,
                        )
                    }
                    RadioDot(selected = option == mode)
                }
            }
        }
        Spacer(Modifier.height(140.dp))
    }
}

/** 20dp radio indicator: ring plus a dot that scales in when selected. */
@Composable
private fun RadioDot(selected: Boolean) {
    val dot by animateFloatAsState(if (selected) 1f else 0f, tween(180), label = "radioDot")
    val ringColor by animateColorAsState(
        if (selected) AppTheme.colors.primary else AppTheme.colors.placeholder,
        tween(180),
        label = "radioRing",
    )
    val dotColor = AppTheme.colors.primary
    Canvas(Modifier.size(20.dp)) {
        drawCircle(ringColor, radius = size.minDimension / 2f - 1.dp.toPx(), style = Stroke(width = 2.dp.toPx()))
        if (dot > 0f) drawCircle(dotColor, radius = (size.minDimension / 2f - 5.dp.toPx()) * dot)
    }
}

// ── Notification & Reminder ───────────────────────────────────────────────────

private val ClassLeadOptions = listOf(5, 10, 15, 30, 60)
private val ReminderLeadOptions = listOf(0, 10, 60, 1440)

@Composable
private fun NotificationsContent(
    prefs: NotificationPrefs,
    onChange: ((NotificationPrefs) -> NotificationPrefs) -> Unit,
) {
    val context = LocalContext.current
    var allowed by remember { mutableStateOf(Notifier.canPost(context)) }
    var askedOnce by rememberSaveable { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        allowed = Notifier.canPost(context)
    }
    // Coming back from the system settings: pick up any change.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { allowed = Notifier.canPost(context) }

    Column(modifier = Modifier.padding(horizontal = ScreenMargin)) {
        if (!allowed) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .appShadow(cornerRadius = CardRadius)
                    .background(AppTheme.colors.surface, RoundedCornerShape(CardRadius))
                    .padding(Spacing.md),
            ) {
                Text(
                    text = "Notifications are off",
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.colors.text,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Allow notifications for eSchedule, or class and reminder alerts won't show up.",
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    color = FigmaTextMuted,
                )
                Spacer(Modifier.height(Spacing.md))
                EmptyAction(
                    text = "Allow notifications",
                    onClick = {
                        if (Build.VERSION.SDK_INT >= 33 && !askedOnce) {
                            askedOnce = true
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            // Already asked (or switched off in the system): the system settings decide.
                            context.startActivity(
                                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    },
                )
            }
            Spacer(Modifier.height(Spacing.lg))
        }

        GroupLabel("Classes")
        SettingsCard {
            ToggleRow(
                title = "Class reminders",
                subtitle = "Get a heads-up before a class starts",
                checked = prefs.classAlerts,
                onCheckedChange = { v -> onChange { it.copy(classAlerts = v) } },
            )
            AnimatedVisibility(
                visible = prefs.classAlerts,
                enter = fadeIn(tween(200)) + expandVertically(tween(220)),
                exit = fadeOut(tween(160)) + shrinkVertically(tween(200)),
            ) {
                ChipPicker(
                    title = "Remind me",
                    options = ClassLeadOptions,
                    selected = prefs.classLeadMinutes,
                    label = { "$it min before" },
                    onSelect = { v -> onChange { it.copy(classLeadMinutes = v) } },
                )
            }
        }

        Spacer(Modifier.height(Spacing.lg))
        GroupLabel("Reminders")
        SettingsCard {
            ToggleRow(
                title = "Reminder alerts",
                subtitle = "Get notified when a reminder is due",
                checked = prefs.reminderAlerts,
                onCheckedChange = { v -> onChange { it.copy(reminderAlerts = v) } },
            )
            AnimatedVisibility(
                visible = prefs.reminderAlerts,
                enter = fadeIn(tween(200)) + expandVertically(tween(220)),
                exit = fadeOut(tween(160)) + shrinkVertically(tween(200)),
            ) {
                ChipPicker(
                    title = "Alert me",
                    options = ReminderLeadOptions,
                    selected = prefs.reminderLeadMinutes,
                    label = {
                        when (it) {
                            0 -> "At due time"
                            60 -> "1 hour before"
                            1440 -> "1 day before"
                            else -> "$it min before"
                        }
                    },
                    onSelect = { v -> onChange { it.copy(reminderLeadMinutes = v) } },
                )
            }
        }

        Spacer(Modifier.height(Spacing.lg))
        GroupLabel("Alert style")
        SettingsCard {
            ToggleRow(
                title = "Sound",
                subtitle = null,
                checked = prefs.sound,
                onCheckedChange = { v -> onChange { it.copy(sound = v) } },
            )
            ToggleRow(
                title = "Vibrate",
                subtitle = null,
                checked = prefs.vibrate,
                onCheckedChange = { v -> onChange { it.copy(vibrate = v) } },
            )
        }

        Spacer(Modifier.height(Spacing.lg))
        EmptyAction(
            text = "Send a test notification",
            primary = false,
            onClick = {
                val shown = Notifier.show(
                    context = context,
                    notificationId = 9_999,
                    kind = Notifier.Kind.Reminder,
                    title = "Test notification",
                    text = "This is how your alerts will look and sound.",
                    sound = prefs.sound,
                    vibrate = prefs.vibrate,
                )
                if (!shown) Toast.makeText(context, "Notifications are turned off", Toast.LENGTH_SHORT).show()
            },
        )

        Spacer(Modifier.height(140.dp))
    }
}

@Composable
internal fun GroupLabel(text: String) {
    Text(
        text = text,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.Bold,
        color = AppTheme.colors.groupLabel,
    )
    Spacer(Modifier.height(Spacing.sm))
}

@Composable
internal fun SettingsCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .appShadow(cornerRadius = CardRadius)
            .background(AppTheme.colors.surface, RoundedCornerShape(CardRadius))
            .clip(RoundedCornerShape(CardRadius))
            .padding(vertical = Spacing.xs),
    ) {
        content()
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 12.sp, lineHeight = 18.sp, color = AppTheme.colors.text)
            if (subtitle != null) {
                Text(text = subtitle, fontSize = 11.sp, lineHeight = 16.sp, color = FigmaTextMuted)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            // Material's switch is 52 × 32dp; shrink it a little, anchored to the right edge.
            modifier = Modifier.graphicsLayer {
                scaleX = 0.8f
                scaleY = 0.8f
                transformOrigin = TransformOrigin(1f, 0.5f)
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = AppTheme.colors.primary,
                checkedBorderColor = Color.Transparent,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = AppTheme.colors.switchTrackOff,
                uncheckedBorderColor = Color.Transparent,
            ),
        )
    }
}

@Composable
private fun ChipPicker(
    title: String,
    options: List<Int>,
    selected: Int,
    label: (Int) -> String,
    onSelect: (Int) -> Unit,
) {
    Column(modifier = Modifier.padding(bottom = Spacing.md)) {
        Text(
            text = title,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            color = FigmaTextMuted,
            modifier = Modifier.padding(horizontal = Spacing.md),
        )
        Spacer(Modifier.height(Spacing.sm))
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            options.forEach { option ->
                FilterChip(label = label(option), selected = option == selected, onClick = { onSelect(option) })
            }
        }
    }
}

// ── Developer: API keys ───────────────────────────────────────────────────────

/** What the add / edit form is currently showing. */
private data class KeyDraft(val id: String?, val label: String, val value: String)

@Composable
private fun ApiKeysContent(
    keys: List<ApiKeyEntry>,
    activeId: String?,
    onSave: (id: String?, label: String, value: String) -> Unit,
    onDelete: (String) -> Unit,
    onSetActive: (String) -> Unit,
) {
    val context = LocalContext.current
    var draft by remember { mutableStateOf<KeyDraft?>(null) }
    var pendingDelete by remember { mutableStateOf<ApiKeyEntry?>(null) }

    Column(modifier = Modifier.padding(horizontal = ScreenMargin).animateContentSize()) {
        Text(
            text = "Keys for the schedule-recognition service. They are stored only on this device. " +
                "Tap a key to make it the one the app uses.",
            fontSize = 12.sp,
            lineHeight = 18.sp,
            color = FigmaTextMuted,
        )
        Spacer(Modifier.height(Spacing.md))

        if (keys.isEmpty()) {
            EmptyCard(
                title = "No keys saved yet",
                message = "Add one to use the recognition service.",
                art = EmptyArt.Inbox,
            )
        } else {
            GroupLabel("Saved keys")
            SettingsCard {
                keys.forEach { entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 64.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onSetActive(entry.id) },
                            )
                            .padding(start = Spacing.md, end = Spacing.sm, top = Spacing.sm, bottom = Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioDot(selected = entry.id == activeId)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = entry.label.ifBlank { "Unnamed key" },
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AppTheme.colors.text,
                                maxLines = 1,
                            )
                            Text(
                                text = maskKey(entry.value) + if (entry.id == activeId) "  \u00B7  In use" else "",
                                fontSize = 11.sp,
                                lineHeight = 16.sp,
                                color = FigmaTextMuted,
                                maxLines = 1,
                            )
                        }
                        TextAction("Edit", AppTheme.colors.title) {
                            draft = KeyDraft(entry.id, entry.label, entry.value)
                        }
                        TextAction("Delete", Color(0xFFD93025)) { pendingDelete = entry }
                    }
                }
            }
        }

        Spacer(Modifier.height(Spacing.lg))

        val editing = draft
        if (editing == null) {
            EmptyAction(text = "Add key", onClick = { draft = KeyDraft(null, "", "") })
        } else {
            KeyEditor(
                draft = editing,
                onChange = { draft = it },
                onSave = {
                    if (editing.value.isBlank()) {
                        Toast.makeText(context, "Paste a key first", Toast.LENGTH_SHORT).show()
                    } else {
                        onSave(editing.id, editing.label.ifBlank { "Key ${keys.size + 1}" }, editing.value)
                        draft = null
                    }
                },
                onCancel = { draft = null },
            )
        }

        Spacer(Modifier.height(140.dp))
    }

    pendingDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            containerColor = AppTheme.colors.surface,
            title = { Text("Delete key?") },
            text = { Text("\u201C${entry.label.ifBlank { "Unnamed key" }}\u201D will be removed from this device.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(entry.id)
                        pendingDelete = null
                    },
                ) { Text("Delete", color = Color(0xFFD93025)) }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } },
        )
    }
}

private fun maskKey(value: String): String =
    if (value.length <= 8) "\u2022\u2022\u2022\u2022\u2022\u2022" else "\u2022\u2022\u2022\u2022" + value.takeLast(4)

@Composable
private fun TextAction(text: String, color: Color, onClick: () -> Unit) {
    Text(
        text = text,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.SemiBold,
        color = color,
        modifier = Modifier
            .heightIn(min = 40.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 10.dp)
            .wrapContentHeightCentered(),
    )
}

@Composable
private fun KeyEditor(
    draft: KeyDraft,
    onChange: (KeyDraft) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    var reveal by remember { mutableStateOf(false) }

    SettingsCard {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Text(
                text = if (draft.id == null) "Add key" else "Edit key",
                fontSize = 12.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Bold,
                color = AppTheme.colors.text,
            )
            Spacer(Modifier.height(Spacing.sm))
            KeyTextField(
                value = draft.label,
                onValueChange = { onChange(draft.copy(label = it)) },
                placeholder = "Name (e.g. Gemini)",
            )
            Spacer(Modifier.height(Spacing.sm))
            KeyTextField(
                value = draft.value,
                onValueChange = { onChange(draft.copy(value = it)) },
                placeholder = "Paste your API key",
                password = !reveal,
                trailing = {
                    Text(
                        text = if (reveal) "Hide" else "Show",
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppTheme.colors.title,
                        modifier = Modifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { reveal = !reveal }
                            .padding(horizontal = Spacing.sm, vertical = Spacing.sm),
                    )
                },
            )
            Spacer(Modifier.height(Spacing.md))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                EmptyAction(text = "Save", onClick = onSave)
                EmptyAction(text = "Cancel", primary = false, onClick = onCancel)
            }
        }
    }
}

@Composable
private fun KeyTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    password: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AppTheme.colors.hairline, RoundedCornerShape(10.dp))
            .padding(start = Spacing.md, end = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(fontSize = 12.sp, lineHeight = 18.sp, color = AppTheme.colors.text),
            cursorBrush = SolidColor(AppTheme.colors.primary),
            visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = if (password) KeyboardType.Password else KeyboardType.Text),
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 14.dp),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            color = AppTheme.colors.placeholder,
                        )
                    }
                    inner()
                }
            },
        )
        if (trailing != null) trailing()
    }
}

/** Vertically centres a one-line label inside the taller touch target it sits in. */
private fun Modifier.wrapContentHeightCentered(): Modifier = this.then(Modifier.padding(vertical = 14.dp))
