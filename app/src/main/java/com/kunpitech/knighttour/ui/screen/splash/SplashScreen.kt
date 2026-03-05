package com.kunpitech.knighttour.ui.screen.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kunpitech.knighttour.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ═══════════════════════════════════════════════════════════════
//  KNIGHT TOUR — SPLASH SCREEN
//
//  Animation timeline (total ~3000ms):
//
//   0ms  ─── Background grid fades in
//  200ms ─── Outer ring draws (sweep animation)
//  400ms ─── Inner ring draws
//  600ms ─── Chess board grid appears inside ring
//  800ms ─── Knight piece scales + fades in
// 1000ms ─── L-path traces across board
// 1400ms ─── Title "KNIGHT" fades up
// 1700ms ─── Title "TOUR" fades up
// 2000ms ─── Tagline fades in
// 2200ms ─── Divider line draws left→right
// 2500ms ─── "THE DEVIL'S GAME" fades in
// 2800ms ─── Navigate to Home
// ═══════════════════════════════════════════════════════════════

@Composable
fun SplashScreen(
    onSplashComplete: () -> Unit,
) {
    // ── ANIMATION STATES ────────────────────────────────────────

    // Background grid
    var gridAlpha by remember { mutableFloatStateOf(0f) }

    // Rings
    var outerRingSweep by remember { mutableFloatStateOf(0f) }
    var innerRingSweep by remember { mutableFloatStateOf(0f) }
    var ringAlpha by remember { mutableFloatStateOf(0f) }

    // Board inside ring
    var boardAlpha by remember { mutableFloatStateOf(0f) }

    // Knight piece
    var knightAlpha by remember { mutableFloatStateOf(0f) }
    var knightScale by remember { mutableFloatStateOf(0.3f) }

    // L-path trace
    var pathProgress by remember { mutableFloatStateOf(0f) }

    // Text elements
    var titleLine1Alpha by remember { mutableFloatStateOf(0f) }
    var titleLine1Offset by remember { mutableFloatStateOf(30f) }
    var titleLine2Alpha by remember { mutableFloatStateOf(0f) }
    var titleLine2Offset by remember { mutableFloatStateOf(30f) }
    var taglineAlpha by remember { mutableFloatStateOf(0f) }
    var dividerProgress by remember { mutableFloatStateOf(0f) }
    var bottomTagAlpha by remember { mutableFloatStateOf(0f) }

    // Infinite glow pulse on knight
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "knightGlow",
    )
    val rotationAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue  = 360f,
        animationSpec = infiniteRepeatable(
            animation  = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "outerRingRotation",
    )

    // ── ORCHESTRATED ANIMATION SEQUENCE ─────────────────────────
    LaunchedEffect(Unit) {

        // 0ms — grid fades in
        launch {
            animate(0f, 1f, animationSpec = tween(600, easing = EaseOut)) { v, _ ->
                gridAlpha = v
            }
        }

        // 200ms — outer ring sweep
        delay(200)
        launch {
            animate(0f, 1f, animationSpec = tween(600, easing = EaseInOutCubic)) { v, _ ->
                ringAlpha = v
                outerRingSweep = v * 360f
            }
        }

        // 400ms — inner ring sweep
        delay(400)
        launch {
            animate(0f, 360f, animationSpec = tween(500, easing = EaseInOutCubic)) { v, _ ->
                innerRingSweep = v
            }
        }

        // 600ms — board alpha
        delay(600)
        launch {
            animate(0f, 1f, animationSpec = tween(400, easing = EaseOut)) { v, _ ->
                boardAlpha = v
            }
        }

        // 800ms — knight piece
        delay(800)
        launch {
            animate(0f, 1f, animationSpec = tween(500, easing = EaseOutBack)) { v, _ ->
                knightAlpha = v
                knightScale = 0.3f + v * 0.7f
            }
        }

        // 1000ms — L-path trace
        delay(1000)
        launch {
            animate(0f, 1f, animationSpec = tween(800, easing = EaseInOutCubic)) { v, _ ->
                pathProgress = v
            }
        }

        // 1400ms — "KNIGHT" title
        delay(1400)
        launch {
            animate(0f, 1f, animationSpec = tween(500, easing = EaseOut)) { v, _ ->
                titleLine1Alpha  = v
                titleLine1Offset = 30f * (1f - v)
            }
        }

        // 1700ms — "TOUR" title
        delay(1700)
        launch {
            animate(0f, 1f, animationSpec = tween(500, easing = EaseOut)) { v, _ ->
                titleLine2Alpha  = v
                titleLine2Offset = 30f * (1f - v)
            }
        }

        // 2000ms — tagline
        delay(2000)
        launch {
            animate(0f, 1f, animationSpec = tween(400, easing = EaseOut)) { v, _ ->
                taglineAlpha = v
            }
        }

        // 2200ms — divider
        delay(2200)
        launch {
            animate(0f, 1f, animationSpec = tween(400, easing = EaseInOut)) { v, _ ->
                dividerProgress = v
            }
        }

        // 2500ms — bottom tag
        delay(2500)
        launch {
            animate(0f, 1f, animationSpec = tween(400, easing = EaseOut)) { v, _ ->
                bottomTagAlpha = v
            }
        }

        // 2800ms — navigate
        delay(2800)
        onSplashComplete()
    }

    // ── UI ───────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AbyssBlack),
        contentAlignment = Alignment.Center,
    ) {

        // ── LAYER 1: BACKGROUND GRID ─────────────────────────────
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .alpha(gridAlpha),
        ) {
            drawBackgroundGrid(this)
        }

        // ── LAYER 2: RADIAL GLOW ─────────────────────────────────
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .alpha(ringAlpha * 0.6f),
        ) {
            drawRadialGlow(this, glowPulse)
        }

        // ── LAYER 3: CHESS EMBLEM ─────────────────────────────────
        Canvas(
            modifier = Modifier
                .size(260.dp)
                .alpha(ringAlpha),
        ) {
            drawChessEmblem(
                scope         = this,
                outerSweep    = outerRingSweep,
                innerSweep    = innerRingSweep,
                boardAlpha    = boardAlpha,
                knightAlpha   = knightAlpha,
                knightScale   = knightScale,
                pathProgress  = pathProgress,
                glowPulse     = glowPulse,
                rotationAngle = rotationAnim,
            )
        }

        // ── LAYER 4: TEXT CONTENT ─────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 40.dp)
                .offset(y = 160.dp),            // below the emblem
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {

            // "KNIGHT"
            Text(
                text     = "KNIGHT",
                style    = MaterialTheme.knightType.GameTitle.copy(
                    fontSize      = 48.sp,
                    letterSpacing = 12.sp,
                    textAlign     = TextAlign.Center,
                ),
                color    = CrownGold,
                modifier = Modifier
                    .alpha(titleLine1Alpha)
                    .offset(y = titleLine1Offset.dp),
            )

            // "TOUR"
            Text(
                text     = "TOUR",
                style    = MaterialTheme.knightType.GameTitle.copy(
                    fontSize      = 48.sp,
                    letterSpacing = 20.sp,
                    textAlign     = TextAlign.Center,
                ),
                color    = TextPrimary,
                modifier = Modifier
                    .alpha(titleLine2Alpha)
                    .offset(y = titleLine2Offset.dp),
            )

            Spacer(Modifier.height(20.dp))

            // Tagline
            Text(
                text     = "CONQUER EVERY SQUARE",
                style    = MaterialTheme.knightType.Eyebrow.copy(
                    letterSpacing = 6.sp,
                    textAlign     = TextAlign.Center,
                ),
                color    = TextTertiary,
                modifier = Modifier.alpha(taglineAlpha),
            )

            Spacer(Modifier.height(20.dp))

            // Animated divider
            Canvas(
                modifier = Modifier
                    .width(180.dp)
                    .height(1.dp)
                    .alpha(dividerProgress),
            ) {
                val lineWidth = size.width * dividerProgress
                drawLine(
                    brush       = Brush.horizontalGradient(
                        colors = listOf(Color.Transparent, KnightGold, Color.Transparent),
                    ),
                    start       = Offset(size.width / 2f - lineWidth / 2f, 0f),
                    end         = Offset(size.width / 2f + lineWidth / 2f, 0f),
                    strokeWidth = 1.dp.toPx(),
                )
            }

            Spacer(Modifier.height(20.dp))

            // "THE DEVIL'S GAME"
            Text(
                text     = "THE DEVIL'S GAME",
                style    = MaterialTheme.knightType.Eyebrow.copy(
                    letterSpacing = 5.sp,
                    textAlign     = TextAlign.Center,
                ),
                color    = DevilRed.copy(alpha = 0.8f),
                modifier = Modifier.alpha(bottomTagAlpha),
            )
        }

        // ── LAYER 5: VERSION TAG (bottom) ─────────────────────────
        Text(
            text      = "v1.0",
            style     = MaterialTheme.knightType.Caption,
            color     = TextTertiary.copy(alpha = 0.4f),
            modifier  = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .alpha(bottomTagAlpha),
        )
    }
}

