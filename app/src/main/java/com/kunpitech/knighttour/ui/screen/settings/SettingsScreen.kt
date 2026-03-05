package com.kunpitech.knighttour.ui.screen.settings

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.kunpitech.knighttour.ui.theme.Scrim
import com.kunpitech.knighttour.ui.theme.SurfaceDark
import com.kunpitech.knighttour.ui.theme.SurfaceElevated
import com.kunpitech.knighttour.ui.theme.SurfaceHighest
import com.kunpitech.knighttour.ui.theme.TextPrimary
import com.kunpitech.knighttour.ui.theme.TextSecondary
import com.kunpitech.knighttour.ui.theme.TextTertiary
import com.kunpitech.knighttour.ui.theme.VictoryGreen
import com.kunpitech.knighttour.ui.theme.knightType
import kotlinx.coroutines.delay

// ===============================================================
//  KNIGHT TOUR -- SETTINGS SCREEN
//
//  Sections (scrollable):
//    TopBar: back + title + saved flash
//    AccountCard: avatar, name, sign in/out
//    Section: Sound & Haptics  (3 toggles)
//    Section: Gameplay         (difficulty picker + 3 toggles)
//    Section: Display          (board theme selector 4 options)
//    Section: Data             (clear data with confirm dialog)
//    AppVersion footer
// ===============================================================

