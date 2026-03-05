package com.kunpitech.knighttour.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

// ===============================================================
//  SCORE ENTITY
//
//  Stores personal best per board size + all finished session
//  scores for local leaderboard history.
//
//  Package: data/local/entity/ScoreEntity.kt
// ===============================================================

@Entity(tableName = "scores")
data class ScoreEntity(
    @PrimaryKey(autoGenerate = true)
    val id            : Int    = 0,
    val sessionId     : String,        // FK to game_sessions.id
    val playerName    : String,
    val score         : Int,
    val boardSize     : Int,
    val difficulty    : String,        // Difficulty.name
    val mode          : String,        // GameMode.name
    val elapsedSeconds: Int,
    val hintsUsed     : Int,
    val finishedAt    : Long,
    val isPersonalBest: Boolean = false,
)