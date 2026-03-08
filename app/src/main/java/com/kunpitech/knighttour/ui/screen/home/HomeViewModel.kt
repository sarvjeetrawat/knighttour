package com.kunpitech.knighttour.ui.screen.home

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kunpitech.knighttour.data.repository.GameRepository
import com.kunpitech.knighttour.data.repository.UserPreferencesRepository
import com.kunpitech.knighttour.domain.engine.ScoreCalculator
import com.kunpitech.knighttour.data.repository.FirebaseGameRepository
import com.kunpitech.knighttour.data.online.OnlineSessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ═══════════════════════════════════════════════════════════════
//  HOME VIEWMODEL  — fully wired to repositories
// ═══════════════════════════════════════════════════════════════

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext
    private val context         : Context,
    private val gameRepository  : GameRepository,
    private val prefsRepository : UserPreferencesRepository,
    private val scoreCalculator : ScoreCalculator,
    private val firebaseRepo    : FirebaseGameRepository,
    private val sessionManager  : OnlineSessionManager,
) : ViewModel() {

    private val _uiState: MutableStateFlow<HomeUiState> = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // Expose the in-progress session ID so NavGraph can resume it
    private val _resumeSessionId = MutableStateFlow<String?>(null)
    val resumeSessionId: StateFlow<String?> = _resumeSessionId.asStateFlow()

    // Fired when host accepts — NavGraph navigates to game screen
    private val _navigateToGame = MutableStateFlow<String?>(null)
    val navigateToGame: StateFlow<String?> = _navigateToGame.asStateFlow()

    // Room watcher job — cancelled when no longer needed
    private var roomWatchJob: Job? = null

    // ConnectivityManager callback — updates isOnline in real time
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _uiState.update { it.copy(isOnline = true) }
        }
        override fun onLost(network: Network) {
            val stillOnline = connectivityManager.activeNetwork != null
            _uiState.update { it.copy(isOnline = stillOnline) }
        }
    }

    init {
        observePreferences()
        observeActiveGame()
        loadStats()
        // Refresh stats whenever a game completes
        gameRepository.getFinishedSessions(limit = 1)
            .onEach { loadStats() }
            .launchIn(viewModelScope)

        // Set initial online state from current active network
        val active = connectivityManager.activeNetwork
        val caps   = connectivityManager.getNetworkCapabilities(active)
        val hasNet = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        _uiState.update { it.copy(isOnline = hasNet) }

        // Register live connectivity callback
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, networkCallback)

        // Start watching for incoming guest requests immediately — no UI trigger needed
        startRoomWatcher()
    }

    override fun onCleared() {
        super.onCleared()
        roomWatchJob?.cancel()
        try { connectivityManager.unregisterNetworkCallback(networkCallback) } catch (_: Exception) {}
    }

    fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.SelectDifficulty ->
                _uiState.update { it.copy(selectedDifficulty = event.difficulty) }
            HomeEvent.PlayOnline ->
                Unit   // navigation handled in HomeRoute
            HomeEvent.GoToGame   -> goToGame()
            HomeEvent.RejectGame -> rejectGame()
            else -> Unit   // navigation events handled in HomeRoute
        }
    }

    // ── Called by NavGraph after navigating away to clear the trigger ──
    fun onNavigateToGameHandled() {
        _navigateToGame.value = null
    }

    // ── Start watching host's room for a guest joining ─────────────
    // Called from NavGraph / HomeRoute when player has an open room
    /**
     * Continuously watches the host's room for incoming guest requests.
     * Runs as a permanent loop — polls every 3s until a room is found, then
     * observes status in real time. If the room disappears, loops again.
     * Called once from init — no UI dependency needed.
     */
    private fun startRoomWatcher() {
        roomWatchJob?.cancel()
        roomWatchJob = viewModelScope.launch {
            val hostId = prefsRepository.getOrCreatePlayerId()
            if (hostId.isBlank()) return@launch

            while (true) {
                // Find the host's active room
                var roomCode: String? = null
                while (roomCode == null) {
                    try { roomCode = firebaseRepo.findRoomCodeByHost(hostId) } catch (_: Exception) {}
                    if (roomCode == null) delay(3_000)
                }

                val foundRoomCode = roomCode!!

                // Read current state BEFORE restoring isConnected
                val currentRoom     = try { firebaseRepo.getRoomOnce(foundRoomCode) } catch (_: Exception) { null }
                val hostWasConn     = currentRoom?.host?.isConnected  ?: false
                val guestWasConn    = currentRoom?.guest?.isConnected ?: false
                val currentStatus   = currentRoom?.status ?: "WAITING"
                val bothDisconnected = !hostWasConn && !guestWasConn
                        && currentStatus != "WAITING" && currentStatus != "FINISHED"

                // Now restore isConnected
                try { firebaseRepo.setConnected(foundRoomCode, "host", true) } catch (_: Exception) {}

                // Both were offline — reset to WAITING
                if (bothDisconnected) {
                    try {
                        firebaseRepo.resetRoomForRematch(
                            roomCode  = foundRoomCode,
                            hostId    = currentRoom?.hostId ?: "",
                            hostName  = currentRoom?.host?.name ?: "",
                            guestName = "",
                            boardSize = currentRoom?.boardSize ?: 6,
                        )
                    } catch (_: Exception) {}
                }

                // Observe status changes on this room
                var shouldLoop = false
                try {
                    firebaseRepo.observeRoomStatus(foundRoomCode)
                        .collect { status ->
                            when (status) {
                                "PENDING_ACCEPT" -> {
                                    // Read room to get guest name
                                    val room = try {
                                        firebaseRepo.getRoomOnce(foundRoomCode)
                                    } catch (_: Exception) { null }
                                    val guestName = room?.guest?.name ?: ""
                                    if (guestName.isNotEmpty()) {
                                        _uiState.update { it.copy(
                                            incomingGuestName = guestName,
                                            incomingRoomCode  = foundRoomCode,
                                        )}
                                    }
                                }
                                "WAITING" -> {
                                    _uiState.update { it.copy(
                                        incomingGuestName = "", incomingRoomCode = "",
                                    )}
                                }
                                "PLAYING" -> {
                                    // Host accepted from another screen — clear banner
                                    _uiState.update { it.copy(
                                        incomingGuestName = "", incomingRoomCode = "",
                                    )}
                                }
                                "FINISHED" -> {
                                    _uiState.update { it.copy(
                                        incomingGuestName = "", incomingRoomCode = "",
                                    )}
                                    shouldLoop = true
                                }
                            }
                        }
                } catch (_: Exception) {}

                // observeRoomStatus ended — re-poll after short delay
                _uiState.update { it.copy(incomingGuestName = "", incomingRoomCode = "") }
                delay(2_000)
            }
        }
    }

    // Called from HomeScreen ON_RESUME — restarts watcher so it picks up new rooms immediately
    fun startWatchingRoom(playerName: String) {
        startRoomWatcher()
    }

    fun stopWatchingRoom() {
        roomWatchJob?.cancel()
        roomWatchJob = null
        _uiState.update { it.copy(incomingGuestName = "", incomingRoomCode = "") }
    }


    // ── Host taps GO TO GAME — attach session and navigate ─────────
    private fun goToGame() {
        val roomCode   = _uiState.value.incomingRoomCode
        val playerName = _uiState.value.playerName
        if (roomCode.isEmpty()) return
        viewModelScope.launch {
            try {
                val room     = firebaseRepo.getRoomOnce(roomCode) ?: return@launch
                val stableId = prefsRepository.getOrCreatePlayerId()
                // Attach session first
                sessionManager.attachToExistingRoom(
                    localId   = stableId,
                    localName = playerName,
                    roomCode  = roomCode,
                    boardSize = room.boardSize,
                )
                // Set PLAYING — triggers guest's watcher to navigate too
                firebaseRepo.acceptGuestJoin(roomCode)
                _uiState.update { it.copy(incomingGuestName = "", incomingRoomCode = "") }
                stopWatchingRoom()
                _navigateToGame.value = roomCode
            } catch (_: Exception) {}
        }
    }

    private fun rejectGame() {
        val roomCode   = _uiState.value.incomingRoomCode
        val playerName = _uiState.value.playerName
        if (roomCode.isEmpty()) return
        viewModelScope.launch {
            try {
                val room = firebaseRepo.getRoomOnce(roomCode) ?: return@launch
                firebaseRepo.rejectGuestJoin(
                    roomCode  = roomCode,
                    hostId    = room.hostId.ifEmpty { prefsRepository.getOrCreatePlayerId() },
                    hostName  = playerName,
                    boardSize = room.boardSize,
                )
            } catch (_: Exception) {}
        }
        _uiState.update { it.copy(incomingGuestName = "", incomingRoomCode = "") }
    }


    // ── Observe DataStore preferences ─────────────────────────────

    private fun observePreferences() {
        prefsRepository.preferences
            .onEach { prefs ->
                val name = prefs.playerName
                _uiState.update { it.copy(playerName = name) }
            }
            .launchIn(viewModelScope)
    }

    // ── Observe in-progress game ──────────────────────────────────

    private fun observeActiveGame() {
        gameRepository.hasInProgressSession()
            .onEach { has ->
                if (has) {
                    val session = gameRepository.getLatestInProgress()
                    _resumeSessionId.value = session?.id
                    _uiState.update { state ->
                        state.copy(
                            hasActiveGame    = session != null,
                            activeDifficulty = session?.difficulty?.uiLabel ?: "",
                            activeMoveCount  = session?.moveCount ?: 0,
                        )
                    }
                } else {
                    _resumeSessionId.value = null
                    _uiState.update { it.copy(hasActiveGame = false) }
                }
            }
            .launchIn(viewModelScope)
    }

    // ── Load player stats from DB ──────────────────────────────────

    private fun loadStats() {
        viewModelScope.launch {
            val wins        = gameRepository.getTotalWins()
            val played      = gameRepository.getTotalGamesPlayed()
            val bestScore   = gameRepository.getGlobalPersonalBest() ?: 0
            val rankInfo    = scoreCalculator.rankInfo(bestScore)
            val winRate     = if (played > 0) (wins * 100) / played else 0
            val fastestSecs = gameRepository.getFastestWinSeconds()
            val bestTime    = if (fastestSecs != null) {
                "%d:%02d".format(fastestSecs / 60, fastestSecs % 60)
            } else "--:--"

            _uiState.update { state ->
                state.copy(
                    playerScore  = bestScore,
                    playerRank   = rankInfo.label,
                    gamesPlayed  = played,
                    winRate      = winRate,
                    bestTime     = bestTime,
                )
            }
        }
    }
}