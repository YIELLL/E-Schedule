package com.eschedule.app.ui.scan

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eschedule.app.theme.AppTheme
import com.eschedule.app.R
import com.eschedule.app.data.ScanDay
import com.eschedule.app.data.ScannedClass
import com.eschedule.app.theme.Spacing
import com.eschedule.app.ui.components.CardRadius
import com.eschedule.app.ui.components.EmptyArt
import com.eschedule.app.ui.components.EmptyState
import com.eschedule.app.ui.components.FigmaTextMuted
import com.eschedule.app.ui.components.ScreenMargin
import com.eschedule.app.ui.components.SheetSaveButton
import com.eschedule.app.ui.components.appShadow

/**
 * Review Scan (Figma "Review Scan"): the classes recognised from a photo, grouped by day.
 * The user can drop wrong rows, then Save (check-in with the backend) or close (discard).
 *
 * Stateless: [items] comes from `ScheduleScanRepository.scan`, and every action is a callback,
 * so wiring a real backend only touches the caller.
 */
@Composable
fun ReviewScanScreen(
    items: List<ScannedClass>,
    isSaving: Boolean,
    onClose: () -> Unit,
    onSave: () -> Unit,
    onMore: (ScannedClass) -> Unit,
    modifier: Modifier = Modifier,
) {
    val grouped = remember(items) {
        ScanDay.entries.mapNotNull { day ->
            items.filter { it.day == day }.takeIf { it.isNotEmpty() }?.let { day to it }
        }
    }
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colors.background),
    ) {
        // ── Header: title · close · save ─────────────────────────────────────
        Row(
            modifier = Modifier
                .statusBarsPadding()
                .padding(start = ScreenMargin, end = ScreenMargin, top = Spacing.lg, bottom = Spacing.md)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Review Scan",
                fontSize = 20.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.Bold,
                color = AppTheme.colors.title,
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(AppTheme.colors.controlFill, CircleShape)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClose,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_review_close),
                    contentDescription = "Discard scan",
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(45f),
                )
            }
            Spacer(Modifier.width(Spacing.sm))
            SheetSaveButton(onClick = { if (!isSaving) onSave() })
        }

        if (items.isEmpty()) {
            EmptyState(
                art = EmptyArt.Inbox,
                title = "Nothing found",
                message = "We couldn't find any classes in this photo. Close this screen and try scanning again.",
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = ScreenMargin,
                    end = ScreenMargin,
                    bottom = Spacing.lg + navBottom,
                ),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                grouped.forEachIndexed { index, (day, classes) ->
                    item(key = "label_${day.name}") {
                        Text(
                            text = day.label,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = FigmaTextMuted,
                            modifier = Modifier.padding(top = if (index == 0) 0.dp else Spacing.md),
                        )
                    }
                    item(key = "card_${day.name}") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .appShadow(cornerRadius = CardRadius)
                                .background(AppTheme.colors.surface, RoundedCornerShape(CardRadius)),
                        ) {
                            classes.forEachIndexed { i, cls ->
                                if (i > 0) {
                                    Box(
                                        Modifier
                                            .padding(horizontal = Spacing.md)
                                            .fillMaxWidth()
                                            .height(1.dp)
                                            .background(AppTheme.colors.hairline),
                                    )
                                }
                                ScannedClassRow(cls = cls, onMore = { onMore(cls) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScannedClassRow(
    cls: ScannedClass,
    onMore: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .padding(start = Spacing.md, top = 12.dp, bottom = 12.dp, end = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = cls.title,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppTheme.colors.text,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = cls.timeRange,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                color = FigmaTextMuted,
            )
            Text(
                text = cls.location.ifBlank { "No room set" },
                fontSize = 11.sp,
                lineHeight = 16.sp,
                color = FigmaTextMuted,
            )
        }
        // The three dots open the same details sheet used everywhere else (view / edit / delete).
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onMore,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_review_more),
                contentDescription = "More options",
                modifier = Modifier.size(20.dp),
                colorFilter = AppTheme.colors.blackIconFilter,
            )
        }
    }
}
