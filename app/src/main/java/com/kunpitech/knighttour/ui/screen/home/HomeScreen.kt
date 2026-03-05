package com.kunpitech.knighttour.ui.screen.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kunpitech.knighttour.ui.theme.*
import kotlinx.coroutines.delay

// ═══════════════════════════════════════════════════════════════
//  KNIGHT TOUR — HOME SCREEN
//
//  Layout (top → bottom):
//  ┌─────────────────────────────────────────┐
//  │  Top Bar: rank badge + settings icon    │
//  │  Hero: animated board + title           │
//  │  Stats Row: score / games / best time   │
//  │  [Resume Card] — if active game         │
//  │  Mode Cards: Offline / Online / Devil   │
//  │  Quick Actions: Leaderboard / Daily     │
//  └─────────────────────────────────────────┘
//
//  Enter animation: staggered fade+slide up per section
// ═══════════════════════════════════════════════════════════════

@Composable
fun HomeScreen(
    uiState    : HomeUiState,
    onEvent    : (HomeEvent) -> Unit,
) {
    // ── ENTER ANIMATION ─────────────────────────────────────────
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(80); visible = true }

    fun animSpec(delayMs: Int) = tween<Float>(500, delayMs, EaseOutCubic)

    val headerAlpha by animateFloatAsState(
        targetValue   = if (visible) 1f else 0f,
        animationSpec = animSpec(0), label = "header"
    )
    val heroAlpha by animateFloatAsState(
        targetValue   = if (visible) 1f else 0f,
        animationSpec = animSpec(120), label = "hero"
    )
    val heroOffset by animateFloatAsState(
        targetValue   = if (visible) 0f else 24f,
        animationSpec = animSpec(120), label = "heroOff"
    )
    val statsAlpha by animateFloatAsState(
        targetValue   = if (visible) 1f else 0f,
        animationSpec = animSpec(220), label = "stats"
    )
    val statsOffset by animateFloatAsState(
        targetValue   = if (visible) 0f else 24f,
        animationSpec = animSpec(220), label = "statsOff"
    )
    val cardsAlpha by animateFloatAsState(
        targetValue   = if (visible) 1f else 0f,
        animationSpec = animSpec(340), label = "cards"
    )
    val cardsOffset by animateFloatAsState(
        targetValue   = if (visible) 0f else 24f,
        animationSpec = animSpec(340), label = "cardsOff"
    )
    val actionsAlpha by animateFloatAsState(
        targetValue   = if (visible) 1f else 0f,
        animationSpec = animSpec(460), label = "actions"
    )

    // ── INFINITE ANIMATIONS ──────────────────────────────────────
    val infiniteTransition = rememberInfiniteTransition(label = "home_inf")

    val boardRotation by infiniteTransition.animateFloat(
        initialValue  = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(20000, easing = LinearEasing)),
        label         = "boardRot"
    )
    val glowPulse by infiniteTransition.animateFloat(
        initialValue  = 0.5f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000, easing = EaseInOutSine), RepeatMode.Reverse),
        label         = "glow"
    )
    val knightFloat by infiniteTransition.animateFloat(
        initialValue  = 0f, targetValue = 8f,
        animationSpec = infiniteRepeatable(tween(2400, easing = EaseInOutSine), RepeatMode.Reverse),
        label         = "float"
    )

    // ── SCROLL STATE ─────────────────────────────────────────────
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AbyssBlack)
            .systemBarsPadding(),
    ) {

        // ── BACKGROUND LAYERS ────────────────────────────────────
        HomeBgCanvas(
            modifier      = Modifier.fillMaxSize(),
            boardRotation = boardRotation,
            glowPulse     = glowPulse,
        )

        // ── SCROLLABLE CONTENT ───────────────────────────────────
        Column(
            modifier            = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            // ── TOP BAR ──────────────────────────────────────────
            HomeTopBar(
                uiState  = uiState,
                onEvent  = onEvent,
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(headerAlpha)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            )

            // ── HERO SECTION ─────────────────────────────────────
            HeroSection(
                uiState     = uiState,
                glowPulse   = glowPulse,
                knightFloat = knightFloat,
                modifier    = Modifier
                    .fillMaxWidth()
                    .alpha(heroAlpha)
                    .offset(y = heroOffset.dp)
                    .padding(top = 8.dp),
            )

            Spacer(Modifier.height(28.dp))

            // ── STATS ROW ────────────────────────────────────────
            StatsRow(
                uiState  = uiState,
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(statsAlpha)
                    .offset(y = statsOffset.dp)
                    .padding(horizontal = 20.dp),
            )

            Spacer(Modifier.height(24.dp))

            // ── RESUME CARD (conditional) ─────────────────────────
            if (uiState.hasActiveGame) {
                ResumeCard(
                    uiState  = uiState,
                    onResume = { onEvent(HomeEvent.ResumeGame) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(cardsAlpha)
                        .offset(y = cardsOffset.dp)
                        .padding(horizontal = 20.dp),
                )
                Spacer(Modifier.height(16.dp))
            }

            // ── DIVIDER ───────────────────────────────────────────
            SectionDivider(
                label    = "SELECT MODE",
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(cardsAlpha)
                    .padding(horizontal = 20.dp),
            )

            Spacer(Modifier.height(16.dp))

            // ── MODE CARDS ───────────────────────────────────────
            ModeCardsSection(
                uiState  = uiState,
                onEvent  = onEvent,
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(cardsAlpha)
                    .offset(y = cardsOffset.dp)
                    .padding(horizontal = 20.dp),
            )

            Spacer(Modifier.height(24.dp))

            // ── QUICK ACTIONS ─────────────────────────────────────
            QuickActionsRow(
                uiState  = uiState,
                onEvent  = onEvent,
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(actionsAlpha)
                    .padding(horizontal = 20.dp),
            )

            Spacer(Modifier.height(40.dp))
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  BACKGROUND CANVAS
// ═══════════════════════════════════════════════════════════════

@Composable
private fun HomeBgCanvas(
    modifier      : Modifier,
    boardRotation : Float,
    glowPulse     : Float,
) {
    Canvas(modifier = modifier) {
        // Fine grid
        val gridColor = KnightGold.copy(alpha = 0.03f)
        val cell      = 38.dp.toPx()
        var x = 0f; while (x <= size.width)  { drawLine(gridColor, Offset(x,0f), Offset(x,size.height), 1f); x += cell }
        var y = 0f; while (y <= size.height) { drawLine(gridColor, Offset(0f,y), Offset(size.width,y), 1f); y += cell }

        // Large rotating ring behind hero
        val cx   = size.width / 2f
        val topY = size.height * 0.28f
        val r    = 180.dp.toPx()

        rotate(boardRotation, Offset(cx, topY)) {
            drawCircle(
                color       = KnightGold.copy(alpha = 0.06f),
                radius      = r,
                center      = Offset(cx, topY),
                style       = Stroke(width = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx())))
            )
            // Tick marks
            for (i in 0 until 24) {
                val angle = Math.toRadians(i * 15.0 - 90.0)
                val inner = r - if (i % 6 == 0) 12.dp.toPx() else 6.dp.toPx()
                drawLine(
                    color       = KnightGold.copy(alpha = 0.15f),
                    start       = Offset(cx + (inner * Math.cos(angle)).toFloat(), topY + (inner * Math.sin(angle)).toFloat()),
                    end         = Offset(cx + (r * Math.cos(angle)).toFloat(), topY + (r * Math.sin(angle)).toFloat()),
                    strokeWidth = if (i % 6 == 0) 1.5.dp.toPx() else 0.7.dp.toPx()
                )
            }
        }

        // Central gold radial glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(KnightGold.copy(alpha = 0.09f * glowPulse), Color.Transparent),
                center = Offset(cx, topY), radius = 260.dp.toPx()
            ),
            radius = 260.dp.toPx(), center = Offset(cx, topY)
        )

        // Bottom gradient fade
        drawRect(
            brush = Brush.verticalGradient(
                colors      = listOf(Color.Transparent, AbyssBlack),
                startY      = size.height * 0.55f,
                endY        = size.height * 0.75f,
            ),
        )
    }
}

