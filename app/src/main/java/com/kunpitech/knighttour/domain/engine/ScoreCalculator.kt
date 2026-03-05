package com.kunpitech.knighttour.domain.engine

import com.kunpitech.knighttour.domain.model.Difficulty
import com.kunpitech.knighttour.domain.model.GameSession
import javax.inject.Inject
import javax.inject.Singleton

// ===============================================================
//  SCORE CALCULATOR
//
//  Single source of truth for all score arithmetic.
//  Called from UseCases — never from ViewModel directly.
//
//  Formula:
//    base        = moveCount × 100
//    timeBonus   = timeRemaining × 8
//    sizeBonus   = boardSize² × 4
//    hintPenalty = hintsUsed × 300
//    total       = max(0, base + timeBonus + sizeBonus - hintPenalty)
//
//  Package: domain/engine/ScoreCalculator.kt
// ===============================================================

@Singleton
class ScoreCalculator @Inject constructor() {

    data class ScoreBreakdown(
        val base        : Int,
        val timeBonus   : Int,
        val sizeBonus   : Int,
        val hintPenalty : Int,
        val total       : Int,
    )

    /**
     * Compute a full score breakdown for a game session snapshot.
     */
    fun calculate(session: GameSession): ScoreBreakdown =
        calculate(
            moveCount      = session.moveCount,
            timeRemaining  = session.timeRemaining,
            boardSize      = session.boardSize,
            hintsUsed      = session.hintsUsed,
        )

    /**
     * Compute a score breakdown from raw values.
     * Useful during live gameplay before a session object exists.
     */
    fun calculate(
        moveCount     : Int,
        timeRemaining : Int,
        boardSize     : Int,
        hintsUsed     : Int,
    ): ScoreBreakdown {
        val base        = moveCount * 100
        val timeBonus   = timeRemaining * 8
        val sizeBonus   = boardSize * boardSize * 4
        val hintPenalty = hintsUsed * 300
        val total       = (base + timeBonus + sizeBonus - hintPenalty).coerceAtLeast(0)

        return ScoreBreakdown(
            base        = base,
            timeBonus   = timeBonus,
            sizeBonus   = sizeBonus,
            hintPenalty = hintPenalty,
            total       = total,
        )
    }

    // ── Rank resolution ──────────────────────────────────────────

    data class RankInfo(
        val label        : String,
        val progressFrac : Float,   // 0.0–1.0 within current rank band
    )

    private val rankBands: List<Pair<Int, String>> = listOf(
        0     to "NOVICE",
        1500  to "SQUIRE",
        3000  to "KNIGHT",
        5000  to "MASTER",
        8000  to "GRANDMASTER",
    )

    /**
     * Returns the rank label and progress fraction within that rank band
     * for a given total [score].
     */
    fun rankInfo(score: Int): RankInfo {
        val idx = rankBands.indexOfLast { (threshold, _) -> score >= threshold }
            .coerceAtLeast(0)

        val (lo, label) = rankBands[idx]
        val hi          = rankBands.getOrNull(idx + 1)?.first

        val progress = if (hi == null) {
            1f  // already at max rank
        } else {
            ((score - lo).toFloat() / (hi - lo).toFloat()).coerceIn(0f, 1f)
        }

        return RankInfo(label = label, progressFrac = progress)
    }
}