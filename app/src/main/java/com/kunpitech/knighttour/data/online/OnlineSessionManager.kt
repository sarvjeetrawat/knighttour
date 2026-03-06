package com.kunpitech.knighttour.data.online

import com.kunpitech.knighttour.data.repository.FirebaseGameRepository
import com.kunpitech.knighttour.data.repository.PlayerStateDto
import com.kunpitech.knighttour.data.repository.toPosition
import com.kunpitech.knighttour.domain.model.Position
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

// ================================================================
//  ONLINE SESSION MANAGER
//
//  Single source of truth for an active multiplayer session.
//  Injected into LobbyViewModel (room setup) and
//  GameViewModel (in-game sync).
//
//  Responsibilities:
//  1. Generate / validate room codes
//  2. Create and join Firebase rooms
//  3. Push local moves to Firebase
//  4. Observe opponent state and emit updates
//  5. Manage presence (isConnected flag + onDisconnect handler)
//  6. Clean up on game end
//
//  Package: data/online/OnlineSessionManager.kt
// ================================================================

enum class OnlineRole { HOST, GUEST }

data class OnlineSession(
    val roomCode     : String,
    val localRole    : OnlineRole,
    val localId      : String,
    val localName    : String,
    val opponentName : String  = "",
    val boardSize    : Int     = 6,
)

sealed interface RoomEvent {
    data object  WaitingForOpponent                   : RoomEvent
    data class   OpponentJoined(val name: String)     : RoomEvent
    data object  GameStarted                          : RoomEvent
    data class   OpponentMoved(val moveCount: Int,
                               val history: List<Position>) : RoomEvent
    data object  OpponentDisconnected                 : RoomEvent
    data object  OpponentReconnected                  : RoomEvent
    data object  GameFinished                         : RoomEvent
    data class   Error(val message: String)           : RoomEvent
}

