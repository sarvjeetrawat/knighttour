package com.kunpitech.knighttour.data.repository

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.kunpitech.knighttour.domain.model.Position
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

// ================================================================
//  FIREBASE GAME REPOSITORY
//
//  RTDB structure:
//
//  rooms/{roomCode}/
//    status          : "WAITING" | "PLAYING" | "FINISHED"
//    hostId          : String
//    guestId         : String
//    boardSize       : Int       (set by host, 6 for MEDIUM ONLINE)
//    createdAt       : Long
//    host/
//      name          : String
//      moveCount     : Int
//      lastMove      : {row, col}
//      moveHistory   : [{row,col}, ...]
//      isConnected   : Boolean
//      finishedAt    : Long?
//    guest/
//      name          : String
//      moveCount     : Int
//      lastMove      : {row, col}
//      moveHistory   : [{row,col}, ...]
//      isConnected   : Boolean
//      finishedAt    : Long?
//
//  Package: data/repository/FirebaseGameRepository.kt
// ================================================================

// ── Data transfer objects ────────────────────────────────────────

data class RoomDto(
    val status    : String           = "WAITING",
    val hostId    : String           = "",
    val guestId   : String           = "",
    val boardSize : Int              = 6,
    val createdAt : Long             = 0L,
    val host      : PlayerStateDto   = PlayerStateDto(),
    val guest     : PlayerStateDto   = PlayerStateDto(),
)

data class PlayerStateDto(
    val name         : String             = "",
    val moveCount    : Int                = 0,
    val lastMove     : MoveDto?           = null,
    val moveHistory  : List<MoveDto>      = emptyList(),
    val isConnected  : Boolean            = true,
    val finishedAt   : Long               = 0L,
    val finalScore   : Int                = 0,
    val isCompleted  : Boolean            = false,   // true = cleared the full board
)

data class MoveDto(
    val row : Int = 0,
    val col : Int = 0,
)

fun MoveDto.toPosition() = Position(row, col)
fun Position.toDto()     = MoveDto(row, col)

// ── Repository ───────────────────────────────────────────────────

@Singleton
class FirebaseGameRepository @Inject constructor() {

    private val db = FirebaseDatabase.getInstance("https://knight-tour-58864-default-rtdb.asia-southeast1.firebasedatabase.app")

    private fun roomRef(roomCode: String) =
        db.getReference("rooms/$roomCode")

    private fun playerRef(roomCode: String, role: String) =
        db.getReference("rooms/$roomCode/$role")

    // ── Room lifecycle ───────────────────────────────────────────

    /** Create a new room as host. Returns the room code. */
    suspend fun createRoom(
        roomCode  : String,
        hostId    : String,
        hostName  : String,
        boardSize : Int = 6,
        roomName  : String = roomCode,   // display name shown in browse
    ) {
        val room = mapOf(
            "status"    to "WAITING",
            "roomName"  to roomName,
            "hostId"    to hostId,
            "guestId"   to "",
            "boardSize" to boardSize,
            "createdAt" to System.currentTimeMillis(),
            "host"      to mapOf(
                "name"        to hostName,
                "moveCount"   to 0,
                "isConnected" to true,
                "moveHistory" to emptyList<Any>(),
                "finishedAt"  to 0L,
            ),
            "guest" to mapOf(
                "name"        to "",
                "moveCount"   to 0,
                "isConnected" to false,
                "moveHistory" to emptyList<Any>(),
                "finishedAt"  to 0L,
            ),
        )
        roomRef(roomCode).setValue(room).await()

        // Seed matchResults node so it exists before game ends
        db.getReference("matchResults/$roomCode").setValue(mapOf(
            "host"  to mapOf("finalScore" to 0, "name" to hostName),
            "guest" to mapOf("finalScore" to 0, "name" to ""),
        )).await()
    }

