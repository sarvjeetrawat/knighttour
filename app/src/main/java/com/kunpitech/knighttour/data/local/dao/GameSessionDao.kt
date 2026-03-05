package com.kunpitech.knighttour.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.kunpitech.knighttour.data.local.entity.GameSessionEntity
import kotlinx.coroutines.flow.Flow

// ===============================================================
//  GAME SESSION DAO
//
//  Package: data/local/dao/GameSessionDao.kt
// ===============================================================

@Dao
interface GameSessionDao {

    // ── Write ────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: GameSessionEntity)

    @Update
    suspend fun update(session: GameSessionEntity)

    @Query("DELETE FROM game_sessions WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM game_sessions")
    suspend fun deleteAll()

    // ── Read — single session ─────────────────────────────────────

    @Query("SELECT * FROM game_sessions WHERE id = :id")
    suspend fun getById(id: String): GameSessionEntity?

    // ── Read — in-progress (resume game) ─────────────────────────

    /**
     * Returns the most recently started unfinished session.
     * Used by the Home screen "RESUME" button.
     */
    @Query("""
        SELECT * FROM game_sessions
        WHERE status = 'IN_PROGRESS'
        ORDER BY startedAt DESC
        LIMIT 1
    """)
    suspend fun getLatestInProgress(): GameSessionEntity?

    @Query("""
        SELECT COUNT(*) FROM game_sessions
        WHERE status = 'IN_PROGRESS'
    """)
    fun hasInProgressSession(): Flow<Int>

    // ── Read — history ────────────────────────────────────────────

    /**
     * All finished sessions, newest first.
     * Used by the history / stats screen.
     */
    @Query("""
        SELECT * FROM game_sessions
        WHERE status != 'IN_PROGRESS'
        ORDER BY finishedAt DESC
        LIMIT :limit
    """)
    fun getFinishedSessions(limit: Int = 50): Flow<List<GameSessionEntity>>

    /**
     * All completed (victory only) sessions for a specific board size.
     * Used by ScoreRepository to compute personal bests.
     */
    @Query("""
        SELECT * FROM game_sessions
        WHERE status = 'COMPLETED'
        AND boardSize = :boardSize
        ORDER BY score DESC
        LIMIT :limit
    """)
    suspend fun getCompletedByBoardSize(
        boardSize : Int,
        limit     : Int = 10,
    ): List<GameSessionEntity>

    /**
     * Best score ever for a given board size.
     */
    @Query("""
        SELECT MAX(score) FROM game_sessions
        WHERE status = 'COMPLETED'
        AND boardSize = :boardSize
    """)
    suspend fun getBestScoreForBoard(boardSize: Int): Int?

    /**
     * Top N scores across all board sizes (local leaderboard).
     */
    @Query("""
        SELECT * FROM game_sessions
        WHERE status = 'COMPLETED'
        ORDER BY score DESC
        LIMIT :limit
    """)
    fun getTopScores(limit: Int = 50): Flow<List<GameSessionEntity>>

    // ── Stats ─────────────────────────────────────────────────────

    @Query("SELECT COUNT(*) FROM game_sessions WHERE status = 'COMPLETED'")
    suspend fun getTotalWins(): Int

    @Query("SELECT COUNT(*) FROM game_sessions WHERE status != 'IN_PROGRESS' AND status != 'ABANDONED'")
    suspend fun getTotalGamesPlayed(): Int

    @Query("SELECT MAX(score) FROM game_sessions WHERE status = 'COMPLETED'")
    suspend fun getGlobalPersonalBest(): Int?
}