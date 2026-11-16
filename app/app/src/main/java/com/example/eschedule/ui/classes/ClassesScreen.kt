package com.example.eschedule.ui.classes

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
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
import com.example.eschedule.ui.components.EScheduleFab
import com.example.eschedule.ui.components.FigmaTextMuted
import com.example.eschedule.ui.components.PageTitle
import com.example.eschedule.ui.components.ScreenMargin
import java.util.Calendar

// ── Geometry (Figma proportions, re-spaced on a 4dp grid) ─────────────────────
//   Day header   : y 160, Poppins Regular 12 black
//   Grid         : x 73 → 372 (299 wide), 7 columns of ~39 with 4.5 gaps
//   Hour pitch   : 39dp, 6:00 AM line at y 215, hairlines black @ 10%
//   Time labels  : x 31, Poppins Regular 9, black @ 50%, centred on their line
//   Class blocks : cyan #00C0E8, radius 2.5, height = hours × 39 − 1, label Poppins 8 white
//   Now marker   : red @ 50% line from x 72 + 6.9 × 6 triangle
private val DAYS        = listOf("Mon", "Tue", "Wed", "Thur", "Fri", "Sat", "Sun")
private val HOUR_HEIGHT = 48.dp
private val GRID_LEFT   = 48.dp          // room for the "12:00 PM" labels
private val COL_GAP     = 4.dp
private val GRID_TOP_PAD = 8.dp          // room so the first label isn't clipped by the viewport
private const val TIME_START     = 0     // midnight — full 24h
private const val TIME_END       = 24
private const val SCROLL_TO_HOUR = 6     // default view: 6 AM at y ≈ 215

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
    val scrollState = rememberScrollState()
    LaunchedEffect(Unit) {
        val targetPx = with(density) { (HOUR_HEIGHT * SCROLL_TO_HOUR).toPx().toInt() }
        scrollState.scrollTo(targetPx)
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {

            PageTitle(title = "Class Schedule")

            // ── Day header row (sticky)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = ScreenMargin),
            ) {
                Spacer(Modifier.width(GRID_LEFT))
                Row(
                    modifier = Modifier.weight(1f),
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

            Spacer(Modifier.height(Spacing.sm))

            // ── Scrollable 24h grid ────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(horizontal = ScreenMargin),
            ) {
                WeeklyGrid(
                    blocks = blocks,
                    currentHour = currentHour,
                    onBlockClick = onClassClick,
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
// Weekly grid — time labels + hairlines + class blocks + now marker, all positioned
// from the same hour → y mapping so they can never drift apart.
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun WeeklyGrid(
    blocks: List<ClassBlock>,
    currentHour: Float,
    onBlockClick: (String) -> Unit,
) {
    val hours = TIME_START until TIME_END
    val gridHeight = GRID_TOP_PAD + HOUR_HEIGHT * (TIME_END - TIME_START) + 140.dp   // + room above the nav bar

    fun yOf(hour: Float): Dp = GRID_TOP_PAD + HOUR_HEIGHT * (hour - TIME_START)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(gridHeight),
    ) {
        val gridWidth = maxWidth - GRID_LEFT
        val colWidth = (gridWidth - COL_GAP * 6) / 7

        // Hairlines + hour labels (label is vertically centred on its line)
        hours.forEach { hour ->
            val y = yOf(hour.toFloat())
            Text(
                text = hourLabel(hour),
                fontSize = 10.sp,
                lineHeight = 15.sp,
                fontWeight = FontWeight.Normal,
                color = FigmaTextMuted,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.offset(x = 0.dp, y = y - 7.5.dp),
            )
            Box(
                modifier = Modifier
                    .offset(x = GRID_LEFT, y = y)
                    .width(gridWidth)
                    .height(1.dp)
                    .background(Color(0x1A000000)),
            )
        }

        // Class blocks
        blocks.forEach { block ->
            Box(
                modifier = Modifier
                    .offset(
                        x = GRID_LEFT + (colWidth + COL_GAP) * block.dayIndex,
                        y = yOf(block.startHour),
                    )
                    .width(colWidth)
                    .height(HOUR_HEIGHT * block.durationHours - 1.dp)
                    .clip(RoundedCornerShape(2.5.dp))
                    .background(ClassCyan)
                    .clickable { onBlockClick(block.id) },
            ) {
                Text(
                    text = block.subjectCode,
                    fontSize = 9.sp,
                    lineHeight = 12.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.White,
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp),
                )
            }
        }

        // Current-time marker: triangle (base at x 72) + red @ 50% line
        if (currentHour in TIME_START.toFloat()..TIME_END.toFloat()) {
            val y = yOf(currentHour)
            Box(
                modifier = Modifier
                    .offset(x = GRID_LEFT - 1.dp, y = y)
                    .width(gridWidth + 1.dp)
                    .height(1.dp)
                    .background(Color(0x80FF0000)),
            )
            Image(
                painter = painterResource(R.drawable.ic_time_marker),
                contentDescription = null,
                modifier = Modifier
                    .offset(x = 41.5.dp, y = y - 2.5.dp)
                    .size(width = 6.9282.dp, height = 6.dp)
                    .rotate(90f),
            )
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────
private fun hourLabel(hour: Int): String = when {
    hour == 0  -> "12:00 AM"
    hour < 12  -> "$hour:00 AM"
    hour == 12 -> "12:00 PM"
    else       -> "${hour - 12}:00 PM"
}
