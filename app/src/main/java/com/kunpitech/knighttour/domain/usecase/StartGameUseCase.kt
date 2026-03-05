package com.kunpitech.knighttour.domain.usecase

import com.kunpitech.knighttour.domain.model.Board
import com.kunpitech.knighttour.domain.model.Difficulty
import com.kunpitech.knighttour.domain.model.GameMode
import com.kunpitech.knighttour.domain.model.GameSession
import com.kunpitech.knighttour.domain.model.SessionStatus
import javax.inject.Inject

// ===============================================================
//  START GAME USE CASE
//
//  Creates a fresh GameSession for the given difficulty and mode.
//  Package: domain/usecase/StartGameUseCase.kt
// ===============================================================

class StartGameUseCase @Inject constructor() {

    operator fun invoke(
        difficulty : Difficulty,
        mode       : GameMode,
        roomCode   : String = "",
        opponentId : String = "",
    ): GameSession {
        return GameSession(
            difficulty     = difficulty,
            mode           = mode,
            board          = Board.blank(difficulty.boardSize),
            status         = SessionStatus.IN_PROGRESS,
            elapsedSeconds = 0,
            hintsUsed      = 0,
            score          = 0,
            roomCode       = roomCode,
            opponentId     = opponentId,
            startedAt      = System.currentTimeMillis(),
        )
    }
}