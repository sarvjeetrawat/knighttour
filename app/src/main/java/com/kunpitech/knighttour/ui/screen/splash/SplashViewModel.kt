package com.kunpitech.knighttour.ui.screen.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kunpitech.knighttour.data.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

// ═══════════════════════════════════════════════════════════════
//  SPLASH VIEWMODEL
//
//  Responsibilities:
//  1. Wait for splash animation to complete (2800ms)
//  2. Check if user has completed onboarding (chosen username)
//  3. Route to Onboarding (first launch) or Home (returning user)
// ═══════════════════════════════════════════════════════════════

sealed interface SplashUiState {
    data object Loading              : SplashUiState
    data object Ready                : SplashUiState
    data object NavigateToHome       : SplashUiState
    data object NavigateToOnboarding : SplashUiState
}

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val prefsRepository: UserPreferencesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<SplashUiState>(SplashUiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        initialize()
    }

    private fun initialize() {
        viewModelScope.launch {
            // Minimum display time = 2800ms (matches animation timeline)
            delay(2800)

            val prefs = prefsRepository.preferences.first()
            _uiState.value = if (prefs.isSignedIn && prefs.playerName.isNotBlank()
                && prefs.playerName != "Knight") {
                SplashUiState.NavigateToHome
            } else {
                SplashUiState.NavigateToOnboarding
            }
        }
    }
}