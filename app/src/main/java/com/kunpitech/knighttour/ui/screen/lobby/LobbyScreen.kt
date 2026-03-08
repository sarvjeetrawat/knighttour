package com.kunpitech.knighttour.ui.screen.lobby

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kunpitech.knighttour.data.repository.RoomBrowserEntry
import com.kunpitech.knighttour.ui.theme.AbyssBlack
import com.kunpitech.knighttour.ui.theme.BloodRedBright
import com.kunpitech.knighttour.ui.theme.BorderDefault
import com.kunpitech.knighttour.ui.theme.BorderWidth
import com.kunpitech.knighttour.ui.theme.GoldDim
import com.kunpitech.knighttour.ui.theme.KnightGold
import com.kunpitech.knighttour.ui.theme.KnightTourShapes
import com.kunpitech.knighttour.ui.theme.OnlineTeal
import com.kunpitech.knighttour.ui.theme.SurfaceDark
import com.kunpitech.knighttour.ui.theme.SurfaceElevated
import com.kunpitech.knighttour.ui.theme.TextPrimary
import com.kunpitech.knighttour.ui.theme.TextSecondary
import com.kunpitech.knighttour.ui.theme.TextTertiary
import com.kunpitech.knighttour.ui.theme.knightType

// ================================================================
//  LOBBY SCREEN
//  File: ui/screen/lobby/LobbyScreen.kt
// ================================================================

@Composable
fun LobbyRoute(
    onGameStart    : (roomCode: String) -> Unit,
    onNavigateBack : () -> Unit,
    viewModel      : LobbyViewModel = hiltViewModel(),
) {
    val uiState        by viewModel.uiState.collectAsStateWithLifecycle()
    val navigateToGame by viewModel.navigateToGame.collectAsStateWithLifecycle()

    LaunchedEffect(navigateToGame) {
        navigateToGame?.let { code ->
            viewModel.onNavigationConsumed()
            onGameStart(code)
        }
    }

    LobbyScreen(
        uiState        = uiState,
        onEvent        = viewModel::onEvent,
        onNavigateBack = onNavigateBack,
    )
}