// ═══════════════════════════════════════════════════════════════
//  TOP BAR
// ═══════════════════════════════════════════════════════════════

@Composable
private fun HomeTopBar(
    uiState  : HomeUiState,
    onEvent  : (HomeEvent) -> Unit,
    modifier : Modifier = Modifier,
) {
    Row(
        modifier            = modifier,
        verticalAlignment   = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // Player rank badge
        Row(
            verticalAlignment   = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Avatar circle
            Box(
                modifier            = Modifier
                    .size(40.dp)
                    .border(1.dp, KnightGold.copy(alpha = 0.6f), CircleShape)
                    .background(SurfaceElevated, CircleShape),
                contentAlignment    = Alignment.Center,
            ) {
                Text("♞", fontSize = 20.sp)
            }

            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text  = uiState.playerName.uppercase(),
                    style = MaterialTheme.knightType.CardTitle.copy(fontSize = 13.sp),
                    color = TextPrimary,
                )
                // Rank badge
                Box(
                    modifier = Modifier
                        .background(GoldDim, KnightTourShapes.extraSmall)
                        .border(BorderWidth.thin, KnightGold.copy(alpha = 0.35f), KnightTourShapes.extraSmall)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text  = uiState.playerRank,
                        style = MaterialTheme.knightType.BadgeText,
                        color = KnightGold,
                    )
                }
            }
        }

        // Right side icons
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Online indicator
            if (uiState.isOnline) {
                OnlineDot()
            }
            // Settings
            IconButton24(
                icon    = "⚙",
                onClick = { onEvent(HomeEvent.OpenSettings) },
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  HERO SECTION
// ═══════════════════════════════════════════════════════════════

@Composable
private fun HeroSection(
    uiState     : HomeUiState,
    glowPulse   : Float,
    knightFloat : Float,
    modifier    : Modifier = Modifier,
) {
    Column(
        modifier            = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        // Knight piece with glow
        Box(
            modifier         = Modifier.size(140.dp),
            contentAlignment = Alignment.Center,
        ) {
            // Glow ring
            Canvas(modifier = Modifier.size(140.dp)) {
                val cx = size.width / 2f; val cy = size.height / 2f
                drawCircle(
                    brush  = Brush.radialGradient(
                        colors = listOf(KnightGold.copy(alpha = 0.18f * glowPulse), Color.Transparent),
                        center = Offset(cx, cy), radius = 70.dp.toPx()
                    ),
                    radius = 70.dp.toPx(), center = Offset(cx, cy)
                )
                // Inner ring
                drawCircle(
                    color  = KnightGold.copy(alpha = 0.25f * glowPulse),
                    radius = 54.dp.toPx(), center = Offset(cx, cy),
                    style  = Stroke(width = 1.dp.toPx())
                )
                // Outer ring
                drawCircle(
                    color  = KnightGold.copy(alpha = 0.10f),
                    radius = 66.dp.toPx(), center = Offset(cx, cy),
                    style  = Stroke(
                        width = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()))
                    )
                )
            }

            // Knight piece
            Text(
                text     = "♞",
                fontSize = 72.sp,
                modifier = Modifier
                    .offset(y = (-knightFloat).dp)
                    .graphicsLayer {
                        shadowElevation = 32f
                        ambientShadowColor = KnightGold
                        spotShadowColor    = KnightGold
                    },
                color = CrownGold,
            )
        }

        Spacer(Modifier.height(16.dp))

        // Title
        Text(
            text      = "KNIGHT",
            style     = MaterialTheme.knightType.GameTitle.copy(
                fontSize      = 42.sp,
                letterSpacing = 10.sp,
            ),
            color     = CrownGold,
            textAlign = TextAlign.Center,
        )
        Text(
            text      = "TOUR",
            style     = MaterialTheme.knightType.GameTitle.copy(
                fontSize      = 42.sp,
                letterSpacing = 18.sp,
            ),
            color     = TextPrimary,
            textAlign = TextAlign.Center,
            modifier  = Modifier.offset(y = (-4).dp),
        )

        Spacer(Modifier.height(10.dp))

        Text(
            text      = "THE DEVIL'S GAME",
            style     = MaterialTheme.knightType.Eyebrow.copy(letterSpacing = 5.sp),
            color     = DevilRed.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
        )
    }
}

