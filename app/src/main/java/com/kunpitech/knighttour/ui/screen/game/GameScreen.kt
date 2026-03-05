package com.kunpitech.knighttour.ui.screen.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kunpitech.knighttour.ui.theme.AbyssBlack
import com.kunpitech.knighttour.ui.theme.BloodRedBright
import com.kunpitech.knighttour.ui.theme.BorderDefault
import com.kunpitech.knighttour.ui.theme.BorderSubtle
import com.kunpitech.knighttour.ui.theme.BorderWidth
import com.kunpitech.knighttour.ui.theme.CellDark
import com.kunpitech.knighttour.ui.theme.CellHint
import com.kunpitech.knighttour.ui.theme.CellKnight
import com.kunpitech.knighttour.ui.theme.CellLight
import com.kunpitech.knighttour.ui.theme.CellShape
import com.kunpitech.knighttour.ui.theme.CellValidMove
import com.kunpitech.knighttour.ui.theme.CellVisited
import com.kunpitech.knighttour.ui.theme.CrownGold
import com.kunpitech.knighttour.ui.theme.DevilRed
import com.kunpitech.knighttour.ui.theme.DialogShape
import com.kunpitech.knighttour.ui.theme.GoldDim
import com.kunpitech.knighttour.ui.theme.KnightGold
import com.kunpitech.knighttour.ui.theme.KnightTourShapes
import com.kunpitech.knighttour.ui.theme.KnightTourTheme
import com.kunpitech.knighttour.ui.theme.OnlineTeal
import com.kunpitech.knighttour.ui.theme.RedDim
import com.kunpitech.knighttour.ui.theme.RoundedSmall
import com.kunpitech.knighttour.ui.theme.Scrim
import com.kunpitech.knighttour.ui.theme.SurfaceDark
import com.kunpitech.knighttour.ui.theme.SurfaceElevated
import com.kunpitech.knighttour.ui.theme.TextSecondary
import com.kunpitech.knighttour.ui.theme.TextTertiary
import com.kunpitech.knighttour.ui.theme.WarningAmber
import com.kunpitech.knighttour.ui.theme.knightType

// ═══════════════════════════════════════════════════════════════
//  KNIGHT TOUR — GAME SCREEN
//
//  Layout (top → bottom):
//  ┌────────────────────────────────────┐
//  │  TopHUD   back · title · timer     │
//  │  StatsBar moves · left · score     │
//  │  ProgressBar ───────────────────   │
//  │  ┌──────────────────────────────┐  │
//  │  │       CHESS BOARD            │  │
//  │  │   (square · fills width)     │  │
//  │  └──────────────────────────────┘  │
//  │  CoordRow  A · B · C · D · E · F   │
//  │  [Opponent bar — online only]      │
//  │  BottomBar undo·pause·hint·restart │
//  └────────────────────────────────────┘
//
//  Overlays: PauseDialog · VictoryOverlay · DefeatOverlay
// ═══════════════════════════════════════════════════════════════