@Composable
fun LobbyScreen(
    uiState        : LobbyUiState,
    onEvent        : (LobbyEvent) -> Unit,
    onNavigateBack : () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AbyssBlack)
            .systemBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(16.dp))

            // ── Top bar ──────────────────────────────────────────
            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(SurfaceDark, KnightTourShapes.small)
                        .border(BorderWidth.thin, BorderDefault, KnightTourShapes.small)
                        .clickable { onNavigateBack() },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("‹", fontSize = 22.sp, color = TextSecondary)
                }
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text  = "🌐  ONLINE",
                        style = MaterialTheme.knightType.ScreenHeader,
                        color = OnlineTeal,
                    )
                    Text(
                        text  = "RACE MODE  ·  ${uiState.playerName}",
                        style = MaterialTheme.knightType.StatLabel.copy(letterSpacing = 1.sp),
                        color = TextTertiary,
                    )
                }
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.size(40.dp))
            }

            Spacer(Modifier.height(28.dp))

            // ── Tab selector ─────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceDark, KnightTourShapes.medium)
                    .border(BorderWidth.thin, BorderDefault, KnightTourShapes.medium)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                listOf(
                    LobbyTab.BROWSE to "⊞ BROWSE",
                    LobbyTab.CREATE to "✚ CREATE",
                    LobbyTab.JOIN   to "→ JOIN",
                ).forEach { (tab, label) ->
                    val selected = uiState.tab == tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (selected) OnlineTeal.copy(alpha = 0.15f) else Color.Transparent,
                                KnightTourShapes.small,
                            )
                            .border(
                                BorderWidth.thin,
                                if (selected) OnlineTeal.copy(alpha = 0.7f) else Color.Transparent,
                                KnightTourShapes.small,
                            )
                            .clickable {
                                onEvent(when (tab) {
                                    LobbyTab.CREATE  -> LobbyEvent.SelectCreateTab
                                    LobbyTab.JOIN    -> LobbyEvent.SelectJoinTab
                                    LobbyTab.BROWSE  -> LobbyEvent.SelectBrowseTab
                                })
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text  = label,
                            style = MaterialTheme.knightType.TabLabel,
                            color = if (selected) OnlineTeal else TextTertiary,
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Tab content ──────────────────────────────────────
            AnimatedContent(
                targetState = uiState.tab,
                transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(180)) },
                label = "lobbyTab",
                modifier = Modifier.fillMaxWidth(),
            ) { tab ->
                when (tab) {
                    LobbyTab.CREATE -> CreateTab(uiState = uiState, onEvent = onEvent)
                    LobbyTab.JOIN   -> JoinTab(uiState = uiState, onEvent = onEvent)
                    LobbyTab.BROWSE -> BrowseTab(uiState = uiState, onEvent = onEvent)
                }
            }

            // ── Error message ────────────────────────────────────
            AnimatedVisibility(visible = uiState.error != null) {
                uiState.error?.let { err ->
                    Spacer(Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                BloodRedBright.copy(alpha = 0.10f),
                                KnightTourShapes.small,
                            )
                            .border(
                                BorderWidth.thin,
                                BloodRedBright.copy(alpha = 0.40f),
                                KnightTourShapes.small,
                            )
                            .clickable { onEvent(LobbyEvent.DismissError) }
                            .padding(14.dp),
                    ) {
                        Text(
                            text  = "⚠  $err",
                            style = MaterialTheme.knightType.BodySecondary,
                            color = BloodRedBright,
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }

        // ── JOINING OVERLAY ───────────────────────────────────────
        if (uiState.isJoining) {
            Box(
                modifier         = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.75f)),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    CircularProgressIndicator(
                        color       = KnightGold,
                        strokeWidth = 3.dp,
                        modifier    = Modifier.size(48.dp),
                    )
                    Text(
                        text  = "JOINING ROOM…",
                        style = MaterialTheme.knightType.StatLabel.copy(letterSpacing = 4.sp),
                        color = KnightGold,
                    )
                }
            }
        }

        // ── REQUEST SENT OVERLAY (Guest waiting for host) ─────────
        if (uiState.requestSent) {
            Box(
                modifier         = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.75f)),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier            = Modifier
                        .padding(horizontal = 32.dp)
                        .background(
                            Brush.verticalGradient(listOf(SurfaceElevated, SurfaceDark)),
                            KnightTourShapes.large,
                        )
                        .border(BorderWidth.default, OnlineTeal.copy(alpha = 0.40f), KnightTourShapes.large)
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    CircularProgressIndicator(color = OnlineTeal, strokeWidth = 3.dp, modifier = Modifier.size(40.dp))
                    Text(
                        text      = "REQUEST SENT",
                        style     = MaterialTheme.knightType.ScreenHeader.copy(letterSpacing = 4.sp),
                        color     = KnightGold,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text      = "Waiting for host to accept…",
                        style     = MaterialTheme.knightType.BodySecondary,
                        color     = TextSecondary,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        // ── INCOMING REQUEST BANNER (Host in lobby) — always on top ──
        AnimatedVisibility(
            visible  = uiState.hasIncomingGuest,
            enter    = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit     = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .zIndex(10f),                // always above requestSent overlay
        ) {
            GameRequestBanner(
                guestName = uiState.incomingGuestName,
                onAccept  = { onEvent(LobbyEvent.AcceptGuest) },
                onReject  = { onEvent(LobbyEvent.RejectGuest) },
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════
//  CREATE TAB
// ════════════════════════════════════════════════════════════════

@Composable
private fun CreateTab(
    uiState : LobbyUiState,
    onEvent : (LobbyEvent) -> Unit,
) {
    val keyboard = LocalSoftwareKeyboardController.current

    Column(
        modifier            = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (!uiState.roomCreated) {

            // ── Room name input ──────────────────────────────────
            Text(
                text  = "ROOM NAME",
                style = MaterialTheme.knightType.StatLabel.copy(letterSpacing = 3.sp),
                color = TextTertiary,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value         = uiState.roomNameInput,
                onValueChange = { onEvent(LobbyEvent.RoomNameChanged(it)) },
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true,
                placeholder   = {
                    Text(
                        text  = "e.g. 1v1 Ranked, Chill Game…",
                        style = MaterialTheme.knightType.BodySecondary,
                        color = TextTertiary.copy(alpha = 0.5f),
                    )
                },
                textStyle = MaterialTheme.knightType.BodySecondary.copy(color = TextPrimary),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction    = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { keyboard?.hide() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = OnlineTeal,
                    unfocusedBorderColor = OnlineTeal.copy(alpha = 0.35f),
                    focusedTextColor     = TextPrimary,
                    unfocusedTextColor   = TextPrimary,
                    cursorColor          = OnlineTeal,
                ),
            )
            Text(
                text  = "${uiState.roomNameInput.length} / 20",
                style = MaterialTheme.knightType.StatLabel,
                color = if (uiState.roomNameInput.isNotEmpty()) OnlineTeal else TextTertiary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End,
            )

            // ── Already have a room warning ──────────────────────
            if (uiState.hasExistingRoom) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFE05050).copy(alpha = 0.08f), KnightTourShapes.medium)
                        .border(BorderWidth.thin, Color(0xFFE05050).copy(alpha = 0.3f), KnightTourShapes.medium)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    Text("⚠", fontSize = 16.sp)
                    Column {
                        Text(
                            text  = "You already have an open room",
                            style = MaterialTheme.knightType.CardTitle.copy(fontSize = 13.sp),
                            color = Color(0xFFE05050),
                        )
                        Text(
                            text  = "Close your existing room first, or join via Browse.",
                            style = MaterialTheme.knightType.StatLabel,
                            color = Color(0xFFE05050).copy(alpha = 0.7f),
                        )
                    }
                }
            }

            // ── Board size picker ────────────────────────────────
            SizePickerCard(
                selectedSize = uiState.selectedSize,
                onSelect     = { onEvent(LobbyEvent.BoardSizeSelected(it)) },
            )

            // ── Create button ────────────────────────────────────
            LobbyButton(
                label     = if (uiState.isCreating) "CREATING…" else "CREATE ROOM",
                color     = OnlineTeal,
                isLoading = uiState.isCreating,
                enabled   = uiState.roomNameInput.isNotEmpty() && !uiState.isCreating,
                onClick   = {
                    keyboard?.hide()
                    onEvent(LobbyEvent.CreateRoom)
                },
            )
        } else {
            WaitingCard(
                roomCode     = uiState.generatedCode,
                boardSize    = uiState.selectedSize.label,
                onCancel     = { onEvent(LobbyEvent.CancelRoom) },
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════
//  JOIN TAB
// ════════════════════════════════════════════════════════════════

@Composable
private fun JoinTab(
    uiState : LobbyUiState,
    onEvent : (LobbyEvent) -> Unit,
) {
    val keyboard = LocalSoftwareKeyboardController.current

    Column(
        modifier            = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // Instruction
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceElevated, KnightTourShapes.medium)
                .border(BorderWidth.thin, OnlineTeal.copy(alpha = 0.18f), KnightTourShapes.medium)
                .padding(18.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "JOIN A FRIEND'S ROOM",
                    style = MaterialTheme.knightType.StatLabel.copy(letterSpacing = 2.sp),
                    color = OnlineTeal,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "Enter your friend's room code (their player name).",
                    style = MaterialTheme.knightType.BodySecondary,
                    color = TextSecondary,
                )
            }
        }

        // Code input label
        Text(
            text  = "ROOM CODE",
            style = MaterialTheme.knightType.StatLabel.copy(letterSpacing = 3.sp),
            color = TextTertiary,
            modifier = Modifier.fillMaxWidth(),
        )

        // Large code input
        OutlinedTextField(
            value         = uiState.joinCodeInput,
            onValueChange = { onEvent(LobbyEvent.JoinCodeChanged(it)) },
            modifier      = Modifier.fillMaxWidth(),
            singleLine    = true,
            placeholder   = {
                Text(
                    text      = "PLAYERNAME",
                    style     = MaterialTheme.knightType.GameTitle.copy(
                        letterSpacing = 4.sp,
                        fontFamily    = FontFamily.Monospace,
                    ),
                    color     = TextTertiary.copy(alpha = 0.4f),
                    modifier  = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            },
            textStyle = MaterialTheme.knightType.GameTitle.copy(
                letterSpacing = 4.sp,
                textAlign     = TextAlign.Center,
                fontFamily    = FontFamily.Monospace,
                color         = OnlineTeal,
            ),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction    = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    keyboard?.hide()
                    if (uiState.joinCodeInput.isNotEmpty()) onEvent(LobbyEvent.JoinRoom)
                }
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = OnlineTeal,
                unfocusedBorderColor = OnlineTeal.copy(alpha = 0.35f),
                focusedTextColor     = OnlineTeal,
                unfocusedTextColor   = OnlineTeal,
                cursorColor          = OnlineTeal,
            ),
        )

        LobbyButton(
            label     = if (uiState.isJoining) "JOINING…" else "JOIN ROOM",
            color     = OnlineTeal,
            isLoading = uiState.isJoining,
            enabled   = uiState.joinCodeInput.isNotEmpty() && !uiState.isJoining,
            onClick   = {
                keyboard?.hide()
                onEvent(LobbyEvent.JoinRoom)
            },
        )
    }
}

