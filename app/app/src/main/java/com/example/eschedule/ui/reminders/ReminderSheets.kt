package com.example.eschedule.ui.reminders

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.eschedule.theme.AppTheme
import com.example.eschedule.R
import com.example.eschedule.data.model.Reminder
import com.example.eschedule.data.model.ReminderStatus
import com.example.eschedule.theme.Spacing
import com.example.eschedule.ui.components.FigmaSheet
import com.example.eschedule.ui.components.FigmaTextMuted
import com.example.eschedule.ui.components.OptionDivider
import com.example.eschedule.ui.components.OptionRow
import com.example.eschedule.ui.components.SheetCard
import com.example.eschedule.ui.components.SheetCircleAction
import com.example.eschedule.ui.components.SheetGap
import com.example.eschedule.ui.components.SheetHeader
import com.example.eschedule.ui.components.SheetIconField
import com.example.eschedule.ui.components.SheetIconText
import com.example.eschedule.ui.components.SheetLabel
import com.example.eschedule.ui.components.SheetSaveButton
import com.example.eschedule.ui.components.SheetTextPlaceholder
import com.example.eschedule.ui.components.SheetTextValue
import com.example.eschedule.ui.components.SheetTitleField
import com.example.eschedule.ui.display
import com.example.eschedule.ui.displayLong
import com.example.eschedule.ui.relativeDay
import java.time.LocalDate
import java.time.LocalTime

// ─── Add / Edit Reminder bottom sheet ────────────────────────────────────────

/**
 * Add ([initial] = null) or edit a reminder. Validates the form and hands a complete [Reminder]
 * to [onSave]; persisting it is the caller's job.
 */
@Composable
fun AddReminderSheet(
    initial: Reminder?,
    onDismiss: () -> Unit,
    onSave: (Reminder) -> Unit,
    sheetTitle: String = "Add Reminder",
) {
    val context = LocalContext.current
    val pickerTheme = if (AppTheme.colors.dark) R.style.Picker_Dark else R.style.Picker_Light

    var title by remember { mutableStateOf(initial?.title.orEmpty()) }
    var date by remember { mutableStateOf(initial?.date ?: LocalDate.now()) }
    // New reminders default to the next full hour with the time switched on.
    var time by remember {
        mutableStateOf(initial?.time ?: LocalTime.now().withMinute(0).withSecond(0).withNano(0).plusHours(1))
    }
    var timeEnabled by remember { mutableStateOf(initial == null || initial.time != null) }
    var location by remember { mutableStateOf(initial?.location.orEmpty()) }
    var notes by remember { mutableStateOf(initial?.notes.orEmpty()) }

    fun pickDate() {
        DatePickerDialog(
            context,
            pickerTheme,
            { _, y, m, d -> date = LocalDate.of(y, m + 1, d) },
            date.year, date.monthValue - 1, date.dayOfMonth,
        ).show()
    }

    fun pickTime() {
        TimePickerDialog(context, pickerTheme, { _, h, m -> time = LocalTime.of(h, m) }, time.hour, time.minute, false).show()
    }

    fun trySave() {
        if (title.isBlank()) {
            Toast.makeText(context, "Give the reminder a title", Toast.LENGTH_SHORT).show()
            return
        }
        onSave(
            Reminder(
                id = initial?.id ?: 0,
                title = title,
                date = date,
                time = if (timeEnabled) time else null,
                location = location,
                notes = notes,
                completed = initial?.completed ?: false,
                wontDo = initial?.wontDo ?: false,
                completedAt = initial?.completedAt,
            )
        )
    }

    FigmaSheet(onDismiss = onDismiss) {
        SheetHeader(title = sheetTitle) {
            SheetSaveButton(onClick = ::trySave)
        }

        SheetGap(Spacing.md)
        SheetTitleField(value = title, onValueChange = { title = it })

        SheetGap(Spacing.lg)
        SheetLabel("Options")
        SheetGap(Spacing.sm)
        SheetCard {
            Column {
                OptionRow("Date", date.displayLong(), ::pickDate)
                OptionDivider()

                // Time row: label (+ picker line while enabled) on the left, switch on the right
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 64.dp)
                        .padding(horizontal = Spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Time",
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            color = AppTheme.colors.text,
                        )
                        // The time line grows / fades in and out with the switch.
                        AnimatedVisibility(
                            visible = timeEnabled,
                            enter = fadeIn(tween(200)) + expandVertically(tween(220)),
                            exit = fadeOut(tween(160)) + shrinkVertically(tween(200)),
                        ) {
                            Row(
                                modifier = Modifier
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = ::pickTime,
                                    )
                                    .padding(vertical = Spacing.xs),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = time.display(),
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp,
                                    color = FigmaTextMuted,
                                )
                                Spacer(Modifier.width(Spacing.sm))
                                Image(painterResource(R.drawable.ic_sheet_expand10), null, Modifier.size(10.dp), colorFilter = AppTheme.colors.blackIconFilter)
                            }
                        }
                    }
                    Switch(
                        checked = timeEnabled,
                        onCheckedChange = { timeEnabled = it },
                        // Material's switch is 52 × 32dp; shrink to ~39 × 24dp, anchored to the right edge.
                        modifier = Modifier.graphicsLayer {
                            scaleX = 0.75f
                            scaleY = 0.75f
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
        }

        SheetGap(Spacing.lg)
        SheetLabel("Location")
        SheetGap(Spacing.sm)
        SheetIconField(
            iconRes = R.drawable.ic_sheet_location,
            value = location,
            onValueChange = { location = it },
            placeholder = "Add Location",
        )

        SheetGap(Spacing.lg)
        SheetLabel("Details")
        SheetGap(Spacing.sm)
        SheetIconField(
            iconRes = R.drawable.ic_sheet_pencil,
            value = notes,
            onValueChange = { notes = it },
            placeholder = "Notes",
            bottomRadius = 24.dp,
        )
    }
}

