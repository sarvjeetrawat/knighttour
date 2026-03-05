package com.kunpitech.knighttour.ui.screen.result

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kunpitech.knighttour.data.repository.GameRepository
import com.kunpitech.knighttour.data.repository.ScoreRepository
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
//  RESULT VIEWMODEL — wired to repositories
//
//  Flow:
//    1. Read sessionId nav arg
//    2. Load GameSession from Room DB
//    3. Compute score breakdown via ScoreCalculator
//    4. Record score in ScoreRepository (only for victories)
//    5. Check personal best against previous best for this board size
// ═══════════════════════════════════════════════════════════════

@HiltViewModel
class ResultViewModel @Inject constructor(
    private val savedStateHandle  : SavedStateHandle,
    private val gameRepository    : GameRepository,
    private val scoreRepository   : ScoreRepository,
    private val prefsRepository   : UserPreferencesRepository,
    private val scoreCalculator   : ScoreCalculator,
) : ViewModel() {

    private val _uiState: MutableStateFlow<ResultUiState> =
        MutableStateFlow(ResultUiState(isLoading = true))
    val uiState: StateFlow<ResultUiState> = _uiState.asStateFlow()

    init {
        loadResult()
    }

    private fun loadResult() {
        viewModelScope.launch {
            val sessionId = savedStateHandle.get<String>("sessionId") ?: ""

            // Load session from Room DB
            val session = gameRepository.getById(sessionId)

            if (session == null) {
                // Fallback to nav args for edge cases (e.g. first launch)
                loadFromNavArgs()
                return@launch
            }

            // Get player name from preferences
            val playerName = prefsRepository.preferences.first().playerName

            // Score breakdown
            val breakdown = scoreCalculator.calculate(session)
            val rankInfo  = scoreCalculator.rankInfo(breakdown.total)

            // Check personal best BEFORE recording new score
            val prevBest = gameRepository.getBestScoreForBoard(session.boardSize) ?: 0
            val isNewPB  = session.status == SessionStatus.COMPLETED &&
                    breakdown.total > prevBest

            // Record score for victories
            if (session.status == SessionStatus.COMPLETED) {
                scoreRepository.recordScore(session, playerName)
            }

            val defeatReason = DefeatReason.fromStatus(session.status)
            val isVictory    = session.status == SessionStatus.COMPLETED

            val diffLabel = session.difficulty.uiLabel
            val gameModeStr = session.mode.name

            _uiState.value = ResultUiState(
                isVictory        = isVictory,
                defeatReason     = defeatReason,
                boardSize        = session.boardSize,
                difficulty       = diffLabel,
                gameMode         = gameModeStr,
                squaresVisited   = session.moveCount,
                totalSquares     = session.totalCells,
                moveCount        = session.moveCount,
                elapsedSeconds   = session.elapsedSeconds,
                timeLimitSeconds = session.difficulty.timeLimitSeconds,
                baseScore        = breakdown.base,
                timeBonus        = breakdown.timeBonus,
                sizeBonus        = breakdown.sizeBonus,
                hintPenalty      = breakdown.hintPenalty,
                totalScore       = breakdown.total,
                rankLabel        = rankInfo.label,
                rankProgress     = rankInfo.progressFrac,
                isNewPersonalBest = isNewPB,
                isOnlineMode     = session.mode == GameMode.ONLINE,
                opponentName     = session.opponentId.take(12),
                opponentScore    = 0,   // Phase 4: from Firebase match result
                didWinOnline     = false,
                isLoading        = false,
            )
        }
    }

    // ── Fallback: reconstruct from nav args (no session in DB yet) ─

    private suspend fun loadFromNavArgs() {
        val isVictory  = savedStateHandle.get<Boolean>("isVictory")     ?: true
        val boardSize  = savedStateHandle.get<Int>("boardSize")          ?: 6
        val diffStr    = savedStateHandle.get<String>("difficulty")      ?: "MEDIUM"
        val gameMode   = savedStateHandle.get<String>("gameMode")        ?: "OFFLINE"
        val moveCount  = savedStateHandle.get<Int>("moveCount")          ?: 0
        val totalCells = savedStateHandle.get<Int>("totalCells")         ?: 36
        val elapsed    = savedStateHandle.get<Int>("elapsedSeconds")     ?: 0
        val timeLimit  = savedStateHandle.get<Int>("timeLimitSeconds")   ?: 240
        val hintsUsed  = savedStateHandle.get<Int>("hintsUsed")          ?: 0
        val defeatStr  = savedStateHandle.get<String>("defeatReason")    ?: "NONE"

        val defeatReason = DefeatReason.entries.find { it.name == defeatStr } ?: DefeatReason.NONE
        val diffLabel    = when (diffStr) {
            "EASY"   -> "5×5"
            "MEDIUM" -> "6×6"
            "HARD"   -> "8×8"
            "DEVIL"  -> "DEVIL 10×10"
            else     -> diffStr
        }

        val breakdown = scoreCalculator.calculate(
            moveCount     = moveCount,
            timeRemaining = (timeLimit - elapsed).coerceAtLeast(0),
            boardSize     = boardSize,
            hintsUsed     = hintsUsed,
        )
        val rankInfo = scoreCalculator.rankInfo(breakdown.total)

        _uiState.value = ResultUiState(
            isVictory        = isVictory,
            defeatReason     = defeatReason,
            boardSize        = boardSize,
            difficulty       = diffLabel,
            gameMode         = gameMode,
            squaresVisited   = moveCount,
            totalSquares     = totalCells,
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