@Composable
fun SettingsScreen(
    uiState        : SettingsUiState,
    onEvent        : (SettingsEvent) -> Unit,
    onNavigateBack : () -> Unit,
) {
    // Staggered entry
    val headerAlpha  = remember { Animatable(0f) }
    val section1Alpha = remember { Animatable(0f) }
    val section2Alpha = remember { Animatable(0f) }
    val section3Alpha = remember { Animatable(0f) }
    val section4Alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        headerAlpha.animateTo(1f,   tween(380, easing = EaseOut))
        delay(50)
        section1Alpha.animateTo(1f, tween(320, easing = EaseOut))
        delay(40)
        section2Alpha.animateTo(1f, tween(320, easing = EaseOut))
        delay(40)
        section3Alpha.animateTo(1f, tween(320, easing = EaseOut))
        delay(40)
        section4Alpha.animateTo(1f, tween(320, easing = EaseOut))
    }

    // Confirm clear-data dialog state
    var showClearConfirm by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AbyssBlack)
            .systemBarsPadding(),
    ) {

        // Background
        SettingsBackground(Modifier.fillMaxSize())

        // Scrollable content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {

            Spacer(Modifier.height(12.dp))

            // Top Bar
            SettingsTopBar(
                savedFeedback  = uiState.savedFeedback,
                onBack         = onNavigateBack,
                modifier       = Modifier
                    .fillMaxWidth()
                    .alpha(headerAlpha.value),
            )

            Spacer(Modifier.height(20.dp))

            // Account
            AccountCard(
                uiState  = uiState,
                onEvent  = onEvent,
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(section1Alpha.value),
            )

            Spacer(Modifier.height(24.dp))

            // Sound & Haptics
            SettingsSection(
                title    = "SOUND & HAPTICS",
                icon     = "\uD83D\uDD0A",
                modifier = Modifier.alpha(section1Alpha.value),
            ) {
                ToggleRow(
                    label       = "Sound Effects",
                    description = "Board taps, moves, victory",
                    checked     = uiState.soundEnabled,
                    onToggle    = { onEvent(SettingsEvent.SoundToggled(it)) },
                )
                SectionDivider()
                ToggleRow(
                    label       = "Background Music",
                    description = "Atmospheric chess soundtrack",
                    checked     = uiState.musicEnabled,
                    onToggle    = { onEvent(SettingsEvent.MusicToggled(it)) },
                )
                SectionDivider()
                ToggleRow(
                    label       = "Haptic Feedback",
                    description = "Vibration on moves and events",
                    checked     = uiState.hapticsEnabled,
                    onToggle    = { onEvent(SettingsEvent.HapticsToggled(it)) },
                )
            }

            Spacer(Modifier.height(16.dp))

            // Gameplay
            SettingsSection(
                title    = "GAMEPLAY",
                icon     = "\u265E",
                modifier = Modifier.alpha(section2Alpha.value),
            ) {
                // Default difficulty picker
                DifficultyPicker(
                    selected  = uiState.defaultDifficulty,
                    onSelect  = { onEvent(SettingsEvent.DifficultyChanged(it)) },
                )
                SectionDivider()
                ToggleRow(
                    label       = "Show Move Numbers",
                    description = "Display step count inside visited cells",
                    checked     = uiState.showMoveNumbers,
                    onToggle    = { onEvent(SettingsEvent.MoveNumbersToggled(it)) },
                )
                SectionDivider()
                ToggleRow(
                    label       = "Highlight Valid Moves",
                    description = "Show red dots for reachable cells",
                    checked     = uiState.showValidMoves,
                    onToggle    = { onEvent(SettingsEvent.ValidMovesToggled(it)) },
                )
                SectionDivider()
                ToggleRow(
                    label       = "Auto-Hint",
                    description = "Subtly highlight the Warnsdorff suggestion",
                    checked     = uiState.autoHint,
                    onToggle    = { onEvent(SettingsEvent.AutoHintToggled(it)) },
                    tint        = DevilRed,
                    badge       = "DEVIL MODE",
                )
            }

            Spacer(Modifier.height(16.dp))

            // Board Theme
            SettingsSection(
                title    = "BOARD THEME",
                icon     = "\u25A0",
                modifier = Modifier.alpha(section3Alpha.value),
            ) {
                BoardThemePicker(
                    selected = uiState.boardTheme,
                    onSelect = { onEvent(SettingsEvent.BoardThemeChanged(it)) },
                )
            }

            Spacer(Modifier.height(16.dp))

            // Data
            SettingsSection(
                title    = "DATA",
                icon     = "\u2263",
                modifier = Modifier.alpha(section4Alpha.value),
            ) {
                DangerRow(
                    label       = "Clear All Game Data",
                    description = "Erase scores, history, and saved games",
                    buttonLabel = "CLEAR",
                    tint        = BloodRedBright,
                    onClick     = { showClearConfirm = true },
                )
            }

            Spacer(Modifier.height(32.dp))

            // Version footer
            Text(
                text      = "KNIGHT TOUR  v1.0.0",
                style     = MaterialTheme.knightType.Eyebrow.copy(letterSpacing = 3.sp),
                color     = TextTertiary.copy(alpha = 0.5f),
                modifier  = Modifier
                    .fillMaxWidth()
                    .alpha(section4Alpha.value),
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(32.dp))
        }

        // Clear confirm dialog
        AnimatedVisibility(
            visible = showClearConfirm,
            enter   = fadeIn() + scaleIn(initialScale = 0.88f),
            exit    = fadeOut() + scaleOut(targetScale = 0.88f),
        ) {
            ConfirmDialog(
                title       = "CLEAR ALL DATA?",
                body        = "This will erase all scores, game history, and saved sessions. This cannot be undone.",
                confirmText = "CLEAR",
                cancelText  = "CANCEL",
                confirmTint = BloodRedBright,
                onConfirm   = {
                    showClearConfirm = false
                    onEvent(SettingsEvent.ClearData)
                },
                onCancel    = { showClearConfirm = false },
            )
        }
    }
}

// ===============================================================
//  BACKGROUND
// ===============================================================

@Composable
private fun SettingsBackground(modifier: Modifier) {
    Canvas(modifier = modifier) {
        val gc = KnightGold.copy(alpha = 0.022f)
        val g  = 38.dp.toPx()
        var x = 0f; while (x <= size.width)  { drawLine(gc, Offset(x,0f), Offset(x,size.height), 1f); x += g }
        var y = 0f; while (y <= size.height) { drawLine(gc, Offset(0f,y), Offset(size.width,y), 1f);   y += g }
    }
}

