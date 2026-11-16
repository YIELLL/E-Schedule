package com.example.eschedule.ui.classes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animate
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.eschedule.R
import com.example.eschedule.theme.ClassCyan
import com.example.eschedule.theme.Spacing
import com.example.eschedule.theme.UepBlue
import com.example.eschedule.ui.components.EScheduleFab
import com.example.eschedule.ui.components.FigmaTextMuted
import com.example.eschedule.ui.components.PageTitle
import com.example.eschedule.ui.components.ScreenMargin
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.roundToInt

// ── Geometry ───────────────────────────────────────────────────────────────────
//   Day header   : Poppins Regular 12, black
//   Grid         : 7 day columns (4dp gaps) right of a 48dp time-label column
//   Hour pitch   : 48dp at 1× zoom, hairlines black @ 10%
//   Class blocks : cyan #00C0E8, radius 2.5, height = hours × pitch − 1
//   Now marker   : red @ 50% line + 6.9 × 6 triangle
//
// Zoom: pinch with two fingers to scale the grid up to 3× (both axes; the time labels stay
// pinned while the days scroll sideways). 1× is the original, fully zoomed-out layout — pinching
// slightly past it rubber-bands and snaps back cleanly when the fingers lift.
private val DAYS        = listOf("Mon", "Tue", "Wed", "Thur", "Fri", "Sat", "Sun")
private val HOUR_HEIGHT = 48.dp
private val LABEL_W     = 48.dp
private val COL_GAP     = 4.dp
private val GRID_TOP_PAD = 8.dp          // room so the first label isn't clipped by the viewport
private const val TIME_START     = 0     // midnight — full 24h
private const val TIME_END       = 24
private const val SCROLL_TO_HOUR = 6     // default view: 6 AM

private const val MIN_ZOOM = 1f          // original size: the maximum zoom-out
private const val MAX_ZOOM = 3f
private const val RUBBER_ZOOM = 0.85f    // how far a pinch may overshoot below 1× before snapping back

/** One block on the weekly schedule grid */
data class ClassBlock(
    val id: String,
    val subjectCode: String,
    val dayIndex: Int,          // 0 = Mon … 6 = Sun
    val startHour: Float,       // e.g. 8.0 for 8:00 AM
    val durationHours: Float,
)