// ═══════════════════════════════════════════════════════════════
//  CANVAS DRAW FUNCTIONS
// ═══════════════════════════════════════════════════════════════

/** Subtle dark grid across the full background */
private fun drawBackgroundGrid(scope: DrawScope) = with(scope) {
    val cellSize = 40.dp.toPx()
    val gridColor = KnightGold.copy(alpha = 0.04f)

    var x = 0f
    while (x <= size.width) {
        drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), 1f)
        x += cellSize
    }
    var y = 0f
    while (y <= size.height) {
        drawLine(gridColor, Offset(0f, y), Offset(size.width, y), 1f)
        y += cellSize
    }
}

/** Central radial glow behind the emblem */
private fun drawRadialGlow(scope: DrawScope, pulse: Float) = with(scope) {
    val cx = size.width / 2f
    val cy = size.height / 2f

    // Gold glow
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                KnightGold.copy(alpha = 0.12f * pulse),
                KnightGold.copy(alpha = 0.0f),
            ),
            center = Offset(cx, cy),
            radius = 400.dp.toPx(),
        ),
        radius = 400.dp.toPx(),
        center = Offset(cx, cy),
    )
    // Red accent glow (devil)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                DevilRed.copy(alpha = 0.05f * pulse),
                DevilRed.copy(alpha = 0.0f),
            ),
            center = Offset(cx, cy),
            radius = 250.dp.toPx(),
        ),
        radius = 250.dp.toPx(),
        center = Offset(cx, cy),
    )
}