// ===============================================================
//  TOP BAR
// ===============================================================

@Composable
private fun SettingsTopBar(
    savedFeedback : Boolean,
    onBack        : () -> Unit,
    modifier      : Modifier = Modifier,
) {
    Row(
        modifier              = modifier,
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        SettingsIconBtn(icon = "\u2039", onClick = onBack)

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text  = "SETTINGS",
                style = MaterialTheme.knightType.ScreenHeader.copy(letterSpacing = 5.sp),
                color = CrownGold,
            )
        }

        // "Saved" flash feedback chip (replaces the right icon when active)
        AnimatedVisibility(
            visible = savedFeedback,
            enter   = fadeIn(tween(200)) + scaleIn(initialScale = 0.8f),
            exit    = fadeOut(tween(300)),
        ) {
            Box(
                modifier = Modifier
                    .background(VictoryGreen.copy(alpha = 0.15f), KnightTourShapes.small)
                    .border(BorderWidth.thin, VictoryGreen.copy(alpha = 0.4f), KnightTourShapes.small)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text  = "\u2713  SAVED",
                    style = MaterialTheme.knightType.BadgeText,
                    color = VictoryGreen,
                )
            }
        }
        if (!savedFeedback) {
            Spacer(Modifier.size(36.dp))
        }
    }
}

// ===============================================================
//  ACCOUNT CARD
// ===============================================================

@Composable
private fun AccountCard(
    uiState  : SettingsUiState,
    onEvent  : (SettingsEvent) -> Unit,
    modifier : Modifier = Modifier,
) {
    val inf = rememberInfiniteTransition(label = "acc")
    val pulse by inf.animateFloat(
        initialValue  = 0.6f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1600, easing = EaseInOutSine), RepeatMode.Reverse),
        label         = "accPulse",
    )

    Row(
        modifier = modifier
            .background(
                Brush.linearGradient(
                    listOf(GoldDim, SurfaceDark),
                    start = Offset(0f, 0f),
                    end   = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
                ),
                KnightTourShapes.large,
            )
            .border(
                BorderWidth.default,
                KnightGold.copy(alpha = if (uiState.isSignedIn) 0.35f else 0.20f),
                KnightTourShapes.large,
            )
            .padding(16.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Avatar
        Box(
            modifier         = Modifier.size(56.dp),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val cx = size.width / 2f; val cy = size.height / 2f
                if (uiState.isSignedIn) {
                    drawCircle(
                        brush  = Brush.radialGradient(
                            listOf(KnightGold.copy(alpha = 0.25f * pulse), Color.Transparent),
                            center = Offset(cx, cy), radius = cx,
                        ),
                        radius = cx, center = Offset(cx, cy),
                    )
                }
                drawCircle(
                    color  = KnightGold.copy(alpha = if (uiState.isSignedIn) 0.5f else 0.2f),
                    radius = cx - 2.dp.toPx(), center = Offset(cx, cy),
                    style  = Stroke(1.5.dp.toPx()),
                )
            }
            Text(
                text     = "\u265E",
                fontSize = 28.sp,
                color    = if (uiState.isSignedIn) CrownGold else TextTertiary,
            )
        }

        // Name + status
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text  = uiState.playerName,
                style = MaterialTheme.knightType.CardTitle.copy(fontSize = 16.sp),
                color = if (uiState.isSignedIn) TextPrimary else TextSecondary,
            )
            Text(
                text  = if (uiState.isSignedIn) "SIGNED IN" else "PLAYING OFFLINE",
                style = MaterialTheme.knightType.BadgeText,
                color = if (uiState.isSignedIn) VictoryGreen else TextTertiary,
            )
        }

        // Sign in / out button
        Box(
            modifier = Modifier
                .background(
                    if (uiState.isSignedIn) RedDim else GoldDim,
                    KnightTourShapes.small,
                )
                .border(
                    BorderWidth.thin,
                    if (uiState.isSignedIn) BloodRedBright.copy(alpha = 0.35f)
                    else KnightGold.copy(alpha = 0.4f),
                    KnightTourShapes.small,
                )
                .clickable {
                    if (uiState.isSignedIn) onEvent(SettingsEvent.SignOut)
                    else onEvent(SettingsEvent.SignIn)
                }
                .padding(horizontal = 14.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text  = if (uiState.isSignedIn) "SIGN OUT" else "SIGN IN",
                style = MaterialTheme.knightType.ButtonSecondary.copy(fontSize = 11.sp),
                color = if (uiState.isSignedIn) BloodRedBright else KnightGold,
            )
        }
    }
}

