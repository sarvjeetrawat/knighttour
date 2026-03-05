package com.kunpitech.knighttour

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.kunpitech.knighttour.ui.navigation.KnightTourNavHost
import com.kunpitech.knighttour.ui.theme.KnightTourTheme
import dagger.hilt.android.AndroidEntryPoint

// ═══════════════════════════════════════════════════════════════
//  KNIGHT TOUR — MAIN ACTIVITY
//
//  Single-activity architecture.
//  Responsibilities:
//  1. Install the Android SplashScreen API (no white flash on launch)
//  2. Enable edge-to-edge rendering
//  3. Set Compose content with KnightTourTheme
// ═══════════════════════════════════════════════════════════════

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        // ── STEP 1: Install splash BEFORE super.onCreate ─────────
        // This replaces the window background with our splash theme
        // and prevents any white flash on cold start.
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)

        // ── STEP 2: Keep splash on screen until Compose is ready ──
        // The condition returns true = keep showing splash.
        // Once Compose renders its first frame, this returns false
        // and the system splash dismisses with an exit animation.
        //
        // For a more controlled dismiss (e.g. wait for data):
        // splashScreen.setKeepOnScreenCondition { viewModel.isLoading.value }
        //
        // Here we let the Compose SplashScreen handle timing itself,
        // so we dismiss the system splash immediately after first frame.
        splashScreen.setKeepOnScreenCondition { false }

        // ── STEP 3: Optional — customize exit animation ───────────
        // splashScreen.setOnExitAnimationListener { splashScreenViewProvider ->
        //     val splashScreenView = splashScreenViewProvider.view
        //     ObjectAnimator.ofFloat(splashScreenView, View.ALPHA, 1f, 0f).apply {
        //         duration = 300L
        //         doOnEnd { splashScreenViewProvider.remove() }
        //         start()
        //     }
        // }

        // ── STEP 4: Edge-to-edge (transparent status/nav bars) ────
        enableEdgeToEdge()

        // ── STEP 5: Set Compose content ───────────────────────────
        setContent {
            KnightTourTheme {
                KnightTourNavHost()
            }
        }
    }
}