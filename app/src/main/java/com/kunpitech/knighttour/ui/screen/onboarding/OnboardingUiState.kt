package com.kunpitech.knighttour.ui.screen.onboarding

// ═══════════════════════════════════════════════════════════════
//  ONBOARDING UI STATE
// ═══════════════════════════════════════════════════════════════

data class OnboardingUiState(
    val username        : String            = "",
    val status          : UsernameStatus    = UsernameStatus.Idle,
    val isSubmitting    : Boolean           = false,
    val canSubmit       : Boolean           = false,
)

sealed interface UsernameStatus {
    data object Idle        : UsernameStatus   // not yet checked
    data object Checking    : UsernameStatus   // debounce / network call in flight
    data object Available   : UsernameStatus   // green — good to go
    data object Taken       : UsernameStatus   // red — already registered
    data object TooShort    : UsernameStatus   // < 3 chars
    data object TooLong     : UsernameStatus   // > 16 chars
    data object InvalidChars: UsernameStatus   // contains illegal characters
}