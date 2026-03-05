package com.kunpitech.knighttour.domain.model

// ===============================================================
//  BOARD — Core domain model
//
//  Immutable value object representing a Knight Tour board at a
//  single point in time. All mutation returns a new Board copy.
//
//  Package: domain/model/Board.kt
// ===============================================================

/**
 * A single cell on the board.
 *
 * @param row       0-based row index
 * @param col       0-based column index
 * @param visitOrder  0 = unvisited; 1..N = the move number that visited this cell
 */
data class Cell(
    val row        : Int,
    val col        : Int,
    val visitOrder : Int = 0,
) {
    val isVisited: Boolean get() = visitOrder > 0
}

/**
 * Immutable snapshot of the Knight Tour board.
 *
 * @param size        Board is [size × size]
 * @param cells       Flat list of all cells, row-major order.
 *                    Index = row * size + col
 * @param moveHistory Ordered list of (row, col) positions the knight visited,
 *                    oldest first. Empty = no knight placed yet.
 */
data class Board(
    val size        : Int,
    val cells       : List<Cell>,
    val moveHistory : List<Position> = emptyList(),
) {
    // ── Computed properties ──────────────────────────────────────

    val totalCells   : Int      get() = size * size
    val moveCount    : Int      get() = moveHistory.size
    val isComplete   : Boolean  get() = moveCount == totalCells
    val hasKnight    : Boolean  get() = moveHistory.isNotEmpty()

    /** Current knight position, or null if no knight placed yet. */
    val knightPosition: Position? get() = moveHistory.lastOrNull()

    // ── Cell accessors ───────────────────────────────────────────

    fun cellAt(row: Int, col: Int): Cell = cells[row * size + col]

    fun cellAt(pos: Position): Cell = cellAt(pos.row, pos.col)

    fun isInBounds(row: Int, col: Int): Boolean =
        row in 0 until size && col in 0 until size

    fun isInBounds(pos: Position): Boolean =
        isInBounds(pos.row, pos.col)

    // ── Mutation helpers (return new Board) ──────────────────────

    /**
     * Place the knight on [pos] as the first move.
     * Requires [moveHistory] to be empty.
     */
    fun placeKnight(pos: Position): Board {
        require(moveHistory.isEmpty()) { "Knight already placed" }
        require(isInBounds(pos)) { "Position out of bounds: $pos" }

        val newCells = cells.map { cell ->
            if (cell.row == pos.row && cell.col == pos.col)
                cell.copy(visitOrder = 1)
            else cell
        }
        return copy(cells = newCells, moveHistory = listOf(pos))
    }

    /**
     * Move the knight from its current position to [pos].
     * Does not validate that the move is a legal L-move — callers
     * must check via [KnightMoveEngine] first.
     */
    fun moveTo(pos: Position): Board {
        require(moveHistory.isNotEmpty()) { "No knight on board yet" }
        require(isInBounds(pos)) { "Position out of bounds: $pos" }
        require(!cellAt(pos).isVisited) { "Cell already visited: $pos" }

        val newOrder = moveCount + 1
        val newCells = cells.map { cell ->
            if (cell.row == pos.row && cell.col == pos.col)
                cell.copy(visitOrder = newOrder)
            else cell
        }
        return copy(cells = newCells, moveHistory = moveHistory + pos)
    }

    /**
     * Undo the last move. Returns the board as it was before that move.
     * If only one move in history (initial placement), returns a blank board.
     */
    fun undoLastMove(): Board {
        require(moveHistory.isNotEmpty()) { "Nothing to undo" }

        val removedPos  = moveHistory.last()
        val newHistory  = moveHistory.dropLast(1)

        val newCells = cells.map { cell ->
            if (cell.row == removedPos.row && cell.col == removedPos.col)
                cell.copy(visitOrder = 0)
            else cell
        }
        return copy(cells = newCells, moveHistory = newHistory)
    }

    companion object {
        /** Create a blank board of the given size. */
        fun blank(size: Int): Board {
            require(size in 3..12) { "Board size must be between 3 and 12" }
            val cells = (0 until size).flatMap { row ->
                (0 until size).map { col -> Cell(row, col) }
            }
            return Board(size = size, cells = cells)
        }
    }
}

/**
 * A (row, col) coordinate on the board.
 * Kept as a separate value class for clarity at call sites.
 */
data class Position(val row: Int, val col: Int) {
    override fun toString() = "($row,$col)"
}