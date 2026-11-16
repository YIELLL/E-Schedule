package com.example.eschedule.ui.classes

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.eschedule.R
import com.example.eschedule.theme.Spacing
import com.example.eschedule.theme.UepBlue
import com.example.eschedule.ui.components.CardRadius
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
import com.example.eschedule.ui.components.SheetTextValue
import com.example.eschedule.ui.components.SheetTitleField
import java.util.Calendar

private val DAY_LABELS = listOf("S", "M", "T", "W", "Th", "F", "S")

// ─── Add / Edit Class bottom sheet ───────────────────────────────────────────
//   Figma "Add Class" / "Edit Class - Calendar" / "Edit Class - Scan" (all identical layout):
//   title row → title field (y+70) → "Options" (y+140) → options card 343 × 236 (y+163)
//   → "Location" (y+419) → field (y+442) → "Description / Additional Information" (y+502)
//   → field with 25dp bottom corners (y+525).

@Composable
fun ClassSheet(
    sheetTitle: String,
    initialTitle: String = "",
    initialSelectedDays: Set<Int> = emptySet(),
    onDismiss: () -> Unit,
    onSave: (title: String, selectedDays: Set<Int>) -> Unit,
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(initialTitle) }
    var selectedDays by remember { mutableStateOf(initialSelectedDays) }

    // ── Date pickers ─────────────────────────────────────────────────────────
    val cal = remember { Calendar.getInstance() }
    var fromText by remember { mutableStateOf("August 1, 2026") }
    var untilText by remember { mutableStateOf("December 17, 2026") }
    var showFromPicker  by remember { mutableStateOf(false) }
    var showUntilPicker by remember { mutableStateOf(false) }

    if (showFromPicker) {
        LaunchedEffect(Unit) {
            DatePickerDialog(
                context,
                { _, y, m, d -> fromText = "${monthName(m)} $d, $y"; showFromPicker = false },
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH),
            ).apply { setOnDismissListener { showFromPicker = false } }.show()
        }
    }
    if (showUntilPicker) {
        LaunchedEffect(Unit) {
            DatePickerDialog(
                context,
                { _, y, m, d -> untilText = "${monthName(m)} $d, $y"; showUntilPicker = false },
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH),
            ).apply { setOnDismissListener { showUntilPicker = false } }.show()
        }
    }

    // ── Time pickers ─────────────────────────────────────────────────────────
    var startsText by remember { mutableStateOf(if (initialTitle.isEmpty()) "7:00 AM" else "8:00 AM") }
    var endsText   by remember { mutableStateOf("9:00 AM") }
    var showStartsPicker by remember { mutableStateOf(false) }
    var showEndsPicker   by remember { mutableStateOf(false) }

    if (showStartsPicker) {
        LaunchedEffect(Unit) {
            TimePickerDialog(
                context,
                { _, h, m -> startsText = formatTime(h, m); showStartsPicker = false },
                7, 0, false,
            ).apply { setOnDismissListener { showStartsPicker = false } }.show()
        }
    }
    if (showEndsPicker) {
        LaunchedEffect(Unit) {
            TimePickerDialog(
                context,
                { _, h, m -> endsText = formatTime(h, m); showEndsPicker = false },
                9, 0, false,
            ).apply { setOnDismissListener { showEndsPicker = false } }.show()
        }
    }

    var locationText by remember { mutableStateOf("") }
    var descText     by remember { mutableStateOf("") }

    FigmaSheet(onDismiss = onDismiss) {
        SheetHeader(title = sheetTitle) {
            SheetSaveButton(onClick = { onSave(title, selectedDays) })
        }

        SheetGap(Spacing.md)
        SheetTitleField(value = title, onValueChange = { title = it })

        SheetGap(Spacing.lg)
        SheetLabel("Options")
        SheetGap(Spacing.sm)
        SheetCard {
            Column {
                OptionRow("Starts", startsText) { showStartsPicker = true }
                OptionDivider()
                OptionRow("Ends", endsText) { showEndsPicker = true }
                OptionDivider()
                OptionRow("From", fromText) { showFromPicker = true }
                OptionDivider()
                OptionRow("Until", untilText) { showUntilPicker = true }
                OptionDivider()

                Text(
                    text = "Repeat",
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = Color.Black,
                    modifier = Modifier.padding(start = Spacing.md, top = 15.dp),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.md, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    DAY_LABELS.forEachIndexed { index, label ->
                        val isSelected = selectedDays.contains(index)
                        DayChip(
                            label = label,
                            selected = isSelected,
                            onClick = {
                                selectedDays = if (isSelected) selectedDays - index
                                else selectedDays + index
                            },
                        )
                    }
                }
                Spacer(Modifier.height(Spacing.xs))
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
        SheetLabel("Description / Additional Information")
        SheetGap(Spacing.sm)
        SheetIconField(
            iconRes = R.drawable.ic_sheet_pencil,
            value = descText,
            onValueChange = { descText = it },
            placeholder = "Description",
            bottomRadius = 24.dp,
        )
    }
}

// ─── View Class bottom sheet ─────────────────────────────────────────────────
//   Figma "View Class - Calendar" / "View Class - Scan".

@Composable
fun ViewClassSheet(
    classId: String,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    FigmaSheet(onDismiss = onDismiss) {
        SheetHeader(title = "Class Details") {
            SheetCircleAction(R.drawable.ic_sheet_delete, "Delete", onDelete)
            Spacer(Modifier.width(Spacing.sm))
            SheetCircleAction(R.drawable.ic_sheet_edit, "Edit", onEdit)
        }

        SheetGap(Spacing.md)
        // Accent bar (4 × content − 5) beside the 20dp title and two Poppins Light detail lines
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
                    text = "IT102 - Mobile App Development",
                    fontSize = 20.sp,
                    lineHeight = 30.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SheetTextValue,
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    text = "Repeats every Tuesday & Thursday",
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Light,
                    color = FigmaTextMuted,
                )
                Text(
                    text = "8:00 AM - 9:00 AM",
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
        SheetIconText(R.drawable.ic_sheet_location, "CSFLD 22", SheetTextValue)

        SheetGap(Spacing.lg)
        SheetLabel("Description / Additional Information")
        SheetGap(Spacing.sm)
        SheetIconText(
            iconRes = R.drawable.ic_sheet_pencil,
            text = "Class ID: 0012764, Instructor: B.PTL2",
            textColor = SheetTextValue,
            bottomRadius = 24.dp,
        )
    }
}

// ─── Private helpers ─────────────────────────────────────────────────────────

/** 36dp day chip (touch-friendly): Poppins Bold 14, white-on-blue when selected. */
@Composable
private fun DayChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(
                if (selected) R.drawable.ic_sheet_day_chip_on else R.drawable.ic_sheet_day_chip_off
            ),
            contentDescription = null,
            modifier = Modifier.size(36.dp),
        )
        Text(
            text = label,
            fontSize = 14.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) Color.White else FigmaTextMuted,
        )
    }
}

private fun monthName(month: Int): String = listOf(
    "January","February","March","April","May","June",
    "July","August","September","October","November","December",
)[month]

private fun formatTime(h: Int, m: Int): String {
    val amPm = if (h < 12) "AM" else "PM"
    val h12  = when { h == 0 -> 12; h > 12 -> h - 12; else -> h }
    return "$h12:${m.toString().padStart(2, '0')} $amPm"
}