@Composable
fun GameScreen(
    uiState          : GameUiState,
    onEvent          : (GameEvent) -> Unit,
    onNavigateBack   : () -> Unit,
    onNavigateResult : () -> Unit,
) {
    val haptic = LocalHapticFeedback.current

    // Auto-navigate to result after victory animation
    LaunchedEffect(uiState.gameState) {
        if (uiState.gameState == GamePhase.COMPLETED) {
            kotlinx.coroutines.delay(2000L)
            onNavigateResult()
        }
    }

    // ── SHARED INFINITE ANIMATIONS ───────────────────────────────
    val infiniteTransition = rememberInfiniteTransition(label = "game_global")

    val glowPulse by infiniteTransition.animateFloat(
        initialValue  = 0.5f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(
            tween(1400, easing = EaseInOutSine), RepeatMode.Reverse,
        ),
        label = "glow",
    )
    val validPulse by infiniteTransition.animateFloat(
        initialValue  = 0.55f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(
            tween(900, easing = EaseInOutSine), RepeatMode.Reverse,
        ),
        label = "validPulse",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AbyssBlack)
            .systemBarsPadding(),
    ) {

        // ── BACKGROUND ───────────────────────────────────────────
        GameBackground(modifier = Modifier.fillMaxSize(), glowPulse = glowPulse)

        // ── MAIN CONTENT ─────────────────────────────────────────
        Column(
            modifier            = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            TopHud(
                uiState  = uiState,
                onBack   = onNavigateBack,
                onEvent  = onEvent,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            )

            StatsBar(
                uiState  = uiState,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )

            Spacer(Modifier.height(8.dp))

            ProgressStrip(
                progress = uiState.moveCount.toFloat() / uiState.totalCells.toFloat(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )

            Spacer(Modifier.height(10.dp))

            // ── BOARD ─────────────────────────────────────────────
            BoxWithConstraints(
                modifier         = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                val boardSize = minOf(maxWidth, 400.dp)
                ChessBoard(
                    uiState      = uiState,
                    glowPulse    = glowPulse,
                    validPulse   = validPulse,
                    onCellTap    = { r, c ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onEvent(GameEvent.CellTapped(r, c))
                    },
                    modifier     = Modifier.size(boardSize),
                )
            }

            Spacer(Modifier.height(4.dp))

            CoordLabels(
                boardSize = uiState.boardSize,
                modifier  = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp),
            )

            Spacer(Modifier.weight(1f))

            // ── ONLINE OPPONENT ───────────────────────────────────
            if (uiState.isOnlineMode) {
                OpponentBar(
                    uiState  = uiState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }

            // ── BOTTOM ACTIONS ────────────────────────────────────
            BottomBar(
                uiState  = uiState,
                onEvent  = onEvent,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, AbyssBlack.copy(alpha = 0.96f)),
                        ),
                    )
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            )
        }

        // ── OVERLAYS ─────────────────────────────────────────────
        AnimatedVisibility(
            visible = uiState.gameState == GamePhase.PAUSED,
            enter   = fadeIn() + scaleIn(initialScale = 0.88f),
            exit    = fadeOut() + scaleOut(targetScale = 0.88f),
        ) {
            PauseOverlay(
                onResume  = { onEvent(GameEvent.Resume) },
                onRestart = { onEvent(GameEvent.Restart) },
                onQuit    = onNavigateBack,
            )
        }

        AnimatedVisibility(
            visible = uiState.gameState == GamePhase.COMPLETED,
            enter   = fadeIn(tween(700)),
            exit    = fadeOut(tween(300)),
        ) {
            VictoryOverlay(uiState = uiState)
        }

        AnimatedVisibility(
            visible = uiState.gameState == GamePhase.FAILED,
            enter   = fadeIn(tween(700)),
            exit    = fadeOut(tween(300)),
        ) {
            DefeatOverlay(
                uiState   = uiState,
                onRestart = { onEvent(GameEvent.Restart) },
                onQuit    = onNavigateBack,
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  BACKGROUND
// ═══════════════════════════════════════════════════════════════

@Composable
private fun GameBackground(modifier: Modifier, glowPulse: Float) {
    Canvas(modifier = modifier) {
        val gridColor = KnightGold.copy(alpha = 0.026f)
        val cell      = 38.dp.toPx()
        var x = 0f
        while (x <= size.width) {
            drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), 1f)
            x += cell
        }
        var y = 0f
        while (y <= size.height) {
            drawLine(gridColor, Offset(0f, y), Offset(size.width, y), 1f)
            y += cell
        }
        // Top edge glow
        drawRect(
            brush  = Brush.verticalGradient(
                listOf(KnightGold.copy(alpha = 0.05f * glowPulse), Color.Transparent),
                startY = 0f, endY = size.height * 0.28f,
            ),
        )
    }
}

// ═══════════════════════════════════════════════════════════════
//  TOP HUD
// ═══════════════════════════════════════════════════════════════

