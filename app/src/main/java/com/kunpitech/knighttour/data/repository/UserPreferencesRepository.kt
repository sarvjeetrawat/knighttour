package com.kunpitech.knighttour.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.kunpitech.knighttour.ui.screen.settings.BoardTheme
import com.kunpitech.knighttour.ui.screen.settings.DefaultDiff
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

// ===============================================================
//  USER PREFERENCES REPOSITORY
//
//  Jetpack DataStore (Preferences) — no SQL, no migrations.
//  Persists all Settings screen values between app launches.
//
//  Package: data/repository/UserPreferencesRepository.kt
// ===============================================================

private val Context.dataStore: DataStore<Preferences>
        by preferencesDataStore(name = "user_preferences")

data class UserPreferences(
    val soundEnabled      : Boolean     = true,
    val musicEnabled      : Boolean     = true,
    val hapticsEnabled    : Boolean     = true,
    val defaultDifficulty : DefaultDiff = DefaultDiff.MEDIUM,
    val showMoveNumbers   : Boolean     = true,
    val showValidMoves    : Boolean     = true,
    val autoHint          : Boolean     = false,
    val boardTheme        : BoardTheme  = BoardTheme.OBSIDIAN,
    val playerName        : String      = "Knight",
    val isSignedIn        : Boolean     = false,
    // Stable device-local ID — generated once, never changes
    // Used as hostId/guestId in Firebase so rooms are always owned by the same identity
    val playerId          : String      = "",
)

interface UserPreferencesRepository {
    val preferences: Flow<UserPreferences>
    suspend fun setSoundEnabled(enabled: Boolean)
    suspend fun setMusicEnabled(enabled: Boolean)
    suspend fun setHapticsEnabled(enabled: Boolean)
    suspend fun setDefaultDifficulty(diff: DefaultDiff)
    suspend fun setShowMoveNumbers(enabled: Boolean)
    suspend fun setShowValidMoves(enabled: Boolean)
    suspend fun setAutoHint(enabled: Boolean)
    suspend fun setBoardTheme(theme: BoardTheme)
    suspend fun setPlayerName(name: String)
    suspend fun setSignedIn(signedIn: Boolean)
    suspend fun clearAll()
    /** Returns the stable player ID, generating and persisting one if not yet set. */
    suspend fun getOrCreatePlayerId(): String
}

// ── Implementation ───────────────────────────────────────────────

@Singleton
class UserPreferencesRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : UserPreferencesRepository {

    private object Keys {
        val SOUND_ENABLED       = booleanPreferencesKey("sound_enabled")
        val MUSIC_ENABLED       = booleanPreferencesKey("music_enabled")
        val HAPTICS_ENABLED     = booleanPreferencesKey("haptics_enabled")
        val DEFAULT_DIFFICULTY  = stringPreferencesKey("default_difficulty")
        val SHOW_MOVE_NUMBERS   = booleanPreferencesKey("show_move_numbers")
        val SHOW_VALID_MOVES    = booleanPreferencesKey("show_valid_moves")
        val AUTO_HINT           = booleanPreferencesKey("auto_hint")
        val BOARD_THEME         = stringPreferencesKey("board_theme")
        val PLAYER_NAME         = stringPreferencesKey("player_name")
        val IS_SIGNED_IN        = booleanPreferencesKey("is_signed_in")
        val PLAYER_ID           = stringPreferencesKey("player_id")
    }

    override val preferences: Flow<UserPreferences> =
        context.dataStore.data.map { prefs ->
            UserPreferences(
                soundEnabled      = prefs[Keys.SOUND_ENABLED]      ?: true,
                musicEnabled      = prefs[Keys.MUSIC_ENABLED]      ?: true,
                hapticsEnabled    = prefs[Keys.HAPTICS_ENABLED]    ?: true,
                defaultDifficulty = DefaultDiff.entries.find {
                    it.name == prefs[Keys.DEFAULT_DIFFICULTY]
                } ?: DefaultDiff.MEDIUM,
                showMoveNumbers   = prefs[Keys.SHOW_MOVE_NUMBERS]  ?: true,
                showValidMoves    = prefs[Keys.SHOW_VALID_MOVES]   ?: true,
                autoHint          = prefs[Keys.AUTO_HINT]          ?: false,
                boardTheme        = BoardTheme.entries.find {
                    it.name == prefs[Keys.BOARD_THEME]
                } ?: BoardTheme.OBSIDIAN,
                playerName        = prefs[Keys.PLAYER_NAME]        ?: "Knight",
                isSignedIn        = prefs[Keys.IS_SIGNED_IN]       ?: false,
                playerId          = prefs[Keys.PLAYER_ID]          ?: "",
            )
        }

    override suspend fun getOrCreatePlayerId(): String {
        val existing = context.dataStore.data.first()[Keys.PLAYER_ID]
        if (!existing.isNullOrEmpty()) return existing
        val newId = UUID.randomUUID().toString()
        edit { it[Keys.PLAYER_ID] = newId }
        return newId
    }

    override suspend fun setSoundEnabled(enabled: Boolean) =
        edit { it[Keys.SOUND_ENABLED] = enabled }

    override suspend fun setMusicEnabled(enabled: Boolean) =
        edit { it[Keys.MUSIC_ENABLED] = enabled }

    override suspend fun setHapticsEnabled(enabled: Boolean) =
        edit { it[Keys.HAPTICS_ENABLED] = enabled }

    override suspend fun setDefaultDifficulty(diff: DefaultDiff) =
        edit { it[Keys.DEFAULT_DIFFICULTY] = diff.name }

    override suspend fun setShowMoveNumbers(enabled: Boolean) =
        edit { it[Keys.SHOW_MOVE_NUMBERS] = enabled }

    override suspend fun setShowValidMoves(enabled: Boolean) =
        edit { it[Keys.SHOW_VALID_MOVES] = enabled }

    override suspend fun setAutoHint(enabled: Boolean) =
        edit { it[Keys.AUTO_HINT] = enabled }

    override suspend fun setBoardTheme(theme: BoardTheme) =
        edit { it[Keys.BOARD_THEME] = theme.name }

    override suspend fun setPlayerName(name: String) =
        edit { it[Keys.PLAYER_NAME] = name }

    override suspend fun setSignedIn(signedIn: Boolean) =
        edit { it[Keys.IS_SIGNED_IN] = signedIn }

    override suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }

    private suspend fun edit(transform: (MutablePreferences) -> Unit) {
        context.dataStore.edit(transform)
    }
}