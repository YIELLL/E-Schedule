package com.example.eschedule.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.eschedule.theme.ClassCyan
import com.example.eschedule.theme.Spacing
import com.example.eschedule.theme.UepBlue
import com.example.eschedule.theme.UepYellow

// ─────────────────────────────────────────────────────────────────────────────
// Empty-state graphics. Drawn in code (no image files) on a 120 × 120 grid so they scale
// crisply to any size and always use the app palette.
// ─────────────────────────────────────────────────────────────────────────────

enum class EmptyArt {
    /** A calendar page — no classes. */
    Calendar,

    /** A big check mark with confetti — nothing left to do. */
    Reminders,

    /** A blank clipboard — nothing here yet. */
    Inbox,
}

private val ArtTint = UepBlue.copy(alpha = 0.08f)

@Composable
fun EmptyIllustration(
    art: EmptyArt,
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
) {
    Canvas(modifier = modifier.size(size)) {
        scale(this.size.minDimension / 120f, pivot = Offset.Zero) {
            when (art) {
                EmptyArt.Calendar -> drawCalendarArt()
                EmptyArt.Reminders -> drawRemindersArt()
                EmptyArt.Inbox -> drawInboxArt()
            }
        }
    }
}

private fun DrawScope.drawSparkle(center: Offset, radius: Float, color: Color) {
    val k = radius * 0.28f
    val path = Path().apply {
        moveTo(center.x, center.y - radius)
        quadraticTo(center.x + k, center.y - k, center.x + radius, center.y)
        quadraticTo(center.x + k, center.y + k, center.x, center.y + radius)
        quadraticTo(center.x - k, center.y + k, center.x - radius, center.y)
        quadraticTo(center.x - k, center.y - k, center.x, center.y - radius)
        close()
    }
    drawPath(path, color)
}

private fun DrawScope.drawCalendarArt() {
    drawCircle(ArtTint, radius = 56f, center = Offset(60f, 62f))

    // Page
    drawRoundRect(Color.White, Offset(26f, 36f), Size(68f, 58f), CornerRadius(9f))
    drawRoundRect(UepBlue.copy(alpha = 0.22f), Offset(26f, 36f), Size(68f, 58f), CornerRadius(9f), style = Stroke(2f))

    // Header band (rounded top, square bottom)
    drawRoundRect(UepBlue, Offset(26f, 36f), Size(68f, 18f), CornerRadius(9f))
    drawRect(UepBlue, Offset(26f, 46f), Size(68f, 8f))

    // Binder rings
    drawRoundRect(UepYellow, Offset(41f, 28f), Size(7f, 15f), CornerRadius(3.5f))
    drawRoundRect(UepYellow, Offset(72f, 28f), Size(7f, 15f), CornerRadius(3.5f))

    // Empty day slots, one highlighted
    for (row in 0..1) {
        for (col in 0..2) {
            drawCircle(UepBlue.copy(alpha = 0.14f), 4.5f, Offset(42f + col * 18f, 67f + row * 14f))
        }
    }
    drawCircle(ClassCyan, 4.5f, Offset(60f, 67f))

    drawSparkle(Offset(98f, 30f), 7f, UepYellow)
    drawSparkle(Offset(20f, 52f), 4.5f, ClassCyan)
}

private fun DrawScope.drawRemindersArt() {
    val c = Offset(60f, 60f)
    drawCircle(ArtTint, radius = 56f, center = c)
    drawCircle(UepBlue.copy(alpha = 0.12f), radius = 38f, center = c)
    drawCircle(UepBlue, radius = 29f, center = c)

    val check = Path().apply {
        moveTo(47f, 61f)
        lineTo(56f, 70f)
        lineTo(75f, 50f)
    }
    drawPath(check, Color.White, style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round))

    // Confetti
    drawCircle(UepYellow, 4f, Offset(22f, 34f))
    drawCircle(ClassCyan, 3.5f, Offset(98f, 44f))
    drawCircle(UepBlue.copy(alpha = 0.35f), 3f, Offset(28f, 92f))
    drawCircle(UepYellow, 3f, Offset(94f, 92f))
    drawSparkle(Offset(96f, 24f), 6f, UepYellow)
    drawSparkle(Offset(20f, 70f), 4f, ClassCyan)
}

private fun DrawScope.drawInboxArt() {
    drawCircle(ArtTint, radius = 56f, center = Offset(60f, 62f))

    // Board
    drawRoundRect(Color.White, Offset(32f, 30f), Size(56f, 68f), CornerRadius(9f))
    drawRoundRect(UepBlue.copy(alpha = 0.22f), Offset(32f, 30f), Size(56f, 68f), CornerRadius(9f), style = Stroke(2f))

    // Clip
    drawRoundRect(UepBlue, Offset(46f, 24f), Size(28f, 14f), CornerRadius(7f))
    drawCircle(UepYellow, 3f, Offset(60f, 31f))

    // Empty lines
    for (i in 0..2) {
        val width = if (i == 2) 22f else 36f
        drawRoundRect(UepBlue.copy(alpha = 0.14f), Offset(42f, 52f + i * 13f), Size(width, 5f), CornerRadius(2.5f))
    }

    drawSparkle(Offset(94f, 40f), 6f, UepYellow)
    drawSparkle(Offset(24f, 84f), 4.5f, ClassCyan)
}

// ─────────────────────────────────────────────────────────────────────────────
// EmptyState — full-area placeholder: big graphic, title, message, optional action buttons.
// Fades and rises in when it appears. Give it the space to centre in (e.g. fillMaxSize()).
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun EmptyState(
    art: EmptyArt,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actions: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val visible = remember { MutableTransitionState(false).apply { targetState = true } }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        AnimatedVisibility(
            visibleState = visible,
            enter = fadeIn(tween(450)) + slideInVertically(tween(450)) { it / 10 },
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = Spacing.xl),
            ) {
                EmptyIllustration(art, size = 140.dp)
                Spacer(Modifier.height(Spacing.md))
                Text(
                    text = title,
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xBF000000),
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = message,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = FigmaTextMuted,
                    textAlign = TextAlign.Center,
                )
                if (actions != null) {
                    Spacer(Modifier.height(Spacing.lg))
                    actions()
                }
            }
        }
    }
}

/** Pill button used under an [EmptyState]: filled (primary) or outlined. */
@Composable
fun EmptyAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = true,
) {
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier = modifier
            .heightIn(min = 44.dp)
            .clip(shape)
            .then(
                if (primary) Modifier.background(UepBlue, shape)
                else Modifier.border(1.5.dp, UepBlue.copy(alpha = 0.5f), shape)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = Spacing.lg),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (primary) Color.White else UepBlue,
        )
    }
}

/** Two [EmptyAction]s side by side. */
@Composable
fun EmptyActionRow(content: @Composable () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        content()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// EmptyCard — compact in-list placeholder: small graphic + title + one line of help.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun EmptyCard(
    title: String,
    message: String,
    art: EmptyArt,
    modifier: Modifier = Modifier,
) {
    val visible = remember { MutableTransitionState(false).apply { targetState = true } }
    AnimatedVisibility(
        visibleState = visible,
        enter = fadeIn(tween(350)),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .appShadow(cornerRadius = CardRadius)
                .background(Color.White, RoundedCornerShape(CardRadius))
                .padding(horizontal = Spacing.md, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            EmptyIllustration(art, size = 64.dp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xBF000000),
                )
                Text(
                    text = message,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    color = FigmaTextMuted,
                )
            }
        }
    }
}