// ===============================================================
//  SETTINGS SECTION WRAPPER
// ===============================================================

@Composable
private fun SettingsSection(
    title    : String,
    icon     : String,
    modifier : Modifier = Modifier,
    content  : @Composable () -> Unit,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(0.dp)) {
        // Section header
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(icon, fontSize = 13.sp, color = TextTertiary)
            Text(
                text  = title,
                style = MaterialTheme.knightType.Eyebrow.copy(letterSpacing = 4.sp),
                color = TextTertiary,
            )
        }

        // Card wrapping all rows
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceDark, KnightTourShapes.medium)
                .border(BorderWidth.thin, BorderDefault, KnightTourShapes.medium),
        ) {
            content()
        }
    }
}

// ===============================================================
//  TOGGLE ROW
// ===============================================================

@Composable
private fun ToggleRow(
    label       : String,
    description : String,
    checked     : Boolean,
    onToggle    : (Boolean) -> Unit,
    tint        : Color   = KnightGold,
    badge       : String? = null,
    modifier    : Modifier = Modifier,
) {
    val trackAlpha by animateFloatAsState(
        targetValue   = if (checked) 1f else 0f,
        animationSpec = tween(200),
        label         = "trackAlpha",
    )
    val thumbX by animateFloatAsState(
        targetValue   = if (checked) 1f else 0f,
        animationSpec = tween(220, easing = EaseOutBack),
        label         = "thumbX",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onToggle(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Text(
                    text  = label,
                    style = MaterialTheme.knightType.BodyPrimary.copy(fontSize = 14.sp),
                    color = TextPrimary,
                )
                if (badge != null) {
                    Box(
                        modifier = Modifier
                            .background(tint.copy(alpha = 0.12f), RoundedSmall)
                            .border(BorderWidth.thin, tint.copy(alpha = 0.3f), RoundedSmall)
                            .padding(horizontal = 5.dp, vertical = 1.dp),
                    ) {
                        Text(
                            text  = badge,
                            style = MaterialTheme.knightType.BadgeText,
                            color = tint,
                        )
                    }
                }
            }
            Text(
                text  = description,
                style = MaterialTheme.knightType.Caption,
                color = TextTertiary,
            )
        }

        // Custom toggle switch
        Box(
            modifier = Modifier
                .width(46.dp)
                .height(26.dp)
                .background(
                    if (checked) tint.copy(alpha = 0.25f * trackAlpha + 0.08f)
                    else SurfaceHighest,
                    CircleShape,
                )
                .border(
                    BorderWidth.thin,
                    if (checked) tint.copy(alpha = 0.5f) else BorderDefault,
                    CircleShape,
                ),
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                modifier = Modifier
                    .padding(start = (thumbX * 20f + 3f).dp)
                    .size(20.dp)
                    .background(
                        if (checked) tint else TextTertiary.copy(alpha = 0.4f),
                        CircleShape,
                    ),
            )
        }
    }
}

// ===============================================================
//  DIFFICULTY PICKER
// ===============================================================

