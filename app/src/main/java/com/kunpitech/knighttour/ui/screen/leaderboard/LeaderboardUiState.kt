package com.kunpitech.knighttour.ui.screen.leaderboard

// ===============================================================
//  LEADERBOARD SCREEN -- UI STATE
// ===============================================================

data class LeaderboardEntry(
    val rank        : Int,
    val playerName  : String,
    val score       : Int,
    val boardLabel  : String,   // "6x6", "8x8" etc.
    val timeStr     : String,   // "1:42"
    val rankLabel   : String,   // "KNIGHT", "GRANDMASTER" etc.
    val isCurrentUser: Boolean = false,
)

data class LeaderboardUiState(
    val selectedTab    : LeaderboardTab        = LeaderboardTab.GLOBAL,
    val globalEntries  : List<LeaderboardEntry> = emptyList(),
    val friendEntries  : List<LeaderboardEntry> = emptyList(),
    val personalBests  : List<LeaderboardEntry> = emptyList(),
    val currentUserRank: Int                   = 0,
    val currentUserScore: Int                  = 0,
    val isLoading      : Boolean               = false,
    val error          : String?               = null,
)

enum class LeaderboardTab(val label: String) {
    GLOBAL("GLOBAL"),
    FRIENDS("FRIENDS"),
    PERSONAL("MY BESTS"),
}

sealed interface LeaderboardEvent {
    data class TabSelected(val tab: LeaderboardTab) : LeaderboardEvent
    data object NavigateBack : LeaderboardEvent
    data object Refresh      : LeaderboardEvent
}