    /**
     * Reset an existing room for a rematch — same room code, fresh state.
     * Much cheaper than creating a new room; old data is overwritten in place.
     */
    suspend fun resetRoomForRematch(
        roomCode  : String,
        hostId    : String,
        hostName  : String,
        guestName : String,
        boardSize : Int = 6,
    ) {
        // Reset room node to WAITING state, preserve host/guest names
        val resetRoom = mapOf(
            "status"    to "WAITING",
            "hostId"    to hostId,
            "guestId"   to "",
            "boardSize" to boardSize,
            "createdAt" to System.currentTimeMillis(),
            "host"      to mapOf(
                "name"        to hostName,
                "moveCount"   to 0,
                "isConnected" to true,
                "moveHistory" to emptyList<Any>(),
                "finishedAt"  to 0L,
                "finalScore"  to 0,
                "isCompleted" to false,
            ),
            "guest"     to mapOf(
                "name"        to guestName,
                "moveCount"   to 0,
                "isConnected" to false,
                "moveHistory" to emptyList<Any>(),
                "finishedAt"  to 0L,
                "finalScore"  to 0,
                "isCompleted" to false,
            ),
        )
        roomRef(roomCode).setValue(resetRoom).await()

        // Cancel ANY stale onDisconnect handlers BEFORE writing isConnected=true
        // Without this, old handlers fire asynchronously and overwrite isConnected back to false
        playerRef(roomCode, "host").child("isConnected").onDisconnect().cancel().await()
        playerRef(roomCode, "guest").child("isConnected").onDisconnect().cancel().await()

        // Now safe to write isConnected=true — no stale handler will overwrite it
        playerRef(roomCode, "host").child("isConnected").setValue(true).await()

        // Reset matchResults — same node, fresh scores
        db.getReference("matchResults/$roomCode").setValue(mapOf(
            "host"  to mapOf("finalScore" to 0, "name" to hostName,  "isCompleted" to false),
            "guest" to mapOf("finalScore" to 0, "name" to guestName, "isCompleted" to false),
        )).await()

        // Clear any stale rematch signals
        db.getReference("rematch/$roomCode").removeValue().await()
    }

    /** Join an existing room as guest. Returns true if room exists and is WAITING. */
    suspend fun joinRoom(
        roomCode  : String,
        guestId   : String,
        guestName : String,
    ): Boolean {
        val snapshot = roomRef(roomCode).get().await()
        if (!snapshot.exists()) return false
        val status = snapshot.child("status").getValue(String::class.java)
        if (status != "WAITING") return false

        roomRef(roomCode).updateChildren(mapOf(
            "guestId"           to guestId,
            "status"            to "PLAYING",
            "guest/name"        to guestName,
            "guest/isConnected" to true,
        )).await()

        // Update guest name in matchResults
        db.getReference("matchResults/$roomCode/guest/name").setValue(guestName).await()
        return true
    }

    /** Set room status to PLAYING (called by host when guest joins). */
    suspend fun setRoomPlaying(roomCode: String) {
        roomRef(roomCode).child("status").setValue("PLAYING").await()
    }

    /** Set room status to FINISHED. */
    suspend fun setRoomFinished(roomCode: String) {
        roomRef(roomCode).child("status").setValue("FINISHED").await()
    }

    /** Delete a room (cleanup after game ends). */
    suspend fun deleteRoom(roomCode: String) {
        // Cancel disconnect handlers so they don't fire on next session for same room
        playerRef(roomCode, "host").child("isConnected").onDisconnect().cancel()
        playerRef(roomCode, "guest").child("isConnected").onDisconnect().cancel()
        roomRef(roomCode).removeValue().await()
    }

    // ── Move sync ────────────────────────────────────────────────

    /** Push a move for the given player role ("host" or "guest"). */
    suspend fun pushMove(
        roomCode : String,
        role     : String,
        position : Position,
        history  : List<Position>,
    ) {
        val historyDto = history.map { mapOf("row" to it.row, "col" to it.col) }
        playerRef(roomCode, role).updateChildren(mapOf(
            "moveCount"   to history.size,
            "lastMove"    to mapOf("row" to position.row, "col" to position.col),
            "moveHistory" to historyDto,
        )).await()
    }

    /** Mark player as finished with timestamp. */
    suspend fun markFinished(roomCode: String, role: String, finalScore: Int = 0, moveCount: Int = 0, name: String = "", isCompleted: Boolean = false) {
        writeMatchResult(roomCode, role, finalScore, moveCount, name, isCompleted)
        try {
            playerRef(roomCode, role).updateChildren(mapOf(
                "finishedAt"  to System.currentTimeMillis(),
                "finalScore"  to finalScore,
                "moveCount"   to moveCount,
                "isCompleted" to isCompleted,
            )).await()
        } catch (_: Exception) {}
    }

