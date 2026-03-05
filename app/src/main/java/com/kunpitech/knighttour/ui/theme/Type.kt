package com.kunpitech.knighttour.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.kunpitech.knighttour.R

// ═══════════════════════════════════════════════════════════════
//  KNIGHT TOUR — TYPOGRAPHY SYSTEM
//  Uses androidx.compose.ui:ui-text-google-fonts
//  No TTF files needed — fonts fetched via Google Fonts provider
//
//  Font Strategy:
//  ┌─────────────────────────────────────────────────────────┐
//  │  CINZEL DECORATIVE  →  Game title, victory banners      │
//  │  CINZEL             →  UI labels, stats, nav, headers   │
//  │  RAJDHANI           →  Body text, descriptions, scores  │
//  └─────────────────────────────────────────────────────────┘
//
//  Required in build.gradle (app):
//    implementation("androidx.compose.ui:ui-text-google-fonts:1.7.8")
//
//  Required: res/values/font_certs.xml  (see FONT_SETUP.md)
// ═══════════════════════════════════════════════════════════════

// ── GOOGLE FONTS PROVIDER ───────────────────────────────────────

/**
 * GoogleFont.Provider authenticates requests to the Google Fonts API.
 *
 * R.array.com_google_android_gms_fonts_certs is auto-generated when
 * you add google-services.json to your project via Firebase/Google setup.
 *
 * Alternative: copy the cert array manually into res/values/font_certs.xml
 * (see FONT_SETUP.md for the full XML content).
 */
val GoogleFontsProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage   = "com.google.android.gms",
    certificates      = R.array.com_google_android_gms_fonts_certs,
)

// ── GOOGLE FONT REFERENCES ──────────────────────────────────────

private val CinzelGoogleFont           = GoogleFont("Cinzel")
private val CinzelDecorativeGoogleFont = GoogleFont("Cinzel Decorative")
private val RajdhaniGoogleFont         = GoogleFont("Rajdhani")

// ── FONT FAMILIES ───────────────────────────────────────────────

/**
 * Cinzel Decorative — Reserved for game title, mode banners,
 * victory/defeat screens. High drama, maximum presence.
 */
val CinzelDecorativeFontFamily = FontFamily(
    Font(
        googleFont   = CinzelDecorativeGoogleFont,
        fontProvider = GoogleFontsProvider,
        weight       = FontWeight.Normal,
    ),
    Font(
        googleFont   = CinzelDecorativeGoogleFont,
        fontProvider = GoogleFontsProvider,
        weight       = FontWeight.Bold,
    ),
    Font(
        googleFont   = CinzelDecorativeGoogleFont,
        fontProvider = GoogleFontsProvider,
        weight       = FontWeight.Black,
    ),
)

/**
 * Cinzel — Serif with Roman elegance. Used for all UI chrome:
 * stat labels, section headers, button text, nav labels,
 * leaderboard ranks, difficulty chips.
 */
val CinzelFontFamily = FontFamily(
    Font(
        googleFont   = CinzelGoogleFont,
        fontProvider = GoogleFontsProvider,
        weight       = FontWeight.Normal,
    ),
    Font(
        googleFont   = CinzelGoogleFont,
        fontProvider = GoogleFontsProvider,
        weight       = FontWeight.SemiBold,
    ),
    Font(
        googleFont   = CinzelGoogleFont,
        fontProvider = GoogleFontsProvider,
        weight       = FontWeight.Bold,
    ),
    Font(
        googleFont   = CinzelGoogleFont,
        fontProvider = GoogleFontsProvider,
        weight       = FontWeight.Black,
    ),
)

/**
 * Rajdhani — Condensed geometric sans. Used for all readable body
 * content: descriptions, move lists, settings text, score numbers,
 * countdown timer digits, and any multi-line text.
 */
val RajdhaniFontFamily = FontFamily(
    Font(
        googleFont   = RajdhaniGoogleFont,
        fontProvider = GoogleFontsProvider,
        weight       = FontWeight.Light,
    ),
    Font(
        googleFont   = RajdhaniGoogleFont,
        fontProvider = GoogleFontsProvider,
        weight       = FontWeight.Normal,
    ),
    Font(
        googleFont   = RajdhaniGoogleFont,
        fontProvider = GoogleFontsProvider,
        weight       = FontWeight.Medium,
    ),
    Font(
        googleFont   = RajdhaniGoogleFont,
        fontProvider = GoogleFontsProvider,
        weight       = FontWeight.SemiBold,
    ),
    Font(
        googleFont   = RajdhaniGoogleFont,
        fontProvider = GoogleFontsProvider,
        weight       = FontWeight.Bold,
    ),
)