@Composable
private fun TopHud(
    uiState  : GameUiState,
    onBack   : () -> Unit,
    onEvent  : (GameEvent) -> Unit,
    modifier : Modifier = Modifier,
) {
    val remaining  = (uiState.timeLimitSeconds - uiState.elapsedSeconds).coerceAtLeast(0)
    val timeStr    = "%d:%02d".format(remaining / 60, remaining % 60)
    val isPanic    = uiState.isTimerPanic

    // InfiniteTransition only accepts InfiniteRepeatableSpec — never snap().
    // Run the blink animation always, but only apply it when in panic mode.
    val blinkRaw by rememberInfiniteTransition(label = "blink").animateFloat(
        initialValue  = 1f,
        targetValue   = 0.15f,
        animationSpec = infiniteRepeatable(
            animation  = tween(480),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "timerBlink",
    )
    val blinkAlpha = if (isPanic) blinkRaw else 1f

    Row(
        modifier              = modifier,
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // Back
        SmallIconButton(icon = "‹", onClick = onBack)

        // Title
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text  = uiState.difficulty.label,
                style = MaterialTheme.knightType.ScreenHeader.copy(fontSize = 11.sp),
                color = KnightGold,
            )
            Text(
                text  = uiState.gameMode.name,
                style = MaterialTheme.knightType.Eyebrow.copy(letterSpacing = 3.sp),
                color = TextTertiary,
            )
        }

        // Timer chip
        Box(
            modifier = Modifier
                .background(if (isPanic) RedDim else GoldDim, KnightTourShapes.small)
                .border(
                    BorderWidth.thin,
                    if (isPanic) BloodRedBright.copy(alpha = 0.55f)
                    else KnightGold.copy(alpha = 0.4f),
                    KnightTourShapes.small,
                )
                .padding(horizontal = 14.dp, vertical = 6.dp)
                .alpha(blinkAlpha),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text  = timeStr,
                style = if (isPanic)
                    MaterialTheme.knightType.TimerPanic
                else
                    MaterialTheme.knightType.TimerNormal,
                color = if (isPanic) BloodRedBright else CrownGold,
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  STATS BAR
// ═══════════════════════════════════════════════════════════════

@Composable
private fun StatsBar(uiState: GameUiState, modifier: Modifier = Modifier) {
    Row(
        modifier              = modifier
            .background(SurfaceDark, KnightTourShapes.small)
            .border(BorderWidth.thin, BorderSubtle, KnightTourShapes.small)
            .padding(vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        StatChip(value = "${uiState.moveCount}", label = "MOVES")
        VDivider()
        StatChip(
            value      = "${uiState.totalCells - uiState.moveCount}",
            label      = "LEFT",
            valueColor = if (uiState.totalCells - uiState.moveCount <= 5)
                WarningAmber else TextSecondary,
        )
        VDivider()
        StatChip(value = "${uiState.currentScore}", label = "SCORE", valueColor = CrownGold)
        VDivider()
        StatChip(
            value      = "${uiState.hintsRemaining}",
            label      = "HINTS",
            valueColor = if (uiState.hintsRemaining > 0) WarningAmber else TextTertiary,
        )
    }
}

@Composable
private fun StatChip(
    value      : String,
    label      : String,
    valueColor : Color = TextSecondary,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text  = value,
            style = MaterialTheme.knightType.StatDisplay.copy(fontSize = 18.sp),
            color = valueColor,
        )
        Text(
            text  = label,
            style = MaterialTheme.knightType.StatLabel,
            color = TextTertiary,
        )
    }
}

@Composable
private fun VDivider() {
    Box(Modifier.width(1.dp).height(28.dp).background(BorderSubtle))
}

// ═══════════════════════════════════════════════════════════════
//  PROGRESS STRIP
// ═══════════════════════════════════════════════════════════════

@Composable
private fun ProgressStrip(progress: Float, modifier: Modifier = Modifier) {
    val animProg by animateFloatAsState(
        targetValue   = progress.coerceIn(0f, 1f),
        animationSpec = tween(280, easing = EaseOutCubic),
        label         = "progressAnim",
    )
    Column(modifier = modifier) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "PROGRESS",
                style = MaterialTheme.knightType.StatLabel.copy(letterSpacing = 2.sp),
                color = TextTertiary,
            )
            Text(
                "${(progress * 100).toInt()}%",
                style = MaterialTheme.knightType.StatLabel,
                color = KnightGold,
            )
        }
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(BorderSubtle, RoundedSmall),
        ) {
            if (animProg > 0f) {
                Box(
                    Modifier
                        .fillMaxWidth(animProg)
                        .height(3.dp)
                        .background(
                            Brush.horizontalGradient(listOf(KnightGold, CrownGold)),
                            RoundedSmall,
                        )
                        .shadow(
                            elevation    = 4.dp,
                            shape        = RoundedSmall,
                            ambientColor = KnightGold,
                            spotColor    = KnightGold,
                        ),
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  BOARD THEME COLORS
//  Resolved from the boardTheme string in GameUiState.
// ═══════════════════════════════════════════════════════════════

private data class BoardThemeColors(
    val cellLight    : Color,
    val cellDark     : Color,
    val cellVisited  : Color,
    val cellKnight   : Color,
    val cellHint     : Color,
    val cellValid    : Color,
    val accentColor  : Color,
    val boardBg      : Color,   // gap/board background color
)

@Composable
private fun resolveThemeColors(theme: String): BoardThemeColors = when (theme) {

    "CRIMSON" -> BoardThemeColors(
        cellLight   = Color(0xFF3B1020),   // deep wine
        cellDark    = Color(0xFF1E0810),   // near-black red
        cellVisited = Color(0xFF6B1A2A),   // rich crimson visited
        cellKnight  = Color(0xAAC0392B),   // vivid red knight glow
        cellHint    = Color(0xFF7A1F30),   // bright hint red
        cellValid   = Color(0x60E74C3C),   // punchy red valid dot
        accentColor = Color(0xFFFF5252),   // bright blood red accent
        boardBg     = Color(0xFF120509),
    )

    "ABYSS" -> BoardThemeColors(
        cellLight   = Color(0xFF162845),   // rich navy
        cellDark    = Color(0xFF0A1628),   // deep ocean dark
        cellVisited = Color(0xFF1A3F6B),   // bold visited blue
        cellKnight  = Color(0xAA5BA3D4),   // bright silver-blue knight
        cellHint    = Color(0xFF1E4D7A),   // clear hint blue
        cellValid   = Color(0x6064B5F6),   // vivid blue valid dot
        accentColor = Color(0xFF64B5F6),   // bright sky blue accent
        boardBg     = Color(0xFF060D18),
    )

    "VOID" -> BoardThemeColors(
        cellLight   = Color(0xFF2A2A2A),   // clear mid-grey
        cellDark    = Color(0xFF111111),   // near-black
        cellVisited = Color(0xFF484848),   // clearly visible visited
        cellKnight  = Color(0x99FFFFFF),   // bright white knight glow
        cellHint    = Color(0xFF555555),   // clear hint grey
        cellValid   = Color(0x66EEEEEE),   // soft white valid dot
        accentColor = Color(0xFFEEEEEE),   // bright white accent
        boardBg     = Color(0xFF080808),
    )

    else -> BoardThemeColors(              // OBSIDIAN (default) — refreshed
        cellLight   = Color(0xFF1E2340),   // indigo-dark blue — was too grey
        cellDark    = Color(0xFF0E1020),   // deep abyss dark
        cellVisited = Color(0xFF0F3052),   // clear teal-navy visited
        cellKnight  = Color(0xAAC8A84B),   // rich gold knight glow
        cellHint    = Color(0xFF1A3A5C),   // distinct hint blue
        cellValid   = Color(0x55E8C96A),   // warm gold valid dot
        accentColor = Color(0xFFC8A84B),   // knight gold
        boardBg     = Color(0xFF07080F),
    )
}

// ═══════════════════════════════════════════════════════════════
//  CHESS BOARD
// ═══════════════════════════════════════════════════════════════

@Composable
private fun ChessBoard(
    uiState    : GameUiState,
    glowPulse  : Float,
    validPulse : Float,
    onCellTap  : (Int, Int) -> Unit,
    modifier   : Modifier = Modifier,
) {
    val n      = uiState.boardSize
    val gap    = 2.dp
    val colors = resolveThemeColors(uiState.boardTheme)

    Box(
        modifier = modifier
            .shadow(
                elevation    = 16.dp,
                shape        = KnightTourShapes.extraSmall,
                ambientColor = colors.accentColor.copy(alpha = 0.25f),
                spotColor    = colors.accentColor.copy(alpha = 0.20f),
            )
            .background(colors.boardBg, KnightTourShapes.extraSmall)
            .border(BorderWidth.thin, colors.accentColor.copy(alpha = 0.30f), KnightTourShapes.extraSmall),
    ) {
        // ── Trail canvas (drawn behind cells) ────────────────────
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawTrail(uiState, gapPx = gap.toPx(), outerPadPx = (gap / 2).toPx(), accentColor = colors.accentColor)
        }

        // ── Cell grid ────────────────────────────────────────────
        Column(
            modifier            = Modifier
                .fillMaxSize()
                .padding(gap / 2),
            verticalArrangement = Arrangement.spacedBy(gap),
        ) {
            repeat(n) { row ->
                Row(
                    modifier              = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(gap),
                ) {
                    repeat(n) { col ->
                        val cell: CellState = uiState.cells
                            .find { it.row == row && it.col == col }
                            ?: CellState(row, col)

                        BoardCell(
                            cell            = cell,
                            boardSize       = n,
                            validPulse      = validPulse,
                            isShaking       = uiState.shakeCell == row to col,
                            colors          = colors,
                            showMoveNumber  = uiState.showMoveNumbers,
                            showValidMoves  = uiState.showValidMoves,
                            modifier        = Modifier.weight(1f),
                            onClick         = { onCellTap(row, col) },
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  TRAIL DRAW FUNCTION (Canvas)
// ═══════════════════════════════════════════════════════════════

private fun DrawScope.drawTrail(
    uiState     : GameUiState,
    gapPx       : Float,
    outerPadPx  : Float = 0f,
    accentColor : Color = KnightGold,
) {
    val n       = uiState.boardSize
    val availW  = size.width  - outerPadPx * 2
    val availH  = size.height - outerPadPx * 2
    val cellW   = (availW - gapPx * (n - 1)) / n
    val cellH   = (availH - gapPx * (n - 1)) / n

    fun center(row: Int, col: Int) = Offset(
        x = outerPadPx + col * (cellW + gapPx) + cellW / 2f,
        y = outerPadPx + row * (cellH + gapPx) + cellH / 2f,
    )

    val visited: List<CellState> = uiState.cells
        .filter { it.isVisited && it.moveNumber > 0 }
        .sortedBy { it.moveNumber }

    if (visited.size < 2) return

    // Draw only a small progress dot inside each visited cell — no connecting lines
    // Lines between cells looked noisy on small boards and crossed cell boundaries
    visited.forEach { cell ->
        if (!cell.isKnight) {
            val progress = cell.moveNumber.toFloat() / visited.size
            val alpha    = 0.15f + progress * 0.20f
            drawCircle(
                color  = accentColor.copy(alpha = alpha),
                radius = 2.5.dp.toPx(),
                center = center(cell.row, cell.col),
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  BOARD CELL
// ═══════════════════════════════════════════════════════════════

@Composable
private fun BoardCell(
    cell           : CellState,
    boardSize      : Int,
    validPulse     : Float,
    isShaking      : Boolean,
    colors         : BoardThemeColors,
    showMoveNumber : Boolean,
    showValidMoves : Boolean,
    modifier       : Modifier = Modifier,
    onClick        : () -> Unit,
) {
    val isLightCell = (cell.row + cell.col) % 2 == 0

    // Shake on invalid move
    val shakeX by animateFloatAsState(
        targetValue   = if (isShaking) 1f else 0f,
        animationSpec = if (isShaking)
            keyframes {
                durationMillis = 400
                0f at 0; 6f at 50; -6f at 100
                6f at 150; -6f at 200; 3f at 280
                -3f at 330; 0f at 400
            }
        else tween(80),
        label = "cellShake",
    )

    // Knight scale spring
    val knightScale by animateFloatAsState(
        targetValue   = if (cell.isKnight) 1f else 0f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow),
        label         = "knightScale",
    )

    val bgColor: Color = when {
        cell.isKnight    -> colors.cellKnight
        cell.isVisited   -> colors.cellVisited
        cell.isHint      -> colors.cellHint
        cell.isValidMove -> colors.cellValid.copy(
            alpha = colors.cellValid.alpha.coerceAtLeast(0.05f) * validPulse
        )
        isLightCell      -> colors.cellLight
        else             -> colors.cellDark
    }

    val borderMod: Modifier = when {
        cell.isHint      -> Modifier.border(
            BorderWidth.thin, colors.accentColor.copy(alpha = 0.65f), CellShape
        )
        cell.isValidMove -> Modifier.border(
            BorderWidth.thin, colors.cellValid.copy(alpha = 0.55f * validPulse), CellShape
        )
        else -> Modifier
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .offset(x = shakeX.dp)
            .background(bgColor, CellShape)
            .then(borderMod)
            .clickable(
                enabled = cell.isValidMove || cell.isHint ||
                        (!cell.isVisited && !cell.isKnight),
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        // Move number — respects showMoveNumbers preference
        if (showMoveNumber && cell.isVisited && !cell.isKnight && cell.moveNumber > 0) {
            Text(
                text  = cell.moveNumber.toString(),
                style = MaterialTheme.knightType.CellNumber,
                color = colors.accentColor.copy(alpha = 0.70f),
            )
        }

        // Valid move dot — respects showValidMoves preference
        if (showValidMoves && cell.isValidMove && !cell.isKnight) {
            Box(
                modifier = Modifier
                    .size(if (boardSize <= 6) 8.dp else 6.dp)
                    .background(
                        colors.cellValid.copy(alpha = 0.80f * validPulse),
                        CircleShape,
                    ),
            )
        }

        // Hint dot — always shown regardless of prefs
        if (cell.isHint) {
            Box(
                modifier = Modifier
                    .size(if (boardSize <= 6) 10.dp else 7.dp)
                    .background(colors.accentColor.copy(alpha = 0.82f), CircleShape)
                    .shadow(
                        elevation    = 4.dp,
                        shape        = CircleShape,
                        ambientColor = colors.accentColor,
                        spotColor    = colors.accentColor,
                    ),
            )
        }

        // Knight piece
        if (cell.isKnight) {
            Text(
                text     = "♞",
                fontSize = when {
                    boardSize <= 5  -> 24.sp
                    boardSize <= 6  -> 20.sp
                    boardSize <= 8  -> 15.sp
                    else            -> 11.sp
                },
                color    = CrownGold,
                modifier = Modifier.scale(knightScale),
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  COORD LABELS
// ═══════════════════════════════════════════════════════════════

@Composable
private fun CoordLabels(boardSize: Int, modifier: Modifier = Modifier) {
    Row(
        modifier              = modifier.padding(horizontal = 2.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        repeat(boardSize) { i ->
            Text(
                text      = ('A' + i).toString(),
                style     = MaterialTheme.knightType.BoardCoord,
                color     = TextTertiary,
                textAlign = TextAlign.Center,
                modifier  = Modifier.weight(1f),
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  OPPONENT BAR (online mode)
// ═══════════════════════════════════════════════════════════════

@Composable
private fun OpponentBar(uiState: GameUiState, modifier: Modifier = Modifier) {
    val animProg by animateFloatAsState(
        targetValue   = uiState.opponentProgress,
        animationSpec = tween(500),
        label         = "oppProg",
    )
    val dotAlpha by rememberInfiniteTransition(label = "oppDot").animateFloat(
        initialValue  = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label         = "dotA",
    )
    Row(
        modifier              = modifier
            .background(SurfaceDark, KnightTourShapes.small)
            .border(BorderWidth.thin, OnlineTeal.copy(alpha = 0.25f), KnightTourShapes.small)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.size(7.dp).background(OnlineTeal.copy(alpha = dotAlpha), CircleShape))
        Text(
            text     = uiState.opponentName.uppercase(),
            style    = MaterialTheme.knightType.StatLabel.copy(letterSpacing = 1.sp),
            color    = OnlineTeal,
            modifier = Modifier.width(80.dp),
        )
        Box(
            Modifier
                .weight(1f)
                .height(3.dp)
                .background(BorderSubtle, RoundedSmall),
        ) {
            if (animProg > 0f) {
                Box(
                    Modifier
                        .fillMaxWidth(animProg)
                        .height(3.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(OnlineTeal, OnlineTeal.copy(alpha = 0.5f))
                            ),
                            RoundedSmall,
                        ),
                )
            }
        }
        Text(
            text  = "${uiState.opponentMoves}",
            style = MaterialTheme.knightType.StatLabel,
            color = TextTertiary,
        )
    }
}

// ═══════════════════════════════════════════════════════════════
//  BOTTOM ACTION BAR
// ═══════════════════════════════════════════════════════════════

@Composable
private fun BottomBar(
    uiState  : GameUiState,
    onEvent  : (GameEvent) -> Unit,
    modifier : Modifier = Modifier,
) {
    val isPlaying = uiState.gameState == GamePhase.PLAYING
    val isPaused  = uiState.gameState == GamePhase.PAUSED

    Row(
        modifier              = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        ActionButton(
            icon    = "↩",
            label   = "UNDO",
            enabled = uiState.canUndo && isPlaying,
            onClick = { onEvent(GameEvent.Undo) },
        )
        ActionButton(
            icon    = "⟳",
            label   = "RESTART",
            enabled = true,
            onClick = { onEvent(GameEvent.Restart) },
        )

        // Central pause/resume
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(
                    Brush.radialGradient(listOf(GoldDim, Color.Transparent)),
                    CircleShape,
                )
                .border(BorderWidth.default, KnightGold.copy(alpha = 0.5f), CircleShape)
                .clickable {
                    if (isPaused) onEvent(GameEvent.Resume)
                    else onEvent(GameEvent.Pause)
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text  = if (isPaused) "▶" else "⏸",
                fontSize = 20.sp,
                color = CrownGold,
            )
        }

        ActionButton(
            icon    = "💡",
            label   = "HINT",
            enabled = uiState.hintsRemaining > 0 && isPlaying,
            tint    = if (uiState.hintsRemaining > 0) WarningAmber else TextTertiary,
            onClick = { onEvent(GameEvent.Hint) },
        )
        ActionButton(
            icon    = "☰",
            label   = "INFO",
            enabled = true,
            onClick = { /* board info */ },
        )
    }
}

@Composable
private fun ActionButton(
    icon    : String,
    label   : String,
    enabled : Boolean,
    tint    : Color  = TextSecondary,
    onClick : () -> Unit,
) {
    Column(
        modifier = Modifier
            .alpha(if (enabled) 1f else 0.3f)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(icon, fontSize = 20.sp, color = tint)
        Text(
            text  = label,
            style = MaterialTheme.knightType.NavLabel,
            color = if (enabled) tint else TextTertiary,
        )
    }
}

// ═══════════════════════════════════════════════════════════════
//  PAUSE OVERLAY
// ═══════════════════════════════════════════════════════════════

@Composable
private fun PauseOverlay(
    onResume  : () -> Unit,
    onRestart : () -> Unit,
    onQuit    : () -> Unit,
) {
    Box(
        modifier         = Modifier
            .fillMaxSize()
            .background(Scrim),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .width(280.dp)
                .background(
                    Brush.verticalGradient(listOf(SurfaceElevated, SurfaceDark)),
                    DialogShape,
                )
                .border(BorderWidth.default, KnightGold.copy(alpha = 0.28f), DialogShape)
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("⏸", fontSize = 38.sp)
            Text(
                text  = "PAUSED",
                style = MaterialTheme.knightType.ScreenHeader.copy(letterSpacing = 6.sp),
                color = CrownGold,
            )
            Spacer(Modifier.height(2.dp))
            OverlayButton("RESUME",  KnightGold,              onResume)
            OverlayButton("RESTART", TextSecondary,            onRestart)
            OverlayButton("QUIT",    DevilRed.copy(alpha=0.8f), onQuit)
        }
    }
}

@Composable
private fun OverlayButton(label: String, tint: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .background(tint.copy(alpha = 0.08f), KnightTourShapes.small)
            .border(BorderWidth.thin, tint.copy(alpha = 0.28f), KnightTourShapes.small)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text  = label,
            style = MaterialTheme.knightType.ButtonSecondary,
            color = tint,
        )
    }
}

// ═══════════════════════════════════════════════════════════════
//  VICTORY OVERLAY
// ═══════════════════════════════════════════════════════════════

@Composable
private fun VictoryOverlay(uiState: GameUiState) {
    val pulse by rememberInfiniteTransition(label = "vicPulse").animateFloat(
        initialValue  = 0.92f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            tween(900, easing = EaseInOutSine), RepeatMode.Reverse
        ),
        label = "vPulse",
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(AbyssBlack.copy(alpha = 0.55f), AbyssBlack.copy(alpha = 0.93f))
                )
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("🏆", fontSize = 56.sp, modifier = Modifier.scale(pulse))
            Text(
                text     = "VICTORY",
                style    = MaterialTheme.knightType.DisplayBanner,
                color    = CrownGold,
                modifier = Modifier.scale(pulse),
            )
            Text(
                text  = "PERFECT KNIGHT TOUR",
                style = MaterialTheme.knightType.Eyebrow.copy(letterSpacing = 4.sp),
                color = TextSecondary,
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                ResultStat("${uiState.moveCount}", "SQUARES")
                ResultStat(
                    "%d:%02d".format(
                        uiState.elapsedSeconds / 60,
                        uiState.elapsedSeconds % 60
                    ), "TIME"
                )
                ResultStat("${uiState.currentScore}", "SCORE")
            }
        }
    }
}

@Composable
private fun ResultStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text  = value,
            style = MaterialTheme.knightType.StatDisplay.copy(fontSize = 24.sp),
            color = CrownGold,
        )
        Text(
            text  = label,
            style = MaterialTheme.knightType.StatLabel,
            color = TextTertiary,
        )
    }
}

// ═══════════════════════════════════════════════════════════════
//  DEFEAT OVERLAY
// ═══════════════════════════════════════════════════════════════

@Composable
private fun DefeatOverlay(
    uiState   : GameUiState,
    onRestart : () -> Unit,
    onQuit    : () -> Unit,
) {
    val reason = if (uiState.elapsedSeconds >= uiState.timeLimitSeconds)
        "TIME EXPIRED" else "NO MOVES LEFT"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(AbyssBlack.copy(alpha = 0.5f), AbyssBlack.copy(alpha = 0.94f))
                )
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier            = Modifier.padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("💀", fontSize = 52.sp)
            Text(
                text  = "DEFEATED",
                style = MaterialTheme.knightType.DisplayBanner,
                color = BloodRedBright,
            )
            Text(
                text  = reason,
                style = MaterialTheme.knightType.Eyebrow.copy(letterSpacing = 4.sp),
                color = TextSecondary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text  = "${uiState.moveCount} of ${uiState.totalCells} squares visited",
                style = MaterialTheme.knightType.BodySecondary,
                color = TextTertiary,
            )
            Spacer(Modifier.height(14.dp))
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f).height(46.dp)
                        .background(GoldDim, KnightTourShapes.small)
                        .border(BorderWidth.thin, KnightGold.copy(alpha = 0.5f), KnightTourShapes.small)
                        .clickable(onClick = onRestart),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("RETRY", style = MaterialTheme.knightType.ButtonSecondary, color = CrownGold)
                }
                Box(
                    modifier = Modifier
                        .weight(1f).height(46.dp)
                        .background(SurfaceDark, KnightTourShapes.small)
                        .border(BorderWidth.thin, BorderDefault, KnightTourShapes.small)
                        .clickable(onClick = onQuit),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("MENU", style = MaterialTheme.knightType.ButtonSecondary, color = TextSecondary)
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  SHARED SMALL COMPONENTS
// ═══════════════════════════════════════════════════════════════

@Composable
private fun SmallIconButton(icon: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .background(SurfaceElevated, KnightTourShapes.extraSmall)
            .border(BorderWidth.thin, BorderSubtle, KnightTourShapes.extraSmall)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(icon, fontSize = 22.sp, color = TextSecondary)
    }
}

// ═══════════════════════════════════════════════════════════════
//  ROUTE ENTRY POINT
// ═══════════════════════════════════════════════════════════════

@Composable
fun GameRoute(
    onNavigateBack   : () -> Unit,
    onNavigateResult : (String) -> Unit,
    viewModel        : GameViewModel = hiltViewModel(),
) {
    val uiState   by viewModel.uiState.collectAsStateWithLifecycle()
    val sessionId by viewModel.currentSessionId.collectAsStateWithLifecycle()

    GameScreen(
        uiState          = uiState,
        onEvent          = viewModel::onEvent,
        onNavigateBack   = onNavigateBack,
        onNavigateResult = { onNavigateResult(sessionId) },
    )
}

// ═══════════════════════════════════════════════════════════════
//  PREVIEWS
// ═══════════════════════════════════════════════════════════════

@Preview(
    name = "Game — Playing 6×6",
    showBackground = true, backgroundColor = 0xFF050508,
    widthDp = 360, heightDp = 800,
)
@Composable
private fun PreviewPlaying() {
    KnightTourTheme { GameScreen(previewState(), {}, {}, {}) }
}

@Preview(
    name = "Game — Paused",
    showBackground = true, backgroundColor = 0xFF050508,
    widthDp = 360, heightDp = 800,
)
@Composable
private fun PreviewPaused() {
    KnightTourTheme {
        GameScreen(previewState().copy(gameState = GamePhase.PAUSED), {}, {}, {})
    }
}

@Preview(
    name = "Game — Victory",
    showBackground = true, backgroundColor = 0xFF050508,
    widthDp = 360, heightDp = 800,
)
@Composable
private fun PreviewVictory() {
    KnightTourTheme {
        GameScreen(
            previewState().copy(
                gameState  = GamePhase.COMPLETED,
                moveCount  = 36,
                totalCells = 36,
            ), {}, {}, {}
        )
    }
}

@Preview(
    name = "Game — Defeated",
    showBackground = true, backgroundColor = 0xFF050508,
    widthDp = 360, heightDp = 800,
)
@Composable
private fun PreviewDefeated() {
    KnightTourTheme {
        GameScreen(previewState().copy(gameState = GamePhase.FAILED), {}, {}, {})
    }
}

@Preview(
    name = "Game — Devil Mode / Panic",
    showBackground = true, backgroundColor = 0xFF050508,
    widthDp = 360, heightDp = 800,
)
@Composable
private fun PreviewDevil() {
    KnightTourTheme {
        GameScreen(
            previewState().copy(
                gameMode         = GameModeUi.DEVIL,
                difficulty       = DifficultyUi.DEVIL,
                isTimerPanic     = true,
                hintsRemaining   = 0,
            ), {}, {}, {}
        )
    }
}

// ── Preview data ─────────────────────────────────────────────────

private fun previewState(): GameUiState {
    val n = 6
    val visited = listOf(
        0 to 0, 1 to 2, 2 to 4, 4 to 5, 5 to 3,
        3 to 2, 4 to 0, 2 to 1, 0 to 2, 1 to 4,
    )
    val knight  = 2 to 5
    val valid   = listOf(0 to 4, 1 to 3, 3 to 4, 4 to 3)

    val cells = (0 until n).flatMap { row ->
        (0 until n).map { col ->
            val vi = visited.indexOfFirst { it.first == row && it.second == col }
            CellState(
                row         = row,
                col         = col,
                isVisited   = vi >= 0 || (row == knight.first && col == knight.second),
                isKnight    = row == knight.first && col == knight.second,
                isValidMove = valid.any { it.first == row && it.second == col },
                moveNumber  = if (vi >= 0) vi + 1
                else if (row == knight.first && col == knight.second)
                    visited.size + 1
                else 0,
            )
        }
    }

    return GameUiState(
        boardSize        = n,
        cells            = cells,
        moveCount        = visited.size + 1,
        totalCells       = n * n,
        elapsedSeconds   = 43,
        timeLimitSeconds = 240,
        gameState        = GamePhase.PLAYING,
        gameMode         = GameModeUi.OFFLINE,
        difficulty       = DifficultyUi.MEDIUM,
        canUndo          = true,
        hintsRemaining   = 1,
        currentScore     = 2780,
    )
}