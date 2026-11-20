package com.eschedule.app.ui.components

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
import com.eschedule.app.theme.ClassCyan
import com.eschedule.app.theme.Spacing
import com.eschedule.app.theme.AppTheme
import com.eschedule.app.theme.UepYellow

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

    /** A sun over the horizon — an open day with nothing scheduled. */
    Day,

    /** A steaming cup — a break between classes. */
    Break,
}

@Composable
fun EmptyIllustration(
    art: EmptyArt,
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
) {
    // The drawing runs outside composition, so the palette is read here and passed in.
    val blue = AppTheme.colors.primary
    val surface = AppTheme.colors.surface
    Canvas(modifier = modifier.size(size)) {
        scale(this.size.minDimension / 120f, pivot = Offset.Zero) {
            when (art) {
                EmptyArt.Calendar -> drawCalendarArt(blue, surface)
                EmptyArt.Reminders -> drawRemindersArt(blue)
                EmptyArt.Inbox -> drawInboxArt(blue, surface)
                EmptyArt.Day -> drawDayArt(blue, surface)
                EmptyArt.Break -> drawBreakArt(blue, surface)
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

private fun DrawScope.drawCalendarArt(blue: Color, surface: Color) {
    drawCircle(blue.copy(alpha = 0.08f), radius = 56f, center = Offset(60f, 62f))

    // Page
    drawRoundRect(surface, Offset(26f, 36f), Size(68f, 58f), CornerRadius(9f))
    drawRoundRect(blue.copy(alpha = 0.22f), Offset(26f, 36f), Size(68f, 58f), CornerRadius(9f), style = Stroke(2f))

    // Header band (rounded top, square bottom)
    drawRoundRect(blue, Offset(26f, 36f), Size(68f, 18f), CornerRadius(9f))
    drawRect(blue, Offset(26f, 46f), Size(68f, 8f))

    // Binder rings
    drawRoundRect(UepYellow, Offset(41f, 28f), Size(7f, 15f), CornerRadius(3.5f))
    drawRoundRect(UepYellow, Offset(72f, 28f), Size(7f, 15f), CornerRadius(3.5f))

    // Empty day slots, one highlighted
    for (row in 0..1) {
        for (col in 0..2) {
            drawCircle(blue.copy(alpha = 0.14f), 4.5f, Offset(42f + col * 18f, 67f + row * 14f))
        }
    }
    drawCircle(ClassCyan, 4.5f, Offset(60f, 67f))

    drawSparkle(Offset(98f, 30f), 7f, UepYellow)
    drawSparkle(Offset(20f, 52f), 4.5f, ClassCyan)
}

private fun DrawScope.drawRemindersArt(blue: Color) {
    val c = Offset(60f, 60f)
    drawCircle(blue.copy(alpha = 0.08f), radius = 56f, center = c)
    drawCircle(blue.copy(alpha = 0.12f), radius = 38f, center = c)
    drawCircle(blue, radius = 29f, center = c)

    val check = Path().apply {
        moveTo(47f, 61f)
        lineTo(56f, 70f)
        lineTo(75f, 50f)
    }
    drawPath(check, Color.White, style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round))

    // Confetti
    drawCircle(UepYellow, 4f, Offset(22f, 34f))
    drawCircle(ClassCyan, 3.5f, Offset(98f, 44f))
    drawCircle(blue.copy(alpha = 0.35f), 3f, Offset(28f, 92f))
    drawCircle(UepYellow, 3f, Offset(94f, 92f))
    drawSparkle(Offset(96f, 24f), 6f, UepYellow)
    drawSparkle(Offset(20f, 70f), 4f, ClassCyan)
}

private fun DrawScope.drawInboxArt(blue: Color, surface: Color) {
    drawCircle(blue.copy(alpha = 0.08f), radius = 56f, center = Offset(60f, 62f))

    // Board
    drawRoundRect(surface, Offset(32f, 30f), Size(56f, 68f), CornerRadius(9f))
    drawRoundRect(blue.copy(alpha = 0.22f), Offset(32f, 30f), Size(56f, 68f), CornerRadius(9f), style = Stroke(2f))

    // Clip
    drawRoundRect(blue, Offset(46f, 24f), Size(28f, 14f), CornerRadius(7f))
    drawCircle(UepYellow, 3f, Offset(60f, 31f))

    // Empty lines
    for (i in 0..2) {
        val width = if (i == 2) 22f else 36f
        drawRoundRect(blue.copy(alpha = 0.14f), Offset(42f, 52f + i * 13f), Size(width, 5f), CornerRadius(2.5f))
    }

    drawSparkle(Offset(94f, 40f), 6f, UepYellow)
    drawSparkle(Offset(24f, 84f), 4.5f, ClassCyan)
}

// ─────────────────────────────────────────────────────────────────────────────
// Day / Break art
// ─────────────────────────────────────────────────────────────────────────────

private fun DrawScope.drawDayArt(blue: Color, surface: Color) {
    drawCircle(blue.copy(alpha = 0.08f), radius = 56f, center = Offset(60f, 62f))

    // Rays
    val sun = Offset(60f, 66f)
    for (i in 0..6) {
        val angle = Math.toRadians(180.0 + i * 30.0)
        val dx = Math.cos(angle).toFloat()
        val dy = Math.sin(angle).toFloat()
        drawLine(
            UepYellow,
            Offset(sun.x + dx * 30f, sun.y + dy * 30f),
            Offset(sun.x + dx * 40f, sun.y + dy * 40f),
            strokeWidth = 4f,
            cap = StrokeCap.Round,
        )
    }

    // Sun, half set behind the horizon
    drawCircle(UepYellow, 22f, sun)

    // Ground
    drawRoundRect(surface, Offset(22f, 66f), Size(76f, 30f), CornerRadius(9f))
    drawRoundRect(blue.copy(alpha = 0.22f), Offset(22f, 66f), Size(76f, 30f), CornerRadius(9f), style = Stroke(2f))
    drawRoundRect(blue.copy(alpha = 0.14f), Offset(34f, 76f), Size(52f, 5f), CornerRadius(2.5f))
    drawRoundRect(blue.copy(alpha = 0.14f), Offset(34f, 85f), Size(30f, 5f), CornerRadius(2.5f))

    // Clouds
    drawRoundRect(ClassCyan.copy(alpha = 0.55f), Offset(16f, 34f), Size(22f, 7f), CornerRadius(3.5f))
    drawRoundRect(ClassCyan.copy(alpha = 0.35f), Offset(86f, 26f), Size(16f, 6f), CornerRadius(3f))
}

private fun DrawScope.drawBreakArt(blue: Color, surface: Color) {
    drawCircle(blue.copy(alpha = 0.08f), radius = 56f, center = Offset(60f, 62f))

    // Saucer
    drawRoundRect(blue.copy(alpha = 0.22f), Offset(28f, 88f), Size(60f, 6f), CornerRadius(3f))

    // Handle
    drawArc(
        color = blue,
        startAngle = -90f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(78f, 58f),
        size = Size(18f, 22f),
        style = Stroke(5f, cap = StrokeCap.Round),
    )

    // Cup
    val cup = Path().apply {
        moveTo(32f, 54f)
        lineTo(84f, 54f)
        lineTo(80f, 80f)
        quadraticTo(79f, 88f, 70f, 88f)
        lineTo(46f, 88f)
        quadraticTo(37f, 88f, 36f, 80f)
        close()
    }
    drawPath(cup, surface)
    drawPath(cup, blue, style = Stroke(3f, join = StrokeJoin.Round))
    drawRoundRect(UepYellow, Offset(40f, 62f), Size(36f, 7f), CornerRadius(3.5f))

    // Steam
    for (i in 0..2) {
        val x = 44f + i * 14f
        val steam = Path().apply {
            moveTo(x, 46f)
            quadraticTo(x - 5f, 38f, x, 32f - i * 2f)
            quadraticTo(x + 5f, 26f - i * 2f, x, 20f - i * 2f)
        }
        drawPath(steam, ClassCyan, style = Stroke(3f, cap = StrokeCap.Round))
    }

    drawSparkle(Offset(100f, 34f), 6f, UepYellow)
    drawSparkle(Offset(20f, 78f), 4f, ClassCyan)
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
                    color = AppTheme.colors.text75,
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
                if (primary) Modifier.background(AppTheme.colors.primary, shape)
                else Modifier.border(1.5.dp, AppTheme.colors.title.copy(alpha = 0.5f), shape)
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
            color = if (primary) Color.White else AppTheme.colors.title,
        )
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
                .background(AppTheme.colors.surface, RoundedCornerShape(CardRadius))
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
                    color = AppTheme.colors.text75,
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