@Singleton
class OnlineSessionManager @Inject constructor(
    private val firebaseRepo: FirebaseGameRepository,
) {
    // Current session — null when not in a multiplayer game
    private var session: OnlineSession? = null

    private val _roomEvents = MutableStateFlow<RoomEvent>(RoomEvent.WaitingForOpponent)
    val roomEvents: StateFlow<RoomEvent> = _roomEvents.asStateFlow()

    private val _opponentState = MutableStateFlow<PlayerStateDto?>(null)
    val opponentState: StateFlow<PlayerStateDto?> = _opponentState.asStateFlow()

    // ── Room code ────────────────────────────────────────────────

    /** Generate a random 6-digit numeric room code. */
    fun generateRoomCode(): String = Random.nextInt(100_000, 999_999).toString()

    fun currentSession(): OnlineSession? = session

    fun localRole(): OnlineRole? = session?.localRole

    fun opponentRole(): String = when (session?.localRole) {
        OnlineRole.HOST  -> "guest"
        OnlineRole.GUEST -> "host"
        null             -> "guest"
    }

    fun localRoleStr(): String = when (session?.localRole) {
        OnlineRole.HOST  -> "host"
        OnlineRole.GUEST -> "guest"
        null             -> "host"
    }

    // ── Host flow ────────────────────────────────────────────────

    /**
     * Create a room and wait for opponent.
     * Emits WaitingForOpponent immediately, then OpponentJoined when guest connects.
     */
    suspend fun createRoom(
        roomCode  : String,
        localName : String,
        boardSize : Int,
        scope     : CoroutineScope,
    ) {
        val localId = UUID.randomUUID().toString()
        session = OnlineSession(
            roomCode     = roomCode,
            localRole    = OnlineRole.HOST,
            localId      = localId,
            localName    = localName,
            boardSize    = boardSize,
        )

        try {
            firebaseRepo.createRoom(
                roomCode  = roomCode,
                hostId    = localId,
                hostName  = localName,
                boardSize = boardSize,
            )
            firebaseRepo.registerDisconnectHandler(roomCode, "host")
            _roomEvents.value = RoomEvent.WaitingForOpponent

            // Watch for guest joining
            firebaseRepo.observeRoomStatus(roomCode)
                .onEach { status ->
                    when (status) {
                        "PLAYING" -> {
                            val room = firebaseRepo.getRoomOnce(roomCode)
                            val guestName = room?.guest?.name ?: "Opponent"
                            session = session?.copy(opponentName = guestName)
                            _roomEvents.value = RoomEvent.OpponentJoined(guestName)
                            _roomEvents.value = RoomEvent.GameStarted
                            observeOpponent(roomCode, scope)
                        }
                        "FINISHED" -> _roomEvents.value = RoomEvent.GameFinished
                    }
                }
                .launchIn(scope)

        } catch (e: Exception) {
            _roomEvents.value = RoomEvent.Error(e.message ?: "Failed to create room")
        }
    }

    // ── Guest flow ───────────────────────────────────────────────

    /**
     * Join an existing room as guest.
     * Returns false if room not found or already full.
     */
    suspend fun joinRoom(
        roomCode  : String,
        localName : String,
        scope     : CoroutineScope,
    ): Boolean {
        val localId = UUID.randomUUID().toString()

        return try {
            val joined = firebaseRepo.joinRoom(
                roomCode  = roomCode,
                guestId   = localId,
                guestName = localName,
            )
            if (!joined) return false

            val room = firebaseRepo.getRoomOnce(roomCode) ?: return false

            session = OnlineSession(
                roomCode     = roomCode,
                localRole    = OnlineRole.GUEST,
                localId      = localId,
                localName    = localName,
                opponentName = room.host.name,
                boardSize    = room.boardSize,
            )

            firebaseRepo.registerDisconnectHandler(roomCode, "guest")
            _roomEvents.value = RoomEvent.GameStarted
            observeOpponent(roomCode, scope)
            true

        } catch (e: Exception) {
            _roomEvents.value = RoomEvent.Error(e.message ?: "Failed to join room")
            false
        }
    }

    // ── In-game ──────────────────────────────────────────────────

    /** Push a move to Firebase after every local knight move. */
    suspend fun pushMove(position: Position, fullHistory: List<Position>) {
        val s = session ?: return
        try {
            firebaseRepo.pushMove(
                roomCode = s.roomCode,
                role     = localRoleStr(),
                position = position,
                history  = fullHistory,
            )
        } catch (e: Exception) {
            // Non-fatal — local game continues even if push fails
        }
    }

    /** Call when local player finishes the board. */
    suspend fun markLocalFinished() {
        val s = session ?: return
        try {
            firebaseRepo.markFinished(s.roomCode, localRoleStr())
        } catch (_: Exception) {}
    }

    /** Set presence on app foreground/background. */
    suspend fun setConnected(connected: Boolean) {
        val s = session ?: return
        try {
            firebaseRepo.setConnected(s.roomCode, localRoleStr(), connected)
        } catch (_: Exception) {}
    }

    // ── Cleanup ──────────────────────────────────────────────────

    /** Call on game end or screen exit. Marks room FINISHED and deletes it. */
    suspend fun endSession(deleteRoom: Boolean = true) {
        val s = session ?: return
        try {
            firebaseRepo.setConnected(s.roomCode, localRoleStr(), false)
            // Always mark finished so the other player knows game ended
            firebaseRepo.setRoomFinished(s.roomCode)
            // Host deletes the room to keep Firebase clean
            if (s.localRole == OnlineRole.HOST) {
                kotlinx.coroutines.delay(1500) // small delay so guest reads FINISHED first
                firebaseRepo.deleteRoom(s.roomCode)
            }
        } catch (_: Exception) {}
        session = null
        _roomEvents.value = RoomEvent.WaitingForOpponent
        _opponentState.value = null
    }

    // ── Private ──────────────────────────────────────────────────

    private fun observeOpponent(roomCode: String, scope: CoroutineScope) {
        firebaseRepo.observeOpponent(roomCode, opponentRole())
            .onEach { state ->
                _opponentState.value = state

                if (!state.isConnected) {
                    _roomEvents.value = RoomEvent.OpponentDisconnected
                } else {
                    // Emit move event if moveCount changed
                    val history = state.moveHistory.map { it.toPosition() }
                    _roomEvents.value = RoomEvent.OpponentMoved(state.moveCount, history)
                }

                if (state.finishedAt > 0L) {
                    _roomEvents.value = RoomEvent.GameFinished
                }
            }
            .launchIn(scope)
    }
}