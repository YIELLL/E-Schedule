package com.example.eschedule.ui.reminders

import androidx.compose.animation.animateContentSize
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.eschedule.data.model.ReminderStatus
import com.example.eschedule.theme.Spacing
import com.example.eschedule.ui.ReminderFilter
import com.example.eschedule.ui.RemindersUiState
import com.example.eschedule.ui.components.AnimatedReminderRow
import com.example.eschedule.ui.components.CardRadius
import com.example.eschedule.ui.components.EScheduleFab
import com.example.eschedule.ui.components.EmptyArt
import com.example.eschedule.ui.components.EmptyCard
import com.example.eschedule.ui.components.EmptyState
import com.example.eschedule.ui.components.FilterChip
import com.example.eschedule.ui.components.LocalAnimateRowEnter
import com.example.eschedule.ui.components.PageTitle
import com.example.eschedule.ui.components.RowSide
import com.example.eschedule.ui.components.ScreenMargin
import com.example.eschedule.ui.components.appShadow

/** Figma: completed due-dates are UEP blue @ 60%. */
private val CompletedDueColor = Color(0x9917376E)

@Composable
fun RemindersScreen(
    state: RemindersUiState,
    onFilterChange: (ReminderFilter) -> Unit,
    onStatusChange: (Long, ReminderStatus) -> Unit,
    onReminderMoreClick: (Long) -> Unit,
    onAddReminder: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Rows only animate in once the first data has been shown (not on every tab visit).
    var settled by remember { mutableStateOf(false) }
    LaunchedEffect(state.loaded) {
        if (state.loaded) {
            withFrameNanos { }
            settled = true
        }
    }

    CompositionLocalProvider(LocalAnimateRowEnter provides settled) {
        Box(modifier = modifier.fillMaxSize()) {
            // Outer Column: title fixed, content scrolls
            Column(modifier = Modifier.fillMaxSize()) {

                PageTitle(title = "Reminders")

                if (state.loaded && !state.hasAny) {
                    // Nothing at all yet: a friendly full-page placeholder instead of an empty list.
                    EmptyState(
                        art = EmptyArt.Inbox,
                        title = "No reminders yet",
                        message = "Tap + to add your first one — long-press a checkbox to mark it “won’t do”.",
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(bottom = 96.dp),
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                    ) {
                        // Filter chips: 36dp tall, 8dp apart
                        Row(
                            modifier = Modifier
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = ScreenMargin),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        ) {
                            ReminderFilter.entries.forEach { filter ->
                                FilterChip(
                                    label = filter.label,
                                    selected = state.filter == filter,
                                    onClick = { onFilterChange(filter) },
                                )
                            }
                        }

                        Spacer(Modifier.height(Spacing.md))

                        if (state.loaded) {
                            // ── Open reminders
                            if (state.showActive) {
                                Column(
                                    modifier = Modifier
                                        .padding(horizontal = ScreenMargin)
                                        .animateContentSize(),
                                ) {
                                    if (state.active.isEmpty()) {
                                        ActiveEmpty(state.filter)
                                    } else {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .appShadow(cornerRadius = CardRadius)
                                                .background(Color.White, RoundedCornerShape(CardRadius))
                                                .clip(RoundedCornerShape(CardRadius))
                                                .padding(horizontal = Spacing.sm, vertical = Spacing.sm),
                                        ) {
                                            state.active.forEach { reminder ->
                                                key(reminder.id) {
                                                    AnimatedReminderRow(
                                                        data = reminder,
                                                        enterSide = RowSide.Left,
                                                        exitSide = RowSide.Left,
                                                        onCommit = { status -> onStatusChange(reminder.id, status) },
                                                        onMoreClick = { onReminderMoreClick(reminder.id) },
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // ── Completed & won't do: washed out with a white @ 75% layer; tap a box to reopen.
                            if (state.showCompleted) {
                                Column(
                                    modifier = Modifier
                                        .padding(horizontal = ScreenMargin)
                                        .animateContentSize(),
                                ) {
                                    if (state.completed.isEmpty()) {
                                        if (state.filter == ReminderFilter.Completed) {
                                            EmptyCard(
                                                title = "Nothing completed yet",
                                                message = "Reminders you finish or drop will be kept here.",
                                                art = EmptyArt.Inbox,
                                            )
                                        }
                                    } else {
                                        if (state.showActive) Spacer(Modifier.height(Spacing.lg))
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .appShadow(cornerRadius = CardRadius)
                                                .background(Color.White, RoundedCornerShape(CardRadius))
                                                .clip(RoundedCornerShape(CardRadius))
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
                                            state.completed.forEach { reminder ->
                                                key(reminder.id) {
                                                    AnimatedReminderRow(
                                                        data = reminder,
                                                        enterSide = RowSide.Right,
                                                        exitSide = RowSide.Right,
                                                        onCommit = { status -> onStatusChange(reminder.id, status) },
                                                        onMoreClick = { onReminderMoreClick(reminder.id) },
                                                        showMoreButton = false,
                                                        dueColor = CompletedDueColor,
                                                        minHeight = 44.dp,
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Clear the floating "+" button and the nav bar
                        Spacer(Modifier.height(140.dp))
                    }
                }
            }

            // ── FAB: 56dp, 24dp from the right edge, 16dp above the nav pill
            EScheduleFab(
                onClick = onAddReminder,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = Spacing.lg, bottom = 104.dp),
            )
        }
    }
}

/** Compact placeholder for an empty list of open reminders, worded for the active filter. */
@Composable
private fun ActiveEmpty(filter: ReminderFilter) {
    when (filter) {
        ReminderFilter.Today -> EmptyCard("Nothing due today", "Enjoy the free time.", EmptyArt.Reminders)
        ReminderFilter.Week -> EmptyCard("Nothing due this week", "You're all clear for now.", EmptyArt.Reminders)
        ReminderFilter.Month -> EmptyCard("Nothing due this month", "You're all clear for now.", EmptyArt.Reminders)
        else -> EmptyCard("You're all caught up", "No pending reminders. Nice work!", EmptyArt.Reminders)
    }
}
