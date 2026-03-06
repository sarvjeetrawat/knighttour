package com.kunpitech.knighttour.ui.screen.result

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.offset
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kunpitech.knighttour.domain.model.DefeatReason
import com.kunpitech.knighttour.ui.theme.AbyssBlack
import com.kunpitech.knighttour.ui.theme.BloodRedBright
import com.kunpitech.knighttour.ui.theme.BorderDefault
import com.kunpitech.knighttour.ui.theme.BorderSubtle
import com.kunpitech.knighttour.ui.theme.BorderWidth
import com.kunpitech.knighttour.ui.theme.CrownGold
import com.kunpitech.knighttour.ui.theme.DevilRed
import com.kunpitech.knighttour.ui.theme.GoldDim
import com.kunpitech.knighttour.ui.theme.KnightGold
import com.kunpitech.knighttour.ui.theme.KnightTourShapes
import com.kunpitech.knighttour.ui.theme.KnightTourTheme
import com.kunpitech.knighttour.ui.theme.OnlineTeal
import com.kunpitech.knighttour.ui.theme.RedDim
import com.kunpitech.knighttour.ui.theme.RoundedSmall
import com.kunpitech.knighttour.ui.theme.SurfaceDark
import com.kunpitech.knighttour.ui.theme.SurfaceElevated
import com.kunpitech.knighttour.ui.theme.SurfaceHighest
import com.kunpitech.knighttour.ui.theme.TextPrimary
import com.kunpitech.knighttour.ui.theme.TextSecondary
import com.kunpitech.knighttour.ui.theme.TextTertiary
import com.kunpitech.knighttour.ui.theme.VictoryGreen
import com.kunpitech.knighttour.ui.theme.WarningAmber
import com.kunpitech.knighttour.ui.theme.knightType
import kotlinx.coroutines.delay

// ═══════════════════════════════════════════════════════════════
//  KNIGHT TOUR — RESULT SCREEN
//
//  Layout (scrollable):
//  ┌──────────────────────────────────────┐
//  │  TopBar: close button                │
//  │  HeroSection: trophy/skull + outcome │
//  │  ScoreCard: animated score reveal    │
//  │  BreakdownCard: base·time·size·hint  │
//  │  RankCard: progress bar + label      │
//  │  [OnlineCard: vs opponent — online]  │
//  │  GameInfoRow: board · time · mode    │
//  │  ActionButtons: play again · home    │
//  └──────────────────────────────────────┘
//
//  Enter animation: staggered fade+slide per section (0–600ms)
//  Particle burst on victory
// ═══════════════════════════════════════════════════════════════

