// Make sure this is in your project's main package
package vcmsa.projects.wordleandroidclient

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : AppCompatActivity() {
    private lateinit var rvGameBoard: RecyclerView

    // IMPORTANT: Replace "YOUR_API_KEY_HERE" with your actual WordsAPI key.
    private val API_KEY = "YOUR_API_KEY_HERE"
    private val BASE_URL = "https://wordsapi-com.p.rapidapi.com/"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        rvGameBoard = findViewById(R.id.rvGameBoard)

        // Generate a list of 30 empty strings to represent the 6x5 grid (6 rows, 5 columns)
        val letters = List(30) { "" }

        // Set up the RecyclerView with a GridLayoutManager
        val adapter = GameBoardAdapter(letters)
        val layoutManager = GridLayoutManager(this, 5) // 5 columns for a 5-letter word
        rvGameBoard.layoutManager = layoutManager
        rvGameBoard.adapter = adapter

        // Make the API call to your Node.js backend
        fetchRandomWord()
    }

    private fun fetchRandomWord() {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(WordleApiService::class.java)

        // Launch a coroutine to run the network request on a background thread
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = apiService.getRandomWord(API_KEY)
                if (response.isSuccessful) {
                    val wordList = response.body()
                    if (wordList != null && wordList.isNotEmpty()) {
                        val randomWord = wordList.first().word
                        Log.d("API_CALL", "Received Random Word: $randomWord")
                    } else {
                        Log.e("API_CALL", "Received empty response from API")
                    }
                } else {
                    Log.e("API_CALL", "API request failed with code: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("API_CALL", "Network error: ${e.message}")
            }
        }
    }
}
