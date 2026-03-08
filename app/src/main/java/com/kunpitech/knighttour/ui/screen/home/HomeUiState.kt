package com.kunpitech.knighttour.ui.screen.home

// ═══════════════════════════════════════════════════════════════
//  HOME SCREEN — UI STATE
// ═══════════════════════════════════════════════════════════════

data class HomeUiState(
    val playerName        : String  = "Knight",
    val playerRank        : String  = "NOVICE",
    val playerScore       : Int     = 0,
    val gamesPlayed       : Int     = 0,
    val bestTime          : String  = "--:--",
    val winRate           : Int     = 0,
    val hasActiveGame     : Boolean = false,
    val activeDifficulty  : String  = "",
    val activeMoveCount   : Int     = 0,
    val isOnline          : Boolean = false,
    val dailyChallengeDone: Boolean = false,
    val isLoading         : Boolean = false,
    // Tracks which Solo Quest board size chip is selected
    val selectedDifficulty: String  = "MEDIUM",   // "EASY"=5x5, "MEDIUM"=6x6, "HARD"=8x8
    // Incoming game request — set when a guest joins host's room while host is on Home
    val incomingGuestName : String  = "",
    val incomingRoomCode  : String  = "",
) {
    val hasIncomingGame: Boolean get() = incomingGuestName.isNotEmpty()
}

sealed interface HomeEvent {
    /** Fired by PLAY button — carries the currently selected difficulty */
    data class PlayOffline(val difficulty: String) : HomeEvent
    data object PlayOnline           : HomeEvent
    data object PlayDevil            : HomeEvent
    data object ResumeGame           : HomeEvent
    data object OpenLeaderboard      : HomeEvent
    data object OpenSettings         : HomeEvent
    data object OpenDailyChallenge   : HomeEvent
    /** Fired when a board size chip is tapped — updates selection only */
    data class SelectDifficulty(val difficulty: String) : HomeEvent
    /** Host taps GO TO GAME after guest joined */
    data object GoToGame             : HomeEvent
    /** Host rejects guest request */
    data object RejectGame           : HomeEvent
}