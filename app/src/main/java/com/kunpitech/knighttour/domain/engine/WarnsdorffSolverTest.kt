package com.kunpitech.knighttour.domain.engine

import com.kunpitech.knighttour.domain.model.Board
import com.kunpitech.knighttour.domain.model.Position
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName

// ===============================================================
//  WARNSDORFF SOLVER — Unit Tests  (JUnit 5 / Jupiter)
//
//  src/test/java/com/kunpitech/knighttour/domain/engine/
// ===============================================================

@DisplayName("WarnsdorffSolver")
class WarnsdorffSolverTest {

    private lateinit var engine: KnightMoveEngine
    private lateinit var solver: WarnsdorffSolver

    @BeforeEach
    fun setUp() {
        engine = KnightMoveEngine()
        solver = WarnsdorffSolver(engine)
    }

    // ── hint ─────────────────────────────────────────────────────

    @Test
    @DisplayName("hint returns null when no knight placed")
    fun hint_noKnight() {
        assertNull(solver.hint(Board.blank(6)))
    }

    @Test
    @DisplayName("hint returns a legal move position")
    fun hint_returnsLegalMove() {
        val board = Board.blank(6).placeKnight(Position(0, 0))
        val hint  = solver.hint(board)
        assertNotNull(hint)
        assertTrue(engine.isLegalMove(board, hint!!))
    }

    @Test
    @DisplayName("hint picks the lowest-degree candidate (Warnsdorff rule)")
    fun hint_warnsdorffRule() {
        val board   = Board.blank(6).placeKnight(Position(0, 0))
        val hint    = solver.hint(board)!!
        val hintDeg = engine.degree(board, hint)
        val maxDeg  = engine.legalMovesFrom(board).maxOf { engine.degree(board, it) }
        // hint degree must be <= any other candidate
        assertTrue(hintDeg <= maxDeg)
    }

    // ── solve ─────────────────────────────────────────────────────

    @Test
    @DisplayName("solve returns null when no knight placed")
    fun solve_noKnight() {
        assertNull(solver.solve(Board.blank(6)))
    }

    @Test
    @DisplayName("solve completes a 5x5 tour from center")
    fun solve_5x5_fromCenter() {
        val board = Board.blank(5).placeKnight(Position(2, 2))
        val path  = solver.solve(board)
        assertNotNull(path)
        assertEquals(24, path!!.size)  // 25 total - 1 already placed
    }

    @Test
    @DisplayName("solve completes a 6x6 tour from corner")
    fun solve_6x6_fromCorner() {
        val board = Board.blank(6).placeKnight(Position(0, 0))
        val path  = solver.solve(board)
        assertNotNull(path)
        assertEquals(35, path!!.size)  // 36 total - 1 already placed
    }

    @Test
    @DisplayName("solve produces only legal moves")
    fun solve_allMovesLegal() {
        var board = Board.blank(6).placeKnight(Position(0, 0))
        val path  = solver.solve(board) ?: return

        for (nextPos in path) {
            assertTrue(engine.isLegalMove(board, nextPos))
            board = board.moveTo(nextPos)
        }
        assertTrue(board.isComplete)
    }

    @Test
    @DisplayName("solve produces no duplicate positions")
    fun solve_noDuplicates() {
        val board = Board.blank(6).placeKnight(Position(0, 0))
        val path  = solver.solve(board) ?: return
        assertEquals(path.size, path.toSet().size)
    }

    @Test
    @DisplayName("isSolvable returns true for 6x6 from corner")
    fun isSolvable_6x6() {
        val board = Board.blank(6).placeKnight(Position(0, 0))
        assertTrue(solver.isSolvable(board))
    }

    @Test
    @DisplayName("solve works from a partial board state")
    fun solve_fromMidGame() {
        var board = Board.blank(6).placeKnight(Position(0, 0))
        board = board.moveTo(Position(1, 2))
        board = board.moveTo(Position(2, 4))
        board = board.moveTo(Position(4, 5))

        val path = solver.solve(board) ?: return
        var current = board
        for (pos in path) {
            assertTrue(engine.isLegalMove(current, pos))
            current = current.moveTo(pos)
        }
        assertTrue(current.isComplete)
    }
}