// ─── View Reminder (read-only) bottom sheet ──────────────────────────────────

@Composable
fun ViewReminderSheet(
    reminder: Reminder,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val today = remember { LocalDate.now() }

    FigmaSheet(onDismiss = onDismiss) {
        SheetHeader(title = "Details") {
            SheetCircleAction(R.drawable.ic_sheet_delete, "Delete", onDelete)
            Spacer(Modifier.width(Spacing.sm))
            SheetCircleAction(R.drawable.ic_sheet_edit, "Edit", onEdit)
        }

        SheetGap(Spacing.md)
        // Accent bar beside the title + detail lines (same treatment as Class Details)
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .padding(top = 5.dp)
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(AppTheme.colors.primary, RoundedCornerShape(2.dp)),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reminder.title,
                    fontSize = 20.sp,
                    lineHeight = 30.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SheetTextValue,
                )
                Spacer(Modifier.height(Spacing.xs))
                DetailLine(relativeDay(reminder.date, today))
                DetailLine(reminder.time?.display() ?: "All day")
                when (reminder.status) {
                    ReminderStatus.Done -> DetailLine("Completed")
                    ReminderStatus.WontDo -> DetailLine("Won\u2019t do")
                    ReminderStatus.Open -> Unit
                }
            }
        }

        SheetGap(Spacing.lg)
        SheetLabel("Location")
        SheetGap(Spacing.sm)
        SheetIconText(
            iconRes = R.drawable.ic_sheet_location,
            text = reminder.location.ifBlank { "No location" },
            textColor = if (reminder.location.isBlank()) SheetTextPlaceholder else SheetTextValue,
        )

        SheetGap(Spacing.lg)
        SheetLabel("Details")
        SheetGap(Spacing.sm)
        SheetIconText(
            iconRes = R.drawable.ic_sheet_pencil,
            text = reminder.notes.ifBlank { "No notes" },
            textColor = if (reminder.notes.isBlank()) SheetTextPlaceholder else SheetTextValue,
            bottomRadius = 24.dp,
        )
    }
}

@Composable
private fun DetailLine(text: String) {
    Text(
        text = text,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.Light,
        color = FigmaTextMuted,
    )
}