@Composable
fun ResultScreen(
    uiState  : ResultUiState,
    onEvent  : (ResultEvent) -> Unit,
) {
    // ── STAGGERED ENTER ANIMATION ────────────────────────────────
    val heroAlpha    = remember { Animatable(0f) }
    val heroScale    = remember { Animatable(0.85f) }
    val scoreAlpha   = remember { Animatable(0f) }
    val scoreOffset  = remember { Animatable(24f) }
    val breakAlpha   = remember { Animatable(0f) }
    val breakOffset  = remember { Animatable(24f) }
    val rankAlpha    = remember { Animatable(0f) }
    val rankOffset   = remember { Animatable(24f) }
    val actionsAlpha = remember { Animatable(0f) }
    val actionsScale = remember { Animatable(0.9f) }

    LaunchedEffect(Unit) {
        // Hero burst
        heroAlpha.animateTo(1f, tween(500, easing = EaseOut))
        heroScale.animateTo(1f, tween(600, easing = EaseOutBack))

        delay(100)
        // Score card
        scoreAlpha.animateTo(1f, tween(400, easing = EaseOut))
        scoreOffset.animateTo(0f, tween(400, easing = EaseOutCubic))

        delay(80)
        // Breakdown card
        breakAlpha.animateTo(1f, tween(400, easing = EaseOut))
        breakOffset.animateTo(0f, tween(400, easing = EaseOutCubic))

        delay(80)
        // Rank card
        rankAlpha.animateTo(1f, tween(400, easing = EaseOut))
        rankOffset.animateTo(0f, tween(400, easing = EaseOutCubic))

        delay(100)
        // Action buttons
        actionsAlpha.animateTo(1f, tween(350, easing = EaseOut))
        actionsScale.animateTo(1f, tween(400, easing = EaseOutBack))
    }

    // Rank bar animated progress
    val animRankProg by animateFloatAsState(
        targetValue   = uiState.rankProgress,
        animationSpec = tween(1200, 600, EaseOutCubic),
        label         = "rankProg",
    )

    // Particle pulse for victory
    val infiniteTransition = rememberInfiniteTransition(label = "result_inf")
    val particlePulse by infiniteTransition.animateFloat(
        initialValue  = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(1600, easing = EaseInOutSine), RepeatMode.Reverse
        ),
        label = "particlePulse",
    )

    val didWin = if (uiState.isOnlineMode) uiState.didWinOnline else uiState.isVictory
    val accentColor = if (didWin) CrownGold else BloodRedBright

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AbyssBlack),
    ) {

        // ── BACKGROUND ───────────────────────────────────────────
        ResultBackground(
            modifier    = Modifier.fillMaxSize(),
            isVictory   = didWin,
            pulse       = particlePulse,
        )

        // ── SCROLLABLE CONTENT ───────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .systemBarsPadding()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            Spacer(Modifier.height(12.dp))

            // ── CLOSE BUTTON ──────────────────────────────────────
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(SurfaceElevated, KnightTourShapes.extraSmall)
                        .border(BorderWidth.thin, BorderSubtle, KnightTourShapes.extraSmall)
                        .clickable { onEvent(ResultEvent.GoHome) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("✕", fontSize = 14.sp, color = TextSecondary)
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── HERO SECTION ──────────────────────────────────────
            HeroSection(
                uiState      = uiState,
                accentColor  = accentColor,
                pulse        = particlePulse,
                modifier     = Modifier
                    .alpha(heroAlpha.value)
                    .scale(heroScale.value),
            )

            Spacer(Modifier.height(28.dp))

            // ── SCORE CARD ────────────────────────────────────────
            ScoreCard(
                uiState     = uiState,
                accentColor = accentColor,
                modifier    = Modifier
                    .fillMaxWidth()
                    .alpha(scoreAlpha.value)
                    .offset(y = scoreOffset.value.dp),
            )

            Spacer(Modifier.height(12.dp))

            // ── SCORE BREAKDOWN ───────────────────────────────────
            BreakdownCard(
                uiState  = uiState,
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(breakAlpha.value)
                    .offset(y = breakOffset.value.dp),
            )

            Spacer(Modifier.height(12.dp))

            // ── RANK PROGRESS ─────────────────────────────────────
            RankCard(
                uiState      = uiState,
                rankProgress = animRankProg,
                modifier     = Modifier
                    .fillMaxWidth()
                    .alpha(rankAlpha.value)
                    .offset(y = rankOffset.value.dp),
            )

            Spacer(Modifier.height(12.dp))

            // ── ONLINE MATCH RESULT ───────────────────────────────
            if (uiState.isOnlineMode) {
                OnlineResultCard(
                    uiState  = uiState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(rankAlpha.value),
                )
                Spacer(Modifier.height(12.dp))
            }

            // ── GAME INFO ROW ─────────────────────────────────────
            GameInfoRow(
                uiState  = uiState,
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(rankAlpha.value),
            )

            Spacer(Modifier.height(24.dp))

            // ── ACTION BUTTONS ────────────────────────────────────
            ActionButtons(
                uiState     = uiState,
                accentColor = accentColor,
                onEvent     = onEvent,
                modifier    = Modifier
                    .fillMaxWidth()
                    .alpha(actionsAlpha.value)
                    .scale(actionsScale.value),
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  BACKGROUND
// ═══════════════════════════════════════════════════════════════

@Composable
private fun ResultBackground(
    modifier  : Modifier,
    isVictory : Boolean,
    pulse     : Float,
) {
    val topColor = if (isVictory)
        KnightGold.copy(alpha = 0.07f * pulse)
    else
        DevilRed.copy(alpha = 0.06f * pulse)

    Canvas(modifier = modifier) {
        // Grid
        val gridColor = KnightGold.copy(alpha = 0.025f)
        val cell = 38.dp.toPx()
        var x = 0f
        while (x <= size.width) {
            drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), 1f); x += cell
        }
        var y = 0f
        while (y <= size.height) {
            drawLine(gridColor, Offset(0f, y), Offset(size.width, y), 1f); y += cell
        }
        // Top glow
        drawRect(
            brush  = Brush.verticalGradient(
                listOf(topColor, Color.Transparent),
                startY = 0f, endY = size.height * 0.45f,
            ),
        )
        // Bottom fade
        drawRect(
            brush  = Brush.verticalGradient(
                listOf(Color.Transparent, AbyssBlack.copy(alpha = 0.5f)),
                startY = size.height * 0.7f, endY = size.height,
            ),
        )
    }
}

// ═══════════════════════════════════════════════════════════════
//  HERO SECTION
// ═══════════════════════════════════════════════════════════════

@Composable
private fun HeroSection(
    uiState     : ResultUiState,
    accentColor : Color,
    pulse       : Float,
    modifier    : Modifier = Modifier,
) {
    val didWin = if (uiState.isOnlineMode) uiState.didWinOnline else uiState.isVictory
    Column(
        modifier            = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Icon with glow
        Box(
            modifier         = Modifier.size(110.dp),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f; val cy = size.height / 2f
                drawCircle(
                    brush  = Brush.radialGradient(
                        listOf(accentColor.copy(alpha = 0.22f * pulse), Color.Transparent),
                        center = Offset(cx, cy), radius = 55.dp.toPx(),
                    ),
                    radius = 55.dp.toPx(), center = Offset(cx, cy),
                )
                drawCircle(
                    color  = accentColor.copy(alpha = 0.3f * pulse),
                    radius = 46.dp.toPx(), center = Offset(cx, cy),
                    style  = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()),
                )
            }
            Text(
                text     = if (didWin) "🏆" else "💀",
                fontSize = 58.sp,
                modifier = Modifier.scale(0.95f + 0.05f * pulse),
            )
        }

        // Outcome label
        Text(
            text  = when {
                uiState.isOnlineMode && uiState.didWinOnline  -> if (uiState.isVictory) "CHAMPION" else "YOU WIN"
                uiState.isOnlineMode && !uiState.didWinOnline -> "YOU LOSE"
                uiState.isVictory -> "VICTORY"
                uiState.defeatReason == DefeatReason.TIME_UP -> "TIME'S UP"
                else -> "DEFEATED"
            },
            style = MaterialTheme.knightType.DisplayBanner.copy(
                fontSize      = 36.sp,
                letterSpacing = 4.sp,
            ),
            color     = accentColor,
            textAlign = TextAlign.Center,
        )

        // Subtitle
        Text(
            text  = when {
                uiState.isVictory -> "PERFECT KNIGHT TOUR — ${uiState.difficulty}"
                uiState.defeatReason == DefeatReason.TIME_UP ->
                    "${uiState.squaresVisited} of ${uiState.totalSquares} squares"
                else ->
                    "NO VALID MOVES — ${uiState.squaresVisited} of ${uiState.totalSquares}"
            },
            style = MaterialTheme.knightType.Eyebrow.copy(letterSpacing = 3.sp),
            color = TextTertiary,
            textAlign = TextAlign.Center,
        )

        // Personal best badge
        if (uiState.isNewPersonalBest) {
            Box(
                modifier = Modifier
                    .background(GoldDim, KnightTourShapes.extraSmall)
                    .border(BorderWidth.thin, KnightGold.copy(alpha = 0.5f), KnightTourShapes.extraSmall)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            ) {
                Text(
                    text  = "✦  NEW PERSONAL BEST",
                    style = MaterialTheme.knightType.BadgeText.copy(letterSpacing = 3.sp),
                    color = CrownGold,
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  SCORE CARD
// ═══════════════════════════════════════════════════════════════

@Composable
private fun ScoreCard(
    uiState     : ResultUiState,
    accentColor : Color,
    modifier    : Modifier = Modifier,
) {
    // Animated score count-up
    val animScore = remember { Animatable(0f) }
    LaunchedEffect(uiState.totalScore) {
        delay(300)
        animScore.animateTo(
            uiState.totalScore.toFloat(),
            tween(1200, easing = EaseOutCubic),
        )
    }

    Box(
        modifier = modifier
            .background(
                Brush.linearGradient(
                    listOf(
                        accentColor.copy(alpha = 0.08f),
                        SurfaceDark,
                        SurfaceElevated,
                    )
                ),
                KnightTourShapes.medium,
            )
            .border(BorderWidth.default, accentColor.copy(alpha = 0.35f), KnightTourShapes.medium)
            .padding(24.dp),
    ) {
        // Decorative corner
        Canvas(modifier = Modifier.size(50.dp).align(Alignment.TopEnd)) {
            drawPath(
                path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(size.width, 0f)
                    lineTo(0f, 0f)
                    lineTo(size.width, size.height)
                    close()
                },
                color = accentColor.copy(alpha = 0.07f),
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text  = "FINAL SCORE",
                style = MaterialTheme.knightType.Eyebrow.copy(letterSpacing = 5.sp),
                color = TextTertiary,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text  = animScore.value.toInt().toString(),
                style = MaterialTheme.knightType.GameTitle.copy(
                    fontSize      = 52.sp,
                    letterSpacing = 2.sp,
                ),
                color     = accentColor,
                textAlign = TextAlign.Center,
                modifier  = Modifier.fillMaxWidth(),
            )

            // Compare to personal best
            if (!uiState.isNewPersonalBest && uiState.personalBest > 0) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text  = "Personal best: ${uiState.personalBest}",
                    style = MaterialTheme.knightType.Caption,
                    color = TextTertiary,
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  SCORE BREAKDOWN CARD
// ═══════════════════════════════════════════════════════════════

@Composable
private fun BreakdownCard(
    uiState  : ResultUiState,
    modifier : Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(SurfaceDark, KnightTourShapes.medium)
            .border(BorderWidth.thin, BorderDefault, KnightTourShapes.medium)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text  = "SCORE BREAKDOWN",
            style = MaterialTheme.knightType.Eyebrow.copy(letterSpacing = 4.sp),
            color = TextTertiary,
        )
        Spacer(Modifier.height(2.dp))

        BreakdownRow(
            label  = "Base  (${uiState.moveCount} × 100)",
            value  = "+${uiState.baseScore}",
            color  = TextPrimary,
            delay  = 400,
        )
        BreakdownRow(
            label  = "Time bonus  (+${uiState.timeLimitSeconds - uiState.elapsedSeconds}s)",
            value  = "+${uiState.timeBonus}",
            color  = VictoryGreen,
            delay  = 500,
        )
        BreakdownRow(
            label  = "Board size  (${uiState.boardSize}×${uiState.boardSize})",
            value  = "+${uiState.sizeBonus}",
            color  = OnlineTeal,
            delay  = 600,
        )
        if (uiState.hintPenalty > 0) {
            BreakdownRow(
                label = "Hint penalty",
                value = "−${uiState.hintPenalty}",
                color = WarningAmber,
                delay = 700,
            )
        }

        // Divider
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, BorderDefault, Color.Transparent)
                    )
                )
        )

        // Total row
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Text(
                text  = "TOTAL",
                style = MaterialTheme.knightType.StatLabel.copy(letterSpacing = 3.sp),
                color = TextSecondary,
            )
            Text(
                text  = uiState.totalScore.toString(),
                style = MaterialTheme.knightType.CardTitle,
                color = CrownGold,
            )
        }
    }
}

