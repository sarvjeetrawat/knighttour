package com.kunpitech.knighttour.data.online

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
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
    data class   GuestRequestJoin(val name: String)            : RoomEvent
    data object  RequestSent                                   : RoomEvent  // guest: request sent to host
    data object  RequestRejected                               : RoomEvent  // guest: host rejected
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
    private var roomStatusJob     : Job? = null   // cancelled/restarted each new game
    private var opponentObserverJob: Job? = null  // cancelled/restarted each new game

    private var session: OnlineSession? = null
    private var lastSession: OnlineSession? = null

    // replay=0: no stale events replayed to new subscribers (GameViewModel, LobbyViewModel)
    // extraBufferCapacity=8: events aren't dropped if collector is slow
    private val _roomEvents = MutableSharedFlow<RoomEvent>(replay = 0, extraBufferCapacity = 8)
    val roomEvents: SharedFlow<RoomEvent> = _roomEvents.asSharedFlow()

    init {
        // Track app foreground/background — updates host/guest isConnected in Firebase automatically
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                // App came to foreground — session may not be attached yet, so LobbyViewModel/
                // HomeViewModel handle isConnected=true restore after finding the room.
                managerScope.launch {
                    try { setConnected(true) } catch (_: Exception) {}
                }
            }
            override fun onStop(owner: LifecycleOwner) {
                // App went to background — mark disconnected.
                // Use active session OR lastSession so this works even briefly after endSession.
                managerScope.launch {
                    val s = session ?: lastSession ?: return@launch
                    try {
                        firebaseRepo.setConnected(s.roomCode, localRoleStr().ifEmpty {
                            if (s.localRole == OnlineRole.HOST) "host" else "guest"
                        }, false)
                    } catch (_: Exception) {}
                }
            }
        })
    }

    private fun resetRoomEvents() {
        // Cancel stale observers — prevents duplicate events on rematch
        roomStatusJob?.cancel()
        roomStatusJob = null
        opponentObserverJob?.cancel()
        opponentObserverJob = null
        lastSession = null
        _joinedOpponentName.value = ""
        _opponentState.value = null
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
        localId   : String,   // stable ID from UserPreferences
        localName : String,
        roomName  : String,
        boardSize : Int,
        scope     : CoroutineScope,
    ) {
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

    suspend fun attachToExistingRoom(
        localId   : String,   // stable ID from UserPreferences
        localName : String,
        roomCode  : String,
        boardSize : Int,
        scope     : CoroutineScope,
    ) {
        lastSession = null
        resetRoomEvents()
        attachSessionObserver(roomCode, localId, localName, boardSize, scope)
    }

    /** Guest reconnects to a game already in PLAYING state (e.g. after app kill). */
    suspend fun attachGuestToExistingRoom(
        localId   : String,
        localName : String,
        roomCode  : String,
        boardSize : Int,
        scope     : CoroutineScope,
    ) {
        lastSession = null
        resetRoomEvents()
        session = OnlineSession(
            roomCode     = roomCode,
            localRole    = OnlineRole.GUEST,
            localId      = localId,
            localName    = localName,
            boardSize    = boardSize,
        )
        firebaseRepo.registerDisconnectHandler(roomCode, "guest")
        firebaseRepo.setConnected(roomCode, "guest", true)
        // Read opponent (host) name
        val room = firebaseRepo.getRoomOnce(roomCode)
        val hostName = room?.host?.name ?: ""
        session = session?.copy(opponentName = hostName)
        _joinedOpponentName.value  = hostName
        _lastKnownOpponentName.value = hostName
        // Start observing host moves
        observeOpponent(roomCode, scope)
        _roomEvents.tryEmit(RoomEvent.GameStarted)
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
                    "PENDING_ACCEPT" -> {
                        // Guest sent a join request — notify host via event
                        val room = firebaseRepo.getRoomOnce(persistentCode)
                        val guestName = room?.guest?.name ?: "Opponent"
                        _roomEvents.tryEmit(RoomEvent.GuestRequestJoin(guestName))
                    }
                    "PLAYING" -> {
                        // Host accepted — start game
                        val room = firebaseRepo.getRoomOnce(persistentCode)
                        val guestName = room?.guest?.name ?: "Opponent"
                        session = session?.copy(opponentName = guestName)
                        _joinedOpponentName.value = guestName
                        _lastKnownOpponentName.value = guestName
                        _roomEvents.tryEmit(RoomEvent.OpponentJoined(guestName))
                        _roomEvents.tryEmit(RoomEvent.GameStarted)
                        observeOpponent(persistentCode, scope)
                    }
                    "WAITING" -> {
                        // Host rejected or room reset — back to waiting
                        _roomEvents.tryEmit(RoomEvent.WaitingForOpponent)
                    }
                    "FINISHED" -> _roomEvents.tryEmit(RoomEvent.GameFinished)
                }
            }
            .launchIn(managerScope)
    }

    suspend fun joinRoom(
        localId   : String,   // stable ID from UserPreferences
        roomCode  : String,
        localName : String,
        scope     : CoroutineScope,
    ): Boolean {
        lastSession = null
        resetRoomEvents()
        return try {
            val room = firebaseRepo.joinRoom(
                roomCode  = roomCode,
                guestId   = localId,
                guestName = localName,
            ) ?: return false

            session = OnlineSession(
                roomCode     = roomCode,
                localRole    = OnlineRole.GUEST,
                localId      = localId,
                localName    = localName,
                opponentName = room.host.name,
                boardSize    = room.boardSize,
            )
            _lastKnownOpponentName.value = room.host.name

            firebaseRepo.registerDisconnectHandler(roomCode, "guest")

            // Stay in lobby — emit request sent event
            _roomEvents.tryEmit(RoomEvent.RequestSent)

            // Watch for host accept (PLAYING) or reject (WAITING)
            managerScope.launch {
                firebaseRepo.observeRoomStatus(roomCode)
                    .collect { status ->
                        when (status) {
                            "PLAYING" -> {
                                observeOpponent(roomCode, scope)
                                _roomEvents.tryEmit(RoomEvent.GameStarted)
                            }
                            "WAITING" -> {
                                session = null
                                _roomEvents.tryEmit(RoomEvent.RequestRejected)
                            }
                        }
                    }
            }
            true

        } catch (e: Exception) {
            _roomEvents.tryEmit(RoomEvent.Error(e.message ?: "Failed to join room"))
            false
        }
    }

    /** Used for rematch — joins directly with PLAYING status, no accept/reject flow. */
    suspend fun joinRoomForRematch(
        localId   : String,
        roomCode  : String,
        localName : String,
        scope     : CoroutineScope,
    ): Boolean {
        lastSession = null
        resetRoomEvents()
        return try {
            val room = firebaseRepo.joinRoomDirectly(
                roomCode  = roomCode,
                guestId   = localId,
                guestName = localName,
            ) ?: return false

            session = OnlineSession(
                roomCode     = roomCode,
                localRole    = OnlineRole.GUEST,
                localId      = localId,
                localName    = localName,
                opponentName = room.host.name,
                boardSize    = room.boardSize,
            )
            _lastKnownOpponentName.value = room.host.name
            firebaseRepo.registerDisconnectHandler(roomCode, "guest")
            observeOpponent(roomCode, scope)
            _roomEvents.tryEmit(RoomEvent.GameStarted)
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
        // Cancel all active observers immediately
        roomStatusJob?.cancel()
        roomStatusJob = null
        opponentObserverJob?.cancel()
        opponentObserverJob = null
        try {
            // Cancel ALL onDisconnect handlers first — prevents stale handlers
            // from recreating deleted room nodes on next app launch
            firebaseRepo.cancelAllDisconnectHandlers(s.roomCode)
            firebaseRepo.setConnected(s.roomCode, localRoleStr(), false)
            if (s.localRole == OnlineRole.HOST) {
                if (deleteRoom) {
                    // Normal game end — briefly mark FINISHED so guest can read final scores
                    firebaseRepo.setRoomFinished(s.roomCode)
                    kotlinx.coroutines.delay(2000)
                }
                // Reset room to WAITING immediately (no FINISHED detour on mid-game quit)
                firebaseRepo.resetRoomForRematch(
                    roomCode  = s.roomCode,
                    hostId    = s.localId,
                    hostName  = s.localName,
                    guestName = "",
                    boardSize = s.boardSize,
                )
            } else {
                // Guest: on normal game end set FINISHED; on mid-game quit reset room to WAITING
                if (deleteRoom) {
                    firebaseRepo.setRoomFinished(s.roomCode)
                } else {
                    // Clear guest slot so host can accept a new opponent
                    firebaseRepo.clearGuestFromRoom(s.roomCode)
                }
            }
        } catch (_: Exception) {}
        session = null
        _roomEvents.tryEmit(RoomEvent.WaitingForOpponent)
        _opponentState.value = null
    }

    private fun observeOpponent(roomCode: String, scope: CoroutineScope) {
        // Cancel any previous opponent observer before starting new one
        opponentObserverJob?.cancel()
        opponentObserverJob = firebaseRepo.observeOpponent(roomCode, opponentRole())
            .onEach { state ->
                // Ignore stale events if session has moved to a different room
                if (session?.roomCode != roomCode) return@onEach

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