    /** Read both players' final scores from the room. Returns null if room not found. */
    suspend fun getMatchResult(roomCode: String): MatchResult? {
        return try {
            val snap = roomRef(roomCode).get().await()
            if (!snap.exists()) return null

            val hostScore    = snap.child("host/finalScore").getValue(Int::class.java)  ?: 0
            val guestScore   = snap.child("guest/finalScore").getValue(Int::class.java) ?: 0
            val hostName     = snap.child("host/name").getValue(String::class.java)     ?: ""
            val guestName    = snap.child("guest/name").getValue(String::class.java)    ?: ""
            val hostFinished = snap.child("host/finishedAt").getValue(Long::class.java) ?: 0L
            val guestFinished= snap.child("guest/finishedAt").getValue(Long::class.java)?: 0L

            MatchResult(
                hostName      = hostName,
                hostScore     = hostScore,
                hostFinished  = hostFinished,
                guestName     = guestName,
                guestScore    = guestScore,
                guestFinished = guestFinished,
            )
        } catch (_: Exception) { null }
    }

    // ── Presence ─────────────────────────────────────────────────

    /** Set connected flag — call on resume/pause. */
    suspend fun setConnected(roomCode: String, role: String, connected: Boolean) {
        val ref = playerRef(roomCode, role).child("isConnected")
        if (!connected) {
            // Cancel the onDisconnect handler before manually setting false
            // so it doesn't fire again later on the next session's reset
            ref.onDisconnect().cancel().await()
        }
        ref.setValue(connected).await()
    }

    /** Register an onDisconnect handler so Firebase auto-clears presence. */
    fun registerDisconnectHandler(roomCode: String, role: String) {
        val ref = playerRef(roomCode, role).child("isConnected")
        // Cancel any previous onDisconnect for this path before registering a new one
        // This prevents stale handlers from firing on rematch
        ref.onDisconnect().cancel()
        ref.onDisconnect().setValue(false)
    }

    // ── Observation flows ────────────────────────────────────────

