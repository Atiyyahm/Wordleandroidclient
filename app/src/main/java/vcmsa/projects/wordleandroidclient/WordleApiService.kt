// Make sure this is in your project's main package
package vcmsa.projects.wordleandroidclient

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header

interface WordsApiService {
    // This function will make a GET request to a random word endpoint
    @GET("words/?random=true&limit=1&letters=5")
    suspend fun getRandomWord(
        @Header("x-rapidapi-key") apiKey: String
    ): Response<List<WordResponse>>
}