/**
 * The main chess emblem:
 * - Outer rotating dashed ring with tick marks
 * - Inner solid ring
 * - 4×4 mini chess board
 * - Knight symbol at center
 * - L-shaped knight path trace
 */
private fun drawChessEmblem(
    scope         : DrawScope,
    outerSweep    : Float,
    innerSweep    : Float,
    boardAlpha    : Float,
    knightAlpha   : Float,
    knightScale   : Float,
    pathProgress  : Float,
    glowPulse     : Float,
    rotationAngle : Float,
) = with(scope) {

    val cx         = size.width / 2f
    val cy         = size.height / 2f
    val outerR     = size.width / 2f - 4.dp.toPx()
    val innerR     = outerR - 18.dp.toPx()
    val boardSize  = innerR * 1.2f
    val cellSize   = boardSize / 4f

    // ── OUTER RING — sweeping arc ────────────────────────────────
    rotate(rotationAngle, Offset(cx, cy)) {
        // Dashed outer ring
        drawArc(
            color       = KnightGold.copy(alpha = 0.3f),
            startAngle  = -90f,
            sweepAngle  = outerSweep,
            useCenter   = false,
            topLeft     = Offset(cx - outerR, cy - outerR),
            size        = Size(outerR * 2, outerR * 2),
            style       = Stroke(
                width       = 1.5.dp.toPx(),
                pathEffect  = PathEffect.dashPathEffect(
                    floatArrayOf(6.dp.toPx(), 4.dp.toPx()), 0f
                ),
            ),
        )
    }

    // Tick marks around the ring (every 15°)
    for (i in 0 until 24) {
        val angle     = Math.toRadians((i * 15.0) - 90.0)
        val tickOuter = outerR
        val tickInner = outerR - if (i % 6 == 0) 10.dp.toPx() else 5.dp.toPx()
        val alpha     = if (outerSweep >= i * 15f) 0.5f else 0f
        drawLine(
            color       = KnightGold.copy(alpha = alpha),
            start       = Offset(cx + (tickInner * Math.cos(angle)).toFloat(),
                cy + (tickInner * Math.sin(angle)).toFloat()),
            end         = Offset(cx + (tickOuter * Math.cos(angle)).toFloat(),
                cy + (tickOuter * Math.sin(angle)).toFloat()),
            strokeWidth = if (i % 6 == 0) 1.5.dp.toPx() else 0.8.dp.toPx(),
        )
    }

    // ── INNER RING ───────────────────────────────────────────────
    drawArc(
        color      = KnightGold.copy(alpha = 0.6f),
        startAngle = -90f,
        sweepAngle = innerSweep,
        useCenter  = false,
        topLeft    = Offset(cx - innerR, cy - innerR),
        size       = Size(innerR * 2, innerR * 2),
        style      = Stroke(width = 1.dp.toPx()),
    )

    // Corner accent diamonds at 0°, 90°, 180°, 270°
    val corners = listOf(0.0, 90.0, 180.0, 270.0)
    corners.forEachIndexed { idx, deg ->
        val show  = innerSweep >= idx * 90f
        if (!show) return@forEachIndexed
        val angle = Math.toRadians(deg - 90.0)
        val px    = cx + (innerR * Math.cos(angle)).toFloat()
        val py    = cy + (innerR * Math.sin(angle)).toFloat()
        val d     = 4.dp.toPx()
        drawPath(
            path = Path().apply {
                moveTo(px, py - d); lineTo(px + d, py)
                lineTo(px, py + d); lineTo(px - d, py)
                close()
            },
            color = CrownGold,
        )
    }

    // ── MINI CHESS BOARD (4×4) ───────────────────────────────────
    if (boardAlpha > 0f) {
        val bLeft = cx - boardSize / 2f
        val bTop  = cy - boardSize / 2f

        for (row in 0 until 4) {
            for (col in 0 until 4) {
                val isLight = (row + col) % 2 == 0
                drawRect(
                    color   = if (isLight)
                        CellLight.copy(alpha = boardAlpha * 0.9f)
                    else
                        CellDark.copy(alpha = boardAlpha * 0.9f),
                    topLeft = Offset(bLeft + col * cellSize, bTop + row * cellSize),
                    size    = Size(cellSize, cellSize),
                )
            }
        }

        // Board border
        drawRect(
            color   = KnightGold.copy(alpha = boardAlpha * 0.4f),
            topLeft = Offset(bLeft, bTop),
            size    = Size(boardSize, boardSize),
            style   = Stroke(width = 0.5.dp.toPx()),
        )
    }

    // ── L-PATH TRACE ─────────────────────────────────────────────
    if (pathProgress > 0f && boardAlpha > 0f) {
        val bLeft = cx - boardSize / 2f
        val bTop  = cy - boardSize / 2f

        // Knight L-move path: (3,0)→(1,1)→(3,2)→(1,3)→(2,1)→(0,2)
        val pathCells = listOf(
            Offset(3f, 0f), Offset(1f, 1f), Offset(3f, 2f),
            Offset(1f, 3f), Offset(2f, 1f), Offset(0f, 2f),
        )

        val totalSegments = pathCells.size - 1
        val progressCells = pathProgress * totalSegments
        val fullSegments  = progressCells.toInt().coerceAtMost(totalSegments - 1)
        val segFraction   = progressCells - fullSegments

        fun cellCenter(c: Offset) = Offset(
            bLeft + (c.x + 0.5f) * cellSize,
            bTop  + (c.y + 0.5f) * cellSize,
        )

        // Draw completed segments
        for (i in 0 until fullSegments) {
            val alpha = 0.3f + (i.toFloat() / totalSegments) * 0.5f
            drawLine(
                color       = KnightGold.copy(alpha = alpha * boardAlpha),
                start       = cellCenter(pathCells[i]),
                end         = cellCenter(pathCells[i + 1]),
                strokeWidth = 1.5.dp.toPx(),
                cap         = StrokeCap.Round,
            )
        }

        // Draw partial current segment
        if (fullSegments < totalSegments) {
            val segStart = cellCenter(pathCells[fullSegments])
            val segEnd   = cellCenter(pathCells[fullSegments + 1])
            val partEnd  = Offset(
                segStart.x + (segEnd.x - segStart.x) * segFraction,
                segStart.y + (segEnd.y - segStart.y) * segFraction,
            )
            drawLine(
                color       = KnightGold.copy(alpha = 0.7f * boardAlpha),
                start       = segStart,
                end         = partEnd,
                strokeWidth = 1.5.dp.toPx(),
                cap         = StrokeCap.Round,
            )
        }

        // Visited cell dots
        for (i in 0..fullSegments) {
            val dotCenter = cellCenter(pathCells[i])
            drawCircle(
                color  = KnightGold.copy(alpha = 0.6f * boardAlpha),
                radius = 2.5.dp.toPx(),
                center = dotCenter,
            )
        }
    }

    // ── KNIGHT GLOW ──────────────────────────────────────────────
    if (knightAlpha > 0f) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    KnightGold.copy(alpha = 0.25f * knightAlpha * glowPulse),
                    KnightGold.copy(alpha = 0f),
                ),
                center = Offset(cx, cy),
                radius = 40.dp.toPx(),
            ),
            radius = 40.dp.toPx(),
            center = Offset(cx, cy),
        )
    }
}

