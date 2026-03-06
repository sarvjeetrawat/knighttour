package com.kunpitech.knighttour.ui.screen.result

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kunpitech.knighttour.data.repository.FirebaseGameRepository
import com.kunpitech.knighttour.data.repository.GameRepository
import com.kunpitech.knighttour.data.repository.UserPreferencesRepository
import com.kunpitech.knighttour.domain.engine.ScoreCalculator
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
//  Flow:
//    1. Load GameSession from Room DB via sessionId nav arg
//    2. Compute score breakdown via ScoreCalculator
//    3. Push score to Firebase (single source of truth for leaderboard)
//    4. Show Victory / Defeat UI
//
//  NOTE: No local ScoreRepository involved.
//        Firebase is the only leaderboard storage.
// ═══════════════════════════════════════════════════════════════

@HiltViewModel
class ResultViewModel @Inject constructor(
    private val savedStateHandle : SavedStateHandle,
    private val gameRepository   : GameRepository,
    private val prefsRepository  : UserPreferencesRepository,
    private val scoreCalculator  : ScoreCalculator,
    private val firebaseRepo     : FirebaseGameRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ResultUiState(isLoading = true))
    val uiState: StateFlow<ResultUiState> = _uiState.asStateFlow()

    init {
        loadResult()
    }

    private fun loadResult() {
        viewModelScope.launch {
            val sessionId = savedStateHandle.get<String>("sessionId") ?: ""

            val session = gameRepository.getById(sessionId)
            if (session == null) {
                loadFromNavArgs()
                return@launch
            }

            val playerName = prefsRepository.preferences.first().playerName
            val breakdown  = scoreCalculator.calculate(session)
            val rankInfo   = scoreCalculator.rankInfo(breakdown.total)

            // Push to Firebase — only for completed games (victories or online partial)
            val isVictory = session.status == SessionStatus.COMPLETED
            val isOnlineWithMoves = session.mode == GameMode.ONLINE && session.moveCount > 0
            if (isVictory || isOnlineWithMoves) {
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
                } catch (_: Exception) { /* offline — score saved next time */ }
            }

            // Personal best = highest score ever for this board size in Firebase history
            val prevBest = try {
                firebaseRepo.getPlayerHistory(playerName)
                    .filter { it.boardLabel == "${session.boardSize}×${session.boardSize}" }
                    .maxOfOrNull { it.score } ?: 0
            } catch (_: Exception) { 0 }
            val isNewPB = isVictory && breakdown.total > prevBest

            _uiState.value = ResultUiState(
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
                isOnlineMode      = session.mode == GameMode.ONLINE,
                opponentName      = session.opponentId.take(12),
                opponentScore     = 0,
                didWinOnline      = false,
                isLoading         = false,
            )
        }
    }

    // ── Fallback when session not yet in DB ──────────────────────

    private suspend fun loadFromNavArgs() {
        val isVictory  = savedStateHandle.get<Boolean>("isVictory")      ?: true
        val boardSize  = savedStateHandle.get<Int>("boardSize")           ?: 6
        val diffStr    = savedStateHandle.get<String>("difficulty")       ?: "MEDIUM"
        val gameMode   = savedStateHandle.get<String>("gameMode")         ?: "OFFLINE"
        val moveCount  = savedStateHandle.get<Int>("moveCount")           ?: 0
        val elapsed    = savedStateHandle.get<Int>("elapsedSeconds")      ?: 0
        val timeLimit  = savedStateHandle.get<Int>("timeLimitSeconds")    ?: 240
        val hintsUsed  = savedStateHandle.get<Int>("hintsUsed")           ?: 0
        val defeatStr  = savedStateHandle.get<String>("defeatReason")     ?: "NONE"

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