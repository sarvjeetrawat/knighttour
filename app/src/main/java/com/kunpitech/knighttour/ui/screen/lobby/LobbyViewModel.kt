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
                _uiState.update { it.copy(playerName = prefs.playerName) }
            }
        }
        sessionManager.roomEvents
            .onEach { handleRoomEvent(it) }
            .launchIn(viewModelScope)

        // Always observe waiting rooms for the browse tab
        observeWaitingRooms()
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
            LobbyEvent.CreateRoom      -> createRoom()
            LobbyEvent.CancelWaiting   -> cancelWaiting()
            is LobbyEvent.JoinCodeChanged -> _uiState.update {
                it.copy(joinCodeInput = event.code.filter { c -> c.isDigit() }.take(6))
            }
            LobbyEvent.JoinRoom              -> joinRoom(_uiState.value.joinCodeInput.trim())
            is LobbyEvent.JoinFromBrowser    -> joinRoom(event.roomCode)
            LobbyEvent.DismissError          -> _uiState.update { it.copy(error = null) }
        }
    }

    fun onNavigationConsumed() { _navigateToGame.value = null }

    // ── Room observation ─────────────────────────────────────────

    private fun observeWaitingRooms() {
        firebaseRepo.observeWaitingRooms()
            .onEach { rooms ->
                val myName = _uiState.value.playerName
                // Filter out rooms created by the local player
                _uiState.update { it.copy(
                    waitingRooms   = rooms.filter { r -> r.hostName != myName },
                    isLoadingRooms = false,
                ) }
            }
            .launchIn(viewModelScope)
    }

    // ── Create ───────────────────────────────────────────────────

    private fun createRoom() {
        val name = _uiState.value.playerName
        val code = sessionManager.generateRoomCode()
        val boardSize = _uiState.value.selectedSize.size
        _uiState.update { it.copy(isCreating = true, error = null) }
        viewModelScope.launch {
            try {
                sessionManager.createRoom(
                    roomCode  = code,
                    localName = name,
                    boardSize = boardSize,
                    scope     = viewModelScope,
                )
                _uiState.update { it.copy(isCreating = false, isWaiting = false) }
                _navigateToGame.value = code
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isCreating = false,
                        error = "Failed to create room. Check your connection.")
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
        if (code.length != 6) {
            _uiState.update { it.copy(error = "Enter a valid 6-digit code") }
            return
        }
        val name = _uiState.value.playerName
        _uiState.update { it.copy(isJoining = true, error = null) }
        viewModelScope.launch {
            try {
                val joined = sessionManager.joinRoom(
                    roomCode  = code,
                    localName = name,
                    scope     = viewModelScope,
                )
                if (joined) {
                    _uiState.update { it.copy(isJoining = false) }
                    _navigateToGame.value = code
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
            is RoomEvent.Error ->
                _uiState.update {
                    it.copy(isCreating = false, isWaiting = false,
                        isJoining = false, error = event.message)
                }
            else -> Unit
        }
    }
}