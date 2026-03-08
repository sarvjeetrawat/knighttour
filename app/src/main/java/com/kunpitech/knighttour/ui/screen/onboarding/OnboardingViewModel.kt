package com.kunpitech.knighttour.ui.screen.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kunpitech.knighttour.data.repository.FirebaseGameRepository
import com.kunpitech.knighttour.data.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ═══════════════════════════════════════════════════════════════
//  ONBOARDING VIEWMODEL
//
//  Handles first-launch username selection:
//  1. User types a name → debounced availability check (600ms)
//  2. Name checked against Firebase scores/{name} node
//  3. On confirm → saves name + marks isSignedIn=true → navigate home
// ═══════════════════════════════════════════════════════════════

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val prefsRepository : UserPreferencesRepository,
    private val firebaseRepo    : FirebaseGameRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private var checkJob: Job? = null

    // Regex: letters, digits, spaces, underscores only
    private val validNameRegex = Regex("^[a-zA-Z0-9 _]+$")

    fun onUsernameChanged(raw: String) {
        // Limit live input to 16 chars
        val input = raw.take(16)
        val trimmed = input.trim()

        // Cancel any pending check
        checkJob?.cancel()

        // Inline validation (no network needed)
        val inlineStatus: UsernameStatus? = when {
            trimmed.isEmpty()                      -> UsernameStatus.Idle
            trimmed.length < 3                     -> UsernameStatus.TooShort
            trimmed.length > 16                    -> UsernameStatus.TooLong
            !validNameRegex.matches(trimmed)       -> UsernameStatus.InvalidChars
            else                                   -> null   // needs network check
        }

        if (inlineStatus != null) {
            _uiState.value = OnboardingUiState(
                username  = input,
                status    = inlineStatus,
                canSubmit = false,
            )
            return
        }

        // Show checking state immediately, then debounce the actual network call
        _uiState.value = OnboardingUiState(
            username  = input,
            status    = UsernameStatus.Checking,
            canSubmit = false,
        )

        checkJob = viewModelScope.launch {
            delay(600)   // debounce — wait for user to stop typing
            val taken = firebaseRepo.isUsernameTaken(trimmed)
            _uiState.update {
                it.copy(
                    status    = if (taken) UsernameStatus.Taken else UsernameStatus.Available,
                    canSubmit = !taken,
                )
            }
        }
    }

    fun onConfirm(onDone: () -> Unit) {
        val state = _uiState.value
        if (!state.canSubmit || state.isSubmitting) return
        val name = state.username.trim()

        _uiState.update { it.copy(isSubmitting = true) }

        viewModelScope.launch {
            try {
                // Reserve the username in Firebase so concurrent registrations are rejected
                firebaseRepo.reserveUsername(name)
                // Persist locally
                prefsRepository.setPlayerName(name)
                prefsRepository.setSignedIn(true)
                // Generate stable player ID now
                prefsRepository.getOrCreatePlayerId()
                onDone()
            } catch (e: Exception) {
                // Name was just taken by someone else — race condition
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        status       = UsernameStatus.Taken,
                        canSubmit    = false,
                    )
                }
            }
        }
    }
}