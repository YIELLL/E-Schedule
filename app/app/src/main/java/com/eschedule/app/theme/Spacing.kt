package com.eschedule.app.theme

import androidx.compose.ui.unit.dp

/**
 * 4dp-based spacing scale (Material / Android guidance: 8dp rhythm, 16–24dp screen margins,
 * ≥ 48dp touch targets). Use these instead of ad-hoc dp values so every screen shares one rhythm.
 */
object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp

    /** Horizontal screen margin. */
    val screen = lg

    /** Minimum interactive size (Material accessibility guidance). */
    val touchTarget = 48.dp
}