// ═══════════════════════════════════════════════════════════════
//  PREVIEW
// ═══════════════════════════════════════════════════════════════

@Preview(
    name           = "Splash Screen",
    showBackground = true,
    backgroundColor = 0xFF050508,
    widthDp        = 360,
    heightDp       = 800,
)
@Composable
private fun SplashScreenPreview() {
    KnightTourTheme {
        // Static preview — show mid-animation state
        SplashScreenStatic()
    }
}

/** Static version for preview only — shows final fully-revealed state */
@Composable
private fun SplashScreenStatic() {
    Box(
        modifier            = Modifier
            .fillMaxSize()
            .background(AbyssBlack),
        contentAlignment    = Alignment.Center,
    ) {
        // Background grid
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawBackgroundGrid(this)
        }

        // Radial glow
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRadialGlow(this, 0.8f)
        }

        // Emblem
        Canvas(modifier = Modifier.size(260.dp)) {
            drawChessEmblem(
                scope         = this,
                outerSweep    = 360f,
                innerSweep    = 360f,
                boardAlpha    = 1f,
                knightAlpha   = 1f,
                knightScale   = 1f,
                pathProgress  = 0.8f,
                glowPulse     = 0.8f,
                rotationAngle = 0f,
            )
        }

        // Text
        Column(
            modifier                = Modifier
                .fillMaxWidth()
                .padding(horizontal = 40.dp)
                .offset(y = 160.dp),
            horizontalAlignment     = Alignment.CenterHorizontally,
            verticalArrangement     = Arrangement.spacedBy(0.dp),
        ) {
            Text(
                text  = "KNIGHT",
                style = MaterialTheme.knightType.GameTitle.copy(
                    fontSize      = 48.sp,
                    letterSpacing = 12.sp,
                    textAlign     = TextAlign.Center,
                ),
                color = CrownGold,
            )
            Text(
                text  = "TOUR",
                style = MaterialTheme.knightType.GameTitle.copy(
                    fontSize      = 48.sp,
                    letterSpacing = 20.sp,
                    textAlign     = TextAlign.Center,
                ),
                color = TextPrimary,
            )
            Spacer(Modifier.height(20.dp))
            Text(
                text  = "CONQUER EVERY SQUARE",
                style = MaterialTheme.knightType.Eyebrow.copy(letterSpacing = 6.sp),
                color = TextTertiary,
            )
            Spacer(Modifier.height(20.dp))
            Canvas(modifier = Modifier.width(180.dp).height(1.dp)) {
                drawLine(
                    brush       = Brush.horizontalGradient(
                        listOf(Color.Transparent, KnightGold, Color.Transparent)
                    ),
                    start       = Offset(0f, 0f),
                    end         = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            Spacer(Modifier.height(20.dp))
            Text(
                text  = "THE DEVIL'S GAME",
                style = MaterialTheme.knightType.Eyebrow.copy(letterSpacing = 5.sp),
                color = DevilRed.copy(alpha = 0.8f),
            )
        }

        Text(
            text     = "v1.0",
            style    = MaterialTheme.knightType.Caption,
            color    = TextTertiary.copy(alpha = 0.4f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
        )
    }
}