// ════════════════════════════════════════════════════════════════
//  SIZE PICKER CARD
// ════════════════════════════════════════════════════════════════

@Composable
private fun SizePickerCard(
    selectedSize : OnlineBoardSize,
    onSelect     : (OnlineBoardSize) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceElevated, KnightTourShapes.medium)
            .border(BorderWidth.thin, BorderDefault, KnightTourShapes.medium)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "BOARD SIZE",
            style = MaterialTheme.knightType.StatLabel.copy(letterSpacing = 2.sp),
            color = TextTertiary,
        )
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OnlineBoardSize.entries.forEach { size ->
                val selected = size == selectedSize
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            if (selected) OnlineTeal.copy(alpha = 0.15f) else SurfaceDark,
                            KnightTourShapes.small,
                        )
                        .border(
                            BorderWidth.default,
                            if (selected) OnlineTeal else BorderDefault,
                            KnightTourShapes.small,
                        )
                        .clickable { onSelect(size) }
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text  = size.label,
                        style = MaterialTheme.knightType.CardTitle.copy(fontSize = 15.sp),
                        color = if (selected) OnlineTeal else TextSecondary,
                    )
                    Text(
                        text  = "${size.timeLimit / 60}m",
                        style = MaterialTheme.knightType.StatLabel,
                        color = if (selected) OnlineTeal.copy(alpha = 0.7f) else TextTertiary,
                    )
                }
            }
        }
        Text(
            text  = "Both players play the same board size",
            style = MaterialTheme.knightType.BodySecondary,
            color = TextTertiary,
        )
    }
}

