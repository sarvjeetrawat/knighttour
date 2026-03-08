package com.kunpitech.knighttour.ui.screen.result

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kunpitech.knighttour.data.online.OnlineSessionManager
import com.kunpitech.knighttour.data.repository.FirebaseGameRepository
import com.kunpitech.knighttour.data.repository.GameRepository
import com.kunpitech.knighttour.data.repository.UserPreferencesRepository
import com.kunpitech.knighttour.domain.engine.ScoreCalculator
import com.kunpitech.knighttour.domain.engine.ScoreCalculator.RankInfo
import com.kunpitech.knighttour.domain.engine.ScoreCalculator.ScoreBreakdown
import com.kunpitech.knighttour.domain.model.GameSession
import com.kunpitech.knighttour.domain.model.DefeatReason
import com.kunpitech.knighttour.domain.model.GameMode
import com.kunpitech.knighttour.domain.model.SessionStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ═══════════════════════════════════════════════════════════════
//  RESULT VIEWMODEL
//
//  For online games, opponent score is observed REACTIVELY from
//  Firebase — the UI updates when their score arrives.
//  This handles all timing cases:
//    - Opponent finishes before us
//    - Opponent finishes after us
//    - Opponent disconnects
// ═══════════════════════════════════════════════════════════════

@HiltViewModel
class ResultViewModel @Inject constructor(
    private val savedStateHandle : SavedStateHandle,
    private val gameRepository   : GameRepository,
    private val prefsRepository  : UserPreferencesRepository,
    private val scoreCalculator  : ScoreCalculator,
    private val firebaseRepo     : FirebaseGameRepository,
    private val sessionManager   : OnlineSessionManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ResultUiState(isLoading = true))
    val uiState: StateFlow<ResultUiState> = _uiState.asStateFlow()

    init { loadResult() }

    private fun loadResult() {
        viewModelScope.launch {
            val sessionId = savedStateHandle.get<String>("sessionId") ?: ""
            val session   = gameRepository.getById(sessionId)
            if (session == null) { loadFromNavArgs(); return@launch }

            val playerName = prefsRepository.preferences.first().playerName
            val breakdown  = scoreCalculator.calculate(session)
            val rankInfo   = scoreCalculator.rankInfo(breakdown.total)
            val isVictory  = session.status == SessionStatus.COMPLETED
            val isOnline   = session.mode == GameMode.ONLINE

            if (!isOnline) sessionManager.clearLastSession()

            // ── Push score to Firebase leaderboard ────────────────
            if (isVictory || (isOnline && session.moveCount > 0)) {
                try {
                    firebaseRepo.saveScore(
                        playerName     = playerName,
                        sessionId      = session.id,
                        score          = breakdown.total,
                        boardSize      = session.boardSize,
                        elapsedSeconds = session.elapsedSeconds,
                        finishedAt     = session.finishedAt ?: System.currentTimeMillis(),
                        rankLabel      = rankInfo.label,
                    )
                } catch (_: Exception) {}
            }

            // ── Personal best ─────────────────────────────────────
            val isNewPB = try {
                val prevBest = firebaseRepo.getPlayerHistory(playerName)
                    .filter { it.boardLabel == "${session.boardSize}×${session.boardSize}" }
                    .maxOfOrNull { it.score } ?: 0
                isVictory && breakdown.total > prevBest
            } catch (_: Exception) { false }

            // ── Resolve opponent info ─────────────────────────────
            val last         = sessionManager.lastCompletedSession()
            val opponentName = last?.opponentName ?: session.opponentId
            // roomCode: prefer lastSession, fall back to session.roomCode if available
            val roomCode     = last?.roomCode?.takeIf { it.isNotEmpty() }
                ?: session.roomCode.takeIf { it.isNotEmpty() }
                ?: ""
            val localRole    = last?.localRole?.name?.lowercase() ?: "host"
            val myScore      = breakdown.total

            // Show result immediately with whatever opponent score we have
            val initialOpponentScore = last?.opponentFinalScore ?: 0
            val localName = last?.localName ?: ""
            _uiState.value = buildState(
                session       = session,
                breakdown     = breakdown,
                rankInfo      = rankInfo,
                isVictory     = isVictory,
                isOnline      = isOnline,
                isNewPB       = isNewPB,
                opponentName  = opponentName,
                opponentScore = initialOpponentScore,
                myScore       = myScore,
                roomCode      = roomCode,
                localRole     = localRole,
                localName     = localName,
            )

            // ── For online: observe opponent node until score arrives ─
            if (isOnline && roomCode.isNotEmpty() && initialOpponentScore == 0) {
                observeOpponentScore(
                    roomCode     = roomCode,
                    localRole    = localRole,
                    myScore      = myScore,
                    opponentName = opponentName,
                    session      = session,
                    breakdown    = breakdown,
                    rankInfo     = rankInfo,
                    isVictory    = isVictory,
                    isNewPB      = isNewPB,
                )
            }

            // ── Watch for opponent disconnecting (they hit Exit Game) ─
            if (isOnline && roomCode.isNotEmpty()) {
                observeOpponentPresence(roomCode, localRole)
                // Clear our own stale signal from any previous rematch
                // so we start clean — does NOT clear opponent's signal
                try { firebaseRepo.clearMyRematchSignal(roomCode, localRole) } catch (_: Exception) {}
            }
        }
    }

    /**
     * If the opponent disconnects from the result screen, reset rematch state
     * so the local player's "WAITING FOR..." button returns to "PLAY AGAIN".
     */
    private fun observeOpponentPresence(roomCode: String, localRole: String) {
        val opponentRole = if (localRole == "host") "guest" else "host"
        viewModelScope.launch {
            // Small delay — give Firebase time to settle after game end
            // (endSession sets isConnected=false, we don't want to react to that)
            kotlinx.coroutines.delay(3000)
            firebaseRepo.observeOpponent(roomCode, opponentRole)
                .collect { state ->
                    // Only reset rematch waiting if we're actually waiting for them
                    val currentRematch = _uiState.value.rematchState
                    if (!state.isConnected && currentRematch == RematchState.WAITING) {
                        _uiState.update { it.copy(rematchState = RematchState.IDLE) }
                    }
                }
        }
    }

    /**
     * Observe opponent's score from the persistent matchResults node.
     * Works even after the room is deleted.
     */
    private fun observeOpponentScore(
        roomCode     : String,
        localRole    : String,
        localName    : String = "",
        myScore      : Int,
        opponentName : String,
        session      : GameSession,
        breakdown    : ScoreBreakdown,
        rankInfo     : RankInfo,
        isVictory    : Boolean,
        isNewPB      : Boolean,
    ) {
        val opponentRole = if (localRole == "host") "guest" else "host"
        viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            firebaseRepo.observeMatchResult(roomCode, opponentRole)
                .collect { opponentScore ->
                    if (opponentScore > 0) {
                        _uiState.value = buildState(
                            session       = session,
                            breakdown     = breakdown,
                            rankInfo      = rankInfo,
                            isVictory     = isVictory,
                            isOnline      = true,
                            isNewPB       = isNewPB,
                            opponentName  = opponentName,
                            opponentScore = opponentScore,
                            myScore       = myScore,
                            roomCode      = roomCode,
                            localRole     = localRole,
                            localName     = localName,
                        )
                        return@collect
                    }
                    if (System.currentTimeMillis() - startTime > 10_000) {
                        return@collect
                    }
                }
        }
    }

    private fun buildState(
        session       : GameSession,
        breakdown     : ScoreBreakdown,
        rankInfo      : RankInfo,
        isVictory     : Boolean,
        isOnline      : Boolean,
        isNewPB       : Boolean,
        opponentName  : String,
        opponentScore : Int,
        myScore       : Int,
        roomCode      : String = "",
        localRole     : String = "",
        localName     : String = "",
    ): ResultUiState {
        val didWinOnline = when {
            !isOnline          -> false
            opponentScore == 0 -> isVictory
            myScore > opponentScore -> true
            myScore < opponentScore -> false
            else               -> isVictory
        }
        return ResultUiState(
            isVictory         = isVictory,
            defeatReason      = DefeatReason.fromStatus(session.status),
            boardSize         = session.boardSize,
            difficulty        = session.difficulty.uiLabel,
            gameMode          = session.mode.name,
            squaresVisited    = session.moveCount,
            totalSquares      = session.totalCells,
            moveCount         = session.moveCount,
            elapsedSeconds    = session.elapsedSeconds,
            timeLimitSeconds  = session.difficulty.timeLimitSeconds,
            baseScore         = breakdown.base,
            timeBonus         = breakdown.timeBonus,
            sizeBonus         = breakdown.sizeBonus,
            hintPenalty       = breakdown.hintPenalty,
            totalScore        = breakdown.total,
            rankLabel         = rankInfo.label,
            rankProgress      = rankInfo.progressFrac,
            isNewPersonalBest = isNewPB,
            isOnlineMode      = isOnline,
            opponentName      = opponentName,
            opponentScore     = opponentScore,
            didWinOnline      = didWinOnline,
            roomCode          = roomCode,
            localRole         = localRole,
            localName         = localName,
            isLoading         = false,
        )
    }

    private suspend fun loadFromNavArgs() {
        val isVictory = savedStateHandle.get<Boolean>("isVictory")   ?: true
        val boardSize = savedStateHandle.get<Int>("boardSize")        ?: 6
        val diffStr   = savedStateHandle.get<String>("difficulty")    ?: "MEDIUM"
        val gameMode  = savedStateHandle.get<String>("gameMode")      ?: "OFFLINE"
        val moveCount = savedStateHandle.get<Int>("moveCount")        ?: 0
        val elapsed   = savedStateHandle.get<Int>("elapsedSeconds")   ?: 0
        val timeLimit = savedStateHandle.get<Int>("timeLimitSeconds") ?: 240
        val hintsUsed = savedStateHandle.get<Int>("hintsUsed")        ?: 0
        val defeatStr = savedStateHandle.get<String>("defeatReason")  ?: "NONE"

        val breakdown = scoreCalculator.calculate(
            moveCount     = moveCount,
            timeRemaining = (timeLimit - elapsed).coerceAtLeast(0),
            boardSize     = boardSize,
            hintsUsed     = hintsUsed,
        )
        val rankInfo = scoreCalculator.rankInfo(breakdown.total)

        _uiState.value = ResultUiState(
            isVictory        = isVictory,
            defeatReason     = DefeatReason.entries.find { it.name == defeatStr } ?: DefeatReason.NONE,
            boardSize        = boardSize,
            difficulty       = when (diffStr) {
                "EASY" -> "5×5"; "MEDIUM" -> "6×6"
                "HARD" -> "8×8"; "DEVIL"  -> "DEVIL 10×10"; else -> diffStr
            },
            gameMode         = gameMode,
            squaresVisited   = moveCount,
            totalSquares     = boardSize * boardSize,
            moveCount        = moveCount,
            elapsedSeconds   = elapsed,
            timeLimitSeconds = timeLimit,
            baseScore        = breakdown.base,
            timeBonus        = breakdown.timeBonus,
            sizeBonus        = breakdown.sizeBonus,
            hintPenalty      = breakdown.hintPenalty,
            totalScore       = breakdown.total,
            rankLabel        = rankInfo.label,
            rankProgress     = rankInfo.progressFrac,
            isLoading        = false,
        )
    }

    // ── Rematch ──────────────────────────────────────────────────

    /** Called when the local player taps Play Again in online mode. */
    fun requestRematch(onStartRematch: (roomCode: String, isHost: Boolean) -> Unit) {
        val state = _uiState.value
        if (!state.isOnlineMode || state.roomCode.isEmpty()) return
        if (state.rematchState != RematchState.IDLE) return

        val isHost   = state.localRole == "host"
        val roomCode = state.roomCode

        _uiState.update { it.copy(rematchState = RematchState.WAITING) }

        viewModelScope.launch {
            try {
                // Step 1: Signal we're ready (overwrites any stale value for our role only)
                // Do NOT clear the whole node — that would wipe the opponent's signal too
                firebaseRepo.signalRematch(roomCode, state.localRole)

                if (isHost) {
                    // Step 2 (Host): Wait for guest to also signal, then reset room
                    firebaseRepo.observeRematch(roomCode)
                        .collect {
                            _uiState.update { it.copy(rematchState = RematchState.STARTING) }

                            // Reset room FIRST — room goes back to WAITING
                            firebaseRepo.resetRoomForRematch(
                                roomCode  = roomCode,
                                hostId    = sessionManager.currentSession()?.localId ?: "",
                                hostName  = state.localName,
                                guestName = state.opponentName,
                                boardSize = state.boardSize,
                            )
                            // Signal guest that room is ready to join
                            firebaseRepo.signalRoomReady(roomCode)

                            // Room already reset above — just attach session + observer
                            sessionManager.attachToExistingRoom(
                                localId   = prefsRepository.getOrCreatePlayerId(),
                                localName = state.localName,
                                roomCode  = roomCode,
                                boardSize = state.boardSize,
                                scope     = viewModelScope,
                            )
                            onStartRematch(roomCode, true)
                            return@collect
                        }
                } else {
                    // Step 2 (Guest): Wait for host to reset room and mark it ready
                    firebaseRepo.observeRoomReady(roomCode)
                        .collect { ready ->
                            if (!ready) return@collect
                            _uiState.update { it.copy(rematchState = RematchState.STARTING) }

                            // Room is now in WAITING state — safe to join directly (no accept needed for rematch)
                            val joined = sessionManager.joinRoomForRematch(
                                localId   = prefsRepository.getOrCreatePlayerId(),
                                roomCode  = roomCode,
                                localName = state.localName,
                                scope     = viewModelScope,
                            )
                            if (joined) {
                                onStartRematch(roomCode, false)
                            } else {
                                _uiState.update { it.copy(rematchState = RematchState.IDLE) }
                            }
                            return@collect
                        }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(rematchState = RematchState.IDLE) }
            }
        }
    }

    /**
     * Player chose not to rematch — disconnect cleanly from Firebase.
     * Cancels any pending rematch signal so the opponent's button resets.
     * Host deletes the room; guest just marks themselves disconnected.
     */
    fun exitGame(onDone: () -> Unit) {
        val state = _uiState.value
        if (!state.isOnlineMode || state.roomCode.isEmpty()) {
            onDone()
            return
        }
        viewModelScope.launch {
            try {
                val roomCode = state.roomCode
                val role     = state.localRole
                val isHost   = role == "host"

                // Cancel only our own pending signal — don't wipe opponent's
                firebaseRepo.clearMyRematchSignal(roomCode, role)

                if (isHost) {
                    // Host: reset room back to WAITING so it stays available for next challenger
                    // Room is persistent — never deleted, just recycled
                    firebaseRepo.resetRoomForRematch(
                        roomCode  = roomCode,
                        hostId    = "",           // new hostId assigned next time createRoom called
                        hostName  = state.localName,
                        guestName = "",
                        boardSize = state.boardSize,
                    )
                } else {
                    // Guest: just mark disconnected — host's room remains intact
                    firebaseRepo.setConnected(roomCode, role, false)
                }
            } catch (_: Exception) {}
            onDone()
        }
    }
}