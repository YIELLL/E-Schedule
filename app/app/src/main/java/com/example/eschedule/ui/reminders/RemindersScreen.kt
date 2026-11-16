package com.example.eschedule.ui.reminders

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.eschedule.theme.Spacing
import com.example.eschedule.ui.components.CardRadius
import com.example.eschedule.ui.components.EScheduleFab
import com.example.eschedule.ui.components.FilterChip
import com.example.eschedule.ui.components.PageTitle
import com.example.eschedule.ui.components.ReminderItemData
import com.example.eschedule.ui.components.ReminderListItem
import com.example.eschedule.ui.components.ScreenMargin
import com.example.eschedule.ui.components.appShadow

private val FILTERS = listOf("All", "Today", "Week", "Month", "Completed")

/** Figma: completed due-dates are UEP blue @ 60%. */
private val CompletedDueColor = Color(0x9917376E)

@Composable
fun RemindersScreen(
    onAddReminder: () -> Unit,
    onReminderMoreClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val activeReminders = remember {
        listOf(
            ReminderItemData("r1", "Pag laba", "12:51 PM"),
            ReminderItemData("r2", "Do app dev project", "Tomorrow"),
            ReminderItemData("r3", "Make journal", "Sep 21"),
            ReminderItemData("r4", "Assassinate Jons", "Dec 17"),
        )
    }
    val completedReminders = remember {
        listOf(
            ReminderItemData("c1",  "Voiceover",          "10/18/24", isCompleted = true),
            ReminderItemData("c2",  "First Aid Video",    "10/16/24", isCompleted = true),
            ReminderItemData("c3",  "Tree planting",      "10/16/24", isCompleted = true),
            ReminderItemData("c4",  "Upaya im intro",     "10/9/24",  isCompleted = true),
            ReminderItemData("c5",  "NSTP",               "10/2/24",  isCompleted = true),
            ReminderItemData("c6",  "Intro",              "10/2/24",  isCompleted = true),
            ReminderItemData("c7",  "Comprog",            "9/25/24",  isCompleted = true),
            ReminderItemData("c8",  "Assignment sa NSTP", "9/18/24",  isCompleted = true),
            ReminderItemData("c9",  "Edit vid",           "9/2/24",   isCompleted = true),
            ReminderItemData("c10", "PerDev",             "11/21/23", isCompleted = true),
            ReminderItemData("c11", "Sex kit nigga",      "11/21/23", isCompleted = true),
        )
    }

    var selectedFilter by remember { mutableStateOf("All") }
    var checkedIds by remember { mutableStateOf(setOf<String>()) }

    Box(modifier = modifier.fillMaxSize()) {
        // Outer Column: title fixed, content scrolls
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Title (x 30, y 100); chips start 20dp below at y 150
            PageTitle(title = "Reminders")

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                // Filter chips: 25dp tall, 10dp apart, x starts at 30
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = ScreenMargin),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    FILTERS.forEach { filter ->
                        FilterChip(
                            label = filter,
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                        )
                    }
                }

                Spacer(Modifier.height(Spacing.md))

                // ── Active reminders card: 48dp rows
                Column(
                    modifier = Modifier
                        .padding(horizontal = ScreenMargin)
                        .fillMaxWidth()
                        .appShadow(cornerRadius = CardRadius)
                        .background(Color.White, RoundedCornerShape(CardRadius))
                        .padding(horizontal = Spacing.sm, vertical = Spacing.sm),
                ) {
                    activeReminders.forEach { reminder ->
                        ReminderListItem(
                            data = reminder.copy(isCompleted = checkedIds.contains(reminder.id)),
                            onCheckedChange = { checked ->
                                checkedIds = if (checked) checkedIds + reminder.id
                                else checkedIds - reminder.id
                            },
                            onMoreClick = { onReminderMoreClick(reminder.id) },
                        )
                    }
                }

                // ── Completed card (y 331): title + rows 15dp apart, washed out with a
                // white @ 75% "Fade" layer that covers the whole card.
                if (selectedFilter == "All" || selectedFilter == "Completed") {
                    Spacer(Modifier.height(Spacing.lg))
                    Column(
                        modifier = Modifier
                            .padding(horizontal = ScreenMargin)
                            .fillMaxWidth()
                            .appShadow(cornerRadius = CardRadius)
                            .background(Color.White, RoundedCornerShape(CardRadius))
                            .drawWithContent {
                                drawContent()
                                drawRoundRect(
                                    color = Color(0xBFFFFFFF),
                                    cornerRadius = CornerRadius(CardRadius.toPx()),
                                )
                            }
                            .padding(horizontal = Spacing.sm, vertical = Spacing.sm),
                    ) {
                        Text(
                            text = "Completed & Won’t Do",
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            modifier = Modifier.padding(start = Spacing.sm, top = Spacing.sm, bottom = Spacing.xs),
                        )
                        completedReminders.forEach { reminder ->
                            ReminderListItem(
                                data = reminder.copy(isCompleted = false),
                                onCheckedChange = {},
                                onMoreClick = { onReminderMoreClick(reminder.id) },
                                showMoreButton = false,
                                dueColor = CompletedDueColor,
                                minHeight = 44.dp,
                            )
                        }
                    }
                }

                // Clear the floating "+" button and the nav bar
                Spacer(Modifier.height(140.dp))
            }
        }

        // ── FAB: 50 × 50 at x 336, y 721 (16dp from the right edge)
        EScheduleFab(
            onClick = onAddReminder,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = Spacing.lg, bottom = 104.dp),
        )
    }
}
