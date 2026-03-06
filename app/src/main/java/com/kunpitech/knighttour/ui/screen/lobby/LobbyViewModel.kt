package com.kunpitech.knighttour.ui.screen.lobby

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kunpitech.knighttour.data.online.OnlineSessionManager
import com.kunpitech.knighttour.data.online.RoomEvent
import com.kunpitech.knighttour.data.repository.FirebaseGameRepository
import com.kunpitech.knighttour.data.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ================================================================
//  LOBBY VIEW MODEL
//  File: ui/screen/lobby/LobbyViewModel.kt
// ================================================================

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

    init {
        viewModelScope.launch {
            prefsRepository.preferences.collect { prefs ->
                val name = prefs.playerName
                _uiState.update { it.copy(playerName = name) }

                if (name.isNotEmpty()) {
                    observeWaitingRooms(name)
                    if (!reconnectDone) {
                        reconnectDone = true
                        reconnectExistingRoom(name)
                    }
                }
            }
        }
        sessionManager.roomEvents
            .onEach { handleRoomEvent(it) }
            .launchIn(viewModelScope)
    }

    /** If this player already has a room, reset it to WAITING then attach observer so host navigates when guest joins. */
    private fun reconnectExistingRoom(playerName: String) {
        viewModelScope.launch {
            try {
                val roomCode = firebaseRepo.findRoomCodeByHost(playerName) ?: return@launch
                val room     = firebaseRepo.getRoomOnce(roomCode) ?: return@launch

                // Don't interrupt an active game
                val guestActive = room.status == "PLAYING" && room.guest.isConnected
                if (guestActive) return@launch

                // Reset room to WAITING in Firebase first
                firebaseRepo.resetRoomForRematch(
                    roomCode  = roomCode,
                    hostId    = room.hostId,
                    hostName  = playerName,
                    guestName = "",
                    boardSize = room.boardSize,
                )

                // Attach session + observer WITHOUT resetting Firebase again
                // so host auto-navigates when a guest joins from browse
                sessionManager.attachToExistingRoom(
                    localName = playerName,
                    roomCode  = roomCode,
                    boardSize = room.boardSize,
                    scope     = viewModelScope,
                )
            } catch (_: Exception) {}
        }
    }

    fun onEvent(event: LobbyEvent) {
        when (event) {
            LobbyEvent.SelectCreateTab -> _uiState.update {
                it.copy(tab = LobbyTab.CREATE, error = null)
            }
            LobbyEvent.SelectJoinTab   -> _uiState.update {
                it.copy(tab = LobbyTab.JOIN, error = null)
            }
            LobbyEvent.SelectBrowseTab -> _uiState.update {
                it.copy(tab = LobbyTab.BROWSE, error = null)
            }
            is LobbyEvent.BoardSizeSelected -> _uiState.update {
                it.copy(selectedSize = event.size)
            }
            is LobbyEvent.RoomNameChanged -> _uiState.update {
                it.copy(roomNameInput = event.name.take(20), error = null, hasExistingRoom = false)
            }
            LobbyEvent.CreateRoom      -> createRoom()
            LobbyEvent.CancelWaiting   -> cancelWaiting()
            is LobbyEvent.JoinCodeChanged -> _uiState.update {
                it.copy(joinCodeInput = event.code.filter { c -> c.isLetterOrDigit() }.take(12).uppercase())
            }
            LobbyEvent.JoinRoom              -> joinRoom(_uiState.value.joinCodeInput.trim())
            is LobbyEvent.JoinFromBrowser    -> joinRoom(event.roomCode)
            LobbyEvent.DismissError          -> _uiState.update { it.copy(error = null) }
        }
    }

    fun onNavigationConsumed() { _navigateToGame.value = null }

    // ── Room observation ─────────────────────────────────────────

    private var browseStarted   = false
    private var reconnectDone   = false

    private fun observeWaitingRooms(myName: String) {
        if (browseStarted) return
        browseStarted = true
        firebaseRepo.observeWaitingRooms(myName)
            .onEach { rooms ->
                _uiState.update { it.copy(
                    waitingRooms   = rooms,
                    isLoadingRooms = false,
                ) }
            }
            .launchIn(viewModelScope)
    }

    // ── Create ───────────────────────────────────────────────────

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
                // Check if player already has an open room
                if (firebaseRepo.playerHasOpenRoom(playerName)) {
                    _uiState.update {
                        it.copy(isCreating = false, hasExistingRoom = true)
                    }
                    return@launch
                }

                sessionManager.createRoom(
                    localName = playerName,
                    roomName  = roomName,
                    boardSize = boardSize,
                    scope     = viewModelScope,
                )
                val roomCode = sessionManager.currentSession()?.roomCode ?: ""
                _uiState.update { it.copy(isCreating = false, isWaiting = false) }
                _navigateToGame.value = roomCode
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isCreating = false, error = "Failed to create room. Check your connection.")
                }
            }
        }
    }

    private fun cancelWaiting() {
        viewModelScope.launch {
            sessionManager.endSession()
            _uiState.update { it.copy(isWaiting = false, generatedCode = "", error = null) }
        }
    }

    // ── Join ─────────────────────────────────────────────────────

    private fun joinRoom(code: String) {
        val cleaned = code.trim().uppercase()
        if (cleaned.isEmpty()) {
            _uiState.update { it.copy(error = "Enter a room code") }
            return
        }
        val name = _uiState.value.playerName
        _uiState.update { it.copy(isJoining = true, error = null) }
        viewModelScope.launch {
            try {
                val joined = sessionManager.joinRoom(
                    roomCode  = cleaned,
                    localName = name,
                    scope     = viewModelScope,
                )
                if (joined) {
                    _uiState.update { it.copy(isJoining = false) }
                    _navigateToGame.value = cleaned
                } else {
                    _uiState.update {
                        it.copy(isJoining = false,
                            error = "Room not found or already full.")
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isJoining = false,
                        error = "Failed to join room. Check your connection.")
                }
            }
        }
    }

    // ── Room events ──────────────────────────────────────────────

    private fun handleRoomEvent(event: RoomEvent) {
        when (event) {
            is RoomEvent.OpponentJoined ->
                _uiState.update { it.copy(opponentName = event.name) }
            is RoomEvent.GameStarted -> {
                // Guest joined and game is ready — navigate host to game screen
                val roomCode = sessionManager.currentSession()?.roomCode ?: return
                _navigateToGame.value = roomCode
            }
            is RoomEvent.Error ->
                _uiState.update {
                    it.copy(isCreating = false, isWaiting = false,
                        isJoining = false, error = event.message)
                }
            else -> Unit
        }
    }
}