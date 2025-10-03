package vcmsa.projects.wordleandroidclient.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    // ---- Base URLs ----
    private const val BASE_URL_WORD = "http://10.0.2.2:4000/api/v1/word/"
    private const val BASE_URL_SPEEDLE = "http://10.0.2.2:4000/api/v1/speedle/"

    // ---- Logging ----
    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(logging)
        .build()

    // ---- Services ----
    val wordService: WordApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL_WORD)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WordApiService::class.java)
    }

    val speedleService: SpeedleApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL_SPEEDLE)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SpeedleApiService::class.java)
    }
}
