package com.eschedule.app.ui.home

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eschedule.app.theme.AppTheme
import com.eschedule.app.data.model.ReminderStatus
import com.eschedule.app.theme.Spacing
import com.eschedule.app.ui.HomeUiState
import com.eschedule.app.ui.components.AnimatedReminderRow
import com.eschedule.app.ui.components.CardRadius
import com.eschedule.app.ui.components.ClassCard
import com.eschedule.app.ui.components.EmptyAction
import com.eschedule.app.ui.components.EmptyArt
import com.eschedule.app.ui.components.EmptyCard
import com.eschedule.app.ui.components.EmptyState
import com.eschedule.app.ui.components.LocalAnimateRowEnter
import com.eschedule.app.ui.components.PageTitle
import com.eschedule.app.ui.components.RowSide
import com.eschedule.app.ui.components.ScreenMargin
import com.eschedule.app.ui.components.SectionHeader
import com.eschedule.app.ui.components.appShadow

@Composable
fun HomeScreen(
    state: HomeUiState,
    onClassClick: (Long) -> Unit,
    onReminderMoreClick: (Long) -> Unit,
    onReminderStatusChange: (Long, ReminderStatus) -> Unit,
    onSeeAllReminders: () -> Unit,
    onAddClass: () -> Unit,
    onScan: () -> Unit,
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
        // Outer Column: title is fixed (never scrolls), content scrolls below it
        Column(modifier = modifier.fillMaxSize()) {

            PageTitle(title = "Dashboard")

            if (!state.loaded) return@Column

            if (state.isBlank) {
                EmptyState(
                    art = EmptyArt.Day,
                    title = "It looks empty here",
                    message = "Add your classes and your day will show up on this page.",
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(bottom = 96.dp),
                    actions = {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            EmptyAction("Add a class", onClick = onAddClass)
                            EmptyAction("Scan schedule", onClick = onScan, primary = false)
                        }
                    },
                )
                return@Column
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = ScreenMargin),
            ) {
                // ── Current class
                SectionHeader(title = "Current Class", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(Spacing.sm))
                Column(modifier = Modifier.animateContentSize()) {
                    val current = state.currentClass
                    if (current != null) {
                        ClassCard(data = current, onClick = { onClassClick(current.id) })
                    } else {
                        EmptyCard(
                            title = "No class right now",
                            message = if (state.hasAnyClasses) "Enjoy the break — nothing is in session."
                            else "Add your classes to see what's on now.",
                            art = EmptyArt.Break,
                        )
                    }
                }

                Spacer(Modifier.height(Spacing.lg))

                // ── Upcoming classes (the rest of today only)
                SectionHeader(title = "Upcoming Classes")
                Spacer(Modifier.height(Spacing.sm))
                Column(
                    modifier = Modifier.animateContentSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (state.upcomingClasses.isEmpty()) {
                        EmptyCard(
                            title = "No more classes today",
                            message = if (state.hasAnyClasses) "You're done for the day. Time to relax!"
                            else "Your next classes will show up here.",
                            art = EmptyArt.Break,
                        )
                    } else {
                        state.upcomingClasses.forEach { cls ->
                            ClassCard(data = cls, onClick = { onClassClick(cls.id) })
                        }
                    }
                }

                Spacer(Modifier.height(Spacing.lg))

                // ── Reminders (nearest first)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionHeader(title = "Reminders", modifier = Modifier.weight(1f))
                    if (state.moreReminders > 0) {
                        Text(
                            text = "See all",
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AppTheme.colors.title,
                            modifier = Modifier
                                .heightIn(min = 32.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = onSeeAllReminders,
                                )
                                .padding(vertical = 6.dp),
                        )
                    }
                }
                Spacer(Modifier.height(Spacing.sm))
                Column(modifier = Modifier.animateContentSize()) {
                    if (state.reminders.isEmpty()) {
                        EmptyCard(
                            title = "You're all caught up",
                            message = "No pending reminders. Nice work!",
                            art = EmptyArt.Reminders,
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .appShadow(cornerRadius = CardRadius)
                                .background(AppTheme.colors.surface, RoundedCornerShape(CardRadius))
                                .clip(RoundedCornerShape(CardRadius))
                                .padding(horizontal = Spacing.sm, vertical = Spacing.sm),
                        ) {
                            state.reminders.forEach { reminder ->
                                key(reminder.id) {
                                    AnimatedReminderRow(
                                        data = reminder,
                                        enterSide = RowSide.Left,
                                        exitSide = RowSide.Left,
                                        onCommit = { status -> onReminderStatusChange(reminder.id, status) },
                                        onMoreClick = { onReminderMoreClick(reminder.id) },
                                    )
                                }
                            }
                        }
                    }
                }

                // Clear the floating nav bar
                Spacer(Modifier.height(140.dp))
            }
        }
    }
}