// ═══════════════════════════════════════════════════════════════
//  STATS ROW
// ═══════════════════════════════════════════════════════════════

@Composable
private fun StatsRow(
    uiState  : HomeUiState,
    modifier : Modifier = Modifier,
) {
    Row(
        modifier              = modifier
            .background(SurfaceDark, KnightTourShapes.medium)
            .border(BorderWidth.thin, BorderDefault, KnightTourShapes.medium)
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        StatItem(value = uiState.playerScore.toString(), label = "SCORE")
        StatDivider()
        StatItem(value = uiState.gamesPlayed.toString(), label = "GAMES")
        StatDivider()
        StatItem(value = uiState.bestTime, label = "BEST")
        StatDivider()
        StatItem(value = "${uiState.winRate}%", label = "WINS")
    }
}

@Composable
private fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text  = value,
            style = MaterialTheme.knightType.StatDisplay.copy(fontSize = 22.sp),
            color = CrownGold,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text  = label,
            style = MaterialTheme.knightType.StatLabel,
            color = TextTertiary,
        )
    }
}

@Composable
private fun StatDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(36.dp)
            .background(BorderDefault)
    )
}

// ═══════════════════════════════════════════════════════════════
//  RESUME CARD
// ═══════════════════════════════════════════════════════════════

@Composable
private fun ResumeCard(
    uiState  : HomeUiState,
    onResume : () -> Unit,
    modifier : Modifier = Modifier,
) {
    val pulseAnim = rememberInfiniteTransition(label = "resume_pulse")
    val borderAlpha by pulseAnim.animateFloat(
        initialValue  = 0.3f, targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(1200, easing = EaseInOutSine), RepeatMode.Reverse),
        label         = "border"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(listOf(GoldDim, SurfaceDark)),
                KnightTourShapes.medium
            )
            .border(
                BorderWidth.default,
                KnightGold.copy(alpha = borderAlpha),
                KnightTourShapes.medium
            )
            .clickable(onClick = onResume)
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier              = Modifier.fillMaxWidth(),
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text("♟", fontSize = 28.sp, color = KnightGold)
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text  = "RESUME GAME",
                        style = MaterialTheme.knightType.CardTitle.copy(fontSize = 14.sp),
                        color = CrownGold,
                    )
                    Text(
                        text  = "${uiState.activeDifficulty}  ·  ${uiState.activeMoveCount} moves",
                        style = MaterialTheme.knightType.BodySecondary,
                        color = TextSecondary,
                    )
                }
            }
            // Arrow
            Text("›", fontSize = 24.sp, color = KnightGold)
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  SECTION DIVIDER
// ═══════════════════════════════════════════════════════════════

