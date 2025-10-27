package vcmsa.projects.wordleandroidclient.leaderboard

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import vcmsa.projects.wordleandroidclient.todayIso

data class LeaderboardEntry(
    val uid: String = "",
    val username: String? = null,
    val photoUrl: String? = null,
    val guessCount: Int = 0
)

class ResultsLeaderboardRepo(
    private val db: FirebaseFirestore
) {
    /** Fetch today's winners sorted by fewest guesses. */
    suspend fun fetchDailyLeaderboard(limit: Long = 50): List<LeaderboardEntry> {
        val today = todayIso()

        // Get results for today where user won
        val query = db.collection("results")
            .whereEqualTo("date", today)
            .whereEqualTo("won", true)
            .orderBy("guessCount") // ascending: fewest guesses first
            .limit(limit)

        val snaps = query.get().await()
        val entries = mutableListOf<LeaderboardEntry>()

        for (doc in snaps.documents) {
            val uid = doc.getString("uid") ?: continue
            val guessCount = doc.getLong("guessCount")?.toInt() ?: continue

            // Optional: fetch username/photo from "profiles" collection
            val profileDoc = db.collection("profiles").document(uid).get().await()
            val username = profileDoc.getString("username") ?: "Player"
            val photoUrl = profileDoc.getString("photoUrl")

            entries.add(LeaderboardEntry(uid, username, photoUrl, guessCount))
        }

        return entries
    }
}
