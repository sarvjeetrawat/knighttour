package com.kunpitech.knighttour.ui.screen.leaderboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kunpitech.knighttour.data.repository.ScoreRepository
import com.kunpitech.knighttour.data.repository.ScoreSummary
import com.kunpitech.knighttour.domain.engine.ScoreCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import javax.inject.Inject

// ===============================================================
//  LEADERBOARD VIEWMODEL — wired to ScoreRepository
//
//  GLOBAL tab  : top 50 local scores (all board sizes, best first)
//  FRIENDS tab : stub until Firebase friends list is implemented
//  MY BESTS tab: personal bests per board size from Room DB
// ===============================================================

@HiltViewModel
class LeaderboardViewModel @Inject constructor(
    private val scoreRepository : ScoreRepository,
    private val scoreCalculator : ScoreCalculator,
) : ViewModel() {

    private val _uiState: MutableStateFlow<LeaderboardUiState> =
        MutableStateFlow(LeaderboardUiState(isLoading = true))
    val uiState: StateFlow<LeaderboardUiState> = _uiState.asStateFlow()

    init {
        observeScores()
    }

    fun onEvent(event: LeaderboardEvent) {
        when (event) {
            is LeaderboardEvent.TabSelected -> _uiState.update { it.copy(selectedTab = event.tab) }
            LeaderboardEvent.Refresh        -> Unit   // flows auto-refresh from Room
            LeaderboardEvent.NavigateBack   -> Unit   // handled by composable
        }
    }

    // ── Observe both flows and merge into UI state ────────────────

    private fun observeScores() {
        combine(
            scoreRepository.getTopScores(limit = 50),
            scoreRepository.getPersonalBests(),
        ) { topScores, personalBests ->
            Pair(topScores, personalBests)
        }
            .catch { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
            .onEach { (topScores, personalBests) ->
                val playerName = personalBests.firstOrNull()?.playerName ?: "You"

                // ── Global entries ────────────────────────────────
                val globalEntries = topScores.mapIndexed { idx, summary ->
                    summary.toEntry(
                        rank         = idx + 1,
                        playerName   = if (summary.playerName == playerName) "You" else summary.playerName,
                        isCurrentUser = summary.playerName == playerName,
                    )
                }

                // ── Personal bests entries ────────────────────────
                val personalEntries = personalBests.mapIndexed { idx, summary ->
                    summary.toEntry(rank = idx + 1, isCurrentUser = true)
                }

                // Current user's best score and rank position in global
                val userBest = personalBests.maxByOrNull { it.score }
                val userRank = globalEntries.indexOfFirst { it.isCurrentUser }
                    .takeIf { it >= 0 }?.plus(1) ?: 0

                _uiState.update { state ->
                    state.copy(
                        globalEntries    = globalEntries,
                        friendEntries    = globalEntries,   // Phase 4: filter by Firebase friends list
                        personalBests    = personalEntries,
                        currentUserRank  = userRank,
                        currentUserScore = userBest?.score ?: 0,
                        isLoading        = false,
                        error            = null,
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    // ── Mappers ───────────────────────────────────────────────────

    private fun ScoreSummary.toEntry(
        rank         : Int,
        playerName   : String = this.playerName,
        isCurrentUser: Boolean = false,
    ): LeaderboardEntry {
        val mins = elapsedSeconds / 60
        val secs = elapsedSeconds % 60
        val timeStr = "%d:%02d".format(mins, secs)

        val rankInfo = scoreCalculator.rankInfo(score)

        return LeaderboardEntry(
            rank         = rank,
            playerName   = playerName,
            score        = score,
            boardLabel   = "${boardSize}x${boardSize}",
            timeStr      = timeStr,
            rankLabel    = rankInfo.label,
            isCurrentUser = isCurrentUser,
        )
    }

}