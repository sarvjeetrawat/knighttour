package com.kunpitech.knighttour.ui.screen.lobby

import com.kunpitech.knighttour.data.repository.RoomBrowserEntry

// ================================================================
//  LOBBY SCREEN — UI STATE
//  File: ui/screen/lobby/LobbyUiState.kt
// ================================================================

data class LobbyUiState(
    val playerName      : String              = "Knight",
    val tab             : LobbyTab            = LobbyTab.BROWSE,

    // Board size selection (CREATE tab)
    val selectedSize    : OnlineBoardSize     = OnlineBoardSize.SIX,

    // CREATE tab
    val roomNameInput   : String              = "",
    val generatedCode   : String              = "",
    val isCreating      : Boolean             = false,
    val isWaiting       : Boolean             = false,
    val hasExistingRoom : Boolean             = false,   // true = player already has an open room

    // JOIN tab
    val joinCodeInput   : String              = "",
    val isJoining       : Boolean             = false,

    // BROWSE tab — live list of waiting rooms
    val waitingRooms    : List<RoomBrowserEntry> = emptyList(),
    val isLoadingRooms  : Boolean             = false,

    // Shared
    val opponentName    : String              = "",
    val error           : String?             = null,
)

enum class OnlineBoardSize(val label: String, val size: Int, val timeLimit: Int) {
    FIVE( "5×5",  5, 300),
    SIX(  "6×6",  6, 240),
    EIGHT("8×8",  8, 180),
}

enum class LobbyTab { CREATE, JOIN, BROWSE }

sealed interface LobbyEvent {
    data class  RoomNameChanged(val name: String)            : LobbyEvent
    data object CreateRoom                                    : LobbyEvent
    data object CancelWaiting                                 : LobbyEvent
    data class  JoinCodeChanged(val code: String)            : LobbyEvent
    data object JoinRoom                                      : LobbyEvent
    data class  JoinFromBrowser(val roomCode: String)        : LobbyEvent
    data object DismissError                                  : LobbyEvent
    data object SelectCreateTab                               : LobbyEvent
    data object SelectJoinTab                                 : LobbyEvent
    data object SelectBrowseTab                               : LobbyEvent
    data class  BoardSizeSelected(val size: OnlineBoardSize) : LobbyEvent
}