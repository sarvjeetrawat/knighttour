package com.kunpitech.knighttour.domain.usecase

import com.kunpitech.knighttour.domain.engine.ScoreCalculator
import com.kunpitech.knighttour.domain.model.GameSession
import com.kunpitech.knighttour.domain.model.SessionStatus
import javax.inject.Inject

// ===============================================================
//  TICK TIMER USE CASE
//
//  Called every second by the ViewModel's timer coroutine.
//  Advances the elapsed counter and returns a TickResult that
//  tells the ViewModel what changed — no coroutine logic here.
//
//  Package: domain/usecase/TickTimerUseCase.kt
// ===============================================================

class TickTimerUseCase @Inject constructor(
    private val calculator: ScoreCalculator,
) {

    sealed interface TickResult {
        /** Timer advanced normally. */
        data class Tick(
            val session    : GameSession,
            val isTimerPanic: Boolean,   // true when ≤ 15 seconds remain
        ) : TickResult

        /** Time ran out — game over. */
        data class TimeUp(val session: GameSession) : TickResult

        /** Game already finished — caller should stop the timer. */
        data object GameOver : TickResult
    }

    operator fun invoke(session: GameSession): TickResult {
        if (session.isFinished) return TickResult.GameOver

        val newElapsed  = session.elapsedSeconds + 1
        val remaining   = session.difficulty.timeLimitSeconds - newElapsed

        if (remaining <= 0) {
            val timedOut = session.copy(
                elapsedSeconds = newElapsed,
                score          = 0,     // no score for timeout
                status         = SessionStatus.FAILED_TIME,
                finishedAt     = System.currentTimeMillis(),
            )
            return TickResult.TimeUp(timedOut)
        }

        val breakdown = calculator.calculate(
            moveCount     = session.moveCount,
            timeRemaining = remaining,
            boardSize     = session.boardSize,
            hintsUsed     = session.hintsUsed,
        )

        val updated = session.copy(
            elapsedSeconds = newElapsed,
            score          = breakdown.total,
        )

        return TickResult.Tick(
            session      = updated,
            isTimerPanic = remaining <= 15,
        )
    }
}