// ════════════════════════════════════════════════════════════════
//  HOW IT WORKS CARD
// ════════════════════════════════════════════════════════════════

@Composable
private fun HowItWorksCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceElevated, KnightTourShapes.medium)
            .border(BorderWidth.thin, BorderDefault, KnightTourShapes.medium)
            .padding(18.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                "HOW IT WORKS",
                style = MaterialTheme.knightType.StatLabel.copy(letterSpacing = 2.sp),
                color = KnightGold.copy(alpha = 0.8f),
            )
            Spacer(Modifier.height(2.dp))
            listOf(
                "1. Choose board size and tap CREATE ROOM",
                "2. Share your room code (your player name) with your opponent",
                "3. Opponent taps JOIN ROOM and enters the code",
                "4. Both race to visit all squares",
                "5. More moves in less time = higher score",
            ).forEach {
                Text(it, style = MaterialTheme.knightType.BodySecondary, color = TextSecondary)
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════
//  WAITING CARD
// ════════════════════════════════════════════════════════════════

// ════════════════════════════════════════════════════════════════
//  BROWSE TAB  — live list of waiting rooms
// ════════════════════════════════════════════════════════════════

@Composable
private fun BrowseTab(
    uiState : LobbyUiState,
    onEvent : (LobbyEvent) -> Unit,
) {
    Column(
        modifier            = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Header
        Row(
            modifier          = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text  = "OPEN ROOMS",
                style = MaterialTheme.knightType.StatLabel.copy(letterSpacing = 2.sp),
                color = TextTertiary,
            )
            Text(
                text  = "${uiState.waitingRooms.size} waiting",
                style = MaterialTheme.knightType.StatLabel,
                color = if (uiState.waitingRooms.isEmpty()) TextTertiary else OnlineTeal,
            )
        }

        if (uiState.isLoadingRooms) {
            Box(
                modifier         = Modifier.fillMaxWidth().height(120.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    modifier    = Modifier.size(24.dp),
                    color       = OnlineTeal,
                    strokeWidth = 2.dp,
                )
            }
        } else if (uiState.waitingRooms.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceElevated, KnightTourShapes.medium)
                    .border(BorderWidth.thin, BorderDefault, KnightTourShapes.medium)
                    .padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("🔍", fontSize = 28.sp)
                    Text(
                        "No open rooms right now",
                        style = MaterialTheme.knightType.BodySecondary,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        "Create a room and wait for someone to join, or ask a friend to create one.",
                        style = MaterialTheme.knightType.BodySecondary,
                        color = TextTertiary,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        } else {
            uiState.waitingRooms.forEach { room ->
                RoomCard(
                    room    = room,
                    onJoin  = { onEvent(LobbyEvent.JoinFromBrowser(room.roomCode)) },
                    enabled = !uiState.isJoining,
                )
            }
        }
    }
}

