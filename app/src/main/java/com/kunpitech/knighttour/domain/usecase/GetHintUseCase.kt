package com.kunpitech.knighttour.domain.usecase

import com.kunpitech.knighttour.domain.engine.WarnsdorffSolver
import com.kunpitech.knighttour.domain.model.GameSession
import com.kunpitech.knighttour.domain.model.Position
import javax.inject.Inject

// ===============================================================
//  GET HINT USE CASE
//
//  Returns the Warnsdorff-optimal next move and deducts a hint.
//  Package: domain/usecase/GetHintUseCase.kt
// ===============================================================

class GetHintUseCase @Inject constructor(
    private val solver    : WarnsdorffSolver,
    private val calculator: com.kunpitech.knighttour.domain.engine.ScoreCalculator,
) {

    sealed interface HintResult {
        /** Hint granted — [position] is the suggested move, [session] has hintsUsed++. */
        data class Success(val position: Position, val session: GameSession) : HintResult

        /** Player has no hints remaining. */
        data object NoHintsLeft : HintResult

        /** No knight placed yet — hint not applicable. */
        data object NoKnight : HintResult

        /** No moves available from current position. */
        data object NoMovesAvailable : HintResult

        /** Game already finished. */
        data object GameOver : HintResult
    }

    operator fun invoke(session: GameSession): HintResult {
        if (session.isFinished)           return HintResult.GameOver
        if (!session.board.hasKnight)     return HintResult.NoKnight

        val hintsAvailable = session.difficulty.startingHints - session.hintsUsed
        if (hintsAvailable <= 0)          return HintResult.NoHintsLeft

        val best = solver.hint(session.board) ?: return HintResult.NoMovesAvailable

        val newHintsUsed = session.hintsUsed + 1
        val breakdown = calculator.calculate(
            moveCount     = session.moveCount,
            timeRemaining = session.timeRemaining,
            boardSize     = session.boardSize,
            hintsUsed     = newHintsUsed,
        )

        val updatedSession = session.copy(
            hintsUsed = newHintsUsed,
            score     = breakdown.total,
        )

        return HintResult.Success(position = best, session = updatedSession)
    }
}