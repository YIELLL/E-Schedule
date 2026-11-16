package com.example.eschedule.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.eschedule.R
import com.example.eschedule.theme.Spacing
import com.example.eschedule.theme.UepBlue

// ─────────────────────────────────────────────────────────────────────────────
// Figma design tokens shared by the components below.
//   Poppins is used everywhere except the bottom nav (Inter SemiBold).
//   "normal" line height in Figma for Poppins = 1.5 × font size.
// ─────────────────────────────────────────────────────────────────────────────

/** Horizontal screen margin (24dp). */
val ScreenMargin = Spacing.screen

/** Black @ 50% — secondary text colour in every frame. */
val FigmaTextMuted = Color(0x80000000)

/** Black @ 25% — unchecked checkbox border. */
private val CheckboxBorder = Color(0x40000000)

/** Card corner radius used by class cards and reminder cards. */
val CardRadius = 12.dp

// ─────────────────────────────────────────────────────────────────────────────
// ClassCardData + ClassCard
//   Card: full width, ≥ 72dp, radius 12, 16dp horizontal / 12dp vertical padding.
//   Accent bar 4dp wide. Title: Poppins SemiBold 12 / 16, black.
//   Time + room: Poppins Regular 12 / 16, black 50%.
// ─────────────────────────────────────────────────────────────────────────────

data class ClassCardData(
    val id: String,
    val title: String,
    val timeRange: String,
    val location: String,
)

@Composable
fun ClassCard(
    data: ClassCardData,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .appShadow(cornerRadius = CardRadius)
            .background(Color.White, RoundedCornerShape(CardRadius))
            .clip(RoundedCornerShape(CardRadius))
            .then(
                if (onClick != null) Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                ) else Modifier
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .padding(horizontal = Spacing.md, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(UepBlue, RoundedCornerShape(2.dp)),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = data.title,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black,
                )
                Text(
                    text = data.timeRange,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Normal,
                    color = FigmaTextMuted,
                )
                Text(
                    text = data.location,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Normal,
                    color = FigmaTextMuted,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ReminderItemData + ReminderListItem
//   Row (≥ 48dp tall for touch): 40dp checkbox hit-area (18dp box) · title (Poppins 12, black) ·
//   time (Poppins 11, black 50%) · [40dp more-button hit-area with the 20dp dots icon]
// ─────────────────────────────────────────────────────────────────────────────

data class ReminderItemData(
    val id: String,
    val title: String,
    val dueLabel: String,
    val isCompleted: Boolean = false,
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReminderListItem(
    data: ReminderItemData,
    onCheckedChange: (Boolean) -> Unit,
    onMoreClick: () -> Unit,
    onEditClick: (() -> Unit)? = null,
    onDeleteClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    showMoreButton: Boolean = true,
    dueColor: Color = FigmaTextMuted,
    minHeight: Dp = Spacing.touchTarget,
) {
    var showMenu by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
                onLongClick = {
                    if (onEditClick != null || onDeleteClick != null) showMenu = true
                },
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = minHeight),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FigmaCheckbox(
                checked = data.isCompleted,
                onCheckedChange = onCheckedChange,
            )
            Text(
                text = data.title,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Normal,
                color = Color.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 2.dp),
            )
            Spacer(Modifier.width(Spacing.sm))
            Text(
                text = data.dueLabel,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Normal,
                color = dueColor,
                maxLines = 1,
            )
            if (showMoreButton) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onMoreClick,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_more_dots),
                        contentDescription = "More options",
                        modifier = Modifier.size(20.dp),
                    )
                }
            } else {
                Spacer(Modifier.width(Spacing.sm))
            }
        }

        // Long-press context menu
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
        ) {
            if (onEditClick != null) {
                DropdownMenuItem(
                    text = { Text("Edit", fontSize = 14.sp) },
                    leadingIcon = { Icon(AppIcons.Edit, null, Modifier.size(18.dp)) },
                    onClick = { onEditClick(); showMenu = false },
                )
            }
            if (onDeleteClick != null) {
                DropdownMenuItem(
                    text = { Text("Delete", fontSize = 14.sp, color = Color(0xFFE53935)) },
                    leadingIcon = { Icon(AppIcons.Delete, null, Modifier.size(18.dp), tint = Color(0xFFE53935)) },
                    onClick = { onDeleteClick(); showMenu = false },
                )
            }
        }
    }
}

/** 18dp square checkbox (1.5dp black-25% border, 3dp radius) inside a 40dp touch target. */
@Composable
fun FigmaCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(3.dp)
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onCheckedChange(!checked) },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .then(
                    if (checked) Modifier.background(UepBlue, shape)
                    else Modifier.border(1.5.dp, CheckboxBorder, shape)
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) {
                Canvas(Modifier.size(18.dp)) {
                    val w = 1.8.dp.toPx()
                    drawLine(Color.White, Offset(size.width * 0.24f, size.height * 0.52f), Offset(size.width * 0.43f, size.height * 0.70f), w, StrokeCap.Round)
                    drawLine(Color.White, Offset(size.width * 0.43f, size.height * 0.70f), Offset(size.width * 0.76f, size.height * 0.31f), w, StrokeCap.Round)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SectionHeader — Poppins 12 / 18, black 50% (Bold, or SemiBold for "Current Class")
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight = FontWeight.Bold,
) {
    Text(
        text = title,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        fontWeight = fontWeight,
        color = FigmaTextMuted,
        modifier = modifier,
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// PageTitle — Poppins Bold 20 / 30, UEP blue.
//   Sits 24dp under the status bar (inset-aware, so it works with any cutout / status-bar height)
//   and leaves 16dp before the content.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PageTitle(
    title: String,
    modifier: Modifier = Modifier,
    bottomGap: Dp = Spacing.md,
) {
    Text(
        text = title,
        fontSize = 20.sp,
        lineHeight = 30.sp,
        fontWeight = FontWeight.Bold,
        color = UepBlue,
        modifier = modifier
            .statusBarsPadding()
            .padding(start = ScreenMargin, end = ScreenMargin, top = Spacing.lg, bottom = bottomGap),
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// FilterChip — Reminders filter pill: 36dp tall, 1dp black-10% border,
// selected = UEP blue fill + white label, otherwise transparent + black-50% label.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun FilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(25.dp)
    Box(
        modifier = modifier
            .height(36.dp)
            .background(if (selected) UepBlue else Color.Transparent, shape)
            .border(1.dp, Color(0x1A000000), shape)
            .clip(shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.Normal,
            color = if (selected) Color.White else FigmaTextMuted,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// EScheduleFab — 56dp round "+" button (Figma "Add Button" asset, scaled up from 50dp)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun EScheduleFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Image(
        painter = painterResource(R.drawable.ic_fab_add),
        contentDescription = "Add",
        modifier = modifier
            .size(56.dp)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
    )
}