@Composable
private fun RoomCard(
    room    : RoomBrowserEntry,
    onJoin  : () -> Unit,
    enabled : Boolean,
) {
    val isDisconnected = !room.isHostConnected
    val dotColor = when {
        room.isOwn     -> Color(0xFFD4A843)
        room.isPending -> Color(0xFFE05050)           // red — request in progress
        isDisconnected -> Color(0xFFE05050)
        else           -> OnlineTeal
    }
    val borderColor = when {
        room.isOwn     -> Color(0xFFD4A843).copy(alpha = 0.4f)
        room.isPending -> Color(0xFFE05050).copy(alpha = 0.25f)
        isDisconnected -> Color(0xFFE05050).copy(alpha = 0.25f)
        else           -> OnlineTeal.copy(alpha = 0.25f)
    }

    val pulse by rememberInfiniteTransition(label = "live")
        .animateFloat(
            initialValue  = if (isDisconnected || room.isPending) 0.3f else 0.4f,
            targetValue   = if (isDisconnected || room.isPending) 0.3f else 1f,
            animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
            label         = "live",
        )

    val waitingSince = remember(room.createdAt) {
        val diff = System.currentTimeMillis() - room.createdAt
        when {
            diff < 60_000    -> "just now"
            diff < 3_600_000 -> "${diff / 60_000}m ago"
            else             -> "${diff / 3_600_000}h ago"
        }
    }

    // Pending rooms and own rooms are not joinable
    val canJoin = enabled && !room.isOwn && !isDisconnected && !room.isPending

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceElevated, KnightTourShapes.medium)
            .border(BorderWidth.thin, borderColor, KnightTourShapes.medium)
            .then(if (canJoin) Modifier.clickable { onJoin() } else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier              = Modifier.weight(1f),
        ) {
            // Status dot
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(dotColor.copy(alpha = pulse), CircleShape)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    Text(
                        text     = room.roomName,
                        style    = MaterialTheme.knightType.CardTitle,
                        color    = if (isDisconnected) TextTertiary else TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (room.isOwn) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFD4A843).copy(alpha = 0.15f), KnightTourShapes.extraSmall)
                                .padding(horizontal = 6.dp, vertical = 1.dp),
                        ) {
                            Text(
                                text  = "YOUR ROOM",
                                style = MaterialTheme.knightType.StatLabel.copy(fontSize = 9.sp),
                                color = Color(0xFFD4A843),
                            )
                        }
                    }
                    if (room.isPending) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFE05050).copy(alpha = 0.15f), KnightTourShapes.extraSmall)
                                .padding(horizontal = 6.dp, vertical = 1.dp),
                        ) {
                            Text(
                                text  = "REQUEST PENDING",
                                style = MaterialTheme.knightType.StatLabel.copy(fontSize = 9.sp),
                                color = Color(0xFFE05050),
                            )
                        }
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .background(dotColor.copy(alpha = 0.12f), KnightTourShapes.extraSmall)
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text  = "${room.boardSize}×${room.boardSize}",
                            style = MaterialTheme.knightType.DifficultyChip,
                            color = dotColor,
                        )
                    }
                    Text(
                        text     = when {
                            isDisconnected -> "⚠ host left"
                            room.isPending -> "request in progress"
                            else           -> "by ${room.hostName}  ·  $waitingSince"
                        },
                        style    = MaterialTheme.knightType.StatLabel,
                        color    = when {
                            isDisconnected || room.isPending -> Color(0xFFE05050).copy(alpha = 0.7f)
                            else -> TextTertiary
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        // Action badge — right side
        when {
            room.isOwn -> Box(
                modifier = Modifier
                    .background(Color(0xFFD4A843).copy(alpha = 0.1f), KnightTourShapes.small)
                    .border(BorderWidth.default, Color(0xFFD4A843).copy(alpha = 0.4f), KnightTourShapes.small)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text(
                    text  = "WAITING",
                    style = MaterialTheme.knightType.ButtonPrimary.copy(fontSize = 11.sp),
                    color = Color(0xFFD4A843),
                )
            }
            room.isPending -> Box(
                modifier = Modifier
                    .background(Color.Transparent, KnightTourShapes.small)
                    .border(BorderWidth.default, Color(0xFFE05050).copy(alpha = 0.35f), KnightTourShapes.small)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text(
                    text  = "TAKEN",
                    style = MaterialTheme.knightType.ButtonPrimary.copy(fontSize = 11.sp),
                    color = Color(0xFFE05050).copy(alpha = 0.7f),
                )
            }
            isDisconnected -> Box(
                modifier = Modifier
                    .background(Color.Transparent, KnightTourShapes.small)
                    .border(BorderWidth.default, BorderDefault, KnightTourShapes.small)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text(
                    text  = "GONE",
                    style = MaterialTheme.knightType.ButtonPrimary.copy(fontSize = 11.sp),
                    color = TextTertiary,
                )
            }
            else -> Box(
                modifier = Modifier
                    .background(
                        if (canJoin) OnlineTeal.copy(alpha = 0.15f) else Color.Transparent,
                        KnightTourShapes.small,
                    )
                    .border(
                        BorderWidth.default,
                        if (canJoin) OnlineTeal.copy(alpha = 0.7f) else BorderDefault,
                        KnightTourShapes.small,
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(
                    text  = if (canJoin) "JOIN →" else "...",
                    style = MaterialTheme.knightType.ButtonPrimary.copy(fontSize = 12.sp),
                    color = if (canJoin) OnlineTeal else TextTertiary,
                )
            }
        }
    }
}

@Composable
private fun WaitingCard(
    roomCode  : String,
    boardSize : String,
    onCancel  : () -> Unit,
) {
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue  = 0.4f,
        targetValue   = 1.0f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label         = "pa",
    )

    Column(
        modifier            = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Room code card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(OnlineTeal.copy(alpha = 0.10f), SurfaceDark)
                    ),
                    KnightTourShapes.medium,
                )
                .border(BorderWidth.default, OnlineTeal.copy(alpha = 0.50f), KnightTourShapes.medium)
                .padding(vertical = 28.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    "✓  ROOM CREATED",
                    style = MaterialTheme.knightType.StatLabel.copy(letterSpacing = 3.sp),
                    color = OnlineTeal,
                )
                Text(
                    text      = roomCode.chunked(3).joinToString("  "),
                    style     = MaterialTheme.knightType.GameTitle.copy(
                        fontSize      = 44.sp,
                        letterSpacing = 6.sp,
                        fontFamily    = FontFamily.Monospace,
                    ),
                    color     = OnlineTeal,
                    textAlign = TextAlign.Center,
                )
                Box(
                    modifier = Modifier
                        .background(OnlineTeal.copy(alpha = 0.12f), KnightTourShapes.extraSmall)
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                ) {
                    Text(
                        text  = boardSize,
                        style = MaterialTheme.knightType.DifficultyChip,
                        color = OnlineTeal,
                    )
                }
            }
        }

        // Pulsing wait indicator
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier              = Modifier.alpha(pulse),
        ) {
            CircularProgressIndicator(
                modifier    = Modifier.size(14.dp),
                color       = OnlineTeal,
                strokeWidth = 2.dp,
            )
            Text(
                "Waiting for a player to send a request…",
                style = MaterialTheme.knightType.BodySecondary,
                color = OnlineTeal,
            )
        }

        // Cancel
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceDark, KnightTourShapes.small)
                .border(BorderWidth.thin, BorderDefault, KnightTourShapes.small)
                .clickable { onCancel() }
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "CANCEL",
                style = MaterialTheme.knightType.ButtonSecondary,
                color = TextTertiary,
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════
//  SHARED LOBBY BUTTON
// ════════════════════════════════════════════════════════════════

