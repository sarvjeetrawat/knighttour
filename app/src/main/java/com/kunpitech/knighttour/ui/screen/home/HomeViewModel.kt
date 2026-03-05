package com.kunpitech.knighttour.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kunpitech.knighttour.data.repository.GameRepository
import com.kunpitech.knighttour.data.repository.UserPreferencesRepository
import com.kunpitech.knighttour.domain.engine.ScoreCalculator
import com.kunpitech.knighttour.domain.model.Difficulty
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ═══════════════════════════════════════════════════════════════
//  HOME VIEWMODEL  — fully wired to repositories
// ═══════════════════════════════════════════════════════════════

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val gameRepository  : GameRepository,
    private val prefsRepository : UserPreferencesRepository,
    private val scoreCalculator : ScoreCalculator,
) : ViewModel() {

    private val _uiState: MutableStateFlow<HomeUiState> = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // Expose the in-progress session ID so NavGraph can resume it
    private val _resumeSessionId = MutableStateFlow<String?>(null)
    val resumeSessionId: StateFlow<String?> = _resumeSessionId.asStateFlow()

    init {
        observePreferences()
        observeActiveGame()
        loadStats()
        // Refresh stats whenever a game completes
        gameRepository.getFinishedSessions(limit = 1)
            .onEach { loadStats() }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.SelectDifficulty ->
                _uiState.update { it.copy(selectedDifficulty = event.difficulty) }
            else -> Unit   // navigation events handled in HomeRoute
        }
    }

    // ── Observe DataStore preferences ─────────────────────────────

    private fun observePreferences() {
        prefsRepository.preferences
            .onEach { prefs ->
                _uiState.update { it.copy(playerName = prefs.playerName) }
            }
            .launchIn(viewModelScope)
    }

    // ── Observe in-progress game ──────────────────────────────────

    private fun observeActiveGame() {
        gameRepository.hasInProgressSession()
            .onEach { has ->
                if (has) {
                    val session = gameRepository.getLatestInProgress()
                    _resumeSessionId.value = session?.id
                    _uiState.update { state ->
                        state.copy(
                            hasActiveGame    = session != null,
                            activeDifficulty = session?.difficulty?.uiLabel ?: "",
                            activeMoveCount  = session?.moveCount ?: 0,
                        )
                    }
                } else {
                    _resumeSessionId.value = null
                    _uiState.update { it.copy(hasActiveGame = false) }
                }
            }
            .launchIn(viewModelScope)
    }

    // ── Load player stats from DB ──────────────────────────────────

    private fun loadStats() {
        viewModelScope.launch {
            val wins        = gameRepository.getTotalWins()
            val played      = gameRepository.getTotalGamesPlayed()
            val bestScore   = gameRepository.getGlobalPersonalBest() ?: 0
            val rankInfo    = scoreCalculator.rankInfo(bestScore)
            val winRate     = if (played > 0) (wins * 100) / played else 0
            val fastestSecs = gameRepository.getFastestWinSeconds()
            val bestTime    = if (fastestSecs != null) {
                "%d:%02d".format(fastestSecs / 60, fastestSecs % 60)
            } else "--:--"

            _uiState.update { state ->
                state.copy(
                    playerScore  = bestScore,
                    playerRank   = rankInfo.label,
                    gamesPlayed  = played,
                    winRate      = winRate,
                    bestTime     = bestTime,
                )
            }
        }
    }
}