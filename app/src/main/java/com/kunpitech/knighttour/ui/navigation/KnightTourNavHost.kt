package com.kunpitech.knighttour.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kunpitech.knighttour.ui.screen.game.GameRoute
import com.kunpitech.knighttour.ui.screen.home.HomeRoute
import com.kunpitech.knighttour.ui.screen.leaderboard.LeaderboardRoute
import com.kunpitech.knighttour.ui.screen.result.ResultRoute
import com.kunpitech.knighttour.ui.screen.settings.SettingsRoute
import com.kunpitech.knighttour.ui.screen.splash.SplashRoute

// ═══════════════════════════════════════════════════════════════
//  KNIGHT TOUR — NAV GRAPH
// ═══════════════════════════════════════════════════════════════

sealed class Screen(val route: String) {
    data object Splash      : Screen("splash")
    data object Home        : Screen("home")

    // Game — optional sessionId for resume; optional difficulty/mode for new game
    data object Game : Screen(
        "game?sessionId={sessionId}&difficulty={difficulty}&gameMode={gameMode}&roomCode={roomCode}"
    ) {
        fun createRoute(
            sessionId  : String = "",
            difficulty : String = "MEDIUM",
            gameMode   : String = "OFFLINE",
            roomCode   : String = "",
        ) = "game?sessionId=$sessionId&difficulty=$difficulty&gameMode=$gameMode&roomCode=$roomCode"
    }

    data object Lobby       : Screen("lobby")

    data object Result      : Screen("result/{sessionId}") {
        fun createRoute(sessionId: String) = "result/$sessionId"
    }

    data object Leaderboard : Screen("leaderboard")
    data object Settings    : Screen("settings")
}

@Composable
fun KnightTourNavHost(
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController    = navController,
        startDestination = Screen.Splash.route,
    ) {

        // ── SPLASH ───────────────────────────────────────────────
        composable(Screen.Splash.route) {
            SplashRoute(
                onSplashComplete = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        // ── HOME ─────────────────────────────────────────────────
        composable(Screen.Home.route) {
            HomeRoute(
                onPlayOffline = { difficulty ->
                    navController.navigate(
                        Screen.Game.createRoute(difficulty = difficulty, gameMode = "OFFLINE")
                    )
                },
                onPlayOnline = {
                    navController.navigate(Screen.Lobby.route)
                },
                onPlayDevil = {
                    navController.navigate(
                        Screen.Game.createRoute(difficulty = "DEVIL", gameMode = "DEVIL")
                    )
                },
                onResumeGame = { sessionId ->
                    // Pass the real session ID so GameViewModel resumes existing state
                    navController.navigate(
                        Screen.Game.createRoute(sessionId = sessionId)
                    )
                },
                onOpenLeaderboard = { navController.navigate(Screen.Leaderboard.route) },
                onOpenSettings    = { navController.navigate(Screen.Settings.route) },
                onOpenDaily       = {
                    navController.navigate(
                        Screen.Game.createRoute(difficulty = "MEDIUM", gameMode = "OFFLINE")
                    )
                },
            )
        }

        // ── GAME ─────────────────────────────────────────────────
        composable(
            route = Screen.Game.route,
            arguments = listOf(
                navArgument("sessionId") {
                    type         = NavType.StringType
                    defaultValue = ""
                },
                navArgument("difficulty") {
                    type         = NavType.StringType
                    defaultValue = "MEDIUM"
                },
                navArgument("gameMode") {
                    type         = NavType.StringType
                    defaultValue = "OFFLINE"
                },
                navArgument("roomCode") {
                    type         = NavType.StringType
                    defaultValue = ""
                },
            ),
        ) {
            GameRoute(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateResult = { sessionId ->
                    navController.navigate(Screen.Result.createRoute(sessionId)) {
                        popUpTo(Screen.Home.route)
                    }
                },
            )
        }

        // ── LOBBY ────────────────────────────────────────────────
        composable(Screen.Lobby.route) {
            com.kunpitech.knighttour.ui.screen.lobby.LobbyRoute(
                onGameStart = { roomCode ->
                    navController.navigate(
                        Screen.Game.createRoute(gameMode = "ONLINE", roomCode = roomCode)
                    )
                },
                onNavigateBack = { navController.popBackStack() },
            )
        }

        // ── RESULT ───────────────────────────────────────────────
        composable(
            route     = Screen.Result.route,
            arguments = listOf(
                navArgument("sessionId") {
                    type         = NavType.StringType
                    defaultValue = ""
                },
            ),
        ) {
            ResultRoute(
                onPlayAgain = {
                    navController.navigate(Screen.Game.createRoute()) {
                        popUpTo(Screen.Home.route)
                    }
                },
                onGoHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                onOpenLeaderboard = {
                    navController.navigate(Screen.Leaderboard.route)
                },
            )
        }

        // ── LEADERBOARD ──────────────────────────────────────────
        composable(Screen.Leaderboard.route) {
            LeaderboardRoute(
                onNavigateBack = { navController.popBackStack() },
            )
        }

        // ── SETTINGS ─────────────────────────────────────────────
        composable(Screen.Settings.route) {
            SettingsRoute(
                onNavigateBack = { navController.popBackStack() },
            )
        }
    }
}