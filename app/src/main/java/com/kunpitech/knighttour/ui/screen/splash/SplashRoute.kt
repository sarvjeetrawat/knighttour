package com.kunpitech.knighttour.ui.screen.splash

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

// ═══════════════════════════════════════════════════════════════
//  SPLASH ROUTE
//  Entry point used by NavGraph.kt
//
//  Usage in NavGraph:
//    composable(Screen.Splash.route) {
//        SplashRoute(onSplashComplete = { navController.navigate(Screen.Home.route) {
//            popUpTo(Screen.Splash.route) { inclusive = true }
//        }})
//    }
// ═══════════════════════════════════════════════════════════════

@Composable
fun SplashRoute(
    onSplashComplete : () -> Unit,
    viewModel        : SplashViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Navigate when VM signals ready
    LaunchedEffect(uiState) {
        if (uiState is SplashUiState.NavigateToHome) {
            onSplashComplete()
        }
    }

    SplashScreen(
        onSplashComplete = onSplashComplete,
    )
}