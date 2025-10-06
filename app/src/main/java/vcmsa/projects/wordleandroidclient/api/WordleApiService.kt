package vcmsa.projects.wordleandroidclient.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

data class WordTodayResponse(
    val date: String,
    val lang: String,
    val mode: String,
    val length: Int,
    val hasDefinition: Boolean,
    val hasSynonym: Boolean,
    val played: Boolean = false
)

data class GuessRequest(
    val guess: String,
    val lang: String? = null,
    val date: String? = null
)

data class ValidateResponse(
    val date: String,
    val lang: String,
    val mode: String,
    val length: Int,
    val guess: String,
    val feedback: List<String>,
    val won: Boolean
)

data class DefinitionObj(
    val partOfSpeech: String?,
    val definition: String,
    val example: String?
)

data class DefinitionResponse(
    val date: String,
    val lang: String,
    val mode: String,
    val definition: DefinitionObj?
)

data class SynonymResponse(
    val date: String,
    val lang: String,
    val mode: String,
    val synonym: String?
)

data class SubmitDailyRequest(
    val date: String,
    val lang: String,
    val guesses: List<String>,
    val won: Boolean,
    val durationSec: Int? = null,
    val clientId: String? = null
)

data class SubmitDailyResponse(
    val status: String,
    val deduped: Boolean? = null
)

data class MyResultResponse(
    val date: String,
    val lang: String,
    val mode: String,            // "daily"
    val guesses: List<String>,   // ["CRANE","..."]
    val feedbackRows: List<List<String>>, // [["A","Y","G","A","A"], ...]
    val won: Boolean,
    val guessCount: Int,
    val durationSec: Int,
    val submittedAt: String?
)

interface WordApiService {
    @GET("today")
    suspend fun getToday(
        @Query("lang") lang: String? = "en-ZA"
    ): Response<WordTodayResponse>

    @POST("validate")
    suspend fun validateGuess(
        @Body body: GuessRequest
    ): Response<ValidateResponse>

    @GET("definition")
    suspend fun getDefinition(
        @Query("lang") lang: String? = "en-ZA",
        @Query("date") date: String? = null
    ): Response<DefinitionResponse>

    @GET("synonym")
    suspend fun getSynonym(
        @Query("lang") lang: String? = "en-ZA",
        @Query("date") date: String? = null
    ): Response<SynonymResponse>

    @POST("submit")
    suspend fun submitDaily(
        @Body body: SubmitDailyRequest
    ): Response<SubmitDailyResponse>

    @GET("myresult")
    suspend fun getMyResult(
        @Query("date") date: String,
        @Query("lang") lang: String = "en-ZA"
    ): Response<MyResultResponse>
}
