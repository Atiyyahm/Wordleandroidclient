package vcmsa.projects.wordleandroidclient

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import vcmsa.projects.wordleandroidclient.UserProfile

class ProfileRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val profiles = db.collection("profiles")

    suspend fun getMyProfile(): UserProfile? {
        val uid = auth.currentUser?.uid ?: return null
        val snap = profiles.document(uid).get().await()
        return snap.toObject(UserProfile::class.java)
    }

    suspend fun ensureProfileOnFirstLogin(): UserProfile? {
        val u = auth.currentUser ?: return null
        val doc = profiles.document(u.uid)
        val snap = doc.get().await()
        if (!snap.exists()) {
            val profile = UserProfile(
                uid = u.uid,
                email = u.email,
               // displayName = u.displayName,
                photoUrl = u.photoUrl?.toString(),
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            doc.set(profile).await()
            return profile
        }
        // touch updatedAt
        doc.update("updatedAt", FieldValue.serverTimestamp()).await()
        return snap.toObject(UserProfile::class.java)
    }

    suspend fun isNicknameAvailable(nickname: String): Boolean {
        val q = profiles.whereEqualTo("nickname", nickname).limit(1).get().await()
        return q.isEmpty
    }

    suspend fun setNickname(nickname: String): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        // Optional: make this safe with a Cloud Function; for now basic check-then-set.
        if (!isNicknameAvailable(nickname)) return false
        profiles.document(uid).update(
            mapOf(
                "nickname" to nickname,
                "updatedAt" to System.currentTimeMillis()
            )
        ).await()
        return true
    }
}