@Composable
private fun LobbyButton(
    label     : String,
    color     : Color,
    onClick   : () -> Unit,
    isLoading : Boolean = false,
    enabled   : Boolean = true,
) {
    val alpha by animateFloatAsState(
        targetValue   = if (enabled) 1f else 0.38f,
        animationSpec = tween(200),
        label         = "btnAlpha",
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha)
            .background(color.copy(alpha = 0.12f), KnightTourShapes.medium)
            .border(
                BorderWidth.default,
                color.copy(alpha = if (enabled) 0.75f else 0.25f),
                KnightTourShapes.medium,
            )
            .clip(KnightTourShapes.medium)
            .clickable(enabled = enabled && !isLoading) { onClick() }
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier    = Modifier.size(20.dp),
                color       = color,
                strokeWidth = 2.dp,
            )
        } else {
            Text(
                label,
                style = MaterialTheme.knightType.ButtonPrimary,
                color = color,
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════
//  GAME REQUEST BANNER  (slides in from top, non-blocking)
// ════════════════════════════════════════════════════════════════

@Composable
private fun GameRequestBanner(
    guestName : String,
    onAccept  : () -> Unit,
    onReject  : () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Surface(
            shape           = RoundedCornerShape(12.dp),
            color           = Color(0xCC1A1A2E),
            border          = BorderStroke(1.dp, Color(0xFFD4A843)),
            modifier        = Modifier.fillMaxWidth(),
            shadowElevation = 8.dp,
        ) {
            Row(
                modifier          = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("⚔️", fontSize = 18.sp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text          = "$guestName wants to play!",
                        color         = Color(0xFFD4A843),
                        fontSize      = 13.sp,
                        fontWeight    = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        maxLines      = 1,
                        overflow      = TextOverflow.Ellipsis,
                    )
                    Text(
                        text     = "Tap ACCEPT to start the game",
                        color    = Color(0xFFB0B0C0),
                        fontSize = 11.sp,
                    )
                }
                Box(
                    modifier = Modifier
                        .background(OnlineTeal.copy(alpha = 0.20f), RoundedCornerShape(8.dp))
                        .border(1.dp, OnlineTeal.copy(alpha = 0.70f), RoundedCornerShape(8.dp))
                        .clickable { onAccept() }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text          = "ACCEPT",
                        color         = OnlineTeal,
                        fontSize      = 11.sp,
                        fontWeight    = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                    )
                }
                Box(
                    modifier = Modifier
                        .background(Color.Transparent, RoundedCornerShape(8.dp))
                        .border(1.dp, BloodRedBright.copy(alpha = 0.40f), RoundedCornerShape(8.dp))
                        .clickable { onReject() }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text       = "✕",
                        color      = BloodRedBright.copy(alpha = 0.80f),
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}