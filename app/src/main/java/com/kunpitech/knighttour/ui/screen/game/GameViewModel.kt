package com.kunpitech.knighttour.ui.screen.game

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kunpitech.knighttour.data.online.OnlineRole
import com.kunpitech.knighttour.data.online.OnlineSessionManager
import com.kunpitech.knighttour.data.online.RoomEvent
import com.kunpitech.knighttour.domain.model.Difficulty
import com.kunpitech.knighttour.domain.model.GameMode
import com.kunpitech.knighttour.domain.model.GameSession
import com.kunpitech.knighttour.domain.model.Position
import com.kunpitech.knighttour.domain.model.SessionStatus
import com.kunpitech.knighttour.domain.usecase.GetHintUseCase
import com.kunpitech.knighttour.domain.usecase.MakeMoveUseCase
import com.kunpitech.knighttour.domain.usecase.StartGameUseCase
import com.kunpitech.knighttour.domain.usecase.TickTimerUseCase
import com.kunpitech.knighttour.domain.usecase.UndoMoveUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ===============================================================
//  GAME VIEWMODEL  (thin layer over domain UseCases)
//
//  Responsibilities:
//    - Read nav args from SavedStateHandle
//    - Drive the 1-second timer coroutine
//    - Map domain GameSession to GameUiState
//    - Forward events to the appropriate UseCase
//    - Hold the shakeCell animation trigger
//
//  All game logic lives in domain/usecase/*.
// ===============================================================

