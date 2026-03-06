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
        }
    }

    /**
     * Observe opponent's score from the persistent matchResults node.
     * Works even after the room is deleted.
     */
    private fun observeOpponentScore(
        roomCode     : String,
        localRole    : String,
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
    ): ResultUiState {
        val didWinOnline = when {
            !isOnline          -> false
            opponentScore == 0 -> isVictory   // opponent didn't finish — win only if we completed
            myScore > opponentScore -> true
            myScore < opponentScore -> false
            else               -> isVictory   // tie: board completion wins
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
}