@Composable
private fun DifficultyPicker(
    selected : DefaultDiff,
    onSelect : (DefaultDiff) -> Unit,
    modifier : Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text  = "DEFAULT DIFFICULTY",
            style = MaterialTheme.knightType.Caption,
            color = TextTertiary,
        )
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            DefaultDiff.entries.forEach { diff ->
                val isSelected = diff == selected
                val isDevil    = diff == DefaultDiff.DEVIL

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isSelected)
                                if (isDevil) RedDim else GoldDim
                            else SurfaceElevated,
                            KnightTourShapes.extraSmall,
                        )
                        .border(
                            BorderWidth.thin,
                            when {
                                isSelected && isDevil  -> BloodRedBright.copy(alpha = 0.5f)
                                isSelected             -> KnightGold.copy(alpha = 0.5f)
                                else                   -> BorderSubtle
                            },
                            KnightTourShapes.extraSmall,
                        )
                        .clickable { onSelect(diff) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text  = diff.label,
                        style = MaterialTheme.knightType.BodyPrimary.copy(fontSize = 13.sp),
                        color = when {
                            isSelected && isDevil  -> BloodRedBright
                            isSelected             -> CrownGold
                            else                   -> TextSecondary
                        },
                    )
                    if (isSelected) {
                        Text(
                            text  = "\u2713",
                            fontSize = 14.sp,
                            color = if (isDevil) BloodRedBright else CrownGold,
                        )
                    }
                }
            }
        }
    }
}

// ===============================================================
//  BOARD THEME PICKER
// ===============================================================

@Composable
private fun BoardThemePicker(
    selected : BoardTheme,
    onSelect : (BoardTheme) -> Unit,
    modifier : Modifier = Modifier,
) {
    // Theme preview colors
    val themeColors: Map<BoardTheme, Pair<Color, Color>> = mapOf(
        BoardTheme.OBSIDIAN to (Color(0xFF1A1A2E) to CrownGold),
        BoardTheme.CRIMSON  to (Color(0xFF2C0A0A) to BloodRedBright),
        BoardTheme.ABYSS    to (Color(0xFF0A1628) to Color(0xFF90CAF9)),
        BoardTheme.VOID     to (Color(0xFF111111) to Color(0xFFEEEEEE)),
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        BoardTheme.entries.forEach { theme ->
            val isSelected            = theme == selected
            val (bgColor, accentColor) = themeColors[theme]!!

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (isSelected) accentColor.copy(alpha = 0.08f) else SurfaceElevated,
                        KnightTourShapes.small,
                    )
                    .border(
                        BorderWidth.thin,
                        if (isSelected) accentColor.copy(alpha = 0.45f) else BorderSubtle,
                        KnightTourShapes.small,
                    )
                    .clickable { onSelect(theme) }
                    .padding(12.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // Mini board preview
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(bgColor, KnightTourShapes.extraSmall)
                        .border(BorderWidth.thin, accentColor.copy(alpha = 0.4f), KnightTourShapes.extraSmall),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("\u265E", fontSize = 18.sp, color = accentColor)
                }

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text  = theme.label,
                        style = MaterialTheme.knightType.BodyPrimary.copy(fontSize = 13.sp),
                        color = if (isSelected) accentColor else TextPrimary,
                    )
                    Text(
                        text  = theme.description,
                        style = MaterialTheme.knightType.Caption,
                        color = TextTertiary,
                    )
                }

                if (isSelected) {
                    Text("\u2713", fontSize = 14.sp, color = accentColor)
                }
            }
        }
    }
}

// ===============================================================
//  DANGER ROW (Clear Data)
// ===============================================================

@Composable
private fun DangerRow(
    label       : String,
    description : String,
    buttonLabel : String,
    tint        : Color,
    onClick     : () -> Unit,
    modifier    : Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text  = label,
                style = MaterialTheme.knightType.BodyPrimary.copy(fontSize = 14.sp),
                color = tint,
            )
            Text(
                text  = description,
                style = MaterialTheme.knightType.Caption,
                color = TextTertiary,
            )
        }
        Box(
            modifier = Modifier
                .background(tint.copy(alpha = 0.10f), KnightTourShapes.small)
                .border(BorderWidth.thin, tint.copy(alpha = 0.35f), KnightTourShapes.small)
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text  = buttonLabel,
                style = MaterialTheme.knightType.ButtonSecondary.copy(fontSize = 11.sp),
                color = tint,
            )
        }
    }
}

