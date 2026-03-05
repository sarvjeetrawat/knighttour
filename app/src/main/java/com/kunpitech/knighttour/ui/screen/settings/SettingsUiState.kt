package com.kunpitech.knighttour.ui.screen.settings

// ===============================================================
//  SETTINGS SCREEN -- UI STATE
// ===============================================================

data class SettingsUiState(
    // Sound & Haptics
    val soundEnabled      : Boolean         = true,
    val musicEnabled      : Boolean         = true,
    val hapticsEnabled    : Boolean         = true,

    // Gameplay
    val defaultDifficulty : DefaultDiff     = DefaultDiff.MEDIUM,
    val showMoveNumbers   : Boolean         = true,
    val showValidMoves    : Boolean         = true,
    val autoHint          : Boolean         = false,

    // Display
    val boardTheme        : BoardTheme      = BoardTheme.OBSIDIAN,

    // Account
    val playerName        : String          = "Knight",
    val isSignedIn        : Boolean         = false,

    // State
    val isLoading         : Boolean         = false,
    val savedFeedback     : Boolean         = false,

    // Player name edit dialog
    val isEditingName     : Boolean         = false,
    val nameInputValue    : String          = "",
)

enum class DefaultDiff(val label: String) {
    EASY(  "5x5  EASY"),
    MEDIUM("6x6  MEDIUM"),
    HARD(  "8x8  HARD"),
    DEVIL( "DEVIL  10x10"),
}

enum class BoardTheme(val label: String, val description: String) {
    OBSIDIAN("OBSIDIAN",  "Deep black & gold (default)"),
    CRIMSON( "CRIMSON",   "Blood red & charcoal"),
    ABYSS(   "ABYSS",     "Navy & silver"),
    VOID(    "VOID",      "Pure black & white"),
}

sealed interface SettingsEvent {
    data object NavigateBack                              : SettingsEvent
    data class  SoundToggled(val enabled: Boolean)       : SettingsEvent
    data class  MusicToggled(val enabled: Boolean)       : SettingsEvent
    data class  HapticsToggled(val enabled: Boolean)     : SettingsEvent
    data class  DifficultyChanged(val diff: DefaultDiff) : SettingsEvent
    data class  MoveNumbersToggled(val enabled: Boolean) : SettingsEvent
    data class  ValidMovesToggled(val enabled: Boolean)  : SettingsEvent
    data class  AutoHintToggled(val enabled: Boolean)    : SettingsEvent
    data class  BoardThemeChanged(val theme: BoardTheme) : SettingsEvent
    data object SignIn                                    : SettingsEvent
    data object SignOut                                   : SettingsEvent
    data object ClearData                                 : SettingsEvent
    data object EditPlayerName                            : SettingsEvent
    data class  NameInputChanged(val value: String)      : SettingsEvent
    data object SavePlayerName                            : SettingsEvent
    data object DismissNameDialog                         : SettingsEvent
}