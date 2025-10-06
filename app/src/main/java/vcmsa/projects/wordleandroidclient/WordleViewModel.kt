package vcmsa.projects.wordleandroidclient

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vcmsa.projects.wordleandroidclient.api.*





class WordleViewModel(
    private val wordApi: WordApiService,
    private val speedleApi: SpeedleApiService,
    private val appContext: Context
) : ViewModel() {

    // ---- Mode (Daily vs Speedle) ----
    private val _mode = MutableStateFlow(GameMode.DAILY)
    val mode: StateFlow<GameMode> = _mode

    // ---- Shared timers ----
    private val _remainingSeconds = MutableStateFlow<Int?>(null)
    val remainingSeconds: StateFlow<Int?> = _remainingSeconds
    private var timerJob: Job? = null

    private val _preCountdownSeconds = MutableStateFlow<Int?>(null)
    val preCountdownSeconds: StateFlow<Int?> = _preCountdownSeconds

    // ---- Daily metadata ----
    private val _today = MutableStateFlow<WordTodayResponse?>(null)
    val today: StateFlow<WordTodayResponse?> = _today

    // ---- Word length ----
    private var speedleLength: Int? = null
    private val currentLen: Int get() = speedleLength ?: (_today.value?.length ?: 5)

    // ---- Board ----
    private val _boardLetters = MutableStateFlow(List(30) { "" })
    val boardLetters: StateFlow<List<String>> = _boardLetters

    private val _boardStates = MutableStateFlow(List(30) { TileState.EMPTY })
    val boardStates: StateFlow<List<TileState>> = _boardStates

    // Attempts
    private val _attemptIndex = MutableStateFlow(0)
    val attemptIndex: StateFlow<Int> = _attemptIndex

    // ---- Game state ----
    private val _gameState = MutableStateFlow(GameState.LOADING)
    val gameState: StateFlow<GameState> = _gameState

    private val _summary = MutableStateFlow<EndGameSummary?>(null)
    val summary: StateFlow<EndGameSummary?> = _summary

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting

    // ---- Hint ----
    private val _hintMessage = MutableStateFlow<String?>(null)
    val hintMessage: StateFlow<String?> = _hintMessage

    // ---- Internal state ----
    private var currentPosition = 0
    private var currentGuessRow = 0

    // ---- Speedle session ----
    private var speedleSessionId: String? = null
    private var speedleGuessesUsed: Int = 0
    private var speedleWordId: String? = null
    private var speedleDuration: Int = 90

    // ---------- DAILY ----------

    fun setModeDaily() {
        stopTimer()
        _mode.value = GameMode.DAILY
        _remainingSeconds.value = null
        _preCountdownSeconds.value = null
        speedleSessionId = null
        speedleLength = null
        speedleGuessesUsed = 0
        speedleWordId = null
    }

    fun loadDailyWord() {
        _gameState.value = GameState.LOADING
        viewModelScope.launch {
            try {
                val resp = wordApi.getToday()
                val meta = resp.body()
                if (resp.isSuccessful && meta != null) {
                    _today.value = meta

                    if (meta.played) {
                        // Already played → fetch stored board and lock
                        loadMyResultAndRender(meta.date, meta.lang)
                    } else {
                        // Fresh board
                        speedleLength = null
                        resetBoard(meta.length)
                        _gameState.value = GameState.PLAYING
                    }
                } else {
                    Log.e("WordleVM", "getToday failed: ${resp.code()} ${resp.message()}")
                    _userMessage.value = "Couldn’t load today’s word."
                    _gameState.value = GameState.ERROR
                }
            } catch (e: Exception) {
                Log.e("WordleVM", "getToday error", e)
                _userMessage.value = "Network error. Try again."
                _gameState.value = GameState.ERROR
            }
        }
    }


    /** Send today's result to the server so the user is locked for the day. */
    private suspend fun submitDailyResult(won: Boolean) {
        val meta = _today.value ?: return
        val guesses = collectGuessesSoFar()
        runCatching {
            wordApi.submitDaily(
                SubmitDailyRequest(
                    date = meta.date,
                    lang = meta.lang,
                    guesses = guesses,
                    won = won,
                    durationSec = null,   // (optional) add if you track time
                    clientId = null       // (optional) add if you generate an idempotency key
                )
            )
        }
    }

    private fun writeGuessRow(rowIndex: Int, guess: String) {
        val len = guess.length
        val start = rowIndex * len
        _boardLetters.update { list ->
            list.toMutableList().apply {
                for (i in 0 until len) this[start + i] = guess[i].toString()
            }
        }
        _boardStates.update { list ->
            list.toMutableList().apply {
                for (i in 0 until len) this[start + i] = TileState.FILLED
            }
        }
        // keep internal cursors in sync so UI looks consistent
        currentGuessRow = rowIndex
        currentPosition = start + len
    }

    private fun applyFeedbackRow(rowIndex: Int, codes: List<String>) {
        val len = codes.size
        val start = rowIndex * len
        _boardStates.update { list ->
            list.toMutableList().apply {
                for (i in 0 until len) {
                    this[start + i] = when (codes[i]) {
                        "G" -> TileState.CORRECT
                        "Y" -> TileState.PRESENT
                        else -> TileState.ABSENT
                    }
                }
            }
        }
    }

    private suspend fun loadMyResultAndRender(date: String, lang: String) {
        val resp = wordApi.getMyResult(date = date, lang = lang)
        if (!resp.isSuccessful) {
            // Token problem or result not found — fail closed (lock input)
            _gameState.value = GameState.LOST
            _userMessage.value = "You’ve already played today. Come back tomorrow!"
            return
        }
        val body = resp.body() ?: return
        // reset board to the correct width
        resetBoard(len = (body.guesses.firstOrNull()?.length ?: 5))

        body.guesses.forEachIndexed { row, guess ->
            writeGuessRow(row, guess.uppercase())
            val fb = body.feedbackRows.getOrNull(row) ?: emptyList()
            if (fb.isNotEmpty()) applyFeedbackRow(row, fb)
        }

        // Lock the board by moving to a terminal state
        _gameState.value = if (body.won) GameState.WON else GameState.LOST
        _userMessage.value = "You’ve already played today. Come back tomorrow!"
    }

    private fun collectGuessesSoFar(): List<String> {
        val len = currentLen
        val rowsUsed = currentGuessRow + 1  // row index is 0-based
        val result = mutableListOf<String>()
        for (row in 0 until rowsUsed) {
            val start = row * len
            val end = start + len
            val guess = _boardLetters.value.subList(start, end).joinToString("")
            if (guess.length == len && guess.all { it.isLetter() }) {
                result.add(guess.uppercase())
            }
        }
        return result
    }




    // ---------- SPEEDLE ----------

    fun startSpeedleSession(durationSec: Int, countdownSec: Int = 3) {
        _mode.value = GameMode.SPEEDLE
        _gameState.value = GameState.LOADING

        viewModelScope.launch {
            try {
                val startResp = speedleApi.start(SpeedleStartRequest(durationSec = durationSec))
                val body = startResp.body()
                if (!startResp.isSuccessful || body == null) {
                    _userMessage.value = "Couldn’t start Speedle."
                    _gameState.value = GameState.ERROR
                    return@launch
                }
                speedleSessionId = body.sessionId
                speedleWordId = body.wordId
                speedleDuration = durationSec
                speedleLength = (body.length).coerceIn(3, 7)
                speedleGuessesUsed = 0

                resetBoard(speedleLength!!)
                _gameState.value = GameState.PLAYING

                _remainingSeconds.value = body.durationSec
                _preCountdownSeconds.value = countdownSec
                while ((_preCountdownSeconds.value ?: 0) > 0 && _gameState.value == GameState.PLAYING) {
                    delay(1000)
                    _preCountdownSeconds.value = (_preCountdownSeconds.value ?: 0) - 1
                }
                _preCountdownSeconds.value = null
                startTimer()
            } catch (e: Exception) {
                Log.e("WordleVM", "startSpeedle error", e)
                _userMessage.value = "Network error. Try again."
                _gameState.value = GameState.ERROR
            }
        }
    }

    /** Finish Speedle → server + local stats */
    private fun finishSpeedle(endReason: String) {
        val sessionId = speedleSessionId ?: return
        val wordId = speedleWordId
        val duration = speedleDuration

        viewModelScope.launch {
            try {
                val resp = speedleApi.finish(
                    SpeedleFinishRequest(
                        sessionId = sessionId,
                        endReason = endReason,
                        clientGuessesUsed = speedleGuessesUsed,
                        clientTimeTakenSec = duration - (_remainingSeconds.value ?: 0),
                        displayName = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.displayName
                    )
                )

                val body = resp.body()
                if (body != null) {
                    _summary.value = EndGameSummary(body.definition, body.synonym, body.won)
                    _gameState.value = if (body.won) GameState.WON else GameState.LOST

                    // ✅ record locally via SpeedleStatsManager
                    SpeedleStatsManager.recordRun(
                        context = appContext,
                        won = body.won,
                        durationSec = duration,
                        timeRemainingSec = body.timeRemainingSec,
                        guessesUsed = body.guessesUsed,
                        wordId = wordId
                    )
                } else {
                    _summary.value = EndGameSummary(null, null, endReason == "won")
                }
            } catch (e: Exception) {
                _summary.value = EndGameSummary(null, null, endReason == "won")
            } finally {
                stopTimer()
            }
        }
    }

    fun useSpeedleDefinitionHint() {
        val sessionId = speedleSessionId ?: run {
            _userMessage.value = "Start a Speedle run first."
            return
        }
        if (_mode.value != GameMode.SPEEDLE || _gameState.value != GameState.PLAYING) return

        viewModelScope.launch {
            try {
                val resp = speedleApi.hint(SpeedleHintRequest(sessionId = sessionId))
                val body = resp.body()
                if (resp.isSuccessful && body != null) {
                    _remainingSeconds.value = body.remainingSec
                    _hintMessage.value = body.definition ?: "No hint available."
                } else {
                    _userMessage.value = "Couldn’t use hint."
                }
            } catch (e: Exception) {
                _userMessage.value = "Network error. Try again."
            }
        }
    }

    // ---------- Board ops ----------

    fun resetBoard(len: Int = currentLen) {
        currentPosition = 0
        currentGuessRow = 0
        _attemptIndex.value = 0
        _boardLetters.value = List(6 * len) { "" }
        _boardStates.value = List(6 * len) { TileState.EMPTY }
        _summary.value = null
        _hintMessage.value = null
    }

    fun handleLetterInput(key: Char) {
        if (_gameState.value != GameState.PLAYING) return
        val len = currentLen
        val maxPosition = (currentGuessRow + 1) * len
        if (currentPosition < maxPosition) {
            _boardLetters.update { list ->
                list.toMutableList().apply { this[currentPosition] = key.toString() }
            }
            _boardStates.update { list ->
                list.toMutableList().apply { this[currentPosition] = TileState.FILLED }
            }
            currentPosition++
        }
    }

    fun deleteLetter() {
        if (_gameState.value != GameState.PLAYING) return
        val len = currentLen
        val minPosition = currentGuessRow * len
        if (currentPosition > minPosition) {
            currentPosition--
            _boardLetters.update { list ->
                list.toMutableList().apply { this[currentPosition] = "" }
            }
            _boardStates.update { list ->
                list.toMutableList().apply { this[currentPosition] = TileState.EMPTY }
            }
        }
    }

    fun processGuess() {
        if (_gameState.value != GameState.PLAYING) return
        if (_isSubmitting.value) return

        val len = currentLen
        val start = currentGuessRow * len
        val end = start + len
        if (currentPosition < end) {
            _userMessage.value = "Not enough letters."
            return
        }
        val guess = _boardLetters.value.subList(start, end).joinToString("").uppercase()

        if (_mode.value == GameMode.SPEEDLE) {
            submitGuessSpeedle(start, guess)
        } else {
            submitGuessDaily(start, guess)
        }
    }

    // ----- DAILY submit -----
    private fun submitGuessDaily(start: Int, guess: String) {
        val meta = _today.value ?: return
        viewModelScope.launch {
            _isSubmitting.value = true
            try {
                val resp = wordApi.validateGuess(GuessRequest(guess, meta.lang, meta.date))
                if (resp.isSuccessful) {
                    val body = resp.body()
                    if (body != null) {
                        applyFeedbackToRow(start, body.feedback)
                        if (body.won) {
                            _gameState.value = GameState.WON
                            stopTimer()


                            // fire-and-forget submit
                            viewModelScope.launch { submitDailyResult(won = true) }

                            loadEndSummaryDaily(true)
                        } else {
                            advanceOrLoseDaily()
                        }
                    }
                } else {
                    _userMessage.value = "Invalid guess."
                }
            } catch (e: Exception) {
                _userMessage.value = "Network error."
            } finally {
                _isSubmitting.value = false
            }
        }
    }

    // ----- SPEEDLE submit -----
    private fun submitGuessSpeedle(start: Int, guess: String) {
        val sessionId = speedleSessionId ?: return
        viewModelScope.launch {
            _isSubmitting.value = true
            try {
                val resp = speedleApi.validate(SpeedleValidateRequest(sessionId, guess))
                val body = resp.body()
                if (resp.isSuccessful && body != null) {
                    applyFeedbackToRow(start, body.feedback)
                    _remainingSeconds.value = body.remainingSec
                    speedleGuessesUsed = body.guessesUsed

                    if (body.won) {
                        _gameState.value = GameState.WON
                        finishSpeedle("won")
                    } else {
                        if (currentGuessRow < 5) {
                            currentGuessRow++
                            _attemptIndex.value = currentGuessRow
                        } else {
                            _gameState.value = GameState.LOST
                            finishSpeedle("attempts")
                        }
                    }
                } else {
                    _userMessage.value = "Invalid guess."
                }
            } catch (e: Exception) {
                _userMessage.value = "Network error."
            } finally {
                _isSubmitting.value = false
            }
        }
    }

    private fun applyFeedbackToRow(start: Int, codes: List<String>) {
        val newStates = _boardStates.value.toMutableList()
        codes.forEachIndexed { i, code ->
            newStates[start + i] = when (code) {
                "G" -> TileState.CORRECT
                "Y" -> TileState.PRESENT
                else -> TileState.ABSENT
            }
        }
        _boardStates.value = newStates
    }

    private fun advanceOrLoseDaily() {
        if (currentGuessRow < 5) {
            currentGuessRow++
            _attemptIndex.value = currentGuessRow
        } else {
            _gameState.value = GameState.LOST
            stopTimer()
            viewModelScope.launch { submitDailyResult(won = false) }
            loadEndSummaryDaily(false)
        }
    }

    // ---------- DAILY summary ----------
    private fun loadEndSummaryDaily(won: Boolean) {
        val meta = _today.value ?: return
        viewModelScope.launch {
            try {
                val defResp = wordApi.getDefinition(meta.lang, meta.date).body()
                val synResp = wordApi.getSynonym(meta.lang, meta.date).body()
                _summary.value = EndGameSummary(defResp?.definition?.definition, synResp?.synonym, won)
            } catch (_: Exception) {
                _summary.value = EndGameSummary(null, null, won)
            }
        }
    }

    // ---------- Timer helpers ----------
    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while ((_remainingSeconds.value ?: 0) > 0 && _gameState.value == GameState.PLAYING) {
                delay(1000)
                _remainingSeconds.value = (_remainingSeconds.value ?: 0) - 1
            }
            if ((_remainingSeconds.value ?: 0) <= 0 && _gameState.value == GameState.PLAYING) {
                if (_mode.value == GameMode.SPEEDLE) {
                    _gameState.value = GameState.LOST
                    finishSpeedle("timeout")
                } else {
                    _gameState.value = GameState.LOST
                    loadEndSummaryDaily(false)
                }
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopTimer()
    }

    // --- Daily hint helpers ---
    fun revealDefinitionHint() {
        viewModelScope.launch {
            val meta = _today.value ?: return@launch
            try {
                val resp = wordApi.getDefinition(meta.lang, meta.date)
                if (resp.isSuccessful) {
                    _hintMessage.value = resp.body()?.definition?.definition ?: "No definition found."
                } else {
                    _userMessage.value = "Couldn’t load definition."
                }
            } catch (e: Exception) {
                _userMessage.value = "Network error."
            }
        }
    }
    // --- Public helpers for local/multiplayer flows ---

    /** Returns the current row index (0..5). */
    fun currentRow(): Int = currentGuessRow

    /** Returns true if the current row is fully filled for the given len. */
    fun isCurrentRowFilled(len: Int = currentLen): Boolean {
        val end = (currentGuessRow + 1) * len
        return currentPosition >= end
    }

    /** Returns the current row guess (uppercase) if filled, else null. */
    fun getCurrentRowGuess(len: Int = currentLen): String? {
        val start = currentGuessRow * len
        val end = start + len
        if (currentPosition < end) return null
        return _boardLetters.value.subList(start, end).joinToString("").uppercase()
    }

    /**
     * Apply local feedback codes (["G","Y","A"]) to the current row and advance.
     * Returns true if this row is a win (all "G"), false otherwise.
     */
    fun applyLocalFeedbackAndAdvance(codes: List<String>): Boolean {
        val len = currentLen
        val start = currentGuessRow * len
        applyFeedbackToRow(start, codes) // existing private method

        val won = codes.all { it == "G" }
        if (won) {
            _gameState.value = GameState.WON
        } else {
            advanceOrLoseDaily() // uses same 6-row flow
        }
        return won
    }


    fun clearHint() {
        _hintMessage.value = null
    }
}
