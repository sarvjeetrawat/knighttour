package com.kunpitech.knighttour.domain.usecase

import com.kunpitech.knighttour.domain.engine.ScoreCalculator
import com.kunpitech.knighttour.domain.model.GameSession
import com.kunpitech.knighttour.domain.model.SessionStatus
import javax.inject.Inject

// ===============================================================
//  UNDO MOVE USE CASE
//
//  Removes the last move from the board and recalculates score.
//  Package: domain/usecase/UndoMoveUseCase.kt
// ===============================================================

class UndoMoveUseCase @Inject constructor(
    private val calculator: ScoreCalculator,
) {

    sealed interface UndoResult {
        /** Undo applied — [session] is the updated state. */
        data class Success(val session: GameSession) : UndoResult

        /** Nothing to undo — no moves have been made. */
        data object NothingToUndo : UndoResult

        /** Game already finished — undo not allowed. */
        data object GameOver : UndoResult
    }

    operator fun invoke(session: GameSession): UndoResult {
        if (session.isFinished) return UndoResult.GameOver
        if (!session.board.hasKnight) return UndoResult.NothingToUndo

        val newBoard = session.board.undoLastMove()

        val breakdown = calculator.calculate(
            moveCount     = newBoard.moveCount,
            timeRemaining = session.timeRemaining,
            boardSize     = newBoard.size,
            hintsUsed     = session.hintsUsed,
        )

        val updatedSession = session.copy(
            board  = newBoard,
            score  = breakdown.total,
            status = SessionStatus.IN_PROGRESS,   // restore if was stuck
        )

        return UndoResult.Success(updatedSession)
    }
}