// ═══════════════════════════════════════════════════════════════
//  CUSTOM TEXT STYLES (beyond Material3 Typography)
//  Use these directly in Composables via KnightTourType.*
// ═══════════════════════════════════════════════════════════════

object KnightTourType {

    // ── DISPLAY ─────────────────────────────────────────────────

    /** Game title on Splash/Home screen — "KNIGHT TOUR" */
    val GameTitle = TextStyle(
        fontFamily    = CinzelDecorativeFontFamily,
        fontWeight    = FontWeight.Black,
        fontSize      = 36.sp,
        lineHeight    = 40.sp,
        letterSpacing = 2.sp,
    )

    /** Section title on results / mode select — "VICTORY", "DEFEAT" */
    val DisplayBanner = TextStyle(
        fontFamily    = CinzelDecorativeFontFamily,
        fontWeight    = FontWeight.Bold,
        fontSize      = 28.sp,
        lineHeight    = 32.sp,
        letterSpacing = 1.sp,
    )

    /** Large stat display — score number, move count */
    val StatDisplay = TextStyle(
        fontFamily    = CinzelFontFamily,
        fontWeight    = FontWeight.Bold,
        fontSize      = 32.sp,
        lineHeight    = 36.sp,
        letterSpacing = 1.sp,
    )

    // ── TIMER ────────────────────────────────────────────────────

    /** Countdown timer normal state — "2:30" */
    val TimerNormal = TextStyle(
        fontFamily    = RajdhaniFontFamily,
        fontWeight    = FontWeight.Bold,
        fontSize      = 28.sp,
        lineHeight    = 32.sp,
        letterSpacing = 2.sp,
    )

    /** Countdown timer panic state (<15s) — same size, red + blink */
    val TimerPanic = TextStyle(
        fontFamily    = RajdhaniFontFamily,
        fontWeight    = FontWeight.Bold,
        fontSize      = 28.sp,
        lineHeight    = 32.sp,
        letterSpacing = 2.sp,
        color         = BloodRedBright,
    )

    // ── HEADERS & LABELS ─────────────────────────────────────────

    /** Screen section header — "LEADERBOARD", "SETTINGS" */
    val ScreenHeader = TextStyle(
        fontFamily    = CinzelFontFamily,
        fontWeight    = FontWeight.SemiBold,
        fontSize      = 14.sp,
        lineHeight    = 18.sp,
        letterSpacing = 4.sp,
    )

    /** Card title — "GAME IN PROGRESS", player name in lobby */
    val CardTitle = TextStyle(
        fontFamily    = CinzelFontFamily,
        fontWeight    = FontWeight.SemiBold,
        fontSize      = 16.sp,
        lineHeight    = 20.sp,
        letterSpacing = 1.sp,
    )

    /** Stat label under a number — "MOVES", "TIME", "SCORE" */
    val StatLabel = TextStyle(
        fontFamily    = CinzelFontFamily,
        fontWeight    = FontWeight.Normal,
        fontSize      = 9.sp,
        lineHeight    = 12.sp,
        letterSpacing = 3.sp,
    )

    /** Small eyebrow above a title — "SEASON II · EPISODE VII" */
    val Eyebrow = TextStyle(
        fontFamily    = CinzelFontFamily,
        fontWeight    = FontWeight.Normal,
        fontSize      = 9.sp,
        lineHeight    = 12.sp,
        letterSpacing = 5.sp,
    )

    // ── BODY ─────────────────────────────────────────────────────

    /** Primary body text — rule descriptions, dialog content */
    val BodyPrimary = TextStyle(
        fontFamily    = RajdhaniFontFamily,
        fontWeight    = FontWeight.Normal,
        fontSize      = 15.sp,
        lineHeight    = 22.sp,
        letterSpacing = 0.3.sp,
    )

    /** Secondary body text — hints, secondary descriptions */
    val BodySecondary = TextStyle(
        fontFamily    = RajdhaniFontFamily,
        fontWeight    = FontWeight.Normal,
        fontSize      = 13.sp,
        lineHeight    = 19.sp,
        letterSpacing = 0.2.sp,
    )

    /** Caption — timestamps, metadata, helper text */
    val Caption = TextStyle(
        fontFamily    = RajdhaniFontFamily,
        fontWeight    = FontWeight.Normal,
        fontSize      = 11.sp,
        lineHeight    = 15.sp,
        letterSpacing = 0.5.sp,
    )