    /** Observe room status changes: "WAITING" | "PLAYING" | "FINISHED" */
    fun observeRoomStatus(roomCode: String): Flow<String> = callbackFlow {
        val ref = roomRef(roomCode).child("status")
        val listener = object : ValueEventListener {
            override fun onDataChange(snap: DataSnapshot) {
                trySend(snap.getValue(String::class.java) ?: "WAITING")
            }
            override fun onCancelled(error: DatabaseError) { close() }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    /** Observe opponent's player state (move count + history). */
    fun observeOpponent(roomCode: String, opponentRole: String): Flow<PlayerStateDto> =
        callbackFlow {
            val ref = playerRef(roomCode, opponentRole)
            val listener = object : ValueEventListener {
                override fun onDataChange(snap: DataSnapshot) {
                    val dto = parsePlayerState(snap)
                    trySend(dto)
                }
                override fun onCancelled(error: DatabaseError) { close() }
            }
            ref.addValueEventListener(listener)
            awaitClose { ref.removeEventListener(listener) }
        }

    /** Observe the full room snapshot once (for initial load). */
    suspend fun getRoomOnce(roomCode: String): RoomDto? {
        val snap = roomRef(roomCode).get().await()
        if (!snap.exists()) return null
        return parseRoom(snap)
    }

    /** Write score to the persistent matchResults node only (no room write). */
    suspend fun writeMatchResult(roomCode: String, role: String, finalScore: Int, moveCount: Int, name: String = "", isCompleted: Boolean = false) {
        val data = mutableMapOf<String, Any>(
            "finalScore"  to finalScore,
            "moveCount"   to moveCount,
            "finishedAt"  to System.currentTimeMillis(),
            "isCompleted" to isCompleted,
        )
        if (name.isNotEmpty()) data["name"] = name
        db.getReference("matchResults/$roomCode/$role").updateChildren(data).await()
    }

    /** One-time fetch of a player node. Used to capture finalScore after game ends. */
    suspend fun getOpponentSnapshot(roomCode: String, role: String): DataSnapshot? {
        return try {
            playerRef(roomCode, role).get().await()
        } catch (_: Exception) { null }
    }

    /**
     * Observe opponent's score from the persistent matchResults node.
     * This node is never deleted, so it works even after the room is gone.
     * Emits the opponent's finalScore whenever it changes from 0 to a real value.
     */
    fun observeMatchResult(roomCode: String, opponentRole: String): Flow<Int> = callbackFlow {
        val ref = db.getReference("matchResults/$roomCode/$opponentRole/finalScore")
        val listener = object : ValueEventListener {
            override fun onDataChange(snap: DataSnapshot) {
                val score = snap.getValue(Int::class.java) ?: 0
                trySend(score)
            }
            override fun onCancelled(error: DatabaseError) { trySend(0) }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    /** Signal this player wants a rematch. */
    suspend fun signalRematch(roomCode: String, role: String) {
        db.getReference("rematch/$roomCode/$role").setValue("READY").await()
    }

    /** Clear only this player's own signal — does NOT touch opponent's signal. */
    suspend fun clearMyRematchSignal(roomCode: String, role: String) {
        try { db.getReference("rematch/$roomCode/$role").removeValue().await() } catch (_: Exception) {}
    }

    /** Host calls this after resetting the room — signals guest it's safe to join. */
    suspend fun signalRoomReady(roomCode: String) {
        db.getReference("rematch/$roomCode/roomReady").setValue(true).await()
    }

    /**
     * Guest observes this — emits true only when host has reset room and marked it ready.
     * Prevents guest from joining before room is in WAITING state.
     */
    fun observeRoomReady(roomCode: String): Flow<Boolean> = callbackFlow {
        val ref = db.getReference("rematch/$roomCode/roomReady")
        val listener = object : ValueEventListener {
            override fun onDataChange(snap: DataSnapshot) {
                val ready = snap.getValue(Boolean::class.java) ?: false
                if (ready) trySend(true)
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    /** Observe rematch node — emits when both players have signalled READY. */
    fun observeRematch(roomCode: String): Flow<Unit> = callbackFlow {
        val ref = db.getReference("rematch/$roomCode")
        val listener = object : ValueEventListener {
            override fun onDataChange(snap: DataSnapshot) {
                val hostReady  = snap.child("host").getValue(String::class.java).orEmpty()
                val guestReady = snap.child("guest").getValue(String::class.java).orEmpty()
                if (hostReady == "READY" && guestReady == "READY") {
                    trySend(Unit)
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    /** Clean up rematch node after both navigate away. */
    suspend fun clearRematch(roomCode: String) {
        try { db.getReference("rematch/$roomCode").removeValue().await() } catch (_: Exception) {}
    }

    /** Check if a room code exists and is joinable. */
    /**
     * Derive a stable room code from the player's name.
     * Same player always gets the same room — no new rooms created ever.
     */
    /**
     * Called when host opens lobby — if their persistent room exists in any state,
     * reset it to WAITING and mark host as connected again.
     * Returns the roomCode if reconnected, null if no room existed.
     */
    suspend fun reconnectHostRoom(hostName: String, hostId: String, boardSize: Int = 6): String? {
        return try {
            val roomCode = roomCodeForPlayer(hostName)
            val snap     = roomRef(roomCode).get().await()
            if (!snap.exists()) return null

            // Room exists — reset to WAITING and mark host connected
            resetRoomForRematch(
                roomCode  = roomCode,
                hostId    = hostId,
                hostName  = hostName,
                guestName = "",
                boardSize = boardSize,
            )
            // Re-register disconnect handler so future disconnects are tracked
            registerDisconnectHandler(roomCode, "host")
            roomCode
        } catch (_: Exception) { null }
    }

    /** Derive a room code from a custom room name (e.g. "1v1 Ranked" → "1V1RANKED"). */
    fun roomCodeForName(roomName: String): String =
        sanitizeName(roomName.trim()).take(12).uppercase()

    /** Derive the room code tied to a player — used for reconnect/check. */
    fun roomCodeForPlayer(playerName: String): String =
        sanitizeName(playerName).take(12).uppercase()

    /** Find the room code for a given host player name (if any WAITING room exists). */
    suspend fun findRoomCodeByHost(hostName: String): String? {
        return try {
            val snap = db.getReference("rooms")
                .orderByChild("host/name")
                .equalTo(hostName)
                .get().await()
            snap.children.firstOrNull { room ->
                room.child("status").getValue(String::class.java) != "PLAYING" ||
                        room.child("host/isConnected").getValue(Boolean::class.java) == true
            }?.key
        } catch (_: Exception) { null }
    }

    /** Check if a player already has an open (WAITING) room in Firebase. */
    suspend fun playerHasOpenRoom(playerName: String): Boolean {
        return try {
            val snap = db.getReference("rooms")
                .orderByChild("host/name")
                .equalTo(playerName)
                .get().await()
            snap.children.any { room ->
                room.child("status").getValue(String::class.java) == "WAITING" &&
                        room.child("host/isConnected").getValue(Boolean::class.java) == true
            }
        } catch (_: Exception) { false }
    }

    /**
     * Get or reset a player's persistent room.
     * - If room doesn't exist → create it fresh
     * - If room exists (any status) → reset it to WAITING, clearing old game data
     * This means a player always reuses the same room code forever.
     */
    suspend fun getOrResetPlayerRoom(
        hostName  : String,
        hostId    : String,
        roomName  : String,
        boardSize : Int = 6,
    ): String {
        val roomCode = roomCodeForName(roomName)
        val existing = roomRef(roomCode).get().await()

        if (existing.exists()) {
            resetRoomForRematch(
                roomCode  = roomCode,
                hostId    = hostId,
                hostName  = hostName,
                guestName = "",
                boardSize = boardSize,
            )
        } else {
            createRoom(
                roomCode  = roomCode,
                hostId    = hostId,
                hostName  = hostName,
                boardSize = boardSize,
            )
        }
        return roomCode
    }

    suspend fun isRoomJoinable(roomCode: String): Boolean {
        val snap = roomRef(roomCode).get().await()
        if (!snap.exists()) return false
        val status = snap.child("status").getValue(String::class.java)
        return status == "WAITING"
    }

    /**
     * Observe all rooms with status = WAITING.
     * Used by the lobby room browser to show joinable rooms in real time.
     */
    fun observeWaitingRooms(myName: String = ""): Flow<List<RoomBrowserEntry>> = callbackFlow {
        val ref   = db.getReference("rooms")
        val ttlMs = 15 * 60 * 1000L  // 15 min — rooms older than this are abandoned

        val listener = object : ValueEventListener {
            override fun onDataChange(snap: DataSnapshot) {
                val now = System.currentTimeMillis()
                val rooms = snap.children.mapNotNull { roomSnap ->
                    val status = roomSnap.child("status").getValue(String::class.java)
                    if (status != "WAITING") return@mapNotNull null

                    val hostName  = roomSnap.child("host/name").getValue(String::class.java) ?: return@mapNotNull null
                    if (hostName.isEmpty()) return@mapNotNull null

                    val roomCode       = roomSnap.key ?: return@mapNotNull null
                    val boardSize      = roomSnap.child("boardSize").getValue(Int::class.java) ?: 6
                    val createdAt      = roomSnap.child("createdAt").getValue(Long::class.java) ?: 0L
                    val isConnected    = roomSnap.child("host/isConnected").getValue(Boolean::class.java) ?: false
                    val isExpired      = (now - createdAt) > ttlMs
                    val roomName       = roomSnap.child("roomName").getValue(String::class.java)
                        ?.takeIf { it.isNotEmpty() } ?: roomCode

                    // Auto-delete: disconnected AND expired = abandoned room, clean it up
                    if (!isConnected && isExpired) {
                        roomSnap.ref.removeValue()
                        return@mapNotNull null
                    }

                    RoomBrowserEntry(
                        roomCode        = roomCode,
                        hostName        = hostName,
                        roomName        = roomName,
                        boardSize       = boardSize,
                        createdAt       = createdAt,
                        isHostConnected = isConnected,
                        isOwn           = hostName == myName,
                    )
                }.sortedWith(
                    // Own room pinned to top, then newest first
                    compareByDescending<RoomBrowserEntry> { it.isOwn }
                        .thenByDescending { it.createdAt }
                )
                trySend(rooms)
            }
            override fun onCancelled(error: DatabaseError) {
                trySend(emptyList())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    /**

     * Fetch all rooms that contain a player with the given name (as host or guest).
     * Used by leaderboard to find opponent scores.
     */
    suspend fun getRoomsForPlayer(playerName: String): List<RoomDto> {
        return try {
            val snap = db.getReference("rooms").get().await()
            snap.children.mapNotNull { roomSnap ->
                val room = parseRoom(roomSnap)
                // Include rooms where this player participated
                if (room.host.name == playerName || room.guest.name == playerName) room
                else null
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ── Parsers ──────────────────────────────────────────────────

    private fun parseRoom(snap: DataSnapshot): RoomDto = RoomDto(
        status    = snap.child("status").getValue(String::class.java)    ?: "WAITING",
        hostId    = snap.child("hostId").getValue(String::class.java)    ?: "",
        guestId   = snap.child("guestId").getValue(String::class.java)   ?: "",
        boardSize = snap.child("boardSize").getValue(Int::class.java)    ?: 6,
        createdAt = snap.child("createdAt").getValue(Long::class.java)   ?: 0L,
        host      = parsePlayerState(snap.child("host")),
        guest     = parsePlayerState(snap.child("guest")),
    )

    private fun parsePlayerState(snap: DataSnapshot): PlayerStateDto {
        val historySnap = snap.child("moveHistory")
        val history = historySnap.children.mapNotNull { child ->
            val row = child.child("row").getValue(Int::class.java) ?: return@mapNotNull null
            val col = child.child("col").getValue(Int::class.java) ?: return@mapNotNull null
            MoveDto(row, col)
        }
        val lastMoveSnap = snap.child("lastMove")
        val lastMove = if (lastMoveSnap.exists()) {
            MoveDto(
                row = lastMoveSnap.child("row").getValue(Int::class.java) ?: 0,
                col = lastMoveSnap.child("col").getValue(Int::class.java) ?: 0,
            )
        } else null

        return PlayerStateDto(
            name        = snap.child("name").getValue(String::class.java)         ?: "",
            moveCount   = snap.child("moveCount").getValue(Int::class.java)       ?: 0,
            lastMove    = lastMove,
            moveHistory = history,
            isConnected = snap.child("isConnected").getValue(Boolean::class.java) ?: false,
            finishedAt  = snap.child("finishedAt").getValue(Long::class.java)     ?: 0L,
            finalScore  = snap.child("finalScore").getValue(Int::class.java)      ?: 0,
            isCompleted = snap.child("isCompleted").getValue(Boolean::class.java) ?: false,
        )
    }

    // ── Leaderboard ──────────────────────────────────────────────

    private fun scoresRef() = db.getReference("scores")
    private fun playerScoreRef(playerName: String) =
        db.getReference("scores/${sanitizeName(playerName)}")

    /** Firebase keys can't contain . # $ [ ] / — replace with _ */
    private fun sanitizeName(name: String) =
        name.replace(Regex("[.#\$\\[\\]/]"), "_")

    // ── Write ────────────────────────────────────────────────────

    /**
     * Save a completed game score to Firebase.
     * Updates bestScore if this is a personal best.
     */
    suspend fun saveScore(
        playerName    : String,
        sessionId     : String,
        score         : Int,
        boardSize     : Int,
        elapsedSeconds: Int,
        finishedAt    : Long,
        rankLabel     : String,
    ) {
        val mins      = elapsedSeconds / 60
        val secs      = elapsedSeconds % 60
        val timeStr   = "%d:%02d".format(mins, secs)
        val boardLabel = "${boardSize}×${boardSize}"
        val safeName  = sanitizeName(playerName)

        // Write history entry
        val historyData = mapOf(
            "score"          to score,
            "boardSize"      to boardSize,
            "boardLabel"     to boardLabel,
            "elapsedSeconds" to elapsedSeconds,
            "timeStr"        to timeStr,
            "finishedAt"     to finishedAt,
            "rankLabel"      to rankLabel,
            "playerName"     to playerName,
        )
        db.getReference("scores/$safeName/history/$sessionId")
            .setValue(historyData).await()

        // Update bestScore if new personal best
        val currentBest = db.getReference("scores/$safeName/bestScore")
            .get().await().getValue(Int::class.java) ?: 0
        if (score > currentBest) {
            db.getReference("scores/$safeName/bestScore").setValue(score).await()
        }

        // Increment gamesPlayed
        db.getReference("scores/$safeName/gamesPlayed")
            .get().await().let { snap ->
                val current = snap.getValue(Int::class.java) ?: 0
                db.getReference("scores/$safeName/gamesPlayed").setValue(current + 1).await()
            }
    }

    // ── Read ─────────────────────────────────────────────────────

    /**
     * Fetch global leaderboard — one entry per player (their best score).
     * Returns top [limit] players sorted by bestScore descending.
     */
    suspend fun getGlobalLeaderboard(limit: Int = 50): List<FirebaseScoreEntry> {
        return try {
            val snap = scoresRef().get().await()
            snap.children.mapNotNull { playerSnap ->
                val playerName = playerSnap.child("playerName").getValue(String::class.java)
                    ?: playerSnap.key?.replace("_", " ") ?: return@mapNotNull null
                val bestScore  = playerSnap.child("bestScore").getValue(Int::class.java) ?: 0
                val gamesPlayed = playerSnap.child("gamesPlayed").getValue(Int::class.java) ?: 0

                // Get their best game details
                val bestGame = playerSnap.child("history").children
                    .mapNotNull { parseHistoryEntry(it) }
                    .maxByOrNull { it.score }

                FirebaseScoreEntry(
                    playerName    = playerName,
                    score         = bestScore,
                    boardLabel    = bestGame?.boardLabel ?: "—",
                    timeStr       = bestGame?.timeStr ?: "—",
                    rankLabel     = bestGame?.rankLabel ?: "SQUIRE",
                    gamesPlayed   = gamesPlayed,
                    finishedAt    = bestGame?.finishedAt ?: 0L,
                )
            }
                .filter { it.score > 0 }
                .sortedByDescending { it.score }
                .take(limit)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Fetch full score history for one player (My Stats tab).
     */
    suspend fun getPlayerHistory(playerName: String): List<FirebaseScoreEntry> {
        return try {
            val safeName = sanitizeName(playerName)
            val snap = db.getReference("scores/$safeName/history").get().await()
            snap.children.mapNotNull { parseHistoryEntry(it) }
                .sortedByDescending { it.finishedAt }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Fetch scores of opponents this player has faced in rooms.
     * Looks up each opponent's best score from the scores node.
     */
    suspend fun getFriendScores(playerName: String): List<FirebaseScoreEntry> {
        return try {
            // Get all rooms this player was in
            val rooms = getRoomsForPlayer(playerName)
            val opponentNames = rooms.mapNotNull { room ->
                when {
                    room.host.name  == playerName -> room.guest.name.ifEmpty { null }
                    room.guest.name == playerName -> room.host.name.ifEmpty  { null }
                    else -> null
                }
            }.distinct()

            if (opponentNames.isEmpty()) return emptyList()

            // Fetch best score for each opponent
            opponentNames.mapNotNull { opponent ->
                val safeName = sanitizeName(opponent)
                val snap = db.getReference("scores/$safeName").get().await()
                val bestScore = snap.child("bestScore").getValue(Int::class.java) ?: 0
                if (bestScore == 0) return@mapNotNull null

                val bestGame = snap.child("history").children
                    .mapNotNull { parseHistoryEntry(it) }
                    .maxByOrNull { it.score }

                FirebaseScoreEntry(
                    playerName  = opponent,
                    score       = bestScore,
                    boardLabel  = bestGame?.boardLabel ?: "—",
                    timeStr     = bestGame?.timeStr    ?: "—",
                    rankLabel   = bestGame?.rankLabel  ?: "SQUIRE",
                    gamesPlayed = snap.child("gamesPlayed").getValue(Int::class.java) ?: 0,
                    finishedAt  = bestGame?.finishedAt ?: 0L,
                )
            }
                .sortedByDescending { it.score }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ── DTOs ─────────────────────────────────────────────────────

    private fun parseHistoryEntry(snap: DataSnapshot): FirebaseScoreEntry? {
        val score = snap.child("score").getValue(Int::class.java) ?: return null
        return FirebaseScoreEntry(
            playerName  = snap.child("playerName").getValue(String::class.java) ?: "",
            score       = score,
            boardLabel  = snap.child("boardLabel").getValue(String::class.java) ?: "—",
            timeStr     = snap.child("timeStr").getValue(String::class.java)    ?: "—",
            rankLabel   = snap.child("rankLabel").getValue(String::class.java)  ?: "SQUIRE",
            gamesPlayed = 1,
            finishedAt  = snap.child("finishedAt").getValue(Long::class.java)   ?: 0L,
        )
    }
}

data class MatchResult(
    val hostName      : String,
    val hostScore     : Int,
    val hostFinished  : Long,
    val guestName     : String,
    val guestScore    : Int,
    val guestFinished : Long,
)

data class RoomBrowserEntry(
    val roomCode       : String,
    val hostName       : String,
    val roomName       : String  = roomCode,  // user-chosen display name
    val boardSize      : Int,
    val createdAt      : Long,
    val isHostConnected: Boolean = true,
    val isOwn          : Boolean = false,
)

data class FirebaseScoreEntry(
    val playerName  : String,
    val score       : Int,
    val boardLabel  : String,
    val timeStr     : String,
    val rankLabel   : String,
    val gamesPlayed : Int,
    val finishedAt  : Long,
)