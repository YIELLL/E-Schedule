package com.eschedule.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eschedule.app.theme.AppTheme
import com.eschedule.app.R
import com.eschedule.app.theme.Inter
import com.eschedule.app.theme.NavActiveBlue

sealed interface TabRoute {
    data object Home : TabRoute
    data object Classes : TabRoute
    data object Scan : TabRoute
    data object Reminders : TabRoute
    data object Settings : TabRoute
}

// ── Figma geometry (402 × 874 frame) ──────────────────────────────────────────
//   Nav pill : x 16, y 789, 370 × 65, radius 50, black @ 20%  (20dp above frame bottom)
//   Scan FAB : 85 × 85 asset, centred, top = pill top − 43
//   Icons    : Home 30, Classes 26, Reminders 26, Settings 27
//   Labels   : Inter SemiBold 8 (Scan = 10), white; selected = #0088FF
private val PillHeight: Dp = 65.dp
private val PillRadius: Dp = 50.dp
private val PillSideMargin: Dp = 16.dp
private val PillBottomMargin: Dp = 20.dp
private val FabSize: Dp = 85.dp
private val FabOverhang: Dp = 43.dp
private val NavTabWidth: Dp = 60.dp

private val InactiveTint = Color.White
private val ActiveTint = NavActiveBlue

private data class NavItem(
    val label: String,
    val iconRes: Int,
    val tab: TabRoute,
    /** Horizontal centre of the tab as a fraction of the pill width (Figma x-centre / 370). */
    val centerFraction: Float,
    val iconSize: Dp,
    /** Offsets from the pill's top edge. */
    val iconTop: Dp,
    val labelTop: Dp,
)

private val NavItems = listOf(
    NavItem("Home",      R.drawable.ic_nav_home,      TabRoute.Home,      45f / 370f,    30.dp, 12.dp, 40.dp),
    NavItem("Classes",   R.drawable.ic_nav_classes,   TabRoute.Classes,   105f / 370f,   26.dp, 14.dp, 40.dp),
    NavItem("Reminders", R.drawable.ic_nav_reminders, TabRoute.Reminders, 255f / 370f,   26.dp, 14.dp, 42.dp),
    NavItem("Settings",  R.drawable.ic_nav_settings,  TabRoute.Settings,  323.5f / 370f, 27.dp, 14.dp, 42.dp),
)

@Composable
fun BottomNavBar(
    currentTab: TabRoute,
    onTabSelected: (TabRoute) -> Unit,
    onScanClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // The Figma frame leaves 20dp below the pill; never go below the system nav-bar inset.
    val bottomMargin = maxOf(PillBottomMargin, WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = PillSideMargin)
            .padding(bottom = bottomMargin)
            .height(FabOverhang + PillHeight),
    ) {
        // ── Pill with tabs ────────────────────────────────────────────────────
        BoxWithConstraints(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(PillHeight)
                .background(AppTheme.colors.navPill, RoundedCornerShape(PillRadius)),
        ) {
            val pillWidth = maxWidth

            NavItems.forEach { item ->
                NavTab(
                    item = item,
                    isSelected = currentTab == item.tab,
                    onClick = { onTabSelected(item.tab) },
                    modifier = Modifier.offset(x = pillWidth * item.centerFraction - NavTabWidth / 2),
                )
            }

            // "Scan" label sits under the FAB (Inter SemiBold 10, top = pill top + 46)
            Text(
                text = "Scan",
                fontFamily = Inter,
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.sp,
                lineHeight = 12.sp,
                color = InactiveTint,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = 46.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onScanClick,
                    ),
            )
        }

        // ── Scan FAB (85 × 85 Figma asset: #F7F7F7 ring + yellow disc + scan glyph) ──
        Image(
            // Scan tab uses the solid-yellow camera variant (Figma "Scan" frame)
            painter = painterResource(
                when {
                    AppTheme.colors.dark && currentTab == TabRoute.Scan -> R.drawable.ic_nav_scan_fab_camera_dark
                    AppTheme.colors.dark -> R.drawable.ic_nav_scan_fab_dark
                    currentTab == TabRoute.Scan -> R.drawable.ic_nav_scan_fab_camera
                    else -> R.drawable.ic_nav_scan_fab
                }
            ),
            contentDescription = if (currentTab == TabRoute.Scan) "Capture" else "Scan",
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(FabSize)
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onScanClick,
                ),
        )
    }
}

@Composable
private fun NavTab(
    item: NavItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tint by animateColorAsState(
        targetValue = if (isSelected) ActiveTint else InactiveTint,
        animationSpec = tween(durationMillis = 200),
        label = "tint_${item.label}",
    )

    Box(
        modifier = modifier
            .width(NavTabWidth)
            .fillMaxHeight()
            .clip(RoundedCornerShape(PillRadius))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
    ) {
        Icon(
            painter = painterResource(item.iconRes),
            contentDescription = item.label,
            tint = tint,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = item.iconTop)
                .size(item.iconSize),
        )
        Text(
            text = item.label,
            fontFamily = Inter,
            fontWeight = FontWeight.SemiBold,
            fontSize = 8.sp,
            lineHeight = 10.sp,
            color = tint,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = item.labelTop),
        )
    }
}
