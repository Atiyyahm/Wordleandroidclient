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
    val hasSynonym: Boolean
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

data class DefinitionResponse(
    val date: String,
    val lang: String,
    val mode: String,
    val definition: Map<String, String>?
)

data class SynonymResponse(
    val date: String,
    val lang: String,
    val mode: String,
    val synonym: String?
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
}
