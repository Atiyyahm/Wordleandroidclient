package vcmsa.projects.wordleandroidclient.multiplayer

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.*

data class RoomDoc(
    val code: String = "",
    val createdAt: Long = 0L,
    val length: Int = 5,
    val targetWordHash: String? = null, // TODO: hash if you don’t want plaintext
    val targetWord: String? = null,     // v1: keep it simple (dev), remove later
    val hostId: String = "",
    val guestId: String? = null,
    val hostOnline: Boolean = true,
    val guestOnline: Boolean = false,
    val start: Boolean = false,
    val cancelled: Boolean = false
)

data class GuessEvent(
    val userId: String = "",
    val guess: String = "",
    val feedback: List<String> = emptyList(),
    val row: Int = 0,
    val ts: Long = 0L
)

class MultiplayerRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private fun rooms() = db.collection("rooms")
    private fun room(code: String) = rooms().document(code)
    private fun events(code: String) = room(code).collection("events")

    suspend fun createRoom(code: String, hostId: String, length: Int, targetWord: String) {
        val doc = RoomDoc(
            code = code,
            createdAt = System.currentTimeMillis(),
            length = length,
            targetWord = targetWord,
            hostId = hostId,
            hostOnline = true
        )
        room(code).set(doc).await()
    }

    suspend fun joinRoom(code: String, guestId: String): RoomDoc? {
        val snap = room(code).get().await()
        if (!snap.exists()) return null
        val current = snap.toObject(RoomDoc::class.java) ?: return null
        if (current.guestId != null) return null // already full
        room(code).update(mapOf("guestId" to guestId, "guestOnline" to true)).await()
        return current.copy(guestId = guestId, guestOnline = true)
    }

    suspend fun markOnline(code: String, userId: String, online: Boolean) {
        val field = if (isHost(code, userId)) "hostOnline" else "guestOnline"
        room(code).update(field, online).await()
    }

    private suspend fun isHost(code: String, userId: String): Boolean {
        val r = room(code).get().await().toObject(RoomDoc::class.java) ?: return false
        return r.hostId == userId
    }

    fun observeRoom(code: String): Flow<RoomDoc?> = callbackFlow {
        val reg: ListenerRegistration = room(code).addSnapshotListener { snap, _ ->
            trySend(snap?.toObject(RoomDoc::class.java))
        }
        awaitClose { reg.remove() }
    }

    suspend fun startMatch(code: String) {
        room(code).update("start", true).await()
    }

    fun observeEvents(code: String): Flow<GuessEvent> = callbackFlow {
        val reg = events(code).orderBy("ts").addSnapshotListener { qs, _ ->
            qs?.documentChanges?.forEach { dc ->
                val ev = dc.document.toObject(GuessEvent::class.java)
                trySend(ev)
            }
        }
        awaitClose { reg.remove() }
    }

    suspend fun postGuess(code: String, userId: String, guess: String, feedback: List<String>, row: Int) {
        val ev = GuessEvent(
            userId = userId,
            guess = guess,
            feedback = feedback,
            row = row,
            ts = System.currentTimeMillis()
        )
        events(code).add(ev).await()
    }

    suspend fun leaveRoom(code: String, userId: String) {
        markOnline(code, userId, false)
        // keep room for 45s to allow reconnect; a Cloud Function or TTL rule can clean up later.
    }
}