@Composable
private fun SectionDivider(label: String, modifier: Modifier = Modifier) {
    Row(
        modifier            = modifier,
        verticalAlignment   = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Canvas(modifier = Modifier.weight(1f).height(1.dp)) {
            drawLine(
                brush = Brush.horizontalGradient(listOf(Color.Transparent, BorderDefault)),
                start = Offset(0f, 0f), end = Offset(size.width, 0f), strokeWidth = 1f
            )
        }
        Text(
            text  = label,
            style = MaterialTheme.knightType.Eyebrow.copy(letterSpacing = 4.sp),
            color = TextTertiary,
        )
        Canvas(modifier = Modifier.weight(1f).height(1.dp)) {
            drawLine(
                brush = Brush.horizontalGradient(listOf(BorderDefault, Color.Transparent)),
                start = Offset(0f, 0f), end = Offset(size.width, 0f), strokeWidth = 1f
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  MODE CARDS SECTION
// ═══════════════════════════════════════════════════════════════

@Composable
private fun ModeCardsSection(
    uiState  : HomeUiState,
    onEvent  : (HomeEvent) -> Unit,
    modifier : Modifier = Modifier,
) {
    Column(
        modifier            = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // PRIMARY — Offline (full width, tall)
        ModeCardPrimary(
            icon              = "♞",
            title             = "SOLO QUEST",
            subtitle          = "Offline · Single Player",
            description       = "Conquer the board alone. 5×5 to 8×8. No internet required.",
            accentColor       = KnightGold,
            dimColor          = GoldDim,
            boardSizes        = listOf("5×5" to "EASY", "6×6" to "MEDIUM", "8×8" to "HARD"),
            selectedDifficulty = uiState.selectedDifficulty,
            onSizeSelected    = { diff -> onEvent(HomeEvent.SelectDifficulty(diff)) },
            onClick           = { onEvent(HomeEvent.PlayOffline(uiState.selectedDifficulty)) },
        )

        // SECONDARY ROW — Online + Devil
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ModeCardSecondary(
                modifier    = Modifier.weight(1f),
                icon        = "🌐",
                title       = "ONLINE",
                subtitle    = "Race Mode",
                description = "Challenge another player in real time.",
                accentColor = OnlineTeal,
                dimColor    = OnlineTealDim,
                isEnabled   = uiState.isOnline,
                disabledMsg = "No internet",
                onClick     = { onEvent(HomeEvent.PlayOnline) },
            )
            ModeCardSecondary(
                modifier    = Modifier.weight(1f),
                icon        = "😈",
                title       = "DEVIL",
                subtitle    = "10×10 · 90s",
                description = "No hints. No mercy.",
                accentColor = DevilRed,
                dimColor    = RedDim,
                isEnabled   = true,
                onClick     = { onEvent(HomeEvent.PlayDevil) },
            )
        }
    }
}

@Composable
private fun ModeCardPrimary(
    icon        : String,
    title              : String,
    subtitle           : String,
    description        : String,
    accentColor        : Color,
    dimColor           : Color,
    boardSizes         : List<Pair<String, String>>,   // label to difficulty key
    selectedDifficulty : String,
    onSizeSelected     : (String) -> Unit,
    onClick            : () -> Unit,
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue   = if (pressed) 0.97f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy), label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .background(
                Brush.linearGradient(
                    colors = listOf(dimColor, SurfaceDark, SurfaceElevated),
                    start  = Offset(0f, 0f), end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                ),
                KnightTourShapes.medium
            )
            .border(BorderWidth.default, accentColor.copy(alpha = 0.4f), KnightTourShapes.medium)
            .clickable { pressed = true; onClick() }
            .padding(20.dp),
    ) {
        // Decorative corner accent
        Canvas(modifier = Modifier.size(60.dp).align(Alignment.TopEnd)) {
            drawPath(
                path = Path().apply {
                    moveTo(size.width, 0f); lineTo(size.width - size.width, 0f)
                    lineTo(size.width, size.height); close()
                },
                color = accentColor.copy(alpha = 0.06f)
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier              = Modifier.fillMaxWidth(),
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(icon, fontSize = 32.sp)
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text  = title,
                            style = MaterialTheme.knightType.CardTitle,
                            color = accentColor,
                        )
                        Text(
                            text  = subtitle,
                            style = MaterialTheme.knightType.StatLabel.copy(letterSpacing = 2.sp),
                            color = TextTertiary,
                        )
                    }
                }
                // Play button
                Box(
                    modifier         = Modifier
                        .background(accentColor.copy(alpha = 0.15f), KnightTourShapes.small)
                        .border(BorderWidth.thin, accentColor.copy(alpha = 0.5f), KnightTourShapes.small)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text  = "PLAY",
                        style = MaterialTheme.knightType.ButtonPrimary.copy(fontSize = 11.sp),
                        color = accentColor,
                    )
                }
            }

            Text(
                text  = description,
                style = MaterialTheme.knightType.BodySecondary,
                color = TextSecondary,
            )

            // Board size chips — tappable, selected chip glows gold
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                boardSizes.forEach { (label, diffKey) ->
                    val isSelected = diffKey == selectedDifficulty
                    Box(
                        modifier = Modifier
                            .background(
                                if (isSelected) accentColor.copy(alpha = 0.18f)
                                else SurfaceHighest,
                                KnightTourShapes.extraSmall,
                            )
                            .border(
                                BorderWidth.thin,
                                if (isSelected) accentColor.copy(alpha = 0.9f)
                                else BorderDefault,
                                KnightTourShapes.extraSmall,
                            )
                            .clickable { onSizeSelected(diffKey) }
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text  = label,
                            style = MaterialTheme.knightType.DifficultyChip,
                            color = if (isSelected) accentColor else TextSecondary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeCardSecondary(
    modifier    : Modifier = Modifier,
    icon        : String,
    title       : String,
    subtitle    : String,
    description : String,
    accentColor : Color,
    dimColor    : Color,
    isEnabled   : Boolean = true,
    disabledMsg : String  = "",
    onClick     : () -> Unit,
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue   = if (pressed) 0.95f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy), label = "scale"
    )
    val effectiveAlpha = if (isEnabled) 1f else 0.45f

    Box(
        modifier = modifier
            .scale(scale)
            .alpha(effectiveAlpha)
            .background(
                Brush.verticalGradient(listOf(dimColor, SurfaceDark)),
                KnightTourShapes.medium
            )
            .border(BorderWidth.thin, accentColor.copy(alpha = 0.35f), KnightTourShapes.medium)
            .clickable(enabled = isEnabled) { pressed = true; onClick() }
            .padding(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(icon, fontSize = 28.sp)
            Text(
                text  = title,
                style = MaterialTheme.knightType.CardTitle.copy(fontSize = 15.sp),
                color = accentColor,
            )
            Text(
                text  = subtitle,
                style = MaterialTheme.knightType.StatLabel.copy(letterSpacing = 1.5.sp),
                color = TextTertiary,
            )
            Text(
                text  = if (!isEnabled && disabledMsg.isNotEmpty()) disabledMsg else description,
                style = MaterialTheme.knightType.Caption,
                color = TextTertiary.copy(alpha = 0.8f),
                maxLines = 2,
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  QUICK ACTIONS ROW
// ═══════════════════════════════════════════════════════════════

@Composable
private fun QuickActionsRow(
    uiState  : HomeUiState,
    onEvent  : (HomeEvent) -> Unit,
    modifier : Modifier = Modifier,
) {
    Row(
        modifier              = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Leaderboard
        QuickActionCard(
            modifier    = Modifier.weight(1f),
            icon        = "🏆",
            label       = "LEADERBOARD",
            sublabel    = "Global ranks",
            accentColor = KnightGold,
            onClick     = { onEvent(HomeEvent.OpenLeaderboard) },
        )
        // Daily Challenge
        QuickActionCard(
            modifier    = Modifier.weight(1f),
            icon        = if (uiState.dailyChallengeDone) "✅" else "📅",
            label       = "DAILY",
            sublabel    = if (uiState.dailyChallengeDone) "Completed!" else "Challenge",
            accentColor = if (uiState.dailyChallengeDone) VictoryGreen else WarningAmber,
            onClick     = { onEvent(HomeEvent.OpenDailyChallenge) },
        )
    }
}

@Composable
private fun QuickActionCard(
    modifier    : Modifier = Modifier,
    icon        : String,
    label       : String,
    sublabel    : String,
    accentColor : Color,
    onClick     : () -> Unit,
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue   = if (pressed) 0.95f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy), label = "scale"
    )

    Row(
        modifier = modifier
            .scale(scale)
            .background(SurfaceDark, KnightTourShapes.medium)
            .border(BorderWidth.thin, BorderDefault, KnightTourShapes.medium)
            .clickable { pressed = true; onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(accentColor.copy(alpha = 0.1f), KnightTourShapes.extraSmall)
                .border(BorderWidth.thin, accentColor.copy(alpha = 0.25f), KnightTourShapes.extraSmall),
            contentAlignment = Alignment.Center,
        ) {
            Text(icon, fontSize = 18.sp)
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text  = label,
                style = MaterialTheme.knightType.StatLabel.copy(letterSpacing = 2.sp),
                color = accentColor,
            )
            Text(
                text  = sublabel,
                style = MaterialTheme.knightType.Caption,
                color = TextTertiary,
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  SMALL REUSABLE COMPONENTS
// ═══════════════════════════════════════════════════════════════

@Composable
private fun OnlineDot() {
    val pulse = rememberInfiniteTransition(label = "dot")
    val alpha by pulse.animateFloat(
        initialValue  = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800, easing = EaseInOutSine), RepeatMode.Reverse),
        label         = "dotAlpha"
    )
    Box(
        modifier = Modifier
            .size(8.dp)
            .background(OnlineTeal.copy(alpha = alpha), CircleShape)
    )
}

@Composable
private fun IconButton24(
    icon    : String,
    onClick : () -> Unit,
) {
    Box(
        modifier         = Modifier
            .size(36.dp)
            .background(SurfaceElevated, KnightTourShapes.extraSmall)
            .border(BorderWidth.thin, BorderSubtle, KnightTourShapes.extraSmall)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(icon, fontSize = 16.sp)
    }
}

// ═══════════════════════════════════════════════════════════════
//  ROUTE ENTRY POINT
// ═══════════════════════════════════════════════════════════════

@Composable
fun HomeRoute(
    onPlayOffline     : (difficulty: String) -> Unit,
    onPlayOnline      : () -> Unit,
    onPlayDevil       : () -> Unit,
    onResumeGame      : (sessionId: String) -> Unit,
    onOpenLeaderboard : () -> Unit,
    onOpenSettings    : () -> Unit,
    onOpenDaily       : () -> Unit,
    viewModel         : HomeViewModel = hiltViewModel(),
) {
    val uiState       by viewModel.uiState.collectAsStateWithLifecycle()
    val resumeSession by viewModel.resumeSessionId.collectAsStateWithLifecycle()

    HomeScreen(
        uiState = uiState,
        onEvent = { event ->
            when (event) {
                is HomeEvent.PlayOffline        -> onPlayOffline(event.difficulty)
                HomeEvent.PlayOnline            -> onPlayOnline()
                HomeEvent.PlayDevil             -> onPlayDevil()
                HomeEvent.ResumeGame            -> onResumeGame(resumeSession ?: "")
                HomeEvent.OpenLeaderboard       -> onOpenLeaderboard()
                HomeEvent.OpenSettings          -> onOpenSettings()
                HomeEvent.OpenDailyChallenge    -> onOpenDaily()
                is HomeEvent.SelectDifficulty   -> viewModel.onEvent(event)
            }
        }
    )
}

// ═══════════════════════════════════════════════════════════════
//  PREVIEWS
// ═══════════════════════════════════════════════════════════════

@Preview(
    name = "Home Screen — Default",
    showBackground = true,
    backgroundColor = 0xFF050508,
    widthDp = 360, heightDp = 800,
)
@Composable
private fun HomeScreenPreview() {
    KnightTourTheme {
        HomeScreen(
            uiState = HomeUiState(
                playerName    = "Knight",
                playerRank    = "NOVICE",
                playerScore   = 0,
                gamesPlayed   = 0,
                bestTime      = "--:--",
                winRate       = 0,
                hasActiveGame = false,
                isOnline      = false,
            ),
            onEvent = {},
        )
    }
}

@Preview(
    name = "Home Screen — With Active Game + Online",
    showBackground = true,
    backgroundColor = 0xFF050508,
    widthDp = 360, heightDp = 860,
)
@Composable
private fun HomeScreenActivePreview() {
    KnightTourTheme {
        HomeScreen(
            uiState = HomeUiState(
                playerName       = "Devil_King",
                playerRank       = "GRANDMASTER",
                playerScore      = 12450,
                gamesPlayed      = 87,
                bestTime         = "1:12",
                winRate          = 74,
                hasActiveGame    = true,
                activeDifficulty = "6×6",
                activeMoveCount  = 18,
                isOnline         = true,
                dailyChallengeDone = true,
            ),
            onEvent = {},
        )
    }
}