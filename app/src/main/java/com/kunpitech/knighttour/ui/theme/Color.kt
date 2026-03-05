package com.kunpitech.knighttour.ui.theme

import androidx.compose.ui.graphics.Color

// ═══════════════════════════════════════════════════════════════
//  KNIGHT TOUR — COLOR SYSTEM
//  Devil's Plan inspired: dark luxury · gold · blood red
// ═══════════════════════════════════════════════════════════════

// ── BACKGROUND SCALE ────────────────────────────────────────────
/** Deepest background — near-black with a cold blue-violet tint */
val AbyssBlack        = Color(0xFF050508)

/** Primary surface — cards, dialogs, bottom sheets */
val SurfaceDark       = Color(0xFF0D0D14)

/** Elevated surface — panels, headers, raised cards */
val SurfaceElevated   = Color(0xFF12121E)

/** Highest surface — input fields, chips, nav bar */
val SurfaceHighest    = Color(0xFF18182A)

// ── BORDERS & DIVIDERS ──────────────────────────────────────────
/** Subtle border — most dividers, card outlines */
val BorderSubtle      = Color(0xFF1E1E30)

/** Default border — focused cards, active elements */
val BorderDefault     = Color(0xFF2A2A3D)

/** Strong border — selected / highlighted state */
val BorderStrong      = Color(0xFF3D3D5C)

// ── GOLD — PRIMARY ACCENT ───────────────────────────────────────
/** Knight Gold — primary brand accent, CTAs, active state */
val KnightGold        = Color(0xFFC8A84B)

/** Crown Gold — brighter gold for headings, glow source */
val CrownGold         = Color(0xFFE8C96A)

/** Pale Gold — disabled / secondary gold elements */
val PaleGold          = Color(0xFF8B7535)

/** Gold Glow — used in shadow/glow composable params */
val GoldGlow          = Color(0x4DC8A84B)

/** Gold Dim — faint gold tint for backgrounds */
val GoldDim           = Color(0x1AC8A84B)

// ── DEVIL RED — DANGER / ALERT / SPECIAL ────────────────────────
/** Devil Red — danger, timer warning, invalid move */
val DevilRed          = Color(0xFFC0392B)

/** Blood Red Bright — glowing danger state */
val BloodRedBright    = Color(0xFFE74C3C)

/** Red Glow — for box-shadow equivalents */
val RedGlow           = Color(0x4DE74C3C)

/** Red Dim — faint danger tint */
val RedDim            = Color(0x1AE74C3C)

// ── STATUS COLORS ───────────────────────────────────────────────
/** Victory Green — game complete, success */
val VictoryGreen      = Color(0xFF2ECC71)

/** Victory Green Dim */
val VictoryGreenDim   = Color(0x1A2ECC71)

/** Warning Amber — hint penalty, caution */
val WarningAmber      = Color(0xFFE67E22)

/** Royal Purple — Devil Mode exclusive */
val DevilPurple       = Color(0xFF9B59B6)

/** Devil Purple Dim */
val DevilPurpleDim    = Color(0x1A9B59B6)

/** Teal — online / connected state */
val OnlineTeal        = Color(0xFF1ABC9C)

/** Teal Dim */
val OnlineTealDim     = Color(0x1A1ABC9C)

// ── TEXT SCALE ──────────────────────────────────────────────────
/** Primary text — headings, important labels */
val TextPrimary       = Color(0xFFEEF0F5)

/** Secondary text — subtitles, body */
val TextSecondary     = Color(0xFF8899AA)

/** Tertiary text — captions, hints, disabled */
val TextTertiary      = Color(0xFF55556A)

/** Inverse text — text on gold/light backgrounds */
val TextInverse       = Color(0xFF050508)

// ── BOARD CELL COLORS ───────────────────────────────────────────
/** Light chess cell */
val CellLight         = Color(0xFF1A1A2E)

/** Dark chess cell */
val CellDark          = Color(0xFF0F0F1A)

/** Visited cell — knight has been here */
val CellVisited       = Color(0xFF0D2035)

/** Visited cell border tint */
val CellVisitedBorder = Color(0x33C8A84B)

/** Knight's current position highlight */
val CellKnight        = Color(0x40C8A84B)

/** Valid move cell — possible L-move targets */
val CellValidMove     = Color(0x1FE74C3C)

/** Hint cell — Warnsdorff best move */
val CellHint          = Color(0x33E8C96A)

// ── SPECIAL / OVERLAY ───────────────────────────────────────────
/** Scrim — modal/dialog backdrop */
val Scrim             = Color(0xCC050508)

/** Path trail color — line drawn on Canvas */
val PathTrail         = Color(0x4DC8A84B)

/** Path trail node dot */
val PathNode          = Color(0x80C8A84B)

/** Scan-line overlay tint (applied as repeating gradient) */
val ScanLine          = Color(0x0F000000)

// ═══════════════════════════════════════════════════════════════
//  MATERIAL3 COLOR SCHEME MAPPINGS
//  Referenced in Theme.kt via lightColorScheme / darkColorScheme
// ═══════════════════════════════════════════════════════════════

// Dark scheme (primary theme)
val md_primary             = KnightGold
val md_onPrimary           = TextInverse
val md_primaryContainer    = Color(0xFF2A2210)
val md_onPrimaryContainer  = CrownGold

val md_secondary           = OnlineTeal
val md_onSecondary         = TextInverse
val md_secondaryContainer  = Color(0xFF0D2A26)
val md_onSecondaryContainer = OnlineTeal

val md_tertiary            = DevilPurple
val md_onTertiary          = TextPrimary
val md_tertiaryContainer   = Color(0xFF1E1026)
val md_onTertiaryContainer = DevilPurple

val md_error               = BloodRedBright
val md_onError             = TextPrimary
val md_errorContainer      = Color(0xFF2A0D0A)
val md_onErrorContainer    = BloodRedBright

val md_background          = AbyssBlack
val md_onBackground        = TextPrimary

val md_surface             = SurfaceDark
val md_onSurface           = TextPrimary
val md_surfaceVariant      = SurfaceElevated
val md_onSurfaceVariant    = TextSecondary

val md_outline             = BorderDefault
val md_outlineVariant      = BorderSubtle

val md_inverseSurface      = TextPrimary
val md_inverseOnSurface    = AbyssBlack
val md_inversePrimary      = PaleGold

val md_surfaceTint         = KnightGold
val md_scrim               = Scrim