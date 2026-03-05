package com.kunpitech.knighttour.ui.theme

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ═══════════════════════════════════════════════════════════════
//  KNIGHT TOUR — DIMENSION TOKENS
//  Single source of truth for spacing, sizing, elevation
// ═══════════════════════════════════════════════════════════════

object Spacing {
    val xxs  = 2.dp
    val xs   = 4.dp
    val sm   = 8.dp
    val md   = 12.dp
    val lg   = 16.dp
    val xl   = 20.dp
    val xxl  = 24.dp
    val xxxl = 32.dp
    val huge = 40.dp
    val max  = 48.dp
}

object BoardDimensions {
    /** Board outer padding from screen edge */
    val boardPadding      = 16.dp

    /** Gap between cells in the grid */
    val cellGap           = 2.dp

    /** Board border width */
    val boardBorderWidth  = 1.dp

    /** Knight piece glow radius */
    val knightGlowRadius  = 12.dp

    /** Valid move dot size (fraction of cell) */
    val validMoveDotSize  = 10.dp

    /** Path trail stroke width */
    val trailStrokeWidth  = 2.dp

    /** Cell move-number font size */
    val cellNumberSize    = 9.sp

    /** Coordinate label size (A–F, 1–6) */
    val coordLabelSize    = 9.sp
}

object ElevationTokens {
    val none   = 0.dp
    val low    = 1.dp
    val medium = 4.dp
    val high   = 8.dp
    val modal  = 16.dp
}

object IconSize {
    val sm   = 16.dp
    val md   = 20.dp
    val lg   = 24.dp
    val xl   = 32.dp
    val xxl  = 48.dp
    val hero = 64.dp
}

object ComponentSize {
    val buttonHeightPrimary   = 52.dp
    val buttonHeightSecondary = 44.dp
    val buttonHeightCompact   = 36.dp

    val bottomNavHeight       = 64.dp
    val topBarHeight          = 56.dp

    val chipHeight            = 32.dp
    val badgeHeight           = 22.dp

    val leaderboardRowHeight  = 52.dp
    val lobbyCardHeight       = 72.dp

    val avatarSm              = 28.dp
    val avatarMd              = 40.dp
    val avatarLg              = 56.dp
}

object BorderWidth {
    val thin    = 1.dp
    val default = 1.5.dp
    val thick   = 2.dp
}