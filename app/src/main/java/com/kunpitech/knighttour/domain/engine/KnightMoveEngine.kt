package com.kunpitech.knighttour.domain.engine

import com.kunpitech.knighttour.domain.model.Board
import com.kunpitech.knighttour.domain.model.Position
import javax.inject.Inject
import javax.inject.Singleton

// ===============================================================
//  KNIGHT MOVE ENGINE
//
//  Pure, stateless logic for all knight L-move rules.
//  No Android dependencies — fully unit-testable.
//
//  Package: domain/engine/KnightMoveEngine.kt
// ===============================================================

@Singleton
class KnightMoveEngine @Inject constructor() {

    // ── All 8 possible knight offsets ────────────────────────────

    val offsets: List<Pair<Int, Int>> = listOf(
        -2 to -1, -2 to +1,
        -1 to -2, -1 to +2,
        +1 to -2, +1 to +2,
        +2 to -1, +2 to +1,
    )

    // ── Core validation ──────────────────────────────────────────

    /**
     * Returns true if moving from [from] to [to] is a valid knight L-move.
     * Does NOT check board bounds or visited status.
     */
    fun isLMove(from: Position, to: Position): Boolean {
        val dr = kotlin.math.abs(to.row - from.row)
        val dc = kotlin.math.abs(to.col - from.col)
        return (dr == 2 && dc == 1) || (dr == 1 && dc == 2)
    }

    /**
     * Returns true if the given [to] position is a legal next move
     * from the knight's current position on [board]:
     *   - Is an L-move from the current knight position
     *   - Is within bounds
     *   - Is not already visited
     */
    fun isLegalMove(board: Board, to: Position): Boolean {
        val from = board.knightPosition ?: return board.isInBounds(to)
        return board.isInBounds(to) &&
                !board.cellAt(to).isVisited &&
                isLMove(from, to)
    }

    /**
     * Returns all legal moves available from the knight's current position.
     * Returns all unvisited in-bounds cells if no knight is placed yet
     * (first move — any cell is valid).
     */
    fun legalMovesFrom(board: Board): List<Position> {
        val from = board.knightPosition
            ?: return allUnvisited(board)   // first move: any cell

        return offsets
            .map { (dr, dc) -> Position(from.row + dr, from.col + dc) }
            .filter { pos ->
                board.isInBounds(pos) && !board.cellAt(pos).isVisited
            }
    }

    /**
     * Returns all reachable positions from an arbitrary [from] position
     * on [board], regardless of current knight position.
     * Useful for hint engines and solvers.
     */
    fun reachableFrom(board: Board, from: Position): List<Position> =
        offsets
            .map { (dr, dc) -> Position(from.row + dr, from.col + dc) }
            .filter { pos ->
                board.isInBounds(pos) && !board.cellAt(pos).isVisited
            }

    /**
     * Returns true if the board is in a dead-end state:
     * knight is placed but has no legal moves remaining,
     * and the tour is not yet complete.
     */
    fun isDeadEnd(board: Board): Boolean {
        if (!board.hasKnight) return false
        if (board.isComplete) return false
        return legalMovesFrom(board).isEmpty()
    }

    /**
     * Returns the degree (number of onward moves) from [pos] on [board].
     * Core of Warnsdorff's heuristic.
     */
    fun degree(board: Board, pos: Position): Int =
        reachableFrom(board, pos).size

    // ── Private helpers ──────────────────────────────────────────

    private fun allUnvisited(board: Board): List<Position> =
        board.cells
            .filter { !it.isVisited }
            .map    { Position(it.row, it.col) }
}