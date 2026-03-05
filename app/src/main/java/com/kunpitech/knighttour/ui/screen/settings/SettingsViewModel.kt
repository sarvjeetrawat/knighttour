package com.kunpitech.knighttour.ui.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kunpitech.knighttour.data.repository.GameRepository
import com.kunpitech.knighttour.data.repository.ScoreRepository
import com.kunpitech.knighttour.data.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ===============================================================
//  SETTINGS VIEWMODEL  (wired to DataStore + repositories)
// ===============================================================

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefsRepository : UserPreferencesRepository,
    private val gameRepository  : GameRepository,
    private val scoreRepository : ScoreRepository,
) : ViewModel() {

    private val _uiState: MutableStateFlow<SettingsUiState> =
        MutableStateFlow(SettingsUiState(isLoading = true))
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        // Observe DataStore and push updates to UI state
        prefsRepository.preferences
            .onEach { prefs ->
                _uiState.update {
                    it.copy(
                        soundEnabled      = prefs.soundEnabled,
                        musicEnabled      = prefs.musicEnabled,
                        hapticsEnabled    = prefs.hapticsEnabled,
                        defaultDifficulty = prefs.defaultDifficulty,
                        showMoveNumbers   = prefs.showMoveNumbers,
                        showValidMoves    = prefs.showValidMoves,
                        autoHint          = prefs.autoHint,
                        boardTheme        = prefs.boardTheme,
                        playerName        = prefs.playerName,
                        isSignedIn        = prefs.isSignedIn,
                        isLoading         = false,
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.SoundToggled       -> save { prefsRepository.setSoundEnabled(event.enabled) }
            is SettingsEvent.MusicToggled       -> save { prefsRepository.setMusicEnabled(event.enabled) }
            is SettingsEvent.HapticsToggled     -> save { prefsRepository.setHapticsEnabled(event.enabled) }
            is SettingsEvent.DifficultyChanged  -> save { prefsRepository.setDefaultDifficulty(event.diff) }
            is SettingsEvent.MoveNumbersToggled -> save { prefsRepository.setShowMoveNumbers(event.enabled) }
            is SettingsEvent.ValidMovesToggled  -> save { prefsRepository.setShowValidMoves(event.enabled) }
            is SettingsEvent.AutoHintToggled    -> save { prefsRepository.setAutoHint(event.enabled) }
            is SettingsEvent.BoardThemeChanged  -> save { prefsRepository.setBoardTheme(event.theme) }
            SettingsEvent.SignIn                -> handleSignIn()
            SettingsEvent.SignOut               -> handleSignOut()
            SettingsEvent.ClearData             -> handleClearData()
            SettingsEvent.NavigateBack          -> Unit
        }
    }

    // ── Helpers ──────────────────────────────────────────────────

    private fun save(block: suspend () -> Unit) {
        viewModelScope.launch {
            block()
            flashSaved()
        }
    }

    private suspend fun flashSaved() {
        _uiState.update { it.copy(savedFeedback = true) }
        delay(1400)
        _uiState.update { it.copy(savedFeedback = false) }
    }

    private fun handleSignIn() {
        viewModelScope.launch {
            // TODO: Firebase / Google Sign-In flow
            prefsRepository.setSignedIn(true)
        }
    }

    private fun handleSignOut() {
        viewModelScope.launch {
            prefsRepository.setSignedIn(false)
        }
    }

    private fun handleClearData() {
        viewModelScope.launch {
            gameRepository.clearAll()
            scoreRepository.clearAll()
            prefsRepository.clearAll()
        }
    }
}