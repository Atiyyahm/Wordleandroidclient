package vcmsa.projects.wordleandroidclient

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import vcmsa.projects.wordleandroidclient.api.*
import vcmsa.projects.wordleandroidclient.data.SettingsStore

class WordleViewModel(
    private val wordApi: WordApiService,
    private val speedleApi: SpeedleApiService,
    private val appContext: Context
) : ViewModel() {
    private val _isLoadingPreviousResult = MutableStateFlow(false)
    val isLoadingPreviousResult: StateFlow<Boolean> = _isLoadingPreviousResult

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

    //Haptics functions
    private fun hapticTick() {
        viewModelScope.launch {
            if (vcmsa.projects.wordleandroidclient.data.SettingsStore.hapticsFlow(appContext).first()) {
                Haptics.tick(appContext)
            }
        }
    }

    private fun hapticSuccess() {
        viewModelScope.launch {
            if (vcmsa.projects.wordleandroidclient.data.SettingsStore.hapticsFlow(appContext).first()) {
                Haptics.success(appContext)
            }
        }
    }

    fun loadDailyWord() {
        _gameState.value = GameState.LOADING
        viewModelScope.launch {
            try {
                val resp = wordApi.getToday()
                val meta = resp.body()
                if (!resp.isSuccessful || meta == null) {
                    Log.e("WordleVM","getToday failed: ${resp.code()} ${resp.message()}")
                    _userMessage.value = "Couldn't load today's word."
                    _gameState.value = GameState.ERROR
                    return@launch
                }
                _today.value = meta

                val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser

                // 1) Server-authoritative lock if signed in
                if (user != null) {
                    val r = wordApi.getMyResult(date = meta.date, lang = meta.lang)
                    if (r.isSuccessful && r.body() != null) {
                        loadMyResultAndRender(r.body()!!, meta.length)
                        return@launch
                    }
                } else {
                    // 2) Device-only fallback
                    val last = SettingsStore.getLastPlayedDate(appContext)
                    if (last == meta.date) {
                        // Load saved game state
                        val savedState = SettingsStore.getLastGameState(appContext)
                        if (savedState != null) {
                            val (guesses, feedbackRows) = savedState
                            resetBoard(meta.length)

                            guesses.forEachIndexed { row, guess ->
                                writeGuessRow(row, guess.uppercase())
                                val fb = feedbackRows.getOrNull(row) ?: emptyList()
                                if (fb.isNotEmpty()) applyFeedbackRow(row, fb)
                            }
                        } else {
                            resetBoard(meta.length)
                        }

                        _gameState.value = GameState.LOST
                        _userMessage.value = "You've already played today. Come back tomorrow!"
                        return@launch
                    }
                }

                // Fresh board
                speedleLength = null
                resetBoard(meta.length)
                _gameState.value = GameState.PLAYING

            } catch (e: Exception) {
                Log.e("WordleVM","getToday error", e)
                _userMessage.value = "Network error. Try again."
                _gameState.value = GameState.ERROR
            }
        }
    }

    private fun loadMyResultAndRender(body: MyResultResponse, length: Int) {
        _isLoadingPreviousResult.value = true

        // ADD THIS LOGGING:
        Log.e("LOAD_PREVIOUS", "==========================================")
        Log.e("LOAD_PREVIOUS", "Loading previous result:")
        Log.e("LOAD_PREVIOUS", "Guesses: ${body.guesses}")
        Log.e("LOAD_PREVIOUS", "Feedback rows: ${body.feedbackRows}")
        Log.e("LOAD_PREVIOUS", "Won: ${body.won}")
        Log.e("LOAD_PREVIOUS", "Answer: ${body.answer}")
        Log.e("LOAD_PREVIOUS", "==========================================")

        resetBoard(len = length)

        body.guesses.forEachIndexed { row, guess ->
            writeGuessRow(row, guess.uppercase())
            val fb = body.feedbackRows.getOrNull(row) ?: emptyList()
            if (fb.isNotEmpty()) applyFeedbackRow(row, fb)
        }

        _gameState.value = if (body.won) GameState.WON else GameState.LOST
        _userMessage.value = "You've already played today. Come back tomorrow!"

        viewModelScope.launch {
            val def = runCatching { wordApi.getDefinition(body.lang, body.date).body()?.definition?.definition }.getOrNull()
            val syn = runCatching { wordApi.getSynonym(body.lang, body.date).body()?.synonym }.getOrNull()
            _summary.value = EndGameSummary(
                definition = def,
                synonym = syn,
                won = body.won,
                word = body.answer?.uppercase()
            )
        }

        _isLoadingPreviousResult.value = false
    }

    private suspend fun submitDailyResult(won: Boolean): String? {
        val meta = _today.value ?: return null
        val guesses = collectGuessesSoFar()
        return try {
            val resp = wordApi.submitDaily(
                SubmitDailyRequest(
                    date = meta.date,
                    lang = meta.lang,
                    guesses = guesses,
                    won = won,
                    durationSec = null,
                    clientId = null
                )
            )
            resp.body()?.answer?.uppercase()    // 👈 return answer
        } catch (e: Exception) {
            Log.e("WordleVM", "submitDaily error", e)
            null
        }
    }


    private suspend fun writeResultDocFallback(
        date: String,
        lang: String,
        won: Boolean,
        guesses: List<String>
    ) {
        try {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
            val db = FirebaseFirestore.getInstance()
            val docId = "${date}_${lang}_${uid}"

            val feedbackRows = extractFeedbackRows() // already in this class

            val payload = hashMapOf(
                "uid" to uid,
                "date" to date,
                "lang" to lang,
                "mode" to "daily",
                "guesses" to guesses,
                "feedbackRows" to feedbackRows,
                "won" to won,
                "guessCount" to guesses.size,
                "durationSec" to 0,
                "clientFallback" to true, // flag so you can see it came from the app
                "submittedAt" to FieldValue.serverTimestamp()
            )

            db.collection("results").document(docId).set(payload, SetOptions.merge()).await()
            Log.d("WordleVM", "Fallback results doc written: $docId")
        } catch (e: Exception) {
            Log.e("WordleVM", "Fallback write failed", e)
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

    private fun collectGuessesSoFar(): List<String> {
        val len = currentLen
        val rowsUsed = currentGuessRow + 1
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

    private fun extractFeedbackRows(): List<List<String>> {
        val len = currentLen
        val result = mutableListOf<List<String>>()
        for (row in 0..currentGuessRow) {
            val start = row * len
            val rowFeedback = mutableListOf<String>()
            for (i in 0 until len) {
                rowFeedback.add(when (_boardStates.value[start + i]) {
                    TileState.CORRECT -> "G"
                    TileState.PRESENT -> "Y"
                    TileState.ABSENT -> "A"
                    else -> "A"
                })
            }
            result.add(rowFeedback)
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
                    _userMessage.value = "Couldn't start Speedle."
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
                    _summary.value = EndGameSummary(
                        definition = body.definition,
                        synonym = body.synonym,
                        won = body.won,
                        word = body.answer?.uppercase()
                    )
                    _gameState.value = if (body.won) GameState.WON else GameState.LOST

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
                    _userMessage.value = "Couldn't use hint."
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
            hapticTick()

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
                            hapticSuccess()

                            val guesses = collectGuessesSoFar()
                            val feedbackRows = extractFeedbackRows()

                            // Write to Firestore directly
                            writeResultToFirestore(won = true)

                            // Also submit to backend (for stats/leaderboard)
                            val answer = submitDailyResult(won = true)

                            val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"

                            _today.value?.date?.let {
                                SettingsStore.setLastPlayedDate(appContext, it, userId)
                                SettingsStore.saveLastGameState(appContext, guesses, feedbackRows)
                            }

                            loadEndSummaryDaily(true, answer)
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
    private suspend fun writeResultToFirestore(won: Boolean) {
        try {
            val user = FirebaseAuth.getInstance().currentUser ?: return
            val meta = _today.value ?: return
            val guesses = collectGuessesSoFar()
            val feedbackRows = extractFeedbackRows()

            val db = FirebaseFirestore.getInstance()
            val docId = "${meta.date}_${meta.lang}_${user.uid}"

            // Convert each feedback row list into a string like "GYAAG"
            val feedbackStrings = feedbackRows.map { it.joinToString("") }

            val payload = hashMapOf(
                "uid" to user.uid,
                "date" to meta.date,
                "lang" to meta.lang,
                "mode" to "daily",
                "guesses" to guesses,
                "feedbackRows" to feedbackStrings,  // ✅ Flattened list
                "won" to won,
                "guessCount" to guesses.size,
                "durationSec" to 0,
                "submittedAt" to FieldValue.serverTimestamp()
            )

            Log.e("WordleVM", "Writing to Firestore: $docId")
            db.collection("results").document(docId).set(payload).await()
            Log.e("WordleVM", "✅ Successfully wrote to Firestore")

        } catch (e: Exception) {
            Log.e("WordleVM", "❌ Firestore write failed: ${e.message}", e)
        }
    }

    private fun advanceOrLoseDaily() {
        if (currentGuessRow < 5) {
            currentGuessRow++
            _attemptIndex.value = currentGuessRow
        } else {
            _gameState.value = GameState.LOST
            stopTimer()

            val todayDate = _today.value?.date
            val guesses = collectGuessesSoFar()
            val feedbackRows = extractFeedbackRows()

            // MOVE THIS LINE HERE (before the viewModelScope.launch)
            val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"

            viewModelScope.launch {
                writeResultToFirestore(won = false)

                submitDailyResult(won = false)

                todayDate?.let {
                    SettingsStore.setLastPlayedDate(appContext, it, userId)  // NOW userId is in scope
                    SettingsStore.saveLastGameState(appContext, guesses, feedbackRows)
                }

                val meta = _today.value
                val my = if (meta != null) {
                    runCatching { wordApi.getMyResult(meta.date, meta.lang).body() }.getOrNull()
                } else null
                val answer = my?.answer

                loadEndSummaryDaily(false, answer)
            }
        }
    }
    private fun loadEndSummaryDaily(won: Boolean, word: String?) {
        val meta = _today.value ?: return
        viewModelScope.launch {
            try {
                val defResp = wordApi.getDefinition(meta.lang, meta.date).body()
                val synResp = wordApi.getSynonym(meta.lang, meta.date).body()
                _summary.value = EndGameSummary(
                    definition = defResp?.definition?.definition,
                    synonym = synResp?.synonym,
                    won = won,
                    word = word?.uppercase()
                )
            } catch (_: Exception) {
                _summary.value = EndGameSummary(null, null, won, word?.uppercase())
            }
        }
    }

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
                    loadEndSummaryDaily(false, null)
                }
            }
        }
    }
    fun testFirestoreWrite() {
        viewModelScope.launch {
            try {
                val user = FirebaseAuth.getInstance().currentUser
                if (user == null) {
                    Log.e("FIRESTORE_TEST", "❌ No user signed in")
                    return@launch
                }

                Log.e("FIRESTORE_TEST", "✅ User signed in: ${user.uid}")

                val db = FirebaseFirestore.getInstance()
                val testDoc = hashMapOf(
                    "test" to "Hello from app",
                    "timestamp" to FieldValue.serverTimestamp(),
                    "userId" to user.uid
                )

                Log.e("FIRESTORE_TEST", "Writing test document...")
                db.collection("test_collection").document("test_doc").set(testDoc).await()
                Log.e("FIRESTORE_TEST", "✅✅✅ Successfully wrote to Firestore!")

            } catch (e: Exception) {
                Log.e("FIRESTORE_TEST", "❌❌❌ Firestore write failed: ${e.message}", e)
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

    fun revealDefinitionHint() {
        viewModelScope.launch {
            val meta = _today.value ?: return@launch
            try {
                val resp = wordApi.getDefinition(meta.lang, meta.date)
                if (resp.isSuccessful) {
                    _hintMessage.value = resp.body()?.definition?.definition ?: "No definition found."
                } else {
                    _userMessage.value = "Couldn't load definition."
                }
            } catch (e: Exception) {
                _userMessage.value = "Network error."
            }
        }
    }

    fun revealSynonymHint() {
        viewModelScope.launch {
            val meta = _today.value ?: return@launch
            try {
                val resp = wordApi.getSynonym(meta.lang, meta.date)
                if (resp.isSuccessful) {
                    _hintMessage.value = resp.body()?.synonym ?: "No synonym found."
                } else {
                    _userMessage.value = "Couldn't load synonym."
                }
            } catch (e: Exception) {
                _userMessage.value = "Network error."
            }
        }
    }

    fun currentRow(): Int = currentGuessRow

    fun isCurrentRowFilled(len: Int = currentLen): Boolean {
        val end = (currentGuessRow + 1) * len
        return currentPosition >= end
    }

    fun getCurrentRowGuess(len: Int = currentLen): String? {
        val start = currentGuessRow * len
        val end = start + len
        if (currentPosition < end) return null
        return _boardLetters.value.subList(start, end).joinToString("").uppercase()
    }

    fun applyLocalFeedbackAndAdvance(codes: List<String>): Boolean {
        val len = currentLen
        val start = currentGuessRow * len
        applyFeedbackToRow(start, codes)

        val won = codes.all { it == "G" }
        if (won) {
            _gameState.value = GameState.WON
            hapticSuccess()
        } else {
            advanceOrLoseDaily()
        }
        return won
    }

    fun clearHint() {
        _hintMessage.value = null
    }

    fun startLocalAiMatch(len: Int = 5) {
        setModeDaily()           // reuse daily board visuals
        resetBoard(len)          // clears rows
        _gameState.value = GameState.PLAYING
    }
}