@HiltViewModel
class GameViewModel @Inject constructor(
    private val savedStateHandle  : SavedStateHandle,
    private val startGame         : StartGameUseCase,
    private val makeMove          : MakeMoveUseCase,
    private val undoMove          : UndoMoveUseCase,
    private val getHint           : GetHintUseCase,
    private val tickTimer         : TickTimerUseCase,
    private val gameRepository    : com.kunpitech.knighttour.data.repository.GameRepository,
    private val scoreRepository   : com.kunpitech.knighttour.data.repository.ScoreRepository,
    private val prefsRepository   : com.kunpitech.knighttour.data.repository.UserPreferencesRepository,
    private val sessionManager    : OnlineSessionManager,
) : ViewModel() {

    private val _uiState: MutableStateFlow<GameUiState> =
        MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    // Exposed so GameRoute can pass the real session ID to the Result screen
    private val _currentSessionId = MutableStateFlow("")
    val currentSessionId: StateFlow<String> = _currentSessionId.asStateFlow()

    private var session: GameSession? = null
    private var timerJob: Job? = null

    init {
        val sessionId = savedStateHandle.get<String>("sessionId") ?: ""
        val diffStr   = savedStateHandle.get<String>("difficulty") ?: "MEDIUM"
        val modeStr   = savedStateHandle.get<String>("gameMode")   ?: "OFFLINE"
        val roomCode  = savedStateHandle.get<String>("roomCode")   ?: ""

        val mode: GameMode = GameMode.entries
            .find { it.name == modeStr } ?: GameMode.OFFLINE

        val difficulty: Difficulty = if (mode == GameMode.ONLINE) {
            // For online games, board size comes from Firebase room via OnlineSessionManager
            val onlineSize = sessionManager.currentSession()?.boardSize ?: 6
            when (onlineSize) {
                5    -> Difficulty.EASY
                8    -> Difficulty.HARD
                10   -> Difficulty.DEVIL
                else -> Difficulty.MEDIUM   // 6×6
            }
        } else {
            Difficulty.entries.find { it.name == diffStr } ?: Difficulty.MEDIUM
        }

        if (sessionId.isNotEmpty()) {
            resumeSession(sessionId, difficulty, mode, roomCode)
        } else {
            newGame(difficulty, mode, roomCode)
        }

        // Observe DataStore preferences and apply live to game UI
        viewModelScope.launch {
            prefsRepository.preferences.collect { prefs ->
                _uiState.update { state ->
                    state.copy(
                        boardTheme      = prefs.boardTheme.name,
                        showMoveNumbers = prefs.showMoveNumbers,
                        showValidMoves  = prefs.showValidMoves,
                    )
                }
            }
        }

        // Online: observe opponent state from Firebase
        if (mode == GameMode.ONLINE) {
            // Both host and guest start paused — GameStarted/OpponentJoined triggers play
            _uiState.update { it.copy(
                waitingForOpponent = true,
                gameState = GamePhase.PAUSED,
            ) }
            // Re-start opponent observation in THIS scope — lobby scope may be cancelled
            sessionManager.startObservingOpponent(viewModelScope)
            observeOnlineOpponent()
            observeRoomEvents()
        }
    }

    // ============================================================
    //  PUBLIC EVENT HANDLER
    // ============================================================

    fun onEvent(event: GameEvent) {
        when (event) {
            is GameEvent.CellTapped -> handleCellTap(event.row, event.col)
            GameEvent.Undo          -> handleUndo()
            GameEvent.Hint          -> handleHint()
            GameEvent.Pause         -> handlePause()
            GameEvent.Resume        -> handleResume()
            GameEvent.Restart       -> session?.let {
                newGame(it.difficulty, it.mode, it.roomCode)
            }
        }
    }

    // ============================================================
    //  GAME LIFECYCLE
    // ============================================================

    private fun newGame(difficulty: Difficulty, mode: GameMode, roomCode: String) {
        timerJob?.cancel()
        val s = startGame(difficulty, mode, roomCode)
        session = s
        _currentSessionId.value = s.id
        _uiState.value = s.toUiState()
        autoSave(s)
        startTimer()
    }

    private fun resumeSession(
        sessionId  : String,
        difficulty : Difficulty,
        mode       : GameMode,
        roomCode   : String,
    ) {
        viewModelScope.launch {
            val existing = gameRepository.getById(sessionId)
            if (existing != null && !existing.isFinished) {
                session = existing
                _currentSessionId.value = existing.id
                _uiState.value = existing.toUiState()
                startTimer()
            } else {
                // Session not found or already finished — start fresh
                newGame(difficulty, mode, roomCode)
            }
        }
    }

    // Auto-save session to Room after every meaningful state change
    private fun autoSave(s: GameSession) {
        viewModelScope.launch {
            gameRepository.save(s)
        }
    }

    // ============================================================
    //  MOVE
    // ============================================================

    private fun handleCellTap(row: Int, col: Int) {
        val current = session ?: return
        if (_uiState.value.gameState == GamePhase.PAUSED) return

        when (val result = makeMove(current, Position(row, col))) {
            is MakeMoveUseCase.MoveResult.Accepted -> {
                session = result.session
                _uiState.value = result.session.toUiState()
                autoSave(result.session)
                pushOnlineMove(result.session)
            }
            is MakeMoveUseCase.MoveResult.Victory -> {
                timerJob?.cancel()
                session = result.session
                _uiState.value = result.session.toUiState()
                // Snapshot session immediately — before async calls clear it
                if (_uiState.value.isOnlineMode) sessionManager.snapshotLastSession()
                viewModelScope.launch {
                    gameRepository.save(result.session)
                    pushOnlineMove(result.session)
                    sessionManager.markLocalFinished(
                        finalScore  = result.session.score,
                        moveCount   = result.session.moveCount,
                        isCompleted = true,
                    )
                    sessionManager.endSession()
                }
            }
            is MakeMoveUseCase.MoveResult.DeadEnd -> {
                timerJob?.cancel()
                session = result.session
                _uiState.value = result.session.toUiState()
                autoSave(result.session)
                if (_uiState.value.isOnlineMode) {
                    // Online: save score and WAIT — don't navigate until opponent finishes
                    sessionManager.snapshotLastSession()
                    _uiState.update { it.copy(gameState = GamePhase.WAITING_FOR_OPPONENT) }
                    viewModelScope.launch {
                        sessionManager.markLocalFinished(
                            finalScore  = result.session.score,
                            moveCount   = result.session.moveCount,
                            isCompleted = false,
                        )
                        // Don't call endSession() yet — wait for OpponentFinished event
                    }
                }
                // Offline: toUiState() already sets FAILED, nothing more needed
            }
            is MakeMoveUseCase.MoveResult.Invalid -> {
                if (result.reason == MakeMoveUseCase.InvalidReason.NOT_L_MOVE) {
                    triggerShake(row, col)
                }
            }
            MakeMoveUseCase.MoveResult.GameOver -> Unit
        }
    }


    /** Build a CellState grid for the opponent board from their move history. */
    private fun buildOpponentCells(
        size    : Int,
        history : List<com.kunpitech.knighttour.domain.model.Position>,
    ): List<CellState> {
        val cells = Array(size * size) { idx ->
            CellState(row = idx / size, col = idx % size)
        }
        history.forEachIndexed { i, pos ->
            val idx = pos.row * size + pos.col
            if (idx in cells.indices) {
                cells[idx] = cells[idx].copy(
                    isVisited  = true,
                    isKnight   = (i == history.lastIndex),
                    moveNumber = i + 1,
                )
            }
        }
        return cells.toList()
    }

    private fun triggerShake(row: Int, col: Int) {
        _uiState.update { it.copy(shakeCell = row to col) }
        viewModelScope.launch {
            delay(420)
            _uiState.update { it.copy(shakeCell = null) }
        }
    }

    // ============================================================
    //  UNDO
    // ============================================================

    private fun handleUndo() {
        val current = session ?: return
        when (val result = undoMove(current)) {
            is UndoMoveUseCase.UndoResult.Success -> {
                session = result.session
                _uiState.value = result.session.toUiState()
            }
            UndoMoveUseCase.UndoResult.NothingToUndo -> Unit
            UndoMoveUseCase.UndoResult.GameOver      -> Unit
        }
    }

    // ============================================================
    //  HINT
    // ============================================================

    private fun handleHint() {
        val current = session ?: return
        when (val result = getHint(current)) {
            is GetHintUseCase.HintResult.Success -> {
                session = result.session
                val hintPos = result.position
                _uiState.update { state ->
                    state.copy(
                        cells = state.cells.map { cell ->
                            cell.copy(
                                isHint = cell.row == hintPos.row && cell.col == hintPos.col,
                            )
                        },
                        hintsRemaining = current.difficulty.startingHints - result.session.hintsUsed,
                        currentScore   = result.session.score,
                    )
                }
            }
            GetHintUseCase.HintResult.NoHintsLeft      -> Unit
            GetHintUseCase.HintResult.NoKnight         -> Unit
            GetHintUseCase.HintResult.NoMovesAvailable -> Unit
            GetHintUseCase.HintResult.GameOver         -> Unit
        }
    }

    // ============================================================
    //  PAUSE / RESUME
    // ============================================================

    private fun handlePause() {
        timerJob?.cancel()
        _uiState.update { it.copy(gameState = GamePhase.PAUSED) }
    }

    private fun handleResume() {
        _uiState.update { it.copy(gameState = GamePhase.PLAYING) }
        startTimer()
    }

    // ============================================================
    //  TIMER
    // ============================================================

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                val current: GameSession = session ?: break
                if (_uiState.value.gameState != GamePhase.PLAYING) break

                when (val tick = tickTimer(current)) {
                    is TickTimerUseCase.TickResult.Tick -> {
                        session = tick.session
                        _uiState.update { state ->
                            state.copy(
                                elapsedSeconds = tick.session.elapsedSeconds,
                                isTimerPanic   = tick.isTimerPanic,
                                currentScore   = tick.session.score,
                            )
                        }
                    }
                    is TickTimerUseCase.TickResult.TimeUp -> {
                        session = tick.session
                        autoSave(tick.session)
                        if (_uiState.value.isOnlineMode) {
                            sessionManager.snapshotLastSession()
                            _uiState.update { it.copy(gameState = GamePhase.WAITING_FOR_OPPONENT) }
                            viewModelScope.launch {
                                sessionManager.markLocalFinished(
                                    finalScore  = tick.session.score,
                                    moveCount   = tick.session.moveCount,
                                    isCompleted = false,
                                )
                                // Don't endSession — wait for OpponentFinished
                            }
                        } else {
                            _uiState.update { it.copy(gameState = GamePhase.FAILED) }
                        }
                        break
                    }
                    TickTimerUseCase.TickResult.GameOver -> break
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }

    // ============================================================
    //  SESSION -> UI STATE MAPPING
    // ============================================================

    // ============================================================
    //  ONLINE SYNC
    // ============================================================

    /** Push the latest move to Firebase (no-op if offline). */
    private fun pushOnlineMove(s: GameSession) {
        if (!_uiState.value.isOnlineMode) return
        val knightPos = s.board.knightPosition ?: return
        val history   = s.board.moveHistory.map { it }
        viewModelScope.launch {
            sessionManager.pushMove(knightPos, history)
        }
    }

    /** Observe opponent's move count and update the opponent progress bar. */
    private fun observeOnlineOpponent() {
        sessionManager.opponentState
            .onEach { state ->
                state ?: return@onEach
                val opp       = sessionManager.currentSession() ?: return@onEach
                val totalCells = opp.boardSize * opp.boardSize
                val progress  = state.moveCount.toFloat() / totalCells.toFloat()
                _uiState.update { it.copy(
                    opponentName     = state.name.ifEmpty { opp.opponentName },
                    opponentMoves    = state.moveCount,
                    opponentProgress = progress.coerceIn(0f, 1f),
                ) }
            }
            .launchIn(viewModelScope)
    }

    /** Observe room-level events: opponent disconnected / game finished. */
    private fun observeRoomEvents() {
        sessionManager.roomEvents
            .onEach { event ->
                when (event) {
                    is RoomEvent.OpponentJoined -> {
                        // Host was waiting — opponent arrived, start the game
                        _uiState.update { it.copy(
                            waitingForOpponent = false,
                            opponentName       = event.name,
                            gameState          = GamePhase.PLAYING,
                        ) }
                        startTimer()
                    }
                    is RoomEvent.GameStarted -> {
                        // Guest receives this — start playing immediately
                        val isGuest = sessionManager.localRole() == OnlineRole.GUEST
                        if (isGuest && _uiState.value.gameState != GamePhase.PLAYING) {
                            _uiState.update { it.copy(
                                waitingForOpponent = false,
                                gameState          = GamePhase.PLAYING,
                            ) }
                            startTimer()
                        }
                    }
                    is RoomEvent.OpponentDisconnected ->
                        _uiState.update { it.copy(opponentName = "${it.opponentName} (disconnected)") }
                    is RoomEvent.OpponentReconnected  ->
                        _uiState.update { it.copy(
                            opponentName = sessionManager.currentSession()?.opponentName ?: it.opponentName
                        ) }
                    is RoomEvent.OpponentMoved -> {
                        // Update live opponent board for waiting overlay
                        val size  = sessionManager.currentSession()?.boardSize ?: 6
                        val cells = buildOpponentCells(size, event.history)
                        _uiState.update { it.copy(
                            opponentMoves    = event.moveCount,
                            opponentProgress = event.moveCount.toFloat() / (size * size),
                            opponentCells    = cells,
                        ) }
                    }
                    is RoomEvent.OpponentFinished -> {
                        val myPhase = _uiState.value.gameState
                        if (event.isCompleted) {
                            // Opponent completed the board
                            when (myPhase) {
                                GamePhase.PLAYING, GamePhase.PAUSED -> {
                                    // Interrupt our game
                                    val current = session
                                    sessionManager.snapshotLastSession()
                                    viewModelScope.launch {
                                        sessionManager.markLocalFinished(
                                            finalScore  = current?.score ?: 0,
                                            moveCount   = current?.moveCount ?: 0,
                                            isCompleted = false,
                                        )
                                        sessionManager.endSession()
                                    }
                                    _uiState.update { it.copy(gameState = GamePhase.OPPONENT_FINISHED) }
                                }
                                GamePhase.WAITING_FOR_OPPONENT -> {
                                    // We were stuck waiting — opponent won, navigate now
                                    viewModelScope.launch { sessionManager.endSession() }
                                    _uiState.update { it.copy(gameState = GamePhase.OPPONENT_FINISHED) }
                                }
                                else -> Unit
                            }
                        } else {
                            // Opponent got stuck (isCompleted=false)
                            when (myPhase) {
                                GamePhase.PLAYING -> {
                                    // We're still playing — show banner, continue
                                    _uiState.update { it.copy(opponentFinished = true) }
                                }
                                GamePhase.WAITING_FOR_OPPONENT -> {
                                    // Both stuck — both done, navigate to result
                                    viewModelScope.launch { sessionManager.endSession() }
                                    _uiState.update { it.copy(gameState = GamePhase.FAILED) }
                                }
                                else -> Unit
                            }
                        }
                    }
                    RoomEvent.GameFinished -> {
                        // Fallback safety net
                        val myPhase = _uiState.value.gameState
                        when (myPhase) {
                            GamePhase.PLAYING, GamePhase.PAUSED -> {
                                val current = session
                                sessionManager.snapshotLastSession()
                                viewModelScope.launch {
                                    sessionManager.markLocalFinished(
                                        finalScore = current?.score ?: 0,
                                        moveCount  = current?.moveCount ?: 0,
                                    )
                                    sessionManager.endSession()
                                }
                                _uiState.update { it.copy(gameState = GamePhase.OPPONENT_FINISHED) }
                            }
                            GamePhase.WAITING_FOR_OPPONENT -> {
                                viewModelScope.launch { sessionManager.endSession() }
                                _uiState.update { it.copy(gameState = GamePhase.FAILED) }
                            }
                            else -> Unit
                        }
                    }
                    else -> Unit
                }
            }
            .launchIn(viewModelScope)
    }

    /** Call when leaving the game screen — cleans up Firebase room. */
    fun onLeaveOnlineGame() {
        if (_uiState.value.isOnlineMode) {
            val current = session
            viewModelScope.launch {
                if (current != null) {
                    sessionManager.markLocalFinished(
                        finalScore = current.score,
                        moveCount  = current.moveCount,
                    )
                }
                sessionManager.endSession()
            }
        }
    }

    private fun GameSession.toUiState(): GameUiState {
        val knightPos = board.knightPosition

        // Compute valid moves from current knight position
        val validSet: Set<Pair<Int, Int>> = if (knightPos != null) {
            val offsets = listOf(-2 to -1, -2 to 1, -1 to -2, -1 to 2, 1 to -2, 1 to 2, 2 to -1, 2 to 1)
            offsets
                .map { (dr, dc) -> (knightPos.row + dr) to (knightPos.col + dc) }
                .filter { (r, c) ->
                    r in 0 until board.size &&
                            c in 0 until board.size &&
                            !board.cellAt(r, c).isVisited
                }
                .toSet()
        } else emptySet()

        val uiCells: List<CellState> = board.cells.map { cell ->
            CellState(
                row         = cell.row,
                col         = cell.col,
                isVisited   = cell.isVisited,
                isKnight    = knightPos?.row == cell.row && knightPos?.col == cell.col,
                isValidMove = (cell.row to cell.col) in validSet,
                moveNumber  = cell.visitOrder,
            )
        }

        val gamePhase: GamePhase = when (status) {
            SessionStatus.IN_PROGRESS  -> GamePhase.PLAYING
            SessionStatus.COMPLETED    -> GamePhase.COMPLETED
            SessionStatus.FAILED_TIME,
            SessionStatus.FAILED_STUCK,
            SessionStatus.ABANDONED    -> GamePhase.FAILED
        }

        val gameModeUi: GameModeUi = when (mode) {
            GameMode.OFFLINE -> GameModeUi.OFFLINE
            GameMode.ONLINE  -> GameModeUi.ONLINE
            GameMode.DEVIL   -> GameModeUi.DEVIL
        }

        val difficultyUi: DifficultyUi = when (difficulty) {
            Difficulty.EASY   -> DifficultyUi.EASY
            Difficulty.MEDIUM -> DifficultyUi.MEDIUM
            Difficulty.HARD   -> DifficultyUi.HARD
            Difficulty.DEVIL  -> DifficultyUi.DEVIL
        }

        return GameUiState(
            boardSize        = board.size,
            cells            = uiCells,
            moveCount        = moveCount,
            totalCells       = totalCells,
            elapsedSeconds   = elapsedSeconds,
            timeLimitSeconds = difficulty.timeLimitSeconds,
            gameState        = gamePhase,
            gameMode         = gameModeUi,
            difficulty       = difficultyUi,
            canUndo          = board.moveHistory.size > 1,
            hintsRemaining   = difficulty.startingHints - hintsUsed,
            currentScore     = score,
            isOnlineMode     = mode == GameMode.ONLINE,
            roomCode         = roomCode,
            // Preserve preference values — never reset on game state updates
            boardTheme       = _uiState.value.boardTheme,
            showMoveNumbers  = _uiState.value.showMoveNumbers,
            showValidMoves   = _uiState.value.showValidMoves,
        )
    }
}