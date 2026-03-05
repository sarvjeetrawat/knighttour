package com.kunpitech.knighttour.ui.screen.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// ═══════════════════════════════════════════════════════════════
//  SPLASH VIEWMODEL
//
//  Responsibilities:
//  1. Trigger any app initialization (prefs, auth check, etc.)
//  2. Emit navigation signal once ready
//  3. Decouple animation timing from business logic
// ═══════════════════════════════════════════════════════════════

sealed interface SplashUiState {
    data object Loading   : SplashUiState
    data object Ready     : SplashUiState
    data object NavigateToHome : SplashUiState
}

@HiltViewModel
class SplashViewModel @Inject constructor(
    // Inject repositories here as app grows, e.g.:
    // private val userRepository: UserRepository,
    // private val appPreferences: AppPreferences,
) : ViewModel() {

    private val _uiState = MutableStateFlow<SplashUiState>(SplashUiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        initialize()
    }

    private fun initialize() {
        viewModelScope.launch {
            // Perform lightweight startup tasks:
            // - Check if user has an active saved game
            // - Load user preferences (theme, sound, haptics)
            // - Verify Firebase anonymous auth token
            // These run in parallel with the visual animation.

            // Minimum display time = 2800ms (matches animation timeline)
            // If init finishes early, we wait. If it's slow, we still
            // navigate after the animation completes.
            delay(2800)

            _uiState.value = SplashUiState.NavigateToHome
        }
    }
}