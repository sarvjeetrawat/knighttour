package com.kunpitech.knighttour.ui.screen.home

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kunpitech.knighttour.data.repository.GameRepository
import com.kunpitech.knighttour.data.repository.UserPreferencesRepository
import com.kunpitech.knighttour.domain.engine.ScoreCalculator
import com.kunpitech.knighttour.domain.model.Difficulty
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    @ApplicationContext
    private val context         : Context,
    private val gameRepository  : GameRepository,
    private val prefsRepository : UserPreferencesRepository,
    private val scoreCalculator : ScoreCalculator,
) : ViewModel() {

    private val _uiState: MutableStateFlow<HomeUiState> = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // Expose the in-progress session ID so NavGraph can resume it
    private val _resumeSessionId = MutableStateFlow<String?>(null)
    val resumeSessionId: StateFlow<String?> = _resumeSessionId.asStateFlow()

    // ConnectivityManager callback — updates isOnline in real time
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _uiState.update { it.copy(isOnline = true) }
        }
        override fun onLost(network: Network) {
            // Check if any other network is still available
            val stillOnline = connectivityManager.activeNetwork != null
            _uiState.update { it.copy(isOnline = stillOnline) }
        }
    }

    init {
        observePreferences()
        observeActiveGame()
        loadStats()
        // Refresh stats whenever a game completes
        gameRepository.getFinishedSessions(limit = 1)
            .onEach { loadStats() }
            .launchIn(viewModelScope)

        // Set initial online state from current active network
        val active = connectivityManager.activeNetwork
        val caps   = connectivityManager.getNetworkCapabilities(active)
        val hasNet = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        _uiState.update { it.copy(isOnline = hasNet) }

        // Register live connectivity callback
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, networkCallback)
    }

    override fun onCleared() {
        super.onCleared()
        try { connectivityManager.unregisterNetworkCallback(networkCallback) } catch (_: Exception) {}
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