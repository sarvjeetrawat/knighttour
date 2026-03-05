package com.kunpitech.knighttour.data.repository

import com.kunpitech.knighttour.data.local.dao.GameSessionDao
import com.kunpitech.knighttour.data.local.entity.toDomain
import com.kunpitech.knighttour.data.local.entity.toEntity
import com.kunpitech.knighttour.domain.model.GameSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// ===============================================================
//  GAME REPOSITORY
//
//  Single source of truth for GameSession persistence.
//  ViewModel and UseCases depend on this interface — the Hilt
//  module binds the Impl so tests can supply a fake.
//
//  Package: data/repository/GameRepository.kt
// ===============================================================

interface GameRepository {
    /** Save or update a session. Called on every move + on game end. */
    suspend fun save(session: GameSession)

    /** Load a specific session by ID. */
    suspend fun getById(id: String): GameSession?

    /** The most recent unfinished session, or null. Used by Home → Resume. */
    suspend fun getLatestInProgress(): GameSession?

    /** Emits true whenever an in-progress session exists. */
    fun hasInProgressSession(): Flow<Boolean>

    /** Stream of finished sessions, newest first. */
    fun getFinishedSessions(limit: Int = 50): Flow<List<GameSession>>

    /** Stream of top scores, best first. */
    fun getTopScores(limit: Int = 50): Flow<List<GameSession>>

    /** Best score for a given board size. */
    suspend fun getBestScoreForBoard(boardSize: Int): Int?

    /** Lifetime win count. */
    suspend fun getTotalWins(): Int

    /** Total games played (excludes abandoned). */
    suspend fun getTotalGamesPlayed(): Int

    /** Best score across all completed sessions. */
    suspend fun getGlobalPersonalBest(): Int?

    /** Delete a specific session (e.g. after user clears data). */
    suspend fun deleteById(id: String)

    /** Wipe all local game data. */
    suspend fun clearAll()
}

// ── Implementation ───────────────────────────────────────────────

@Singleton
class GameRepositoryImpl @Inject constructor(
    private val dao: GameSessionDao,
) : GameRepository {

    override suspend fun save(session: GameSession) {
        dao.upsert(session.toEntity())
    }

    override suspend fun getById(id: String): GameSession? =
        dao.getById(id)?.toDomain()

    override suspend fun getLatestInProgress(): GameSession? =
        dao.getLatestInProgress()?.toDomain()

    override fun hasInProgressSession(): Flow<Boolean> =
        dao.hasInProgressSession().map { count -> count > 0 }

    override fun getFinishedSessions(limit: Int): Flow<List<GameSession>> =
        dao.getFinishedSessions(limit).map { list -> list.map { it.toDomain() } }

    override fun getTopScores(limit: Int): Flow<List<GameSession>> =
        dao.getTopScores(limit).map { list -> list.map { it.toDomain() } }

    override suspend fun getBestScoreForBoard(boardSize: Int): Int? =
        dao.getBestScoreForBoard(boardSize)

    override suspend fun getTotalWins(): Int =
        dao.getTotalWins()

    override suspend fun getTotalGamesPlayed(): Int =
        dao.getTotalGamesPlayed()

    override suspend fun getGlobalPersonalBest(): Int? =
        dao.getGlobalPersonalBest()

    override suspend fun deleteById(id: String) =
        dao.deleteById(id)

    override suspend fun clearAll() =
        dao.deleteAll()
}