// app/src/main/java/vcmsa/projects/wordleandroidclient/api/SpeedleApiService.kt
package vcmsa.projects.wordleandroidclient.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

// ---- Data Models ----

// Start
data class SpeedleStartRequest(
    val lang: String = "en-ZA",
    val durationSec: Int
)

data class SpeedleStartResponse(
    val sessionId: String,
    val wordId: String,
    val length: Int,
    val lang: String,
    val durationSec: Int,
    val startedAt: String
)

// Validate
data class SpeedleValidateRequest(
    val sessionId: String,
    val guess: String
)

data class SpeedleValidateResponse(
    val feedback: List<String>,
    val won: Boolean,
    val guessesUsed: Int,
    val remainingSec: Int
)

// Hint
data class SpeedleHintRequest(
    val sessionId: String,
    val type: String = "definition"
)

data class SpeedleHintResponse(
    val definition: String?,
    val remainingSec: Int
)

// Finish
data class SpeedleFinishRequest(
    val sessionId: String,
    val endReason: String,          // "won" | "timeout" | "attempts"
    val clientGuessesUsed: Int,
    val clientTimeTakenSec: Int,
    val displayName: String? = null // optional: user's display name to show on leaderboard
)

data class SpeedleFinishResponse(
    val won: Boolean,
    val timeRemainingSec: Int,
    val guessesUsed: Int,
    val score: Int,
    val leaderboardPosition: Int?,  // optional
    val definition: String?,
    val synonym: String?
)

// Leaderboard
data class LeaderboardEntry(
    val rank: Int,
    val displayName: String,
    val score: Int,
    val guessesUsed: Int,
    val timeRemainingSec: Int,
    val finishedAt: String?
)

// ---- Retrofit Service ----
// NOTE: Your RetrofitClient BASE_URL_SPEEDLE ends with "/api/v1/speedle/",
// so these paths are relative (no "speedle/" prefix here).
interface SpeedleApiService {
    @POST("start")
    suspend fun start(@Body body: SpeedleStartRequest): Response<SpeedleStartResponse>

    @POST("validate")
    suspend fun validate(@Body body: SpeedleValidateRequest): Response<SpeedleValidateResponse>

    @POST("hint")
    suspend fun hint(@Body body: SpeedleHintRequest): Response<SpeedleHintResponse>

    @POST("finish")
    suspend fun finish(@Body body: SpeedleFinishRequest): Response<SpeedleFinishResponse>

    @GET("leaderboard")
    suspend fun leaderboard(
        @Query("date") dateIso: String,
        @Query("duration") durationSec: Int,
        @Query("limit") limit: Int = 100
    ): Response<List<LeaderboardEntry>>
}
