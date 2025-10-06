package vcmsa.projects.wordleandroidclient.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import vcmsa.projects.wordleandroidclient.auth.AuthInterceptor
import java.util.concurrent.TimeUnit

object RetrofitClient {

    // ---- Base URLs ----
    private const val BASE_URL_WORD = "https://wordleappapi.onrender.com/api/v1/word/"
    private const val BASE_URL_SPEEDLE = "https://wordleappapi.onrender.com/api/v1/speedle/"

    // ---- Logging ----
    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // Timeouts help the first cold-start call on free tier
    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor())
        .addInterceptor(logging)
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
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