@Composable
fun ClassesScreen(
    onClassClick: (String) -> Unit,
    onAddClass: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val blocks = remember {
        listOf(
            ClassBlock("sia101_mon", "SIA101",        0, 8f,  1f),
            ClassBlock("it102_tue",  "IT102",         1, 8f,  1f),
            ClassBlock("sia101_wed", "SIA101",        2, 8f,  1f),
            ClassBlock("ge_wed",     "GE\nELEC 1",    2, 9f,  3f),
            ClassBlock("im101_tue",  "IM101",         1, 10f, 2f),
            ClassBlock("it102_thu",  "IT102",         3, 8f,  1f),
            ClassBlock("sia101_thu", "SIA101",        3, 10f, 3f),
            ClassBlock("ipt101_mon", "IPT101",        0, 13f, 1f),
            ClassBlock("net102_tue", "NET102",        1, 14f, 1f),
            ClassBlock("its101_wed", "ITS101",        2, 14f, 2f),
            ClassBlock("net102_thu", "NET102",        3, 14f, 1f),
            ClassBlock("its101_mon", "ITS101",        0, 15f, 1f),
            ClassBlock("its101_thu", "ITS101",        3, 15f, 2f),
            ClassBlock("net102_mon", "NET102",        0, 16f, 3f),
            ClassBlock("im101_wed2", "IM101",         2, 16f, 3f),
        )
    }

    // Current time in fractional hours (e.g. 12.5 = 12:30 PM)
    val currentHour = remember {
        val cal = Calendar.getInstance()
        cal.get(Calendar.HOUR_OF_DAY) + cal.get(Calendar.MINUTE) / 60f
    }

    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val vScroll = rememberScrollState()
    val hScroll = rememberScrollState()

    var zoom by remember { mutableFloatStateOf(1f) }
    // Pinch focus, applied after the grid has been re-measured at the new size.
    var pendingRatio by remember { mutableFloatStateOf(1f) }
    var pendingFocus by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(Unit) {
        vScroll.scrollTo(with(density) { (HOUR_HEIGHT * SCROLL_TO_HOUR).toPx().roundToInt() })
    }

    // Keep the point under the fingers where it was while the content grows / shrinks.
    LaunchedEffect(zoom) {
        withFrameNanos { }                       // wait for the new layout so max scroll is up to date
        val ratio = pendingRatio
        if (ratio != 1f) {
            val f = pendingFocus
            pendingRatio = 1f
            vScroll.scrollTo(((vScroll.value + f.y) * ratio - f.y).roundToInt().coerceAtLeast(0))
            hScroll.scrollTo(((hScroll.value + f.x) * ratio - f.x).roundToInt().coerceAtLeast(0))
        }
    }

    val labelWPx = with(density) { (ScreenMargin + LABEL_W).toPx() }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val dayAreaWidth = maxWidth - ScreenMargin * 2 - LABEL_W   // visible width of the day columns
        val gridWidth = dayAreaWidth * zoom
        val hourHeight = HOUR_HEIGHT * zoom

        Column(modifier = Modifier.fillMaxSize()) {

            PageTitle(title = "Class Schedule")

            // ── Day header (sticky vertically, follows the grid's horizontal scroll)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = ScreenMargin),
            ) {
                Spacer(Modifier.width(LABEL_W))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(hScroll, enabled = false),
                ) {
                    Row(
                        modifier = Modifier.width(gridWidth),
                        horizontalArrangement = Arrangement.spacedBy(COL_GAP),
                    ) {
                        DAYS.forEach { day ->
                            Text(
                                text = day,
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color.Black,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(Spacing.sm))

            // ── Scrollable, pinch-zoomable 24h grid
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .pinchZoom(
                        onZoom = { change, centroid ->
                            val old = zoom
                            val new = (old * change).coerceIn(RUBBER_ZOOM, MAX_ZOOM)
                            if (new != old) {
                                pendingRatio *= new / old
                                pendingFocus = Offset(
                                    x = (centroid.x - labelWPx).coerceAtLeast(0f),
                                    y = centroid.y,
                                )
                                zoom = new
                            }
                        },
                        onEnd = {
                            if (zoom < MIN_ZOOM) {
                                scope.launch { animate(zoom, MIN_ZOOM) { v, _ -> zoom = v } }
                            }
                        },
                    )
                    .verticalScroll(vScroll)
                    .padding(horizontal = ScreenMargin),
            ) {
                WeeklyGrid(
                    blocks = blocks,
                    currentHour = currentHour,
                    zoom = zoom,
                    hourHeight = hourHeight,
                    gridWidth = gridWidth,
                    hScroll = hScroll,
                    onBlockClick = onClassClick,
                )
            }
        }

        // ── Reset-zoom pill, only while zoomed in
        AnimatedVisibility(
            visible = zoom > 1.02f,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = Spacing.lg, bottom = 104.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(width = 96.dp, height = 40.dp)
                    .background(UepBlue, RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        scope.launch {
                            animate(zoom, MIN_ZOOM) { v, _ -> zoom = v }
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "${(zoom * 100).roundToInt()}% · Reset",
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                )
            }
        }

        // ── FAB: 56dp, 24dp from the right edge, 16dp above the nav pill
        EScheduleFab(
            onClick = onAddClass,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = Spacing.lg, bottom = 104.dp),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Weekly grid — time labels (fixed) + hairlines / blocks / now-marker (scroll sideways),
// all positioned from the same hour → y mapping so they can never drift apart.
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun WeeklyGrid(
    blocks: List<ClassBlock>,
    currentHour: Float,
    zoom: Float,
    hourHeight: Dp,
    gridWidth: Dp,
    hScroll: androidx.compose.foundation.ScrollState,
    onBlockClick: (String) -> Unit,
) {
    val hours = TIME_START until TIME_END
    val totalHeight = GRID_TOP_PAD + hourHeight * (TIME_END - TIME_START) + 140.dp   // + room above the nav bar
    val colWidth = (gridWidth - COL_GAP * 6) / 7

    fun yOf(hour: Float): Dp = GRID_TOP_PAD + hourHeight * (hour - TIME_START)

    val labelSize = (9f + (zoom - 1f) * 2f).coerceIn(9f, 12f)
    val inNow = currentHour in TIME_START.toFloat()..TIME_END.toFloat()

    Row(modifier = Modifier.fillMaxWidth().height(totalHeight)) {

        // ── Time-label column (does not scroll sideways)
        Box(modifier = Modifier.width(LABEL_W).height(totalHeight)) {
            hours.forEach { hour ->
                Text(
                    text = hourLabel(hour),
                    fontSize = 10.sp,
                    lineHeight = 15.sp,
                    fontWeight = FontWeight.Normal,
                    color = FigmaTextMuted,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.offset(y = yOf(hour.toFloat()) - 7.5.dp),
                )
            }
            if (inNow) {
                Image(
                    painter = painterResource(R.drawable.ic_time_marker),
                    contentDescription = null,
                    modifier = Modifier
                        .offset(x = LABEL_W - 6.dp, y = yOf(currentHour) - 2.5.dp)
                        .size(width = 6.9282.dp, height = 6.dp)
                        .rotate(90f),
                )
            }
        }

        // ── Day area: hairlines, blocks, now-line
        Box(
            modifier = Modifier
                .weight(1f)
                .height(totalHeight)
                .horizontalScroll(hScroll),
        ) {
            Box(modifier = Modifier.width(gridWidth).height(totalHeight)) {
                hours.forEach { hour ->
                    Box(
                        modifier = Modifier
                            .offset(y = yOf(hour.toFloat()))
                            .width(gridWidth)
                            .height(1.dp)
                            .background(Color(0x1A000000)),
                    )
                }

                blocks.forEach { block ->
                    Box(
                        modifier = Modifier
                            .offset(
                                x = (colWidth + COL_GAP) * block.dayIndex,
                                y = yOf(block.startHour),
                            )
                            .width(colWidth)
                            .height(hourHeight * block.durationHours - 1.dp)
                            .clip(RoundedCornerShape(2.5.dp))
                            .background(ClassCyan)
                            .clickable { onBlockClick(block.id) },
                    ) {
                        Text(
                            text = block.subjectCode,
                            fontSize = labelSize.sp,
                            lineHeight = (labelSize + 3f).sp,
                            fontWeight = FontWeight.Normal,
                            color = Color.White,
                            modifier = Modifier.padding(start = 4.dp, top = 2.dp),
                        )
                    }
                }

                if (inNow) {
                    Box(
                        modifier = Modifier
                            .offset(y = yOf(currentHour))
                            .width(gridWidth)
                            .height(1.dp)
                            .background(Color(0x80FF0000)),
                    )
                }
            }
        }
    }
}

// ── Gestures ──────────────────────────────────────────────────────────────────

/**
 * Two-finger pinch. Single-finger gestures are left alone (so scrolling and taps still work);
 * once a second finger lands the pinch consumes the events before the scroll containers see them.
 */
private fun Modifier.pinchZoom(
    onZoom: (change: Float, centroid: Offset) -> Unit,
    onEnd: () -> Unit,
): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        var pinching = false
        do {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            if (event.changes.count { it.pressed } >= 2) {
                val change = event.calculateZoom()
                if (change != 1f) {
                    pinching = true
                    onZoom(change, event.calculateCentroid(useCurrent = true))
                }
                if (pinching) event.changes.forEach { it.consume() }
            }
        } while (event.changes.any { it.pressed })
        if (pinching) onEnd()
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────
private fun hourLabel(hour: Int): String = when {
    hour == 0  -> "12:00 AM"
    hour < 12  -> "$hour:00 AM"
    hour == 12 -> "12:00 PM"
    else       -> "${hour - 12}:00 PM"
}