    // ── BUTTONS ──────────────────────────────────────────────────

    /** Primary button label — "BEGIN", "PLAY AGAIN" */
    val ButtonPrimary = TextStyle(
        fontFamily    = CinzelFontFamily,
        fontWeight    = FontWeight.Bold,
        fontSize      = 13.sp,
        lineHeight    = 16.sp,
        letterSpacing = 4.sp,
        textAlign     = TextAlign.Center,
    )

    /** Secondary button label — "MENU", "CANCEL" */
    val ButtonSecondary = TextStyle(
        fontFamily    = CinzelFontFamily,
        fontWeight    = FontWeight.Normal,
        fontSize      = 11.sp,
        lineHeight    = 14.sp,
        letterSpacing = 3.sp,
        textAlign     = TextAlign.Center,
    )

    // ── BOARD ────────────────────────────────────────────────────

    /** Cell move number — the "3", "14" drawn inside visited cells */
    val CellNumber = TextStyle(
        fontFamily    = RajdhaniFontFamily,
        fontWeight    = FontWeight.SemiBold,
        fontSize      = 9.sp,
        lineHeight    = 11.sp,
        letterSpacing = 0.sp,
        textAlign     = TextAlign.Center,
    )

    /** Board coordinate label — "A", "B", "1", "2" */
    val BoardCoord = TextStyle(
        fontFamily    = CinzelFontFamily,
        fontWeight    = FontWeight.Normal,
        fontSize      = 9.sp,
        lineHeight    = 12.sp,
        letterSpacing = 1.sp,
        textAlign     = TextAlign.Center,
    )

    // ── LEADERBOARD ──────────────────────────────────────────────

    /** Rank number — "#1", "#2", "#3" */
    val LeaderboardRank = TextStyle(
        fontFamily    = CinzelFontFamily,
        fontWeight    = FontWeight.Bold,
        fontSize      = 16.sp,
        lineHeight    = 20.sp,
        letterSpacing = 0.sp,
        textAlign     = TextAlign.Center,
    )

    /** Player name in leaderboard row */
    val LeaderboardName = TextStyle(
        fontFamily    = RajdhaniFontFamily,
        fontWeight    = FontWeight.SemiBold,
        fontSize      = 14.sp,
        lineHeight    = 18.sp,
        letterSpacing = 0.5.sp,
    )

    /** Score in leaderboard row */
    val LeaderboardScore = TextStyle(
        fontFamily    = CinzelFontFamily,
        fontWeight    = FontWeight.Bold,
        fontSize      = 16.sp,
        lineHeight    = 20.sp,
        letterSpacing = 0.5.sp,
        textAlign     = TextAlign.End,
    )

    // ── BADGE / CHIP ─────────────────────────────────────────────

    /** Badge text — "VICTORY", "6×6", "DEVIL" */
    val BadgeText = TextStyle(
        fontFamily    = CinzelFontFamily,
        fontWeight    = FontWeight.Normal,
        fontSize      = 9.sp,
        lineHeight    = 12.sp,
        letterSpacing = 2.sp,
        textAlign     = TextAlign.Center,
    )

    /** Difficulty chip — "EASY", "HARD", "DEVIL MODE" */
    val DifficultyChip = TextStyle(
        fontFamily    = CinzelFontFamily,
        fontWeight    = FontWeight.Normal,
        fontSize      = 10.sp,
        lineHeight    = 13.sp,
        letterSpacing = 2.sp,
        textAlign     = TextAlign.Center,
    )

    // ── NAVIGATION ───────────────────────────────────────────────

    /** Bottom nav bar label — "BOARD", "UNDO", "HINT" */
    val NavLabel = TextStyle(
        fontFamily    = CinzelFontFamily,
        fontWeight    = FontWeight.Normal,
        fontSize      = 8.sp,
        lineHeight    = 10.sp,
        letterSpacing = 1.5.sp,
        textAlign     = TextAlign.Center,
    )

    /** Tab row label — "GLOBAL", "FRIENDS", "MY BESTS" */
    val TabLabel = TextStyle(
        fontFamily    = CinzelFontFamily,
        fontWeight    = FontWeight.Normal,
        fontSize      = 10.sp,
        lineHeight    = 13.sp,
        letterSpacing = 2.sp,
        textAlign     = TextAlign.Center,
    )

    // ── ROOM CODE ────────────────────────────────────────────────

