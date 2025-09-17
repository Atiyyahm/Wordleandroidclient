package vcmsa.projects.wordleandroidclient

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vcmsa.projects.wordleandroidclient.api.data.WordleDataService
import vcmsa.projects.wordleandroidclient.api.data.WordleEntry

// This is the Game Manager class.
class WordleViewModel(private val dataService: WordleDataService) : ViewModel() {

    // --- Game State Exposed to the UI ---

    // The current word the player is trying to guess (fetched from API)
    private val _targetWordEntry = MutableStateFlow<WordleEntry?>(null)
    val targetWordEntry: StateFlow<WordleEntry?> = _targetWordEntry

    // The 6x5 grid of letters (30 total tiles). This drives the GameBoardAdapter.
    private val _boardLetters = MutableStateFlow(List(30) { "" })
    val boardLetters: StateFlow<List<String>> = _boardLetters

    // The current state of the game (Playing, Won, Lost, Loading, Error)
    private val _gameState = MutableStateFlow(GameState.LOADING)
    val gameState: StateFlow<GameState> = _gameState

    // Current input position (0 to 29)
    // The cursor position: 0-4 for guess 1, 5-9 for guess 2, etc.
    private var currentPosition: Int = 0

    // The current guess number (0 to 5)
    private var currentGuessRow: Int = 0

    init {
        viewModelScope.launch {
            dataService.targetWordEntry.collect { entry ->
                _targetWordEntry.value = entry
                if (entry != null) {
                    _gameState.value = GameState.PLAYING
                    Log.d("WordleVM", "Target Word Loaded: ${entry.word}")
                }
            }
        }
    }

    /**
     * Triggers the API call to get the word, definition, and synonym.
     */
    fun loadDailyWord() {
        _gameState.value = GameState.LOADING
        viewModelScope.launch {
            dataService.loadDailyWord()
        }
    }

    // --- *** NEW INPUT LOGIC FUNCTIONS *** ---

    /**
     * Handles letter input (Q, W, E, etc.).
     */
    fun handleLetterInput(key: Char) {
        if (_gameState.value != GameState.PLAYING) return

        val maxPosition = (currentGuessRow + 1) * 5

        // Check if we are still within the current guess row (max 5 letters)
        if (currentPosition < maxPosition) {
            _boardLetters.update { currentList ->
                currentList.toMutableList().apply {
                    this[currentPosition] = key.toString()
                }
            }
            currentPosition++
        }
    }

    /**
     * Handles BACKSPACE input.
     */
    fun deleteLetter() {
        if (_gameState.value != GameState.PLAYING) return

        val minPosition = currentGuessRow * 5

        // If the position is past the start of the current row, move back and clear the letter
        if (currentPosition > minPosition) {
            currentPosition--
            _boardLetters.update { currentList ->
                currentList.toMutableList().apply {
                    this[currentPosition] = "" // Clear the letter
                }
            }
        }
    }

    /**
     * Handles ENTER input to submit the guess.
     */
    fun processGuess() {
        if (_gameState.value != GameState.PLAYING) return

        // 1. Check if the row is full (5 letters)
        val startPosition = currentGuessRow * 5
        val endPosition = startPosition + 5

        if (currentPosition < endPosition) {
            Log.w("WordleVM", "Guess is incomplete.")
            // TODO: Show a "Not enough letters" message to the user
            return
        }

        // 2. Extract the word
        val guess = _boardLetters.value.subList(startPosition, endPosition).joinToString("")
        Log.d("WordleVM", "Processing guess: $guess")

        // 3. TODO: Implement actual comparison logic (Green/Yellow/Gray)
        // For now, we just move to the next row.

        // 4. Move to the next row if available
        if (currentGuessRow < 5) {
            currentGuessRow++
        }
    }
}

enum class GameState {
    LOADING, PLAYING, WON, LOST, ERROR
}
