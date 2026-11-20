package com.eschedule.app.ui.classes

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.eschedule.app.theme.AppTheme
import com.eschedule.app.R
import com.eschedule.app.data.model.SchoolClass
import com.eschedule.app.theme.Spacing
import com.eschedule.app.ui.components.FigmaSheet
import com.eschedule.app.ui.components.FigmaTextMuted
import com.eschedule.app.ui.components.OptionDivider
import com.eschedule.app.ui.components.OptionRow
import com.eschedule.app.ui.components.SheetCard
import com.eschedule.app.ui.components.SheetCircleAction
import com.eschedule.app.ui.components.SheetGap
import com.eschedule.app.ui.components.SheetHeader
import com.eschedule.app.ui.components.SheetIconField
import com.eschedule.app.ui.components.SheetIconText
import com.eschedule.app.ui.components.SheetLabel
import com.eschedule.app.ui.components.SheetSaveButton
import com.eschedule.app.ui.components.SheetTextPlaceholder
import com.eschedule.app.ui.components.SheetTextValue
import com.eschedule.app.ui.components.SheetTitleField
import com.eschedule.app.ui.dateRange
import com.eschedule.app.ui.display
import com.eschedule.app.ui.displayLong
import com.eschedule.app.ui.repeatsLabel
import com.eschedule.app.ui.timeRange
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** Chip order + labels for the Repeat row (week starts on Sunday, as in the design). */
private val DAY_CHIPS = listOf(
    DayOfWeek.SUNDAY to "S",
    DayOfWeek.MONDAY to "M",
    DayOfWeek.TUESDAY to "T",
    DayOfWeek.WEDNESDAY to "W",
    DayOfWeek.THURSDAY to "Th",
    DayOfWeek.FRIDAY to "F",
    DayOfWeek.SATURDAY to "S",
)

// ─── Add / Edit Class bottom sheet ───────────────────────────────────────────

/**
 * Add ([initial] = null) or edit a class. The sheet validates the form and hands a complete
 * [SchoolClass] to [onSave]; persisting it is the caller's job.
 */
