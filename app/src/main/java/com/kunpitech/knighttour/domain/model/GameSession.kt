package com.kunpitech.knighttour.domain.model

// ===============================================================
//  GAME SESSION — Domain model
//
//  Represents a complete record of one game — in progress or
//  finished. This is what gets persisted to Room DB and used
//  for leaderboard + resume-game features.
//
//  Package: domain/model/GameSession.kt
// ===============================================================

import java.util.UUID

/**
 * A complete game session.
 *
 * @param id            Unique identifier (UUID string)
 * @param difficulty    Board difficulty setting
 * @param mode          Game mode (offline / online / devil)
 * @param board         Current board state snapshot
 * @param status        Current phase of the game
 * @param elapsedSeconds Seconds elapsed since game started
 * @param hintsUsed     Number of hints the player consumed
 * @param score         Calculated score at this point in time
 * @param startedAt     Unix epoch milliseconds when game started
 * @param finishedAt    Unix epoch milliseconds when game ended (null if in progress)
 * @param roomCode      Online room code (empty string for offline)
 * @param opponentId    Firebase UID of online opponent (empty for offline)
 */
data class GameSession(
    val id             : String         = UUID.randomUUID().toString(),
    val difficulty     : Difficulty,
    val mode           : GameMode,
    val board          : Board,
    val status         : SessionStatus  = SessionStatus.IN_PROGRESS,
    val elapsedSeconds : Int            = 0,
    val hintsUsed      : Int            = 0,
    val score          : Int            = 0,
    val startedAt      : Long           = System.currentTimeMillis(),
    val finishedAt     : Long?          = null,
    val roomCode       : String         = "",
    val opponentId     : String         = "",
) {
    // ── Computed convenience properties ──────────────────────────

    val isFinished   : Boolean get() = status != SessionStatus.IN_PROGRESS
    val isVictory    : Boolean get() = status == SessionStatus.COMPLETED
    val moveCount    : Int     get() = board.moveCount
    val totalCells   : Int     get() = board.totalCells
    val boardSize    : Int     get() = board.size

    val completionRate: Float
        get() = if (totalCells == 0) 0f
        else moveCount.toFloat() / totalCells.toFloat()

    val timeRemaining: Int
        get() = (difficulty.timeLimitSeconds - elapsedSeconds).coerceAtLeast(0)
}

// ── Supporting enums ─────────────────────────────────────────────

enum class Difficulty(
    val label            : String,
    val boardSize        : Int,
    val timeLimitSeconds : Int,
    val startingHints    : Int,
) {
    EASY(   "5×5  Easy",        5,  300, 3),
    MEDIUM( "6×6  Medium",      6,  240, 1),
    HARD(   "8×8  Hard",        8,  180, 1),
    DEVIL(  "Devil  10×10",     10,  90, 0),
    ;

    val uiLabel: String get() = when (this) {
        EASY   -> "5×5"
        MEDIUM -> "6×6"
        HARD   -> "8×8"
        DEVIL  -> "DEVIL 10×10"
    }
}

enum class GameMode {
    OFFLINE,
    ONLINE,
    DEVIL,
}

enum class SessionStatus {
    IN_PROGRESS,
    COMPLETED,    // all cells visited
    FAILED_TIME,  // timer ran out
    FAILED_STUCK, // no moves left, tour incomplete
    ABANDONED,    // player quit manually
}

enum class DefeatReason {
    NONE,
    TIME_UP,
    NO_MOVES,
    ;

    companion object {
        fun fromStatus(status: SessionStatus): DefeatReason = when (status) {
            SessionStatus.FAILED_TIME  -> TIME_UP
            SessionStatus.FAILED_STUCK -> NO_MOVES
            else                       -> NONE
        }
    }
}