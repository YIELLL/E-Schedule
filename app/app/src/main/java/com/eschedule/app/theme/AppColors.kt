package com.eschedule.app.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter

/**
 * The app's semantic colours. Screens ask for "the card colour" or "muted text" instead of a fixed
 * black/white, so the whole UI follows light or dark mode. The light values are exactly the colours
 * the design used before dark mode existed.
 */
@Immutable
data class AppColors(
    val dark: Boolean,
    /** Screen background. */
    val background: Color,
    /** Cards, sheets' fields, dialogs. */
    val surface: Color,
    /** Primary text (black in light mode). */
    val text: Color,
    /** Entered values (black @ 75% in light mode). */
    val text75: Color,
    /** Secondary text (black @ 50%). */
    val muted: Color,
    /** Placeholders, faint borders (black @ 25%). */
    val placeholder: Color,
    /** Hairline dividers (black @ 10%). */
    val hairline: Color,
    /** Brand fills and accents: bars, selected chips, switches, buttons. */
    val primary: Color,
    /** Brand colour used as *text* / thin strokes, lightened in dark mode so it stays readable. */
    val title: Color,
    /** Floating bottom-nav pill. */
    val navPill: Color,
    /** Round control backgrounds (delete / edit / close / flash). */
    val controlFill: Color,
    /** Small grey section labels in Settings. */
    val groupLabel: Color,
    /** Switch track when off. */
    val switchTrackOff: Color,
    /** Tint for icon drawables drawn in black (null = draw as exported). */
    val blackIconFilter: ColorFilter?,
    /** Tint for icon drawables drawn in the brand blue (null = draw as exported). */
    val blueIconFilter: ColorFilter?,
)

val LightAppColors = AppColors(
    dark = false,
    background = Color(0xFFF7F7F7),
    surface = Color(0xFFFFFFFF),
    text = Color(0xFF000000),
    text75 = Color(0xBF000000),
    muted = Color(0x80000000),
    placeholder = Color(0x40000000),
    hairline = Color(0x1A000000),
    primary = UepBlue,
    title = UepBlue,
    navPill = Color(0x33000000),
    controlFill = Color(0x33000000),
    groupLabel = Color(0xFFBBBBBB),
    switchTrackOff = Color(0xFFD0D3D9),
    blackIconFilter = null,
    blueIconFilter = null,
)

private val DarkTitle = Color(0xFFA6C0F7)

val DarkAppColors = AppColors(
    dark = true,
    background = Color(0xFF101319),
    surface = Color(0xFF1B1F27),
    text = Color(0xFFECEEF2),
    text75 = Color(0xBFECEEF2),
    muted = Color(0x99ECEEF2),
    placeholder = Color(0x4DECEEF2),
    hairline = Color(0x1FFFFFFF),
    primary = Color(0xFF4C7BD0),
    title = DarkTitle,
    navPill = Color(0x2EFFFFFF),
    controlFill = Color(0x2EFFFFFF),
    groupLabel = Color(0xFF6E7685),
    switchTrackOff = Color(0xFF3A404C),
    blackIconFilter = ColorFilter.tint(Color.White),
    blueIconFilter = ColorFilter.tint(DarkTitle),
)

val LocalAppColors = staticCompositionLocalOf { LightAppColors }

/** Entry point: `AppTheme.colors.surface`, `AppTheme.colors.muted`, … */
object AppTheme {
    val colors: AppColors
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current
}
