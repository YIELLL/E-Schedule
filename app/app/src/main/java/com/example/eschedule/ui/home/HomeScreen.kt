package com.example.eschedule.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.eschedule.theme.Spacing
import com.example.eschedule.ui.components.CardRadius
import com.example.eschedule.ui.components.ClassCard
import com.example.eschedule.ui.components.ClassCardData
import com.example.eschedule.ui.components.PageTitle
import com.example.eschedule.ui.components.ReminderItemData
import com.example.eschedule.ui.components.ReminderListItem
import com.example.eschedule.ui.components.ScreenMargin
import com.example.eschedule.ui.components.SectionHeader
import com.example.eschedule.ui.components.appShadow

@Composable
fun HomeScreen(
    onClassClick: (String) -> Unit,
    onReminderMoreClick: (String) -> Unit,
    onReminderEditClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentClass = remember {
        ClassCardData(
            id = "it102",
            title = "IT102 - Mobile Application Development",
            timeRange = "8:00 AM - 9:00 AM",
            location = "CSFLD22",
        )
    }
    val upcomingClasses = remember {
        listOf(
            ClassCardData(
                id = "sia101",
                title = "SIA101 - System Integration and Architecture",
                timeRange = "10:00 AM - 1:00 PM",
                location = "L4",
            ),
            ClassCardData(
                id = "net102",
                title = "NET102 - Networking 2",
                timeRange = "2:00 PM - 3:00 PM",
                location = "CSFLD22",
            ),
            ClassCardData(
                id = "its101",
                title = "ITS101 - IT Service Management",
                timeRange = "3:00 PM - 5:00 PM",
                location = "ACAD LAB 2",
            ),
        )
    }
    val reminders = remember {
        listOf(
            ReminderItemData(id = "r1", title = "Pag laba",           dueLabel = "12:51 PM"),
            ReminderItemData(id = "r2", title = "Do app dev project", dueLabel = "Tomorrow"),
            ReminderItemData(id = "r3", title = "Make journal",        dueLabel = "Sep 21"),
            ReminderItemData(id = "r4", title = "Assassinate Jons",   dueLabel = "Dec 17"),
        )
    }
    var checkedIds by remember { mutableStateOf(setOf<String>()) }

    // Outer Column: title is fixed (never scrolls), content scrolls below it
    Column(modifier = modifier.fillMaxSize()) {

        PageTitle(title = "Dashboard")

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ScreenMargin),
        ) {
            // ── Current class
            SectionHeader(title = "Current Class", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(Spacing.sm))
            ClassCard(
                data = currentClass,
                onClick = { onClassClick(currentClass.id) },
            )

            Spacer(Modifier.height(Spacing.lg))

            // ── Upcoming classes — a plain list of cards
            SectionHeader(title = "Upcoming Classes")
            Spacer(Modifier.height(Spacing.sm))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                upcomingClasses.forEach { cls ->
                    ClassCard(
                        data = cls,
                        onClick = { onClassClick(cls.id) },
                    )
                }
            }

            Spacer(Modifier.height(Spacing.lg))

            // ── Reminders
            SectionHeader(title = "Reminders")
            Spacer(Modifier.height(Spacing.sm))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .appShadow(cornerRadius = CardRadius)
                    .background(Color.White, RoundedCornerShape(CardRadius))
                    .padding(horizontal = Spacing.sm, vertical = Spacing.sm),
            ) {
                reminders.forEach { reminder ->
                    ReminderListItem(
                        data = reminder.copy(isCompleted = checkedIds.contains(reminder.id)),
                        onCheckedChange = { checked ->
                            checkedIds = if (checked) checkedIds + reminder.id
                            else checkedIds - reminder.id
                        },
                        onMoreClick = { onReminderMoreClick(reminder.id) },
                        onEditClick = { onReminderEditClick(reminder.id) },
                        onDeleteClick = { /* TODO: hook to data layer */ },
                        showMoreButton = false,
                    )
                }
            }

            // Clear the floating nav bar
            Spacer(Modifier.height(140.dp))
        }
    }
}
