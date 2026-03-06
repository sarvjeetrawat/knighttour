package com.kunpitech.knighttour.ui.screen.result

import com.kunpitech.knighttour.domain.model.DefeatReason

// ═══════════════════════════════════════════════════════════════
//  RESULT SCREEN — UI STATE
// ═══════════════════════════════════════════════════════════════

data class ResultUiState(
    // Outcome
    val isVictory        : Boolean      = true,
    val defeatReason     : DefeatReason = DefeatReason.NONE,

    // Game summary
    val boardSize        : Int          = 6,
    val difficulty       : String       = "6×6",
    val gameMode         : String       = "OFFLINE",
    val squaresVisited   : Int          = 36,
    val totalSquares     : Int          = 36,
    val moveCount        : Int          = 36,

    // Time
    val elapsedSeconds   : Int          = 94,
    val timeLimitSeconds : Int          = 240,

    // Score breakdown
    val baseScore        : Int          = 3600,
    val timeBonus        : Int          = 1168,
    val sizeBonus        : Int          = 180,
    val hintPenalty      : Int          = 0,
    val totalScore       : Int          = 4948,

    // Rank / progression
    val rankLabel        : String       = "KNIGHT",
    val rankProgress     : Float        = 0.62f,
    val isNewPersonalBest: Boolean      = false,
    val personalBest     : Int          = 5200,

    // Online
    val isOnlineMode     : Boolean      = false,
    val opponentName     : String       = "",
    val opponentScore    : Int          = 0,
    val didWinOnline     : Boolean      = false,

    // Rematch (online only)
    val roomCode         : String       = "",
    val localRole        : String       = "",   // "host" or "guest"
    val localName        : String       = "",
    val rematchState     : RematchState = RematchState.IDLE,

    val isLoading        : Boolean      = false,
)


enum class RematchState { IDLE, WAITING, STARTING }

sealed interface ResultEvent {
    data object PlayAgain       : ResultEvent
    data object GoHome          : ResultEvent
    data object OpenLeaderboard : ResultEvent
    data object ShareResult     : ResultEvent
}