package vcmsa.projects.wordleandroidclient.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import vcmsa.projects.wordleandroidclient.api.data.WordleWordsApi

object RetrofitClient {

    private const val BASE_URL = "https://your-api-domain.com/" // **IMPORTANT: Replace with your actual API base URL**

    // This uses the correct interface name: WordleWordsApi
    val apiService: WordleWordsApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WordleWordsApi::class.java)
    }
}