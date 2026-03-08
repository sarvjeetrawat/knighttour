package com.kunpitech.knighttour.ui.screen.lobby

import com.kunpitech.knighttour.data.repository.RoomBrowserEntry

data class LobbyUiState(
    val playerName      : String              = "Knight",
    val tab             : LobbyTab            = LobbyTab.BROWSE,

    // Board size selection (CREATE tab)
    val selectedSize    : OnlineBoardSize     = OnlineBoardSize.SIX,

    // CREATE tab
    val roomNameInput   : String              = "",
    val generatedCode   : String              = "",   // room code shown after creation
    val isCreating      : Boolean             = false,
    val roomCreated     : Boolean             = false, // true = show "Room Created" state
    val hasExistingRoom : Boolean             = false,

    // JOIN tab
    val joinCodeInput   : String              = "",
    val isJoining       : Boolean             = false,
    val requestSent     : Boolean             = false, // true = waiting for host accept/reject

    // BROWSE tab
    val waitingRooms    : List<RoomBrowserEntry> = emptyList(),
    val isLoadingRooms  : Boolean             = false,

    // Shared
    val opponentName    : String              = "",
    val error           : String?             = null,

    // Host: incoming request from guest (shown as popup)
    val incomingGuestName : String            = "",
    val incomingRoomCode  : String            = "",
) {
    val hasIncomingGuest: Boolean get() = incomingGuestName.isNotEmpty()
}

enum class OnlineBoardSize(val label: String, val size: Int, val timeLimit: Int) {
    FIVE( "5×5",  5, 300),
    SIX(  "6×6",  6, 240),
    EIGHT("8×8",  8, 180),
}

enum class LobbyTab { CREATE, JOIN, BROWSE }

sealed interface LobbyEvent {
    data class  RoomNameChanged(val name: String)            : LobbyEvent
    data object CreateRoom                                    : LobbyEvent
    data object CancelRoom                                    : LobbyEvent  // delete created room
    data class  JoinCodeChanged(val code: String)            : LobbyEvent
    data object JoinRoom                                      : LobbyEvent
    data class  JoinFromBrowser(val roomCode: String)        : LobbyEvent
    data object DismissError                                  : LobbyEvent
    data object SelectCreateTab                               : LobbyEvent
    data object SelectJoinTab                                 : LobbyEvent
    data object SelectBrowseTab                               : LobbyEvent
    data class  BoardSizeSelected(val size: OnlineBoardSize) : LobbyEvent
    data object AcceptGuest                                   : LobbyEvent
    data object RejectGuest                                   : LobbyEvent
}