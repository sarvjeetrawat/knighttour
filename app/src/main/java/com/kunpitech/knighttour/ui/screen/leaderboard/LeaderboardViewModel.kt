package com.kunpitech.knighttour.ui.screen.leaderboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kunpitech.knighttour.data.repository.FirebaseGameRepository
import com.kunpitech.knighttour.data.repository.FirebaseScoreEntry
import com.kunpitech.knighttour.data.repository.UserPreferencesRepository
import com.kunpitech.knighttour.domain.engine.ScoreCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ================================================================
//  LEADERBOARD VIEWMODEL — fully Firebase-based
//
//  GLOBAL   : one entry per unique player, best score only
//  FRIENDS  : players you've faced in online rooms, best score each
//  MY STATS : your full game history, all games
// ================================================================

@HiltViewModel
class LeaderboardViewModel @Inject constructor(
    private val firebaseRepo    : FirebaseGameRepository,
    private val prefsRepository : UserPreferencesRepository,
    private val scoreCalculator : ScoreCalculator,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LeaderboardUiState(isLoading = true))
    val uiState: StateFlow<LeaderboardUiState> = _uiState.asStateFlow()

    init {
        loadAll()
    }

    fun onEvent(event: LeaderboardEvent) {
        when (event) {
            is LeaderboardEvent.TabSelected -> _uiState.update { it.copy(selectedTab = event.tab) }
            LeaderboardEvent.Refresh        -> loadAll()
            LeaderboardEvent.NavigateBack   -> Unit
        }
    }

    // ── Load all three tabs concurrently ─────────────────────────

    private fun loadAll() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val playerName = prefsRepository.preferences.first().playerName

                // Load all three in parallel
                val globalDeferred  = async { firebaseRepo.getGlobalLeaderboard(50) }
                val friendsDeferred = async { firebaseRepo.getFriendScores(playerName) }
                val historyDeferred = async { firebaseRepo.getPlayerHistory(playerName) }

                val globalRaw  = globalDeferred.await()
                val friendsRaw = friendsDeferred.await()
                val historyRaw = historyDeferred.await()

                // ── Global: one row per player, best score ────────
                val globalEntries = globalRaw.mapIndexed { idx, entry ->
                    entry.toLeaderboardEntry(
                        rank          = idx + 1,
                        isCurrentUser = entry.playerName == playerName,
                    )
                }

                // Current user's rank and best score in global
                val userRank  = globalEntries.indexOfFirst { it.isCurrentUser }
                    .takeIf { it >= 0 }?.plus(1) ?: 0
                val userScore = globalRaw.find { it.playerName == playerName }?.score ?: 0

                // ── Friends: best score per opponent ──────────────
                val friendEntries = friendsRaw.mapIndexed { idx, entry ->
                    entry.toLeaderboardEntry(rank = idx + 1, isCurrentUser = false)
                }

                // ── My Stats: full history newest first ───────────
                val myEntries = historyRaw.mapIndexed { idx, entry ->
                    entry.toLeaderboardEntry(rank = idx + 1, isCurrentUser = true)
                }

                _uiState.update {
                    it.copy(
                        globalEntries    = globalEntries,
                        friendEntries    = friendEntries,
                        myStatsEntries   = myEntries,
                        currentUserRank  = userRank,
                        currentUserScore = userScore,
                        isLoading        = false,
                        error            = null,
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error     = "Failed to load leaderboard. Check your connection.",
                    )
                }
            }
        }
    }

    private fun FirebaseScoreEntry.toLeaderboardEntry(
        rank          : Int,
        isCurrentUser : Boolean,
    ) = LeaderboardEntry(
        rank          = rank,
        playerName    = playerName,
        score         = score,
        boardLabel    = boardLabel,
        timeStr       = timeStr,
        rankLabel     = rankLabel,
        isCurrentUser = isCurrentUser,
    )
}