@Composable
private fun BreakdownRow(
    label : String,
    value : String,
    color : Color,
    delay : Int,
) {
    val alpha = remember { Animatable(0f) }
    val offsetX = remember { Animatable(-12f) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(delay.toLong())
        alpha.animateTo(1f, tween(300))
        offsetX.animateTo(0f, tween(300, easing = EaseOutCubic))
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha.value)
            .offset(x = offsetX.value.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        Text(
            text  = label,
            style = MaterialTheme.knightType.BodySecondary,
            color = TextSecondary,
        )
        Text(
            text  = value,
            style = MaterialTheme.knightType.BodySecondary.copy(
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
            ),
            color = color,
        )
    }
}

// ═══════════════════════════════════════════════════════════════
//  RANK CARD
// ═══════════════════════════════════════════════════════════════

@Composable
private fun RankCard(
    uiState      : ResultUiState,
    rankProgress : Float,
    modifier     : Modifier = Modifier,
) {
    val rankColors = mapOf(
        "NOVICE"      to TextTertiary,
        "SQUIRE"      to OnlineTeal,
        "KNIGHT"      to KnightGold,
        "MASTER"      to CrownGold,
        "GRANDMASTER" to BloodRedBright,
    )
    val rankColor = rankColors[uiState.rankLabel] ?: KnightGold
    val rankIcon  = when (uiState.rankLabel) {
        "NOVICE"      -> "⚔"
        "SQUIRE"      -> "🛡"
        "KNIGHT"      -> "♞"
        "MASTER"      -> "👑"
        "GRANDMASTER" -> "😈"
        else          -> "♞"
    }

    Column(
        modifier = modifier
            .background(SurfaceDark, KnightTourShapes.medium)
            .border(BorderWidth.thin, rankColor.copy(alpha = 0.3f), KnightTourShapes.medium)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text  = "RANK",
                    style = MaterialTheme.knightType.Eyebrow.copy(letterSpacing = 4.sp),
                    color = TextTertiary,
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(rankIcon, fontSize = 22.sp)
                    Text(
                        text  = uiState.rankLabel,
                        style = MaterialTheme.knightType.CardTitle.copy(fontSize = 18.sp),
                        color = rankColor,
                    )
                }
            }

            // Rank score chip
            Box(
                modifier = Modifier
                    .background(rankColor.copy(alpha = 0.1f), KnightTourShapes.extraSmall)
                    .border(BorderWidth.thin, rankColor.copy(alpha = 0.3f), KnightTourShapes.extraSmall)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text(
                    text  = "${uiState.totalScore} pts",
                    style = MaterialTheme.knightType.DifficultyChip,
                    color = rankColor,
                )
            }
        }

        // Progress bar
        val nextRank = when (uiState.rankLabel) {
            "NOVICE"  -> "SQUIRE (1500)"
            "SQUIRE"  -> "KNIGHT (3000)"
            "KNIGHT"  -> "MASTER (5000)"
            "MASTER"  -> "GRANDMASTER (8000)"
            else      -> "MAX RANK"
        }

        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text  = uiState.rankLabel,
                    style = MaterialTheme.knightType.StatLabel,
                    color = rankColor,
                )
                Text(
                    text  = "NEXT: $nextRank",
                    style = MaterialTheme.knightType.StatLabel,
                    color = TextTertiary,
                )
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(BorderSubtle, RoundedSmall),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(rankProgress)
                        .height(6.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(rankColor.copy(alpha = 0.7f), rankColor)
                            ),
                            RoundedSmall,
                        ),
                )
            }
            Text(
                text  = "${(rankProgress * 100).toInt()}% to next rank",
                style = MaterialTheme.knightType.Caption,
                color = TextTertiary,
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  ONLINE RESULT CARD
// ═══════════════════════════════════════════════════════════════

@Composable
private fun OnlineResultCard(
    uiState  : ResultUiState,
    modifier : Modifier = Modifier,
) {
    val winColor = if (uiState.didWinOnline) VictoryGreen else BloodRedBright
    val winLabel = if (uiState.didWinOnline) "YOU WIN" else "YOU LOSE"

    Row(
        modifier = modifier
            .background(SurfaceDark, KnightTourShapes.medium)
            .border(BorderWidth.thin, winColor.copy(alpha = 0.35f), KnightTourShapes.medium)
            .padding(16.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text  = "ONLINE MATCH",
                style = MaterialTheme.knightType.Eyebrow.copy(letterSpacing = 3.sp),
                color = TextTertiary,
            )
            Text(
                text  = "vs ${uiState.opponentName.uppercase()}",
                style = MaterialTheme.knightType.CardTitle,
                color = OnlineTeal,
            )
            Text(
                text  = "Their score: ${uiState.opponentScore}",
                style = MaterialTheme.knightType.Caption,
                color = TextTertiary,
            )
        }
        Box(
            modifier = Modifier
                .background(winColor.copy(alpha = 0.12f), KnightTourShapes.small)
                .border(BorderWidth.thin, winColor.copy(alpha = 0.4f), KnightTourShapes.small)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text  = winLabel,
                style = MaterialTheme.knightType.ButtonSecondary,
                color = winColor,
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  GAME INFO ROW
// ═══════════════════════════════════════════════════════════════

@Composable
private fun GameInfoRow(
    uiState  : ResultUiState,
    modifier : Modifier = Modifier,
) {
    val timeStr = "%d:%02d".format(
        uiState.elapsedSeconds / 60,
        uiState.elapsedSeconds % 60,
    )

    Row(
        modifier = modifier
            .background(SurfaceHighest, KnightTourShapes.small)
            .border(BorderWidth.thin, BorderSubtle, KnightTourShapes.small)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        InfoChip(icon = "⊞", label = uiState.difficulty, sublabel = "BOARD")
        InfoDivider()
        InfoChip(icon = "⏱", label = timeStr, sublabel = "TIME")
        InfoDivider()
        InfoChip(
            icon     = if (uiState.gameMode == "ONLINE") "🌐" else "♞",
            label    = uiState.gameMode,
            sublabel = "MODE",
        )
    }
}

@Composable
private fun InfoChip(icon: String, label: String, sublabel: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(icon, fontSize = 16.sp)
        Spacer(Modifier.height(3.dp))
        Text(
            text  = label,
            style = MaterialTheme.knightType.DifficultyChip,
            color = TextSecondary,
        )
        Text(
            text  = sublabel,
            style = MaterialTheme.knightType.StatLabel,
            color = TextTertiary,
        )
    }
}

@Composable
private fun InfoDivider() {
    Box(Modifier.width(1.dp).height(36.dp).background(BorderSubtle))
}

// ═══════════════════════════════════════════════════════════════
//  ACTION BUTTONS
// ═══════════════════════════════════════════════════════════════

@Composable
private fun ActionButtons(
    uiState     : ResultUiState,
    accentColor : Color,
    onEvent     : (ResultEvent) -> Unit,
    modifier    : Modifier = Modifier,
) {
    Column(
        modifier            = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Primary: Play Again
        val rematchState    = uiState.rematchState
        val isWaiting       = rematchState == RematchState.WAITING
        val isStarting      = rematchState == RematchState.STARTING
        val opponentName    = uiState.opponentName.ifEmpty { "Opponent" }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isWaiting) 64.dp else 54.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(accentColor.copy(alpha = 0.18f), accentColor.copy(alpha = 0.08f))
                    ),
                    KnightTourShapes.medium,
                )
                .border(BorderWidth.default, accentColor.copy(alpha = if (isWaiting) 0.3f else 0.55f), KnightTourShapes.medium)
                .clickable(enabled = !isWaiting && !isStarting) {
                    onEvent(ResultEvent.PlayAgain)
                },
            contentAlignment = Alignment.Center,
        ) {
            when {
                isStarting -> {
                    // Both agreed — about to navigate
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CircularProgressIndicator(
                            modifier    = Modifier.size(14.dp),
                            color       = accentColor,
                            strokeWidth = 2.dp,
                        )
                        Text(
                            text  = "STARTING REMATCH...",
                            style = MaterialTheme.knightType.ButtonPrimary,
                            color = accentColor,
                            fontSize = 13.sp,
                        )
                    }
                }
                isWaiting -> {
                    // Waiting for opponent to also tap Play Again
                    val pulse by rememberInfiniteTransition(label = "rematch")
                        .animateFloat(
                            initialValue  = 0.4f,
                            targetValue   = 1f,
                            animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
                            label         = "rematch",
                        )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text  = "WAITING FOR ${opponentName.uppercase()}...",
                            style = MaterialTheme.knightType.ButtonPrimary,
                            color = accentColor.copy(alpha = pulse),
                            fontSize = 12.sp,
                        )
                        Text(
                            text  = "Waiting for them to tap Play Again",
                            color = accentColor.copy(alpha = 0.45f),
                            fontSize = 10.sp,
                        )
                    }
                }
                else -> {
                    Text(
                        text  = "PLAY AGAIN",
                        style = MaterialTheme.knightType.ButtonPrimary,
                        color = accentColor,
                    )
                }
            }
        }

        // Secondary row: Home/Exit + Leaderboard
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (uiState.isOnlineMode) {
                // Online: EXIT GAME disconnects cleanly from Firebase
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .background(SurfaceDark, KnightTourShapes.medium)
                        .border(BorderWidth.thin, Color(0xFFE05050).copy(alpha = 0.4f), KnightTourShapes.medium)
                        .clickable { onEvent(ResultEvent.ExitGame) },
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text("✕", fontSize = 13.sp, color = Color(0xFFE05050).copy(alpha = 0.8f))
                        Text(
                            text  = "EXIT GAME",
                            style = MaterialTheme.knightType.ButtonSecondary,
                            color = Color(0xFFE05050).copy(alpha = 0.8f),
                        )
                    }
                }
            } else {
                // Offline: plain HOME button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .background(SurfaceDark, KnightTourShapes.medium)
                        .border(BorderWidth.thin, BorderDefault, KnightTourShapes.medium)
                        .clickable { onEvent(ResultEvent.GoHome) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text  = "HOME",
                        style = MaterialTheme.knightType.ButtonSecondary,
                        color = TextSecondary,
                    )
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .background(GoldDim, KnightTourShapes.medium)
                    .border(BorderWidth.thin, KnightGold.copy(alpha = 0.35f), KnightTourShapes.medium)
                    .clickable { onEvent(ResultEvent.OpenLeaderboard) },
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text("🏆", fontSize = 14.sp)
                    Text(
                        text  = "RANKS",
                        style = MaterialTheme.knightType.ButtonSecondary,
                        color = KnightGold,
                    )
                }
            }
        }

        // Share button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .background(SurfaceElevated, KnightTourShapes.small)
                .border(BorderWidth.thin, BorderSubtle, KnightTourShapes.small)
                .clickable { onEvent(ResultEvent.ShareResult) },
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("↗", fontSize = 14.sp, color = TextTertiary)
                Text(
                    text  = "SHARE RESULT",
                    style = MaterialTheme.knightType.ButtonSecondary,
                    color = TextTertiary,
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  ROUTE ENTRY POINT
// ═══════════════════════════════════════════════════════════════

@Composable
fun ResultRoute(
    onPlayAgainOffline : () -> Unit,
    onStartRematch     : (roomCode: String) -> Unit,
    onGoHome           : () -> Unit,
    onOpenLeaderboard  : () -> Unit,
    viewModel          : ResultViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ResultScreen(
        uiState = uiState,
        onEvent = { event ->
            when (event) {
                ResultEvent.PlayAgain -> {
                    if (uiState.isOnlineMode) {
                        viewModel.requestRematch { newRoomCode, _ ->
                            onStartRematch(newRoomCode)
                        }
                    } else {
                        onPlayAgainOffline()
                    }
                }
                ResultEvent.ExitGame        -> viewModel.exitGame { onGoHome() }
                ResultEvent.GoHome          -> onGoHome()
                ResultEvent.OpenLeaderboard -> onOpenLeaderboard()
                ResultEvent.ShareResult     -> { /* TODO: Android share sheet */ }
            }
        },
    )
}

// ═══════════════════════════════════════════════════════════════
//  PREVIEWS
// ═══════════════════════════════════════════════════════════════

@Preview(
    name = "Result — Victory",
    showBackground = true, backgroundColor = 0xFF050508,
    widthDp = 360, heightDp = 800,
)
@Composable
private fun PreviewVictory() {
    KnightTourTheme {
        ResultScreen(
            uiState = ResultUiState(
                isVictory     = true,
                difficulty    = "6×6",
                gameMode      = "OFFLINE",
                squaresVisited= 36,
                totalSquares  = 36,
                moveCount     = 36,
                elapsedSeconds= 94,
                timeLimitSeconds = 240,
                baseScore     = 3600,
                timeBonus     = 1168,
                sizeBonus     = 180,
                hintPenalty   = 0,
                totalScore    = 4948,
                rankLabel     = "KNIGHT",
                rankProgress  = 0.65f,
                isNewPersonalBest = false,
                personalBest  = 5200,
            ),
            onEvent = {},
        )
    }
}

@Preview(
    name = "Result — New PB",
    showBackground = true, backgroundColor = 0xFF050508,
    widthDp = 360, heightDp = 800,
)
@Composable
private fun PreviewPersonalBest() {
    KnightTourTheme {
        ResultScreen(
            uiState = ResultUiState(
                isVictory         = true,
                difficulty        = "8×8",
                gameMode          = "OFFLINE",
                squaresVisited    = 64,
                totalSquares      = 64,
                moveCount         = 64,
                elapsedSeconds    = 112,
                timeLimitSeconds  = 180,
                baseScore         = 6400,
                timeBonus         = 544,
                sizeBonus         = 256,
                hintPenalty       = 0,
                totalScore        = 7200,
                rankLabel         = "MASTER",
                rankProgress      = 0.44f,
                isNewPersonalBest = true,
                personalBest      = 7200,
            ),
            onEvent = {},
        )
    }
}

@Preview(
    name = "Result — Defeated",
    showBackground = true, backgroundColor = 0xFF050508,
    widthDp = 360, heightDp = 800,
)
@Composable
private fun PreviewDefeated() {
    KnightTourTheme {
        ResultScreen(
            uiState = ResultUiState(
                isVictory      = false,
                defeatReason   = DefeatReason.TIME_UP,
                difficulty     = "DEVIL 10×10",
                gameMode       = "DEVIL",
                squaresVisited = 61,
                totalSquares   = 100,
                moveCount      = 61,
                elapsedSeconds = 90,
                timeLimitSeconds = 90,
                baseScore      = 6100,
                timeBonus      = 0,
                sizeBonus      = 400,
                hintPenalty    = 0,
                totalScore     = 6500,
                rankLabel      = "MASTER",
                rankProgress   = 0.3f,
            ),
            onEvent = {},
        )
    }
}

@Preview(
    name = "Result — Online Win",
    showBackground = true, backgroundColor = 0xFF050508,
    widthDp = 360, heightDp = 800,
)
@Composable
private fun PreviewOnlineWin() {
    KnightTourTheme {
        ResultScreen(
            uiState = ResultUiState(
                isVictory      = true,
                difficulty     = "6×6",
                gameMode       = "ONLINE",
                squaresVisited = 36,
                totalSquares   = 36,
                moveCount      = 36,
                elapsedSeconds = 78,
                timeLimitSeconds = 240,
                baseScore      = 3600,
                timeBonus      = 1296,
                sizeBonus      = 180,
                totalScore     = 5076,
                rankLabel      = "MASTER",
                rankProgress   = 0.015f,
                isOnlineMode   = true,
                opponentName   = "Devil_King",
                opponentScore  = 4200,
                didWinOnline   = true,
            ),
            onEvent = {},
        )
    }
}