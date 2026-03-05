package com.kunpitech.knighttour.domain.engine

import com.kunpitech.knighttour.domain.model.Board
import com.kunpitech.knighttour.domain.model.Position
import javax.inject.Inject
import javax.inject.Singleton

// ===============================================================
//  WARNSDORFF SOLVER
//
//  Two capabilities:
//
//  1. hint(board)      → single best next move (O(1) per call)
//                        Used live during gameplay when the player
//                        taps the hint button.
//
//  2. solve(board)     → complete solution path from current state
//                        (or null if unsolvable).
//                        Uses Warnsdorff's heuristic + tie-breaking
//                        + limited backtracking for robustness.
//                        Used by the auto-solver / daily puzzle
//                        validator.
//
//  No Android dependencies — fully unit-testable.
//
//  Package: domain/engine/WarnsdorffSolver.kt
// ===============================================================

@Singleton
class WarnsdorffSolver @Inject constructor(
    private val engine: KnightMoveEngine,
) {

    // ── Public API ───────────────────────────────────────────────

    /**
     * Returns the single best next move from the current board state
     * using Warnsdorff's heuristic (prefer the cell with the fewest
     * onward moves).
     *
     * Returns null if:
     *   - No knight is placed yet (first move — no hint applies)
     *   - No legal moves remain (dead end)
     */
    fun hint(board: Board): Position? {
        if (!board.hasKnight) return null
        val candidates = engine.legalMovesFrom(board)
        if (candidates.isEmpty()) return null
        return warnsdorffBest(board, candidates)
    }

    /**
     * Attempts to find a complete knight tour solution from the
     * current [board] state using Warnsdorff's heuristic with
     * tie-breaking and limited backtracking.
     *
     * @param maxBacktracks  Maximum number of backtracks before
     *                       giving up. Default 512 keeps it fast
     *                       for boards up to 10×10.
     *
     * @return  The ordered list of [Position]s completing the tour
     *          (starting from the first unplaced move), or null if
     *          no solution was found within [maxBacktracks].
     */
    fun solve(
        board           : Board,
        maxBacktracks   : Int = 512,
    ): List<Position>? {
        // Need a starting position to solve from
        val startPos = board.knightPosition ?: return null

        var backtracks = 0
        val path       = ArrayDeque<Position>()

        fun dfs(current: Board): Boolean {
            if (current.isComplete) return true

            val candidates = engine.legalMovesFrom(current)
            if (candidates.isEmpty()) return false

            // Sort by Warnsdorff degree, with accessibility tie-breaking
            val sorted = candidates.sortedWith(
                compareBy(
                    { engine.degree(current, it) },
                    { accessibilityScore(current, it) },
                )
            )

            for (next in sorted) {
                path.addLast(next)
                val newBoard = current.moveTo(next)

                if (dfs(newBoard)) return true

                // Backtrack
                path.removeLast()
                backtracks++
                if (backtracks > maxBacktracks) return false
            }
            return false
        }

        return if (dfs(board)) path.toList() else null
    }

    /**
     * Returns true if a complete knight tour is possible from the
     * current board state. Cheaper than [solve] since it stops as
     * soon as a solution is confirmed.
     */
    fun isSolvable(board: Board, maxBacktracks: Int = 512): Boolean =
        solve(board, maxBacktracks) != null

    // ── Private helpers ──────────────────────────────────────────

    /**
     * Warnsdorff's rule: among candidates, return the one with the
     * fewest onward moves (lowest degree on the board *after* moving
     * there).
     *
     * Tie-breaking: among equal-degree cells, prefer the one whose
     * reachable neighbours themselves have the lowest average degree
     * (lookahead depth 1). This improves completion rates on larger
     * boards significantly.
     */
    private fun warnsdorffBest(board: Board, candidates: List<Position>): Position {
        return candidates.minWithOrNull(
            compareBy(
                { engine.degree(board, it) },
                { accessibilityScore(board, it) },
            )
        )!!
    }

    /**
     * Accessibility score for tie-breaking: sum of degrees of all
     * cells reachable from [pos] on [board].  Lower = more isolated =
     * should be visited sooner (Warnsdorff logic applied one level
     * deeper).
     */
    private fun accessibilityScore(board: Board, pos: Position): Int {
        val simBoard = board.moveTo(pos)
        return engine.reachableFrom(simBoard, pos).sumOf { next ->
            engine.degree(simBoard, next)
        }
    }
}