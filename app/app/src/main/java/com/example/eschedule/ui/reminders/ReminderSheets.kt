package com.example.eschedule.ui.reminders

import android.app.DatePickerDialog
import android.app.TimePickerDialog
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
import androidx.compose.runtime.LaunchedEffect
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
import com.example.eschedule.R
import com.example.eschedule.theme.Spacing
import com.example.eschedule.theme.UepBlue
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
import java.util.Calendar

// ─── Add / Edit Reminder bottom sheet ────────────────────────────────────────
//   Figma "Add Reminder" / "Edit Reminder":
//   title row → title field (y+70) → "Options" (y+140) → options card 343 × 85 (y+163)
//   → "Location" (y+268) → field → "Details" → field with 25dp bottom corners.

@Composable
fun AddReminderSheet(
    onDismiss: () -> Unit,
    onSave: (title: String) -> Unit,
    initialTitle: String = "",
    sheetTitle: String = "Add Reminder",
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(initialTitle) }

    // ── Date state ────────────────────────────────────────────────────────────
    val cal = remember { Calendar.getInstance() }
    var dateText by remember {
        mutableStateOf(
            "${monthName(cal.get(Calendar.MONTH))} ${cal.get(Calendar.DAY_OF_MONTH)}, ${cal.get(Calendar.YEAR)}"
        )
    }
    var showDatePicker by remember { mutableStateOf(false) }
    if (showDatePicker) {
        LaunchedEffect(Unit) {
            DatePickerDialog(
                context,
                { _, y, m, d ->
                    dateText = "${monthName(m)} $d, $y"
                    showDatePicker = false
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH),
            ).apply { setOnDismissListener { showDatePicker = false } }.show()
        }
    }

    // ── Time state (the Figma frames show the time line + toggle together) ────
    var timeText by remember { mutableStateOf("9:00 AM") }
    var timeEnabled by remember { mutableStateOf(true) }
    var showTimePicker by remember { mutableStateOf(false) }
    if (showTimePicker) {
        LaunchedEffect(Unit) {
            TimePickerDialog(
                context,
                { _, h, m ->
                    val amPm = if (h < 12) "AM" else "PM"
                    val h12  = when { h == 0 -> 12; h > 12 -> h - 12; else -> h }
                    timeText = "$h12:${m.toString().padStart(2, '0')} $amPm"
                    showTimePicker = false
                },
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE),
                false,
            ).apply { setOnDismissListener { showTimePicker = false } }.show()
        }
    }

    var locationText by remember { mutableStateOf("") }
    var notesText    by remember { mutableStateOf("") }

    FigmaSheet(onDismiss = onDismiss) {
        SheetHeader(title = sheetTitle) {
            SheetSaveButton(onClick = { onSave(title) })
        }

        SheetGap(Spacing.md)
        SheetTitleField(value = title, onValueChange = { title = it })

        SheetGap(Spacing.lg)
        SheetLabel("Options")
        SheetGap(Spacing.sm)
        SheetCard {
            Column {
                OptionRow("Date", dateText) { showDatePicker = true }
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
                            color = Color.Black,
                        )
                        if (timeEnabled) {
                            Row(
                                modifier = Modifier
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                    ) { showTimePicker = true }
                                    .padding(vertical = Spacing.xs),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = timeText,
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp,
                                    color = FigmaTextMuted,
                                )
                                Spacer(Modifier.width(Spacing.sm))
                                Image(painterResource(R.drawable.ic_sheet_expand10), null, Modifier.size(10.dp))
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
                            checkedTrackColor = UepBlue,
                            checkedBorderColor = Color.Transparent,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color(0xFFD0D3D9),
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
            value = locationText,
            onValueChange = { locationText = it },
            placeholder = "Add Location",
        )

        SheetGap(Spacing.lg)
        SheetLabel("Details")
        SheetGap(Spacing.sm)
        SheetIconField(
            iconRes = R.drawable.ic_sheet_pencil,
            value = notesText,
            onValueChange = { notesText = it },
            placeholder = "Notes",
            bottomRadius = 24.dp,
        )
    }
}

// ─── View Reminder (read-only) bottom sheet ──────────────────────────────────
//   Figma "View Reminder": 20dp checkbox + 20dp SemiBold title, Poppins Light detail lines.

@Composable
fun ViewReminderSheet(
    reminderId: String,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
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
                    .background(UepBlue, RoundedCornerShape(2.dp)),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Pag laba",
                    fontSize = 20.sp,
                    lineHeight = 30.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SheetTextValue,
                )
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    text = "Today",
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Light,
                    color = FigmaTextMuted,
                )
                Text(
                    text = "12:51 PM",
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Light,
                    color = FigmaTextMuted,
                )
            }
        }

        SheetGap(Spacing.lg)
        SheetLabel("Location")
        SheetGap(Spacing.sm)
        SheetIconText(R.drawable.ic_sheet_location, "Location", SheetTextPlaceholder)

        SheetGap(Spacing.lg)
        SheetLabel("Details")
        SheetGap(Spacing.sm)
        SheetIconText(
            iconRes = R.drawable.ic_sheet_pencil,
            text = "Notes",
            textColor = SheetTextPlaceholder,
            bottomRadius = 24.dp,
        )
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun monthName(month: Int): String = listOf(
    "January","February","March","April","May","June",
    "July","August","September","October","November","December",
)[month]
