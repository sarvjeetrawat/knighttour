package com.kunpitech.knighttour.data.online

import com.kunpitech.knighttour.data.repository.FirebaseGameRepository
import com.kunpitech.knighttour.data.repository.PlayerStateDto
import com.kunpitech.knighttour.data.repository.toPosition
import com.kunpitech.knighttour.domain.model.Position
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

enum class OnlineRole { HOST, GUEST }

data class OnlineSession(
    val roomCode           : String,
    val localRole          : OnlineRole,
    val localId            : String,
    val localName          : String,
    val opponentName       : String  = "",
    val boardSize          : Int     = 6,
    val opponentFinalScore : Int     = 0,
)

sealed interface RoomEvent {
    data object  WaitingForOpponent                            : RoomEvent
    data class   OpponentJoined(val name: String)              : RoomEvent
    data object  GameStarted                                   : RoomEvent
    data class   OpponentMoved(val moveCount: Int,
                               val history: List<Position>)    : RoomEvent
    data object  OpponentDisconnected                          : RoomEvent
    data object  OpponentReconnected                           : RoomEvent
    /** Opponent finished their game. isCompleted=true means they cleared the board. */
    data class   OpponentFinished(val isCompleted: Boolean)    : RoomEvent
    data object  GameFinished                                  : RoomEvent
    data class   Error(val message: String)                    : RoomEvent
}

