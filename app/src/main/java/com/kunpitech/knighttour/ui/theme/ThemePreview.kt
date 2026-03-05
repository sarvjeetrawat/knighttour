package com.kunpitech.knighttour.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

// ═══════════════════════════════════════════════════════════════
//  KNIGHT TOUR — THEME PREVIEW
//  Run this in Android Studio to visually inspect all tokens.
// ═══════════════════════════════════════════════════════════════

@Preview(
    name          = "Knight Tour — Full Theme Preview",
    showBackground = true,
    backgroundColor = 0xFF050508,
    widthDp        = 400,
    heightDp       = 900,
)
@Composable
private fun ThemePreview() {
    KnightTourTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.extendedColors.abyssBlack)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {

            // ── TYPOGRAPHY SAMPLES ───────────────────────────────
            Section("TYPOGRAPHY")

            Text("KNIGHT TOUR",
                style = MaterialTheme.knightType.GameTitle,
                color = MaterialTheme.extendedColors.crownGold)

            Text("VICTORY",
                style = MaterialTheme.knightType.DisplayBanner,
                color = MaterialTheme.extendedColors.crownGold)

            Text("LEADERBOARD",
                style = MaterialTheme.knightType.ScreenHeader,
                color = MaterialTheme.extendedColors.textSecondary)

            Text("Move the knight across every square of the board exactly once.",
                style = MaterialTheme.knightType.BodyPrimary,
                color = MaterialTheme.extendedColors.textSecondary)

            Text("2:30",
                style = MaterialTheme.knightType.TimerNormal,
                color = MaterialTheme.extendedColors.crownGold)

            Text("0:08",
                style = MaterialTheme.knightType.TimerPanic,
                color = MaterialTheme.extendedColors.bloodRedBright)

            Text("MOVES  ·  TIME  ·  SCORE",
                style = MaterialTheme.knightType.StatLabel,
                color = MaterialTheme.extendedColors.textTertiary)

            Text("429 813",
                style = MaterialTheme.knightType.RoomCode,
                color = MaterialTheme.extendedColors.crownGold)

            // ── COLOR SWATCHES ───────────────────────────────────
            Section("GOLD PALETTE")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Swatch("Knight",  KnightGold)
                Swatch("Crown",   CrownGold)
                Swatch("Pale",    PaleGold)
                Swatch("Glow",    GoldGlow)
                Swatch("Dim",     GoldDim)
            }

            Section("DEVIL RED")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Swatch("Devil",   DevilRed)
                Swatch("Blood",   BloodRedBright)
                Swatch("Glow",    RedGlow)
                Swatch("Dim",     RedDim)
            }

            Section("STATUS")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Swatch("Victory", VictoryGreen)
                Swatch("Online",  OnlineTeal)
                Swatch("Warning", WarningAmber)
                Swatch("Devil",   DevilPurple)
            }

            Section("BOARD CELLS")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Swatch("Light",   CellLight)
                Swatch("Dark",    CellDark)
                Swatch("Visited", CellVisited)
                Swatch("Knight",  CellKnight)
                Swatch("Move",    CellValidMove)
            }

            Section("SURFACES")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Swatch("Abyss",   AbyssBlack)
                Swatch("Surface", SurfaceDark)
                Swatch("Elev.",   SurfaceElevated)
                Swatch("High",    SurfaceHighest)
            }

            // ── BUTTONS ──────────────────────────────────────────
            Section("BUTTONS")

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(KnightTourShapes.small)
                    .background(GoldDim)
                    .border(BorderWidth.default, KnightGold, KnightTourShapes.small),
                contentAlignment = Alignment.Center,
            ) {
                Text("BEGIN",
                    style = MaterialTheme.knightType.ButtonPrimary,
                    color = CrownGold)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(KnightTourShapes.small)
                    .background(Color.Transparent)
                    .border(BorderWidth.thin, BorderDefault, KnightTourShapes.small),
                contentAlignment = Alignment.Center,
            ) {
                Text("MENU",
                    style = MaterialTheme.knightType.ButtonSecondary,
                    color = TextSecondary)
            }

            // ── SHAPES ───────────────────────────────────────────
            Section("SHAPES")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "ExtraSm" to KnightTourShapes.extraSmall,
                    "Small"   to KnightTourShapes.small,
                    "Medium"  to KnightTourShapes.medium,
                    "Large"   to KnightTourShapes.large,
                ).forEach { (label, shape) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(shape)
                                .background(SurfaceElevated)
                                .border(BorderWidth.thin, BorderDefault, shape)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(label, style = MaterialTheme.knightType.Caption,
                            color = TextTertiary)
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun Section(title: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f).height(1.dp).background(BorderSubtle))
        Spacer(Modifier.width(8.dp))
        Text(title,
            style = MaterialTheme.knightType.Eyebrow,
            color = TextTertiary)
        Spacer(Modifier.width(8.dp))
        Box(Modifier.weight(1f).height(1.dp).background(BorderSubtle))
    }
}

@Composable
private fun Swatch(name: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(KnightTourShapes.extraSmall)
                .background(color)
                .border(BorderWidth.thin, BorderSubtle, KnightTourShapes.extraSmall)
        )
        Spacer(Modifier.height(4.dp))
        Text(name,
            style = MaterialTheme.knightType.Caption,
            color = TextTertiary)
    }
}