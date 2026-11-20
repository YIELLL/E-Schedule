package com.eschedule.app.ui.components

import androidx.compose.foundation.Canvas
import kotlinx.coroutines.flow.first
import com.eschedule.app.theme.AppTheme
import com.eschedule.app.data.model.ReminderStatus
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.expandVertically
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eschedule.app.R
import com.eschedule.app.theme.Spacing

// ─────────────────────────────────────────────────────────────────────────────
// Figma design tokens shared by the components below.
//   Poppins is used everywhere except the bottom nav (Inter SemiBold).
//   "normal" line height in Figma for Poppins = 1.5 × font size.
// ─────────────────────────────────────────────────────────────────────────────

/** Horizontal screen margin (24dp). */
val ScreenMargin = Spacing.screen

/** Black @ 50% — secondary text colour in every frame. */
val FigmaTextMuted: Color
    @Composable get() = AppTheme.colors.muted

/** Card corner radius used by class cards and reminder cards. */
val CardRadius = 12.dp

// ─────────────────────────────────────────────────────────────────────────────
// ClassCardData + ClassCard
//   Card: full width, ≥ 72dp, radius 12, 16dp horizontal / 12dp vertical padding.
//   Accent bar 4dp wide. Title: Poppins SemiBold 12 / 16, black.
//   Time + room: Poppins Regular 12 / 16, black 50%.
// ─────────────────────────────────────────────────────────────────────────────

data class ClassCardData(
    val id: Long,
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
            .background(AppTheme.colors.surface, RoundedCornerShape(CardRadius))
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
                    .background(AppTheme.colors.primary, RoundedCornerShape(2.dp)),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = data.title,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppTheme.colors.text,
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
//   time (Poppins 11, black 50%) · 40dp more-button hit-area with the 20dp dots icon.
//   Checkbox: tap = done / reopen, long-press = "won't do" (dash). The three-dot button is the
//   only entry point to the reminder's details.
// ─────────────────────────────────────────────────────────────────────────────

data class ReminderItemData(
    val id: Long,
    val title: String,
    val dueLabel: String,
    val status: ReminderStatus = ReminderStatus.Open,
    /** Past its due date — the due label turns red. */
    val isOverdue: Boolean = false,
)

private val OverdueColor = Color(0xFFD93025)

@Composable
fun ReminderListItem(
    data: ReminderItemData,
    onToggle: () -> Unit,
    onWontDo: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier,
    showMoreButton: Boolean = true,
    dueColor: Color = FigmaTextMuted,
    minHeight: Dp = Spacing.touchTarget,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FigmaCheckbox(
            status = data.status,
            onClick = onToggle,
            onLongClick = onWontDo,
        )
        Text(
            text = data.title,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.Normal,
            color = if (data.status == ReminderStatus.Open) AppTheme.colors.text else FigmaTextMuted,
            textDecoration = if (data.status == ReminderStatus.WontDo) TextDecoration.LineThrough else null,
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
            color = if (data.isOverdue) OverdueColor else dueColor,
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
                        colorFilter = AppTheme.colors.blackIconFilter,
                )
            }
        } else {
            Spacer(Modifier.width(Spacing.sm))
        }
    }
}

/** Screens set this to `true` once their first data has been shown, so only *later* rows animate in. */
val LocalAnimateRowEnter = compositionLocalOf { false }

/** Side a row slides towards when it leaves, or arrives from. */
enum class RowSide { Left, Right }

private const val ROW_ANIM_MS = 260