// ===============================================================
//  CONFIRM DIALOG
// ===============================================================

@Composable
private fun ConfirmDialog(
    title       : String,
    body        : String,
    confirmText : String,
    cancelText  : String,
    confirmTint : Color,
    onConfirm   : () -> Unit,
    onCancel    : () -> Unit,
) {
    Box(
        modifier         = Modifier.fillMaxSize().background(Scrim),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 32.dp)
                .background(
                    Brush.verticalGradient(listOf(SurfaceElevated, SurfaceDark)),
                    KnightTourShapes.large,
                )
                .border(BorderWidth.default, confirmTint.copy(alpha = 0.3f), KnightTourShapes.large)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text  = "\u26A0",
                fontSize = 32.sp,
                color = confirmTint,
            )
            Text(
                text      = title,
                style     = MaterialTheme.knightType.ScreenHeader.copy(letterSpacing = 3.sp),
                color     = confirmTint,
                textAlign = TextAlign.Center,
            )
            Text(
                text      = body,
                style     = MaterialTheme.knightType.BodySecondary,
                color     = TextSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f).height(46.dp)
                        .background(SurfaceHighest, KnightTourShapes.small)
                        .border(BorderWidth.thin, BorderDefault, KnightTourShapes.small)
                        .clickable(onClick = onCancel),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(cancelText, style = MaterialTheme.knightType.ButtonSecondary, color = TextSecondary)
                }
                Box(
                    modifier = Modifier
                        .weight(1f).height(46.dp)
                        .background(confirmTint.copy(alpha = 0.12f), KnightTourShapes.small)
                        .border(BorderWidth.thin, confirmTint.copy(alpha = 0.4f), KnightTourShapes.small)
                        .clickable(onClick = onConfirm),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(confirmText, style = MaterialTheme.knightType.ButtonSecondary, color = confirmTint)
                }
            }
        }
    }
}

// ===============================================================
//  HELPERS
// ===============================================================

@Composable
private fun SectionDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(1.dp)
            .background(BorderSubtle),
    )
}

@Composable
private fun SettingsIconBtn(icon: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .background(SurfaceElevated, KnightTourShapes.extraSmall)
            .border(BorderWidth.thin, BorderSubtle, KnightTourShapes.extraSmall)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(icon, fontSize = 20.sp, color = TextSecondary)
    }
}

// ===============================================================
//  ROUTE ENTRY POINT
// ===============================================================

@Composable
fun SettingsRoute(
    onNavigateBack : () -> Unit,
    viewModel      : SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    SettingsScreen(
        uiState        = uiState,
        onEvent        = viewModel::onEvent,
        onNavigateBack = onNavigateBack,
    )
}

// ===============================================================
//  PREVIEWS
// ===============================================================

@Preview(
    name = "Settings -- Default",
    showBackground = true, backgroundColor = 0xFF050508,
    widthDp = 360, heightDp = 800,
)
@Composable
private fun PreviewDefault() {
    KnightTourTheme {
        SettingsScreen(
            uiState        = SettingsUiState(),
            onEvent        = {},
            onNavigateBack = {},
        )
    }
}

@Preview(
    name = "Settings -- Signed In",
    showBackground = true, backgroundColor = 0xFF050508,
    widthDp = 360, heightDp = 800,
)
@Composable
private fun PreviewSignedIn() {
    KnightTourTheme {
        SettingsScreen(
            uiState = SettingsUiState(
                isSignedIn  = true,
                playerName  = "Devil_King",
                savedFeedback = true,
                boardTheme  = BoardTheme.CRIMSON,
                defaultDifficulty = DefaultDiff.DEVIL,
            ),
            onEvent        = {},
            onNavigateBack = {},
        )
    }
}