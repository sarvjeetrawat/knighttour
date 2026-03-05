package com.kunpitech.knighttour.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.kunpitech.knighttour.domain.model.Board
import com.kunpitech.knighttour.domain.model.Cell
import com.kunpitech.knighttour.domain.model.Difficulty
import com.kunpitech.knighttour.domain.model.GameMode
import com.kunpitech.knighttour.domain.model.GameSession
import com.kunpitech.knighttour.domain.model.Position
import com.kunpitech.knighttour.domain.model.SessionStatus
import org.json.JSONArray
import org.json.JSONObject

// ===============================================================
//  GAME SESSION ENTITY
//
//  Flat Room row — Board is serialized to JSON strings so we
//  avoid nested entity complexity while keeping full fidelity.
//
//  Package: data/local/entity/GameSessionEntity.kt
// ===============================================================

@Entity(tableName = "game_sessions")
@TypeConverters(GameSessionConverters::class)
data class GameSessionEntity(
    @PrimaryKey
    val id             : String,
    val difficulty     : String,   // Difficulty.name
    val mode           : String,   // GameMode.name
    val status         : String,   // SessionStatus.name
    val boardSize      : Int,
    val boardCellsJson : String,   // JSON array of Cell visit orders (flat, row-major)
    val moveHistoryJson: String,   // JSON array of {row,col} objects
    val elapsedSeconds : Int,
    val hintsUsed      : Int,
    val score          : Int,
    val startedAt      : Long,
    val finishedAt     : Long?,
    val roomCode       : String,
    val opponentId     : String,
)

// ===============================================================
//  MAPPERS
// ===============================================================

fun GameSession.toEntity(): GameSessionEntity {
    val cells = board.cells
    val cellsArray = JSONArray().apply {
        cells.forEach { put(it.visitOrder) }
    }

    val historyArray = JSONArray().apply {
        board.moveHistory.forEach { pos ->
            put(JSONObject().apply {
                put("r", pos.row)
                put("c", pos.col)
            })
        }
    }

    return GameSessionEntity(
        id              = id,
        difficulty      = difficulty.name,
        mode            = mode.name,
        status          = status.name,
        boardSize       = board.size,
        boardCellsJson  = cellsArray.toString(),
        moveHistoryJson = historyArray.toString(),
        elapsedSeconds  = elapsedSeconds,
        hintsUsed       = hintsUsed,
        score           = score,
        startedAt       = startedAt,
        finishedAt      = finishedAt,
        roomCode        = roomCode,
        opponentId      = opponentId,
    )
}

fun GameSessionEntity.toDomain(): GameSession {
    val size = boardSize

    // Rebuild cells from flat visit-order array
    val cellsJson = JSONArray(boardCellsJson)
    val cells = (0 until size).flatMap { row ->
        (0 until size).map { col ->
            val visitOrder = cellsJson.getInt(row * size + col)
            Cell(row = row, col = col, visitOrder = visitOrder)
        }
    }

    // Rebuild move history
    val historyJson = JSONArray(moveHistoryJson)
    val moveHistory = (0 until historyJson.length()).map { i ->
        val obj = historyJson.getJSONObject(i)
        Position(row = obj.getInt("r"), col = obj.getInt("c"))
    }

    val board = Board(
        size        = size,
        cells       = cells,
        moveHistory = moveHistory,
    )

    return GameSession(
        id             = id,
        difficulty     = Difficulty.valueOf(difficulty),
        mode           = GameMode.valueOf(mode),
        board          = board,
        status         = SessionStatus.valueOf(status),
        elapsedSeconds = elapsedSeconds,
        hintsUsed      = hintsUsed,
        score          = score,
        startedAt      = startedAt,
        finishedAt     = finishedAt,
        roomCode       = roomCode,
        opponentId     = opponentId,
    )
}

// ===============================================================
//  TYPE CONVERTERS  (for nullable Long — Room needs this)
// ===============================================================

class GameSessionConverters {
    @TypeConverter fun longToNullable(value: Long): Long? = value
    @TypeConverter fun nullableToLong(value: Long?): Long = value ?: 0L
}