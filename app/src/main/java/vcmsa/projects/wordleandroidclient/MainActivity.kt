package vcmsa.projects.wordleandroidclient

import android.os.Bundle
import android.util.Log
import android.widget.Button // Necessary for referencing the keyboard buttons
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import vcmsa.projects.wordleandroidclient.api.RetrofitClient
import vcmsa.projects.wordleandroidclient.api.data.WordleDataService
import vcmsa.projects.wordleandroidclient.WordleViewModel
import vcmsa.projects.wordleandroidclient.WordleViewModelFactory

/**
 * MainActivity is the primary Game Screen, hosting the RecyclerView and the Keyboard.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: WordleViewModel
    private lateinit var gameBoardRecyclerView: RecyclerView
    private lateinit var gameBoardAdapter: GameBoardAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Loads activity_main.xml which contains rvGameBoard, llKeyboard, etc.
        setContentView(R.layout.activity_main)

        // 1. Initialize dependencies: API Service, Data Service, and ViewModel
        val api = RetrofitClient.apiService
        val dataService = WordleDataService(api)
        // Use the factory to create the ViewModel with the required dependency
        val factory = WordleViewModelFactory(dataService)
        viewModel = ViewModelProvider(this, factory)[WordleViewModel::class.java]

        // 2. Setup Game Board (RecyclerView)
        gameBoardRecyclerView = findViewById(R.id.rvGameBoard)

        // Initialize the adapter with the ViewModel's initial state (30 empty strings)
        gameBoardAdapter = GameBoardAdapter(viewModel.boardLetters.value)

        // Setup the RecyclerView as a 5-column grid (5 letters wide)
        gameBoardRecyclerView.layoutManager = GridLayoutManager(this, 5)
        gameBoardRecyclerView.adapter = gameBoardAdapter

        // 3. Observe the ViewModel's state (Reactive UI update)
        // Any change to viewModel.boardLetters triggers this block
        lifecycleScope.launch {
            viewModel.boardLetters.collect { letters ->
                // This updates the data list inside the adapter and redraws the tiles
                gameBoardAdapter.updateLetters(letters)
                Log.d("Wordle", "Board successfully updated with ${letters.size} tiles.")
            }
        }

        // 4. Setup Keyboard Listeners
        setupKeyboardListeners()

        // 5. Start the game by loading the daily word (triggers API call)
        lifecycleScope.launch {
            viewModel.loadDailyWord()
        }
    }

    /**
     * Attaches click listeners to all letter keys and special keys defined in activity_main.xml.
     * This links the View (Button press) to the Logic (ViewModel function call).
     */
    private fun setupKeyboardListeners() {
        // List of all 26 letter button IDs
        val letterKeys = listOf(
            R.id.btnQ, R.id.btnW, R.id.btnE, R.id.btnR, R.id.btnT, R.id.btnY, R.id.btnU, R.id.btnI, R.id.btnO, R.id.btnP,
            R.id.btnA, R.id.btnS, R.id.btnD, R.id.btnF, R.id.btnG, R.id.btnH, R.id.btnJ, R.id.btnK, R.id.btnL,
            R.id.btnZ, R.id.btnX, R.id.btnC, R.id.btnV, R.id.btnB, R.id.btnN, R.id.btnM
        )

        // Loop through all letter IDs and attach the input handler
        letterKeys.forEach { id ->
            findViewById<Button>(id)?.setOnClickListener { button ->
                // Extracts the single character (e.g., 'Q')
                val letter = (button as Button).text.toString().first()
                // Sends the input to the ViewModel
                viewModel.handleLetterInput(letter)
            }
        }

        // Attach listener for ENTER button
        findViewById<Button>(R.id.btnEnter)?.setOnClickListener {
            viewModel.processGuess()
        }

        // Attach listener for BACKSPACE button
        findViewById<Button>(R.id.btnBackspace)?.setOnClickListener {
            viewModel.deleteLetter()
        }

        Log.d("Wordle", "Keyboard listeners attached successfully.")
    }
}