/**
 * A reminder row that animates: it slides in (and fades / expands) when it appears after the
 * screen has settled, and when the user checks it, it first shows the new state, then swipes
 * out towards [exitSide] and only then reports the change through [onCommit] — so the data
 * moves to the other list after the animation, not before.
 *
 * Tap → Done (or Open when the row is already finished); long-press on an open row → Won't do.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AnimatedReminderRow(
    data: ReminderItemData,
    enterSide: RowSide,
    exitSide: RowSide,
    onCommit: (ReminderStatus) -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier,
    showMoreButton: Boolean = true,
    dueColor: Color = FigmaTextMuted,
    minHeight: Dp = Spacing.touchTarget,
) {
    val animateEnter = LocalAnimateRowEnter.current
    val visible = remember { MutableTransitionState(!animateEnter).apply { targetState = true } }
    var pending by remember { mutableStateOf<ReminderStatus?>(null) }

    LaunchedEffect(pending) {
        val target = pending ?: return@LaunchedEffect
        visible.targetState = false
        snapshotFlow { visible.isIdle && !visible.currentState }.first { it }
        onCommit(target)
    }

    AnimatedVisibility(
        visibleState = visible,
        modifier = modifier,
        enter = slideInHorizontally(tween(ROW_ANIM_MS)) { w -> if (enterSide == RowSide.Left) -w / 3 else w / 3 } +
            fadeIn(tween(ROW_ANIM_MS)) +
            expandVertically(tween(ROW_ANIM_MS)),
        exit = slideOutHorizontally(tween(ROW_ANIM_MS)) { w -> if (exitSide == RowSide.Left) -w else w } +
            fadeOut(tween(ROW_ANIM_MS)) +
            shrinkVertically(tween(ROW_ANIM_MS, delayMillis = 120)),
    ) {
        ReminderListItem(
            data = data.copy(status = pending ?: data.status),
            onToggle = {
                if (pending == null) {
                    pending = if (data.status == ReminderStatus.Open) ReminderStatus.Done else ReminderStatus.Open
                }
            },
            onWontDo = {
                if (pending == null && data.status == ReminderStatus.Open) pending = ReminderStatus.WontDo
            },
            onMoreClick = onMoreClick,
            showMoreButton = showMoreButton,
            dueColor = dueColor,
            minHeight = minHeight,
        )
    }
}

/**
 * 18dp checkbox inside a 40dp touch target.
 *   Open   → empty box
 *   Done   → blue box with a check that draws itself in
 *   WontDo → box with a dash (the item was dropped, not done)
 * Long-press gives a haptic tick.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FigmaCheckbox(
    status: ReminderStatus,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(3.dp)
    val haptics = LocalHapticFeedback.current
    val dashColor = AppTheme.colors.muted

    val fill by animateColorAsState(
        targetValue = if (status == ReminderStatus.Done) AppTheme.colors.primary else Color.Transparent,
        animationSpec = tween(180),
        label = "checkFill",
    )
    val border by animateColorAsState(
        targetValue = when (status) {
            ReminderStatus.Done -> AppTheme.colors.primary
            ReminderStatus.WontDo -> AppTheme.colors.muted
            ReminderStatus.Open -> AppTheme.colors.placeholder
        },
        animationSpec = tween(180),
        label = "checkBorder",
    )
    val checkProgress by animateFloatAsState(
        targetValue = if (status == ReminderStatus.Done) 1f else 0f,
        animationSpec = tween(200),
        label = "checkDraw",
    )
    val dashProgress by animateFloatAsState(
        targetValue = if (status == ReminderStatus.WontDo) 1f else 0f,
        animationSpec = tween(200),
        label = "dashDraw",
    )

    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier
                .size(18.dp)
                .background(fill, shape)
                .border(1.5.dp, border, shape),
        ) {
            val w = 1.8.dp.toPx()
            if (checkProgress > 0f) {
                // Two strokes: short leg then long leg, revealed by progress.
                val p1 = Offset(size.width * 0.24f, size.height * 0.52f)
                val p2 = Offset(size.width * 0.43f, size.height * 0.70f)
                val p3 = Offset(size.width * 0.76f, size.height * 0.31f)
                val firstEnd = (checkProgress * 2f).coerceAtMost(1f)
                val secondEnd = ((checkProgress - 0.5f) * 2f).coerceIn(0f, 1f)
                drawLine(Color.White, p1, lerp(p1, p2, firstEnd), w, StrokeCap.Round)
                if (secondEnd > 0f) drawLine(Color.White, p2, lerp(p2, p3, secondEnd), w, StrokeCap.Round)
            }
            if (dashProgress > 0f) {
                val cy = size.height / 2f
                val half = size.width * 0.28f * dashProgress
                drawLine(dashColor, Offset(size.width / 2f - half, cy), Offset(size.width / 2f + half, cy), w, StrokeCap.Round)
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
        color = AppTheme.colors.title,
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
    val bg by animateColorAsState(if (selected) AppTheme.colors.primary else Color.Transparent, tween(200), label = "chipBg")
    val fg by animateColorAsState(if (selected) Color.White else FigmaTextMuted, tween(200), label = "chipFg")
    Box(
        modifier = modifier
            .height(36.dp)
            .background(bg, shape)
            .border(1.dp, AppTheme.colors.hairline, shape)
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
            color = fg,
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
        painter = painterResource(if (AppTheme.colors.dark) R.drawable.ic_fab_add_dark else R.drawable.ic_fab_add),
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
