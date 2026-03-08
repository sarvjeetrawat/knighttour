package com.kunpitech.knighttour.ui.screen.splash

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

// ═══════════════════════════════════════════════════════════════
//  SPLASH ROUTE
// ═══════════════════════════════════════════════════════════════

@Composable
fun SplashRoute(
    onNavigateHome       : () -> Unit,
    onNavigateOnboarding : () -> Unit,
    viewModel            : SplashViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState) {
        when (uiState) {
            is SplashUiState.NavigateToHome       -> onNavigateHome()
            is SplashUiState.NavigateToOnboarding -> onNavigateOnboarding()
            else                                  -> Unit
        }
    }

    SplashScreen(
        onSplashComplete = onNavigateHome,
    )
}