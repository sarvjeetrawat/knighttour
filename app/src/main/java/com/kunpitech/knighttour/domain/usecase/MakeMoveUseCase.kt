package com.kunpitech.knighttour.domain.usecase

import com.kunpitech.knighttour.domain.engine.KnightMoveEngine
import com.kunpitech.knighttour.domain.engine.ScoreCalculator
import com.kunpitech.knighttour.domain.model.GameSession
import com.kunpitech.knighttour.domain.model.Position
import com.kunpitech.knighttour.domain.model.SessionStatus
import javax.inject.Inject

// ===============================================================
//  MAKE MOVE USE CASE
//
//  Validates a tap at [position] against the current session and
//  returns a [MoveResult] describing what happened.
//
//  Package: domain/usecase/MakeMoveUseCase.kt
// ===============================================================

class MakeMoveUseCase @Inject constructor(
    private val engine    : KnightMoveEngine,
    private val calculator: ScoreCalculator,
) {

    sealed interface MoveResult {
        /** Move was accepted — [session] contains the updated state. */
        data class Accepted(val session: GameSession) : MoveResult

        /** All cells visited — game is won. */
        data class Victory(val session: GameSession) : MoveResult

        /** Move was accepted but no moves remain — game is lost. */
        data class DeadEnd(val session: GameSession) : MoveResult

        /** Not a legal L-move or cell already visited. */
        data class Invalid(val reason: InvalidReason) : MoveResult

        /** Game is already finished — tap ignored. */
        data object GameOver : MoveResult
    }

    enum class InvalidReason {
        NOT_L_MOVE,
        ALREADY_VISITED,
        OUT_OF_BOUNDS,
    }

    operator fun invoke(session: GameSession, position: Position): MoveResult {
        // Reject if game already ended
        if (session.isFinished) return MoveResult.GameOver

        val board = session.board

        // Out of bounds
        if (!board.isInBounds(position)) {
            return MoveResult.Invalid(InvalidReason.OUT_OF_BOUNDS)
        }

        val cell = board.cellAt(position)

        // Already visited
        if (cell.isVisited) {
            return MoveResult.Invalid(InvalidReason.ALREADY_VISITED)
        }

        // First move — any unvisited cell is valid
        val newBoard = if (!board.hasKnight) {
            board.placeKnight(position)
        } else {
            // Must be a legal L-move
            if (!engine.isLegalMove(board, position)) {
                return MoveResult.Invalid(InvalidReason.NOT_L_MOVE)
            }
            board.moveTo(position)
        }

        // Calculate live score
        val breakdown = calculator.calculate(
            moveCount     = newBoard.moveCount,
            timeRemaining = session.timeRemaining,
            boardSize     = newBoard.size,
            hintsUsed     = session.hintsUsed,
        )

        val updatedSession = session.copy(
            board = newBoard,
            score = breakdown.total,
        )

        // Check victory
        if (newBoard.isComplete) {
            val won = updatedSession.copy(
                status     = SessionStatus.COMPLETED,
                finishedAt = System.currentTimeMillis(),
            )
            return MoveResult.Victory(won)
        }

        // Check dead end
        if (engine.isDeadEnd(newBoard)) {
            val stuck = updatedSession.copy(
                status     = SessionStatus.FAILED_STUCK,
                finishedAt = System.currentTimeMillis(),
            )
            return MoveResult.DeadEnd(stuck)
        }

        return MoveResult.Accepted(updatedSession)
    }
}