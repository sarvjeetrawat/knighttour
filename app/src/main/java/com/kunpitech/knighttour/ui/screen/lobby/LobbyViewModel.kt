package com.kunpitech.knighttour.ui.screen.lobby

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kunpitech.knighttour.data.online.OnlineSessionManager
import com.kunpitech.knighttour.data.online.RoomEvent
import com.kunpitech.knighttour.data.repository.FirebaseGameRepository
import com.kunpitech.knighttour.data.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LobbyViewModel @Inject constructor(
    private val sessionManager  : OnlineSessionManager,
    private val firebaseRepo    : FirebaseGameRepository,
    private val prefsRepository : UserPreferencesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LobbyUiState())
    val uiState: StateFlow<LobbyUiState> = _uiState.asStateFlow()

    private val _navigateToGame = MutableStateFlow<String?>(null)
    val navigateToGame: StateFlow<String?> = _navigateToGame.asStateFlow()

    private var playerId: String = ""
    private var browseStarted   = false
    private var ownRoomWatcherJob: kotlinx.coroutines.Job? = null

    init {
        viewModelScope.launch {
            playerId = prefsRepository.getOrCreatePlayerId()
            prefsRepository.preferences.collect { prefs ->
                val name = prefs.playerName
                _uiState.update { it.copy(playerName = name) }
                if (name.isNotEmpty()) {
                    observeWaitingRooms(name)
                    reconnectExistingRoom(name)      // host path
                    reconnectAsGuest()               // guest path
                    startOwnRoomWatcher()
                }
            }
        }
        sessionManager.roomEvents
            .onEach { handleRoomEvent(it) }
            .launchIn(viewModelScope)
    }

    /**
     * Watches the host's own room status directly in Firebase.
     * This catches PENDING_ACCEPT even when no sessionManager session is attached
     * (e.g. host navigated back from game, or sessionManager wasn't set up yet).
     */
    private fun startOwnRoomWatcher() {
        ownRoomWatcherJob?.cancel()
        ownRoomWatcherJob = viewModelScope.launch {
            while (true) {
                val roomCode = firebaseRepo.findRoomCodeByHost(playerId)
                if (roomCode == null) {
                    kotlinx.coroutines.delay(3_000)
                    continue
                }
                // Observe this room's status changes directly
                firebaseRepo.observeRoomStatus(roomCode).collect { status ->
                    when (status) {
                        "PENDING_ACCEPT" -> {
                            val room = firebaseRepo.getRoomOnce(roomCode) ?: return@collect
                            val guestName = room.guest.name
                            if (guestName.isNotEmpty()) {
                                _uiState.update { it.copy(incomingGuestName = guestName) }
                            }
                        }
                        "WAITING" -> {
                            // Guest rejected/left — clear banner if no game in progress
                            if (_uiState.value.incomingGuestName.isNotEmpty()) {
                                _uiState.update { it.copy(incomingGuestName = "") }
                            }
                        }
                        "FINISHED" -> {
                            // Room done — restart loop to find new room
                            _uiState.update { it.copy(incomingGuestName = "") }
                            return@collect
                        }
                        else -> Unit
                    }
                }
                // observeRoomStatus ended (room deleted or FINISHED) — retry after delay
                kotlinx.coroutines.delay(2_000)
            }
        }
    }

    // ── Reconnect as guest on app reopen ─────────────────────────

    private fun reconnectAsGuest() {
        viewModelScope.launch {
            try {
                val result = firebaseRepo.findRoomByGuest(playerId) ?: return@launch
                val (roomCode, status) = result

                // Read state BEFORE restoring our own isConnected
                val room          = firebaseRepo.getRoomOnce(roomCode) ?: return@launch
                val hostWasConn   = room.host.isConnected
                val bothDisconnected = !hostWasConn && !room.guest.isConnected
                        && status != "WAITING" && status != "FINISHED"

                // Restore guest isConnected
                firebaseRepo.setConnected(roomCode, "guest", true)
                firebaseRepo.registerDisconnectHandler(roomCode, "guest")

                // If both were offline — room will be reset by host on their reconnect.
                // Guest just clears their pending state.
                if (bothDisconnected) {
                    _uiState.update { it.copy(requestSent = false) }
                    return@launch
                }

                when (status) {
                    "PENDING_ACCEPT" -> {
                        // Guest had sent a request — show "waiting for host" overlay again
                        _uiState.update { it.copy(requestSent = true) }
                        sessionManager.joinRoom(
                            localId   = playerId,
                            roomCode  = roomCode,
                            localName = _uiState.value.playerName,
                            scope     = viewModelScope,
                        )
                    }
                    "PLAYING" -> {
                        // Game was in progress — reconnect session
                        sessionManager.attachGuestToExistingRoom(
                            localId   = playerId,
                            localName = _uiState.value.playerName,
                            roomCode  = roomCode,
                            boardSize = room.boardSize,
                            scope     = viewModelScope,
                        )
                    }
                }
            } catch (_: Exception) {}
        }
    }

    private fun reconnectExistingRoom(playerName: String) {
        viewModelScope.launch {
            try {
                val roomCode = firebaseRepo.findRoomCodeByHost(playerId) ?: return@launch
                val room     = firebaseRepo.getRoomOnce(roomCode) ?: return@launch

                // Check both-disconnected state BEFORE restoring our own isConnected
                val guestWasConnected = room.guest.isConnected
                val bothDisconnected  = !room.host.isConnected && !guestWasConnected
                        && room.status != "WAITING" && room.status != "FINISHED"

                // Now restore host isConnected
                firebaseRepo.setConnected(roomCode, "host", true)

                // If both were offline — reset to WAITING regardless of status
                if (bothDisconnected) {
                    firebaseRepo.resetRoomForRematch(
                        roomCode  = roomCode,
                        hostId    = room.hostId.ifEmpty { playerId },
                        hostName  = playerName,
                        guestName = "",
                        boardSize = room.boardSize,
                    )
                    sessionManager.attachToExistingRoom(
                        localId   = playerId,
                        localName = playerName,
                        roomCode  = roomCode,
                        boardSize = room.boardSize,
                        scope     = viewModelScope,
                    )
                    _uiState.update { it.copy(
                        roomCreated   = true,
                        generatedCode = roomCode,
                        tab           = LobbyTab.CREATE,
                    )}
                    return@launch
                }

                // Guest still connected and game active — don't interrupt
                val guestActive = room.status == "PLAYING" && guestWasConnected
                if (guestActive) return@launch

                val resolvedHostId = room.hostId.ifEmpty { playerId }

                if (room.status == "PENDING_ACCEPT") {
                    val guestName = room.guest.name
                    if (guestName.isNotEmpty()) {
                        _uiState.update { it.copy(
                            incomingGuestName = guestName,
                            roomCreated       = true,
                            generatedCode     = roomCode,
                            tab               = LobbyTab.CREATE,
                        )}
                    }
                    return@launch
                }

                firebaseRepo.resetRoomForRematch(
                    roomCode  = roomCode,
                    hostId    = resolvedHostId,
                    hostName  = playerName,
                    guestName = "",
                    boardSize = room.boardSize,
                )
                sessionManager.attachToExistingRoom(
                    localId   = playerId,
                    localName = playerName,
                    roomCode  = roomCode,
                    boardSize = room.boardSize,
                    scope     = viewModelScope,
                )
                _uiState.update { it.copy(
                    roomCreated   = true,
                    generatedCode = roomCode,
                    tab           = LobbyTab.CREATE,
                )}
            } catch (_: Exception) {}
        }
    }

    // ── Events ────────────────────────────────────────────────────

    fun onEvent(event: LobbyEvent) {
        when (event) {
            LobbyEvent.SelectCreateTab -> _uiState.update { it.copy(tab = LobbyTab.CREATE, error = null) }
            LobbyEvent.SelectJoinTab   -> _uiState.update { it.copy(tab = LobbyTab.JOIN,   error = null) }
            LobbyEvent.SelectBrowseTab -> _uiState.update { it.copy(tab = LobbyTab.BROWSE, error = null) }
            is LobbyEvent.BoardSizeSelected -> _uiState.update { it.copy(selectedSize = event.size) }
            is LobbyEvent.RoomNameChanged   -> _uiState.update {
                it.copy(roomNameInput = event.name.take(20), error = null, hasExistingRoom = false)
            }
            LobbyEvent.CreateRoom      -> createRoom()
            LobbyEvent.CancelRoom      -> cancelRoom()
            is LobbyEvent.JoinCodeChanged -> _uiState.update {
                it.copy(joinCodeInput = event.code.filter { c -> c.isLetterOrDigit() }.take(12).uppercase())
            }
            LobbyEvent.JoinRoom           -> joinRoom(_uiState.value.joinCodeInput.trim())
            is LobbyEvent.JoinFromBrowser -> joinRoom(event.roomCode)
            LobbyEvent.DismissError       -> _uiState.update { it.copy(error = null) }
            LobbyEvent.AcceptGuest        -> acceptGuest()
            LobbyEvent.RejectGuest        -> rejectGuest()
        }
    }

    fun onNavigationConsumed() { _navigateToGame.value = null }

    // ── Browse rooms ──────────────────────────────────────────────

    private fun observeWaitingRooms(myName: String) {
        if (browseStarted) return
        browseStarted = true
        firebaseRepo.observeWaitingRooms(myName)
            .onEach { rooms -> _uiState.update { it.copy(waitingRooms = rooms, isLoadingRooms = false) } }
            .launchIn(viewModelScope)
    }

    // ── Create room ───────────────────────────────────────────────

    private fun createRoom() {
        val state     = _uiState.value
        val playerName = state.playerName
        val roomName   = state.roomNameInput.trim()
        val boardSize  = state.selectedSize.size

        if (roomName.isEmpty()) {
            _uiState.update { it.copy(error = "Enter a room name") }
            return
        }
        _uiState.update { it.copy(isCreating = true, error = null, hasExistingRoom = false) }
        viewModelScope.launch {
            try {
                if (firebaseRepo.playerHasOpenRoom(playerId)) {
                    _uiState.update { it.copy(isCreating = false, hasExistingRoom = true) }
                    return@launch
                }
                sessionManager.createRoom(
                    localId   = playerId,
                    localName = playerName,
                    roomName  = roomName,
                    boardSize = boardSize,
                    scope     = viewModelScope,
                )
                val roomCode = sessionManager.currentSession()?.roomCode ?: ""
                // Show "Room Created" state — do NOT navigate to game yet
                _uiState.update { it.copy(
                    isCreating    = false,
                    roomCreated   = true,
                    generatedCode = roomCode,
                )}
            } catch (e: Exception) {
                _uiState.update { it.copy(isCreating = false, error = "Failed to create room. Check your connection.") }
            }
        }
    }

    private fun cancelRoom() {
        viewModelScope.launch {
            sessionManager.endSession(deleteRoom = true)
            _uiState.update { it.copy(roomCreated = false, generatedCode = "", error = null) }
        }
    }

    // ── Join room ─────────────────────────────────────────────────

    private fun joinRoom(code: String) {
        val cleaned = code.trim().uppercase()
        if (cleaned.isEmpty()) {
            _uiState.update { it.copy(error = "Enter a room code") }
            return
        }
        _uiState.update { it.copy(isJoining = true, error = null) }
        viewModelScope.launch {
            try {
                val joined = sessionManager.joinRoom(
                    localId   = playerId,
                    roomCode  = cleaned,
                    localName = _uiState.value.playerName,
                    scope     = viewModelScope,
                )
                if (joined) {
                    // RequestSent event will update UI
                    _uiState.update { it.copy(isJoining = false) }
                } else {
                    _uiState.update { it.copy(isJoining = false, error = "Room not found or already full.") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isJoining = false, error = "Failed to join room. Check your connection.") }
            }
        }
    }

    // ── Host accept/reject ────────────────────────────────────────

    private fun acceptGuest() {
        val playerName = _uiState.value.playerName
        viewModelScope.launch {
            try {
                // Find room code — either from active session or from Firebase
                val roomCode = sessionManager.currentSession()?.roomCode
                    ?: firebaseRepo.findRoomCodeByHost(playerId)
                    ?: return@launch
                val room = firebaseRepo.getRoomOnce(roomCode) ?: return@launch

                // Attach session if not already attached (happens when host was away from lobby)
                if (sessionManager.currentSession() == null) {
                    sessionManager.attachToExistingRoom(
                        localId   = playerId,
                        localName = playerName,
                        roomCode  = roomCode,
                        boardSize = room.boardSize,
                        scope     = viewModelScope,
                    )
                }

                // Set PLAYING — triggers guest's watcher to navigate
                firebaseRepo.acceptGuestJoin(roomCode)
                _uiState.update { it.copy(incomingGuestName = "", incomingRoomCode = "") }
                // GameStarted event from attachSessionObserver will trigger navigation
            } catch (_: Exception) {}
        }
    }

    private fun rejectGuest() {
        viewModelScope.launch {
            try {
                val roomCode = sessionManager.currentSession()?.roomCode
                    ?: firebaseRepo.findRoomCodeByHost(playerId)
                    ?: return@launch
                val room = firebaseRepo.getRoomOnce(roomCode) ?: return@launch
                firebaseRepo.rejectGuestJoin(
                    roomCode  = roomCode,
                    hostId    = room.hostId.ifEmpty { playerId },
                    hostName  = _uiState.value.playerName,
                    boardSize = room.boardSize,
                )
                _uiState.update { it.copy(incomingGuestName = "", incomingRoomCode = "") }
            } catch (_: Exception) {}
        }
    }

    // ── Room event handler ────────────────────────────────────────

    private fun handleRoomEvent(event: RoomEvent) {
        when (event) {
            is RoomEvent.GuestRequestJoin ->
                _uiState.update { it.copy(incomingGuestName = event.name) }
            is RoomEvent.RequestSent ->
                _uiState.update { it.copy(requestSent = true) }
            is RoomEvent.RequestRejected ->
                _uiState.update { it.copy(requestSent = false, error = "Host rejected your request.") }
            is RoomEvent.OpponentJoined ->
                _uiState.update { it.copy(opponentName = event.name) }
            is RoomEvent.GameStarted -> {
                val roomCode = sessionManager.currentSession()?.roomCode ?: return
                _uiState.update { it.copy(
                    roomCreated = false, requestSent = false,
                    incomingGuestName = "", incomingRoomCode = "",
                )}
                _navigateToGame.value = roomCode
            }
            is RoomEvent.WaitingForOpponent ->
                _uiState.update { it.copy(incomingGuestName = "", incomingRoomCode = "") }
            is RoomEvent.Error ->
                _uiState.update { it.copy(
                    isCreating = false, roomCreated = false, requestSent = false,
                    incomingGuestName = "", isJoining = false, error = event.message,
                )}
            else -> Unit
        }
    }
}