package com.kunpitech.knighttour.ui.screen.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

// ═══════════════════════════════════════════════════════════════
//  ONBOARDING SCREEN — Username Selection
//  First-launch screen. Shown once, then user is taken to Home.
// ═══════════════════════════════════════════════════════════════

// ── Color palette (matches game theme) ──────────────────────────
private val Background  = Color(0xFF050508)
private val Surface     = Color(0xFF0D0D14)
private val Gold        = Color(0xFFD4A843)
private val GoldDim     = Color(0xFF8A6B2A)
private val Red         = Color(0xFFB84444)
private val Green       = Color(0xFF44B877)
private val TextPrimary = Color(0xFFE8E0CC)
private val TextDim     = Color(0xFF6B6560)
private val BorderIdle  = Color(0xFF2A2830)

@Composable
fun OnboardingRoute(
    onComplete : () -> Unit,
    viewModel  : OnboardingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    OnboardingScreen(
        uiState         = uiState,
        onUsernameChange = viewModel::onUsernameChanged,
        onConfirm        = { viewModel.onConfirm(onComplete) },
    )
}

@Composable
fun OnboardingScreen(
    uiState          : OnboardingUiState,
    onUsernameChange : (String) -> Unit,
    onConfirm        : () -> Unit,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    // Auto-focus input on launch
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
        contentAlignment = Alignment.Center,
    ) {
        // Subtle radial glow behind the card
        Box(
            modifier = Modifier
                .size(320.dp)
                .background(
                    Brush.radialGradient(
                        listOf(Gold.copy(alpha = 0.06f), Color.Transparent)
                    )
                )
        )

        Column(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {

            // ── Knight icon ──────────────────────────────────────
            Text(
                text     = "♞",
                fontSize = 56.sp,
                color    = Gold,
            )

            Spacer(Modifier.height(16.dp))

            // ── Title ────────────────────────────────────────────
            Text(
                text       = "CHOOSE YOUR NAME",
                fontSize   = 13.sp,
                fontWeight = FontWeight.Bold,
                color      = Gold,
                letterSpacing = 4.sp,
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text      = "This is how opponents will know you",
                fontSize  = 13.sp,
                color     = TextDim,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(32.dp))

            // ── Input field ──────────────────────────────────────
            val borderColor = when (uiState.status) {
                is UsernameStatus.Available    -> Green
                is UsernameStatus.Taken,
                is UsernameStatus.InvalidChars -> Red
                is UsernameStatus.Checking     -> Gold.copy(alpha = 0.5f)
                else                           -> BorderIdle
            }

            val animatedBorderColor by animateColorAsState(
                targetValue = borderColor,
                animationSpec = tween(200),
                label = "border"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Surface)
                    .border(1.dp, animatedBorderColor, RoundedCornerShape(12.dp))
                    .padding(horizontal = 18.dp, vertical = 16.dp),
            ) {
                BasicTextField(
                    value         = uiState.username,
                    onValueChange = onUsernameChange,
                    modifier      = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    textStyle     = TextStyle(
                        color      = TextPrimary,
                        fontSize   = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp,
                    ),
                    cursorBrush   = SolidColor(Gold),
                    singleLine    = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction      = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            keyboard?.hide()
                            if (uiState.canSubmit) onConfirm()
                        }
                    ),
                    decorationBox = { inner ->
                        Box {
                            if (uiState.username.isEmpty()) {
                                Text(
                                    text     = "e.g. Knight Mooncha",
                                    color    = TextDim,
                                    fontSize = 20.sp,
                                )
                            }
                            inner()
                        }
                    }
                )
            }

            Spacer(Modifier.height(10.dp))

            // ── Status message ───────────────────────────────────
            AnimatedContent(
                targetState   = uiState.status,
                transitionSpec = {
                    fadeIn(tween(150)) togetherWith fadeOut(tween(100))
                },
                label = "status",
            ) { status ->
                val (msg, color) = when (status) {
                    is UsernameStatus.Idle         -> "" to TextDim
                    is UsernameStatus.Checking     -> "Checking availability…" to Gold.copy(alpha = 0.7f)
                    is UsernameStatus.Available    -> "✓  Available!" to Green
                    is UsernameStatus.Taken        -> "✗  Already taken — try another" to Red
                    is UsernameStatus.TooShort     -> "At least 3 characters required" to TextDim
                    is UsernameStatus.TooLong      -> "Maximum 16 characters" to TextDim
                    is UsernameStatus.InvalidChars -> "Letters, numbers, spaces and _ only" to Red
                }
                Text(
                    text      = msg,
                    color     = color,
                    fontSize  = 12.sp,
                    modifier  = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                )
            }

            // Char counter
            Text(
                text     = "${uiState.username.trim().length}/16",
                color    = if (uiState.username.trim().length >= 14) Gold else TextDim,
                fontSize = 11.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End,
            )

            Spacer(Modifier.height(28.dp))

            // ── Confirm button ───────────────────────────────────
            val buttonAlpha by animateFloatAsState(
                targetValue = if (uiState.canSubmit) 1f else 0.35f,
                label = "btnAlpha"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .alpha(buttonAlpha)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (uiState.canSubmit)
                            Brush.horizontalGradient(listOf(Gold, GoldDim))
                        else
                            Brush.horizontalGradient(listOf(GoldDim.copy(0.3f), GoldDim.copy(0.3f)))
                    )
                    .then(
                        if (uiState.canSubmit && !uiState.isSubmitting)
                            Modifier.clickable {
                                keyboard?.hide()
                                onConfirm()
                            }
                        else Modifier
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (uiState.isSubmitting) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(20.dp),
                        color       = Background,
                        strokeWidth = 2.5.dp,
                    )
                } else {
                    Text(
                        text       = "ENTER THE BOARD",
                        color      = Background,
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Rules hint ───────────────────────────────────────
            Text(
                text      = "Your name will appear on the leaderboard\nand in online matches",
                color     = TextDim,
                fontSize  = 11.sp,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp,
            )
        }
    }
}