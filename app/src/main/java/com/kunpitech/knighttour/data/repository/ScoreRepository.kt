package com.kunpitech.knighttour.data.repository

import com.kunpitech.knighttour.data.local.dao.ScoreDao
import com.kunpitech.knighttour.data.local.entity.ScoreEntity
import com.kunpitech.knighttour.domain.model.GameSession
import com.kunpitech.knighttour.domain.model.SessionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// ===============================================================
//  SCORE REPOSITORY
//
//  Saves completed game scores and exposes personal bests +
//  local leaderboard as Flow streams.
//
//  Package: data/repository/ScoreRepository.kt
// ===============================================================

/** Domain-friendly score summary (not tied to Room). */
data class ScoreSummary(
    val sessionId     : String,
    val playerName    : String,
    val score         : Int,
    val boardSize     : Int,
    val difficulty    : String,
    val mode          : String,
    val elapsedSeconds: Int,
    val hintsUsed     : Int,
    val finishedAt    : Long,
    val isPersonalBest: Boolean,
)

interface ScoreRepository {
    /** Record a completed game's score. Handles personal-best flag internally. */
    suspend fun recordScore(session: GameSession, playerName: String)

    /** Stream of personal bests, one per board size, ordered by boardSize. */
    fun getPersonalBests(): Flow<List<ScoreSummary>>

    /** Stream of all scores, best first. */
    fun getTopScores(limit: Int = 50): Flow<List<ScoreSummary>>

    /** Stream of recent scores, newest first. */
    fun getRecentScores(limit: Int = 20): Flow<List<ScoreSummary>>

    /** Best score ever recorded, or null. */
    suspend fun getGlobalBest(): Int?

    /** Wipe all scores. */
    suspend fun clearAll()
}

// ── Implementation ───────────────────────────────────────────────

@Singleton
class ScoreRepositoryImpl @Inject constructor(
    private val dao: ScoreDao,
) : ScoreRepository {

    override suspend fun recordScore(session: GameSession, playerName: String) {
        if (session.status != SessionStatus.COMPLETED) return

        val existing = dao.getBestForBoard(session.boardSize)
        val isNewBest = existing == null || session.score > (existing.score)

        // If this is a new personal best, clear the old PB flag for this board size
        if (isNewBest && existing != null) {
            dao.insert(existing.copy(isPersonalBest = false))
        }

        dao.insert(
            ScoreEntity(
                sessionId      = session.id,
                playerName     = playerName,
                score          = session.score,
                boardSize      = session.boardSize,
                difficulty     = session.difficulty.name,
                mode           = session.mode.name,
                elapsedSeconds = session.elapsedSeconds,
                hintsUsed      = session.hintsUsed,
                finishedAt     = session.finishedAt ?: System.currentTimeMillis(),
                isPersonalBest = isNewBest,
            )
        )
    }

    override fun getPersonalBests(): Flow<List<ScoreSummary>> =
        dao.getPersonalBests().map { list -> list.map { it.toSummary() } }

    override fun getTopScores(limit: Int): Flow<List<ScoreSummary>> =
        dao.getTopScores(limit).map { list -> list.map { it.toSummary() } }

    override fun getRecentScores(limit: Int): Flow<List<ScoreSummary>> =
        dao.getRecentScores(limit).map { list -> list.map { it.toSummary() } }

    override suspend fun getGlobalBest(): Int? =
        dao.getGlobalBest()

    override suspend fun clearAll() =
        dao.deleteAll()

    private fun ScoreEntity.toSummary() = ScoreSummary(
        sessionId      = sessionId,
        playerName     = playerName,
        score          = score,
        boardSize      = boardSize,
        difficulty     = difficulty,
        mode           = mode,
        elapsedSeconds = elapsedSeconds,
        hintsUsed      = hintsUsed,
        finishedAt     = finishedAt,
        isPersonalBest = isPersonalBest,
    )
}