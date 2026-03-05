package com.kunpitech.knighttour.ui.screen.game

// ═══════════════════════════════════════════════════════════════
//  GAME SCREEN — UI STATE MODELS
// ═══════════════════════════════════════════════════════════════

data class CellState(
    val row         : Int,
    val col         : Int,
    val isVisited   : Boolean = false,
    val isKnight    : Boolean = false,
    val isValidMove : Boolean = false,
    val isHint      : Boolean = false,
    val moveNumber  : Int     = 0,   // 0 = unvisited
)

data class GameUiState(
    // Board
    val boardSize        : Int             = 6,
    val cells            : List<CellState> = buildInitialCells(6),
    val moveCount        : Int             = 0,
    val totalCells       : Int             = 36,

    // Timer
    val elapsedSeconds   : Int             = 0,
    val timeLimitSeconds : Int             = 240,
    val isTimerPanic     : Boolean         = false,   // last 15 seconds

    // Game flow
    val gameState        : GamePhase       = GamePhase.PLAYING,
    val gameMode         : GameModeUi      = GameModeUi.OFFLINE,
    val difficulty       : DifficultyUi    = DifficultyUi.MEDIUM,

    // Actions
    val canUndo          : Boolean         = false,
    val hintsRemaining   : Int             = 1,

    // Animation triggers
    val shakeCell        : Pair<Int, Int>? = null,   // invalid move

    // Online
    val isOnlineMode     : Boolean         = false,
    val opponentName     : String          = "",
    val opponentMoves    : Int             = 0,
    val opponentProgress : Float           = 0f,
    val roomCode         : String          = "",

    // Score
    val currentScore     : Int             = 0,

    // Preferences (read from DataStore, applied live)
    val boardTheme       : String          = "OBSIDIAN",  // matches BoardTheme.name
    val showMoveNumbers  : Boolean         = true,
    val showValidMoves   : Boolean         = true,
)

enum class GamePhase { PLAYING, PAUSED, COMPLETED, FAILED }

enum class GameModeUi { OFFLINE, ONLINE, DEVIL }

enum class DifficultyUi(
    val label     : String,
    val boardSize : Int,
    val timeLimit : Int,
    val hints     : Int,
) {
    EASY(   "5×5",         5, 300, 3),
    MEDIUM( "6×6",         6, 240, 1),
    HARD(   "8×8",         8, 180, 1),
    DEVIL(  "DEVIL 10×10", 10, 90, 0),
}

sealed interface GameEvent {
    data class CellTapped(val row: Int, val col: Int) : GameEvent
    data object Undo    : GameEvent
    data object Hint    : GameEvent
    data object Pause   : GameEvent
    data object Resume  : GameEvent
    data object Restart : GameEvent
}

fun buildInitialCells(size: Int): List<CellState> =
    (0 until size).flatMap { row ->
        (0 until size).map { col -> CellState(row, col) }
    }