    /** Online lobby room code — "429 813" */
    val RoomCode = TextStyle(
        fontFamily    = RajdhaniFontFamily,
        fontWeight    = FontWeight.Bold,
        fontSize      = 38.sp,
        lineHeight    = 44.sp,
        letterSpacing = 8.sp,
        textAlign     = TextAlign.Center,
    )
}

// ═══════════════════════════════════════════════════════════════
//  MATERIAL3 TYPOGRAPHY MAPPING
//  Passed into MaterialTheme(typography = KnightTourTypography)
// ═══════════════════════════════════════════════════════════════

val KnightTourTypography = Typography(

    // Display styles — Cinzel Decorative (max drama)
    displayLarge = TextStyle(
        fontFamily    = CinzelDecorativeFontFamily,
        fontWeight    = FontWeight.Black,
        fontSize      = 57.sp,
        lineHeight    = 64.sp,
        letterSpacing = (-0.25).sp,
    ),
    displayMedium = TextStyle(
        fontFamily    = CinzelDecorativeFontFamily,
        fontWeight    = FontWeight.Bold,
        fontSize      = 45.sp,
        lineHeight    = 52.sp,
        letterSpacing = 0.sp,
    ),
    displaySmall = TextStyle(
        fontFamily    = CinzelDecorativeFontFamily,
        fontWeight    = FontWeight.Bold,
        fontSize      = 36.sp,
        lineHeight    = 44.sp,
        letterSpacing = 0.sp,
    ),

    // Headline styles — Cinzel
    headlineLarge = TextStyle(
        fontFamily    = CinzelFontFamily,
        fontWeight    = FontWeight.Bold,
        fontSize      = 32.sp,
        lineHeight    = 40.sp,
        letterSpacing = 1.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily    = CinzelFontFamily,
        fontWeight    = FontWeight.SemiBold,
        fontSize      = 28.sp,
        lineHeight    = 36.sp,
        letterSpacing = 1.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily    = CinzelFontFamily,
        fontWeight    = FontWeight.SemiBold,
        fontSize      = 24.sp,
        lineHeight    = 32.sp,
        letterSpacing = 0.5.sp,
    ),

    // Title styles — Cinzel
    titleLarge = TextStyle(
        fontFamily    = CinzelFontFamily,
        fontWeight    = FontWeight.SemiBold,
        fontSize      = 22.sp,
        lineHeight    = 28.sp,
        letterSpacing = 0.5.sp,
    ),
    titleMedium = TextStyle(
        fontFamily    = CinzelFontFamily,
        fontWeight    = FontWeight.SemiBold,
        fontSize      = 16.sp,
        lineHeight    = 24.sp,
        letterSpacing = 1.sp,
    ),
    titleSmall = TextStyle(
        fontFamily    = CinzelFontFamily,
        fontWeight    = FontWeight.Normal,
        fontSize      = 14.sp,
        lineHeight    = 20.sp,
        letterSpacing = 1.sp,
    ),

    // Body styles — Rajdhani
    bodyLarge = TextStyle(
        fontFamily    = RajdhaniFontFamily,
        fontWeight    = FontWeight.Normal,
        fontSize      = 16.sp,
        lineHeight    = 24.sp,
        letterSpacing = 0.5.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily    = RajdhaniFontFamily,
        fontWeight    = FontWeight.Normal,
        fontSize      = 14.sp,
        lineHeight    = 20.sp,
        letterSpacing = 0.25.sp,
    ),
    bodySmall = TextStyle(
        fontFamily    = RajdhaniFontFamily,
        fontWeight    = FontWeight.Normal,
        fontSize      = 12.sp,
        lineHeight    = 16.sp,
        letterSpacing = 0.4.sp,
    ),

    // Label styles — Cinzel
    labelLarge = TextStyle(
        fontFamily    = CinzelFontFamily,
        fontWeight    = FontWeight.Bold,
        fontSize      = 14.sp,
        lineHeight    = 20.sp,
        letterSpacing = 3.sp,
    ),
    labelMedium = TextStyle(
        fontFamily    = CinzelFontFamily,
        fontWeight    = FontWeight.Normal,
        fontSize      = 12.sp,
        lineHeight    = 16.sp,
        letterSpacing = 2.sp,
    ),
    labelSmall = TextStyle(
        fontFamily    = CinzelFontFamily,
        fontWeight    = FontWeight.Normal,
        fontSize      = 9.sp,
        lineHeight    = 12.sp,
        letterSpacing = 2.sp,
    ),
)