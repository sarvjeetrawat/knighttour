package com.kunpitech.knighttour.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kunpitech.knighttour.data.local.entity.ScoreEntity
import kotlinx.coroutines.flow.Flow

// ===============================================================
//  SCORE DAO
//
//  Package: data/local/dao/ScoreDao.kt
// ===============================================================

@Dao
interface ScoreDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(score: ScoreEntity)

    @Query("DELETE FROM scores")
    suspend fun deleteAll()

    // ── Personal bests ────────────────────────────────────────────

    /**
     * Best score per board size — one row per size, ordered by boardSize.
     * Used by the Personal Bests tab in Leaderboard.
     */
    @Query("""
        SELECT * FROM scores
        WHERE isPersonalBest = 1
        ORDER BY boardSize ASC
    """)
    fun getPersonalBests(): Flow<List<ScoreEntity>>

    /**
     * Best score for a specific board size.
     */
    @Query("""
        SELECT * FROM scores
        WHERE boardSize = :boardSize
        ORDER BY score DESC
        LIMIT 1
    """)
    suspend fun getBestForBoard(boardSize: Int): ScoreEntity?

    // ── Recent scores ─────────────────────────────────────────────

    @Query("""
        SELECT * FROM scores
        ORDER BY finishedAt DESC
        LIMIT :limit
    """)
    fun getRecentScores(limit: Int = 20): Flow<List<ScoreEntity>>

    // ── Top scores (local leaderboard) ───────────────────────────

    @Query("""
        SELECT * FROM scores
        ORDER BY score DESC
        LIMIT :limit
    """)
    fun getTopScores(limit: Int = 50): Flow<List<ScoreEntity>>

    // ── Stats ─────────────────────────────────────────────────────

    @Query("SELECT MAX(score) FROM scores")
    suspend fun getGlobalBest(): Int?

    @Query("SELECT COUNT(*) FROM scores")
    suspend fun getTotalScores(): Int
}