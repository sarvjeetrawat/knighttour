package com.kunpitech.knighttour.ui.theme

import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// ═══════════════════════════════════════════════════════════════
//  KNIGHT TOUR — SHAPE SYSTEM
//
//  Design Language: Angular, tactical, chess-board-inspired.
//  Cut corners evoke a medieval/strategy game aesthetic.
//  Rounded corners for softer components (dialogs, cards).
// ═══════════════════════════════════════════════════════════════

// ── CUSTOM SHAPES ───────────────────────────────────────────────

/** Sharp cut corner — primary buttons, mode chips, badges */
val CutCornerSmall   = CutCornerShape(4.dp)

/** Medium cut corner — game cards, result panels */
val CutCornerMedium  = CutCornerShape(8.dp)

/** Large cut corner — full-width section cards */
val CutCornerLarge   = CutCornerShape(12.dp)

/** Subtle round corner — bottom sheets, dialogs */
val RoundedSmall     = RoundedCornerShape(4.dp)

/** Standard round corner — input fields, snackbars */
val RoundedMedium    = RoundedCornerShape(8.dp)

/** Large round corner — profile avatar containers */
val RoundedLarge     = RoundedCornerShape(16.dp)

/** Pill shape — toggle chips, online badge */
val PillShape        = RoundedCornerShape(50)

/** Board cell — perfectly square, no radius */
val CellShape        = RoundedCornerShape(0.dp)

/** Knight piece container — small cut corner for drama */
val KnightContainer  = CutCornerShape(
    topStart     = 8.dp,
    topEnd       = 0.dp,
    bottomStart  = 0.dp,
    bottomEnd    = 8.dp
)

/** Leaderboard row — subtle left-cut for rank */
val LeaderboardRow   = CutCornerShape(
    topStart     = 4.dp,
    topEnd       = 0.dp,
    bottomStart  = 4.dp,
    bottomEnd    = 0.dp
)

/** Bottom navigation bar — top cut corners */
val BottomNavShape   = CutCornerShape(
    topStart     = 8.dp,
    topEnd       = 8.dp,
    bottomStart  = 0.dp,
    bottomEnd    = 0.dp
)

/** Dialog card — symmetrical cut corners */
val DialogShape      = CutCornerShape(
    topStart     = 12.dp,
    topEnd       = 0.dp,
    bottomStart  = 0.dp,
    bottomEnd    = 12.dp
)

// ── MATERIAL3 SHAPE MAPPING ──────────────────────────────────────

/**
 * Passed into MaterialTheme(shapes = KnightTourShapes)
 *
 * Material3 shape roles:
 *   extraSmall → chips, tooltips, small buttons
 *   small      → standard buttons, text fields
 *   medium     → cards, menus
 *   large      → bottom sheets, navigation drawers
 *   extraLarge → dialogs, full-screen modals
 */
val KnightTourShapes = Shapes(
    extraSmall = CutCornerShape(2.dp),   // tiny chips, badges
    small      = CutCornerShape(4.dp),   // buttons, input fields
    medium     = CutCornerShape(6.dp),   // cards, game panels
    large      = CutCornerShape(10.dp),  // bottom sheets
    extraLarge = CutCornerShape(14.dp),  // dialogs
)