@Singleton
class OnlineSessionManager @Inject constructor(
    private val firebaseRepo: FirebaseGameRepository,
) {
    // Singleton scope — survives ViewModel navigation, lives as long as the app
    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var roomStatusJob: Job? = null   // cancelled/restarted each new game

    private var session: OnlineSession? = null
    private var lastSession: OnlineSession? = null

    // replay=0: no stale events replayed to new subscribers (GameViewModel, LobbyViewModel)
    // extraBufferCapacity=8: events aren't dropped if collector is slow
    private val _roomEvents = MutableSharedFlow<RoomEvent>(replay = 0, extraBufferCapacity = 8)
    val roomEvents: SharedFlow<RoomEvent> = _roomEvents.asSharedFlow()

    private fun resetRoomEvents() {
        // Flow is stable — just clear session state
        lastSession = null
        _joinedOpponentName.value = ""
        // NOTE: do NOT clear lastKnownOpponentName here — it must survive resets for rematch overlay
    }

    private val _opponentState = MutableStateFlow<PlayerStateDto?>(null)
    val opponentState: StateFlow<PlayerStateDto?> = _opponentState.asStateFlow()

    // Set to opponent name when guest joins — persists across navigation so GameViewModel reads it on init
    private val _joinedOpponentName = MutableStateFlow<String>("")
    val joinedOpponentName: StateFlow<String> = _joinedOpponentName.asStateFlow()

    // Survives session resets — used for rematch overlay so name never shows as blank/disconnected
    private val _lastKnownOpponentName = MutableStateFlow<String>("")
    val lastKnownOpponentName: StateFlow<String> = _lastKnownOpponentName.asStateFlow()

    fun generateRoomCode(): String = Random.nextInt(100_000, 999_999).toString()

    fun currentSession(): OnlineSession? = session
    fun lastCompletedSession(): OnlineSession? = lastSession
    fun clearLastSession() { lastSession = null }

    fun snapshotLastSession() { lastSession = session }

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

    suspend fun createRoom(
        localName : String,
        roomName  : String,   // user-chosen room name
        boardSize : Int,
        scope     : CoroutineScope,
    ) {
        val localId = UUID.randomUUID().toString()
        lastSession = null
        resetRoomEvents()

        try {
            val persistentCode = firebaseRepo.getOrResetPlayerRoom(
                hostName  = localName,
                hostId    = localId,
                roomName  = roomName,
                boardSize = boardSize,
            )
            attachSessionObserver(persistentCode, localId, localName, boardSize, scope)
        } catch (e: Exception) {
            _roomEvents.tryEmit(RoomEvent.Error(e.message ?: "Failed to create room"))
        }
    }

    /**
     * Attach session + observer to an already-reset room without resetting Firebase again.
     * Used by reconnectExistingRoom in LobbyViewModel — room is already in WAITING state.
     */
    suspend fun attachToExistingRoom(
        localName : String,
        roomCode  : String,   // passed in directly from reconnectExistingRoom which already found it
        boardSize : Int,
        scope     : CoroutineScope,
    ) {
        val localId = UUID.randomUUID().toString()
        lastSession = null
        resetRoomEvents()

        attachSessionObserver(roomCode, localId, localName, boardSize, scope)
    }

    private fun attachSessionObserver(
        persistentCode : String,
        localId        : String,
        localName      : String,
        boardSize      : Int,
        scope          : CoroutineScope,
    ) {
        session = OnlineSession(
            roomCode  = persistentCode,
            localRole = OnlineRole.HOST,
            localId   = localId,
            localName = localName,
            boardSize = boardSize,
        )

        firebaseRepo.registerDisconnectHandler(persistentCode, "host")
        _roomEvents.tryEmit(RoomEvent.WaitingForOpponent)

        // Cancel any previous room status observer before starting a new one
        roomStatusJob?.cancel()
        roomStatusJob = firebaseRepo.observeRoomStatus(persistentCode)
            .onEach { status ->
                when (status) {
                    "PLAYING" -> {
                        val room = firebaseRepo.getRoomOnce(persistentCode)
                        val guestName = room?.guest?.name ?: "Opponent"
                        session = session?.copy(opponentName = guestName)
                        _joinedOpponentName.value = guestName
                        _lastKnownOpponentName.value = guestName
                        _roomEvents.tryEmit(RoomEvent.OpponentJoined(guestName))
                        _roomEvents.tryEmit(RoomEvent.GameStarted)
                        observeOpponent(persistentCode, scope)
                    }
                    "FINISHED" -> _roomEvents.tryEmit(RoomEvent.GameFinished)
                }
            }
            .launchIn(managerScope)
    }

    suspend fun joinRoom(
        roomCode  : String,
        localName : String,
        scope     : CoroutineScope,
    ): Boolean {
        val localId = UUID.randomUUID().toString()
        lastSession = null
        resetRoomEvents()
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
            _lastKnownOpponentName.value = room.host.name   // survives resets

            firebaseRepo.registerDisconnectHandler(roomCode, "guest")
            _roomEvents.tryEmit(RoomEvent.GameStarted)
            observeOpponent(roomCode, scope)
            true

        } catch (e: Exception) {
            _roomEvents.tryEmit(RoomEvent.Error(e.message ?: "Failed to join room"))
            false
        }
    }

    suspend fun pushMove(position: Position, fullHistory: List<Position>) {
        val s = session ?: return
        try {
            firebaseRepo.pushMove(
                roomCode = s.roomCode,
                role     = localRoleStr(),
                position = position,
                history  = fullHistory,
            )
        } catch (_: Exception) {}
    }

    /**
     * Called by GameViewModel to restart opponent observation in the game's scope.
     * The lobby scope may be cancelled by the time GameViewModel starts.
     */
    fun startObservingOpponent(scope: CoroutineScope) {
        val s = session ?: return
        observeOpponent(s.roomCode, scope)
    }

    suspend fun markLocalFinished(finalScore: Int = 0, moveCount: Int = 0, isCompleted: Boolean = false) {
        val s = session ?: return
        val roomCode = s.roomCode
        val role     = localRoleStr()
        val name     = s.localName
        try {
            firebaseRepo.markFinished(roomCode, role, finalScore, moveCount, name, isCompleted)
        } catch (e: Exception) {
            try {
                firebaseRepo.writeMatchResult(roomCode, role, finalScore, moveCount, name, isCompleted)
            } catch (_: Exception) {}
        }
    }

    suspend fun setConnected(connected: Boolean) {
        val s = session ?: return
        try {
            firebaseRepo.setConnected(s.roomCode, localRoleStr(), connected)
        } catch (_: Exception) {}
    }

    suspend fun endSession(deleteRoom: Boolean = true) {
        val s = session ?: return
        lastSession = s
        try {
            firebaseRepo.setConnected(s.roomCode, localRoleStr(), false)
            if (s.localRole == OnlineRole.HOST) {
                firebaseRepo.setRoomFinished(s.roomCode)
                // Short delay so guest can read final scores before room resets
                // Only delay if game ended normally (deleteRoom = true means normal end)
                if (deleteRoom) kotlinx.coroutines.delay(2000)
                // Reset room immediately so it reappears in browse
                firebaseRepo.resetRoomForRematch(
                    roomCode  = s.roomCode,
                    hostId    = s.localId,
                    hostName  = s.localName,
                    guestName = "",
                    boardSize = s.boardSize,
                )
            } else {
                firebaseRepo.setRoomFinished(s.roomCode)
            }
        } catch (_: Exception) {}
        session = null
        _roomEvents.tryEmit(RoomEvent.WaitingForOpponent)
        _opponentState.value = null
    }

    private fun observeOpponent(roomCode: String, scope: CoroutineScope) {
        firebaseRepo.observeOpponent(roomCode, opponentRole())
            .onEach { state ->
                _opponentState.value = state

                if (!state.isConnected) {
                    _roomEvents.tryEmit(RoomEvent.OpponentDisconnected)
                } else {
                    val history = state.moveHistory.map { it.toPosition() }
                    _roomEvents.tryEmit(RoomEvent.OpponentMoved(state.moveCount, history))
                }

                if (state.finishedAt > 0L) {
                    if (state.finalScore > 0) {
                        session = session?.copy(opponentFinalScore = state.finalScore)
                    }
                    _roomEvents.tryEmit(RoomEvent.OpponentFinished(state.isCompleted))
                }
            }
            .launchIn(scope)
    }
}