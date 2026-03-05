package com.kunpitech.knighttour.domain.engine

import com.kunpitech.knighttour.domain.model.Board
import com.kunpitech.knighttour.domain.model.Position
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName

// ===============================================================
//  KNIGHT MOVE ENGINE — Unit Tests  (JUnit 5 / Jupiter)
//
//  src/test/java/com/kunpitech/knighttour/domain/engine/
// ===============================================================

@org.junit.jupiter.api.DisplayName("KnightMoveEngine")
class KnightMoveEngineTest {

    private lateinit var engine: KnightMoveEngine

    @BeforeEach
    fun setUp() {
        engine = KnightMoveEngine()
    }

    // ── isLMove ──────────────────────────────────────────────────

    @Test
    @DisplayName("isLMove returns true for all 8 valid knight offsets")
    fun isLMove_validOffsets() {
        val from = Position(4, 4)
        val validTargets = listOf(
            Position(2, 3), Position(2, 5),
            Position(3, 2), Position(3, 6),
            Position(5, 2), Position(5, 6),
            Position(6, 3), Position(6, 5),
        )
        validTargets.forEach { to ->
            assertTrue(engine.isLMove(from, to), "Expected L-move from $from to $to")
        }
    }

    @Test
    @DisplayName("isLMove returns false for non-L moves")
    fun isLMove_invalidOffsets() {
        val from = Position(4, 4)
        listOf(
            Position(4, 5),
            Position(4, 6),
            Position(3, 3),
            Position(4, 4),
            Position(2, 2),
            Position(6, 6),
        ).forEach { to ->
            assertFalse(engine.isLMove(from, to), "Expected NOT L-move from $from to $to")
        }
    }

    // ── isLegalMove ───────────────────────────────────────────────

    @Test
    @DisplayName("isLegalMove true for valid unvisited target")
    fun isLegalMove_valid() {
        val board = Board.blank(6).placeKnight(Position(0, 0))
        assertTrue(engine.isLegalMove(board, Position(1, 2)))
        assertTrue(engine.isLegalMove(board, Position(2, 1)))
    }

    @Test
    @DisplayName("isLegalMove false for already visited cell")
    fun isLegalMove_alreadyVisited() {
        val board = Board.blank(6)
            .placeKnight(Position(0, 0))
            .moveTo(Position(1, 2))
        assertFalse(engine.isLegalMove(board, Position(0, 0)))
    }

    @Test
    @DisplayName("isLegalMove false for out of bounds")
    fun isLegalMove_outOfBounds() {
        val board = Board.blank(6).placeKnight(Position(0, 0))
        assertFalse(engine.isLegalMove(board, Position(-1, -2)))
        assertFalse(engine.isLegalMove(board, Position(8, 8)))
    }

    @Test
    @DisplayName("isLegalMove false for non L-move")
    fun isLegalMove_notLMove() {
        val board = Board.blank(6).placeKnight(Position(3, 3))
        assertFalse(engine.isLegalMove(board, Position(3, 4)))
        assertFalse(engine.isLegalMove(board, Position(4, 4)))
    }

    // ── legalMovesFrom ────────────────────────────────────────────

    @Test
    @DisplayName("legalMovesFrom returns all cells before first move")
    fun legalMovesFrom_beforeFirstMove() {
        val board = Board.blank(5)
        assertEquals(25, engine.legalMovesFrom(board).size)
    }

    @Test
    @DisplayName("legalMovesFrom returns 2 moves from corner on 6x6")
    fun legalMovesFrom_corner() {
        val board = Board.blank(6).placeKnight(Position(0, 0))
        val moves = engine.legalMovesFrom(board)
        assertEquals(2, moves.size)
        assertTrue(moves.contains(Position(1, 2)))
        assertTrue(moves.contains(Position(2, 1)))
    }

    @Test
    @DisplayName("legalMovesFrom returns 8 moves from center of 8x8")
    fun legalMovesFrom_center() {
        val board = Board.blank(8).placeKnight(Position(4, 4))
        assertEquals(8, engine.legalMovesFrom(board).size)
    }

    @Test
    @DisplayName("legalMovesFrom excludes visited cells")
    fun legalMovesFrom_excludesVisited() {
        val board = Board.blank(6)
            .placeKnight(Position(0, 0))
            .moveTo(Position(1, 2))
        assertFalse(engine.legalMovesFrom(board).contains(Position(0, 0)))
    }

    // ── degree ────────────────────────────────────────────────────

    @Test
    @DisplayName("degree returns 2 for corner of 6x6")
    fun degree_corner() {
        assertEquals(2, engine.degree(Board.blank(6), Position(0, 0)))
    }

    @Test
    @DisplayName("degree returns 8 for center of empty 8x8")
    fun degree_center() {
        assertEquals(8, engine.degree(Board.blank(8), Position(4, 4)))
    }

    @Test
    @DisplayName("degree decreases as cells are visited")
    fun degree_decreasesWithVisits() {
        val board0 = Board.blank(8)
        val deg0   = engine.degree(board0, Position(4, 4))
        val board1 = board0.placeKnight(Position(2, 3))
        val deg1   = engine.degree(board1, Position(4, 4))
        assertTrue(deg1 < deg0)
    }

    // ── isDeadEnd ─────────────────────────────────────────────────

    @Test
    @DisplayName("isDeadEnd false for fresh board")
    fun isDeadEnd_freshBoard() {
        assertFalse(engine.isDeadEnd(Board.blank(6)))
    }

    @Test
    @DisplayName("isDeadEnd false when moves still available")
    fun isDeadEnd_movesAvailable() {
        val board = Board.blank(6).placeKnight(Position(0, 0))
        assertFalse(engine.isDeadEnd(board))
    }
}