@Composable
fun ClassSheet(
    sheetTitle: String,
    initial: SchoolClass?,
    onDismiss: () -> Unit,
    onSave: (SchoolClass) -> Unit,
) {
    val context = LocalContext.current
    val today = remember { LocalDate.now() }
    val pickerTheme = if (AppTheme.colors.dark) R.style.Picker_Dark else R.style.Picker_Light

    var title by remember { mutableStateOf(initial?.title.orEmpty()) }
    var startTime by remember { mutableStateOf(initial?.startTime ?: LocalTime.of(7, 0)) }
    var endTime by remember { mutableStateOf(initial?.endTime ?: LocalTime.of(9, 0)) }
    var fromDate by remember { mutableStateOf(initial?.startDate ?: today) }
    var untilDate by remember { mutableStateOf(initial?.endDate ?: today.plusMonths(4)) }
    var days by remember { mutableStateOf(initial?.days ?: emptySet()) }
    var location by remember { mutableStateOf(initial?.location.orEmpty()) }
    var description by remember { mutableStateOf(initial?.description.orEmpty()) }

    fun pickTime(current: LocalTime, onPicked: (LocalTime) -> Unit) {
        TimePickerDialog(context, pickerTheme, { _, h, m -> onPicked(LocalTime.of(h, m)) }, current.hour, current.minute, false).show()
    }

    fun pickDate(current: LocalDate, minDate: LocalDate? = null, onPicked: (LocalDate) -> Unit) {
        DatePickerDialog(
            context,
            pickerTheme,
            { _, y, m, d -> onPicked(LocalDate.of(y, m + 1, d)) },
            current.year, current.monthValue - 1, current.dayOfMonth,
        ).apply {
            // Dates before the minimum are greyed out, so an invalid range can't be picked at all.
            minDate?.let { datePicker.minDate = it.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() }
        }.show()
    }

    fun toast(message: String) = Toast.makeText(context, message, Toast.LENGTH_SHORT).show()

    // Changing the start keeps the class at least an hour long: the end is pushed out if needed.
    fun onStartPicked(picked: LocalTime) {
        startTime = picked
        val minEnd = if (picked.hour >= 23) LocalTime.of(23, 59) else picked.plusHours(1)
        if (endTime.isBefore(minEnd)) endTime = minEnd
    }

    // The end can't be at or before the start.
    fun onEndPicked(picked: LocalTime) {
        if (picked.isAfter(startTime)) endTime = picked
        else toast("End time must be after the start time")
    }

    // Changing "From" keeps "Until" at least a day later.
    fun onFromPicked(picked: LocalDate) {
        fromDate = picked
        if (!untilDate.isAfter(picked)) untilDate = picked.plusDays(1)
    }

    fun onUntilPicked(picked: LocalDate) {
        if (picked.isAfter(fromDate)) untilDate = picked
        else toast("\"Until\" must be at least a day after \"From\"")
    }

    fun trySave() {
        val error = when {
            title.isBlank() -> "Give the class a title"
            !endTime.isAfter(startTime) -> "The end time must be after the start time"
            days.isEmpty() -> "Pick at least one day it repeats on"
            !untilDate.isAfter(fromDate) -> "\"Until\" must be at least a day after \"From\""
            else -> null
        }
        if (error != null) {
            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
            return
        }
        onSave(
            SchoolClass(
                id = initial?.id ?: 0,
                title = title,
                startTime = startTime,
                endTime = endTime,
                startDate = fromDate,
                endDate = untilDate,
                days = days,
                location = location,
                description = description,
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
                OptionRow("Starts", startTime.display()) { pickTime(startTime, ::onStartPicked) }
                OptionDivider()
                OptionRow("Ends", endTime.display()) { pickTime(endTime, ::onEndPicked) }
                OptionDivider()
                OptionRow("From", fromDate.displayLong()) { pickDate(fromDate, onPicked = ::onFromPicked) }
                OptionDivider()
                OptionRow("Until", untilDate.displayLong()) { pickDate(untilDate, minDate = fromDate.plusDays(1), onPicked = ::onUntilPicked) }
                OptionDivider()

                Text(
                    text = "Repeat",
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = AppTheme.colors.text,
                    modifier = Modifier.padding(start = Spacing.md, top = 15.dp),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.md, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    DAY_CHIPS.forEach { (day, label) ->
                        val isSelected = day in days
                        DayChip(
                            label = label,
                            selected = isSelected,
                            onClick = { days = if (isSelected) days - day else days + day },
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
            value = location,
            onValueChange = { location = it },
            placeholder = "Add Location",
        )

        SheetGap(Spacing.lg)
        SheetLabel("Description / Additional Information")
        SheetGap(Spacing.sm)
        SheetIconField(
            iconRes = R.drawable.ic_sheet_pencil,
            value = description,
            onValueChange = { description = it },
            placeholder = "Description",
            bottomRadius = 24.dp,
        )
    }
}

// ─── View Class bottom sheet ─────────────────────────────────────────────────

@Composable
fun ViewClassSheet(
    cls: SchoolClass,
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
        // Accent bar beside the title and the Poppins Light detail lines
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
                    text = cls.title,
                    fontSize = 20.sp,
                    lineHeight = 30.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SheetTextValue,
                )
                Spacer(Modifier.height(5.dp))
                DetailLine(repeatsLabel(cls.days))
                DetailLine(timeRange(cls.startTime, cls.endTime))
                DetailLine(dateRange(cls.startDate, cls.endDate))
            }
        }

        SheetGap(Spacing.lg)
        SheetLabel("Location")
        SheetGap(Spacing.sm)
        SheetIconText(
            iconRes = R.drawable.ic_sheet_location,
            text = cls.location.ifBlank { "No location" },
            textColor = if (cls.location.isBlank()) SheetTextPlaceholder else SheetTextValue,
        )

        SheetGap(Spacing.lg)
        SheetLabel("Description / Additional Information")
        SheetGap(Spacing.sm)
        SheetIconText(
            iconRes = R.drawable.ic_sheet_pencil,
            text = cls.description.ifBlank { "No description" },
            textColor = if (cls.description.isBlank()) SheetTextPlaceholder else SheetTextValue,
            bottomRadius = 24.dp,
        )
    }
}

// ─── Private helpers ─────────────────────────────────────────────────────────

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
                if (AppTheme.colors.dark) {
                    if (selected) R.drawable.ic_sheet_day_chip_on_dark else R.drawable.ic_sheet_day_chip_off_dark
                } else {
                    if (selected) R.drawable.ic_sheet_day_chip_on else R.drawable.ic_sheet_day_chip_off
                }
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
