package vcmsa.projects.wordleandroidclient

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import vcmsa.projects.wordleandroidclient.api.RetrofitClient
import vcmsa.projects.wordleandroidclient.multiplayer.*

import java.util.UUID

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: WordleViewModel

    private lateinit var gameBoardRecyclerView: RecyclerView
    private lateinit var gameBoardAdapter: GameBoardAdapter

    private lateinit var hintDefinitionContainer: android.view.View
    private lateinit var hintSynonymContainer: android.view.View

    private lateinit var btnEnter: Button
    private lateinit var btnBackspace: Button

    private lateinit var btnBackButton: ImageButton

    private lateinit var btnHintDefinition: ImageButton
    private lateinit var btnHintSynonym: ImageButton
    private var lastHintTitle: String = "Hint"
    private var opponentLoop: AiOpponent? = null
    private var friendEventsJob: Job? = null

    private var playMode: String = "DAILY"

    // Friends MP extras
    private var roomCode: String? = null
    private var targetWord: String? = null

    // Shared opponent view
    private var opponentView: OpponentProgressView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // --- ViewModel (injecting applicationContext via factory) ---
        val factory = WordleViewModelFactory(
            appContext = applicationContext,
            wordApi = RetrofitClient.wordService,
            speedleApi = RetrofitClient.speedleService
        )
        viewModel = ViewModelProvider(this, factory)[WordleViewModel::class.java]

        // --- Layout refs ---
        opponentView       = findViewById(R.id.opponentProgress)
        btnEnter           = findViewById(R.id.btnEnter)
        btnBackspace       = findViewById(R.id.btnBackspace)
        btnBackButton      = findViewById(R.id.btnBack)
        btnHintDefinition  = findViewById(R.id.btnHintDefinition)
        btnHintSynonym     = findViewById(R.id.btnHintSynonym)



        hintDefinitionContainer = findViewById(R.id.hintDefinitionContainer)
        hintSynonymContainer    = findViewById(R.id.hintSynonymContainer)
        // Do NOT wire hint click listeners here anymore — we set them per-mode later.

        btnBackButton.setOnClickListener {
            startActivity(Intent(this, DashboardActivity::class.java))
        }

        // --- RecyclerView (5 columns initially; will still look fine for 3–7) ---
        gameBoardRecyclerView = findViewById(R.id.rvGameBoard)
        gameBoardAdapter = GameBoardAdapter(
            letters = viewModel.boardLetters.value,
            states  = viewModel.boardStates.value
        )
        gameBoardRecyclerView.layoutManager = GridLayoutManager(this, 5)
        gameBoardRecyclerView.adapter = gameBoardAdapter

        // --- Observe board updates ---
        lifecycleScope.launch {
            viewModel.boardLetters.collect { letters ->
                gameBoardAdapter.updateLetters(letters)
                Log.d("WordRush", "Board letters updated: ${letters.size}")
            }
        }
        lifecycleScope.launch {
            viewModel.boardStates.collect { states ->
                gameBoardAdapter.updateStates(states)
            }
        }

        // Timer display observer
        lifecycleScope.launch {
            viewModel.remainingSeconds.collect { seconds ->
                val timerView = findViewById<TextView>(R.id.tvSpeedleTimer)
                if (seconds != null && viewModel.mode.value == GameMode.SPEEDLE) {
                    timerView?.visibility = android.view.View.VISIBLE
                    val mins = seconds / 60
                    val secs = seconds % 60
                    timerView?.text = String.format("%d:%02d", mins, secs)
                    timerView?.setTextColor(
                        if (seconds <= 10) android.graphics.Color.RED
                        else android.graphics.Color.parseColor("#333333")
                    )
                } else {
                    timerView?.visibility = android.view.View.GONE
                }
            }
        }

        // Pre-countdown display
        lifecycleScope.launch {
            viewModel.preCountdownSeconds.collect { seconds ->
                if (seconds != null && seconds > 0) {
                    Toast.makeText(this@MainActivity, "Starting in $seconds...", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // --- End-of-game summary dialogs ---
        lifecycleScope.launch {
            viewModel.summary.collect { summary ->
                if (summary != null) {
                    if (viewModel.mode.value == GameMode.DAILY) {
                        showDailyEndDialog(summary)
                    } else {
                        showEndSummaryDialog(summary)
                    }
                }
            }
        }
        lifecycleScope.launch {
            viewModel.gameState.collect { state ->
                if (!viewModel.isLoadingPreviousResult.value) {
                    when (state) {
                        GameState.WON  -> StatsManager.recordGame(applicationContext, won = true)
                        GameState.LOST -> StatsManager.recordGame(applicationContext, won = false)
                        else -> Unit
                    }
                }
            }
        }

        // --- Toasts for user messages ---
        lifecycleScope.launch {
            viewModel.userMessage.collect { msg ->
                if (!msg.isNullOrBlank()) {
                    Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                }
            }
        }

        // --- Keyboard wiring (default listeners; may be overridden in MP handlers) ---
        setupKeyboardListeners()

        // Disable ENTER while a request is in flight (server modes)
        lifecycleScope.launch {
            viewModel.isSubmitting.collect { busy -> btnEnter.isEnabled = !busy }
        }

        // Show hint content in a dialog when VM sets it
        lifecycleScope.launch {
            viewModel.hintMessage.collect { msg ->
                if (msg != null) {
                    androidx.appcompat.app.AlertDialog.Builder(this@MainActivity)
                        .setTitle(lastHintTitle)
                        .setMessage(msg)
                        .setPositiveButton("OK") { d, _ -> d.dismiss() }
                        .show()
                    viewModel.clearHint()
                }
            }
        }

        // --- Decide Mode from Intent Extras ---
        playMode = intent.getStringExtra("mode") ?: "DAILY"
        when (playMode) {
            "SPEEDLE" -> {
                val seconds = intent.getIntExtra("seconds", 90)
                startSpeedle(seconds)
                opponentView?.visibility = android.view.View.GONE
            }
            "AI_MULTIPLAYER" -> {
                opponentView?.visibility = android.view.View.VISIBLE
                startAiMultiplayer()
            }
            "FRIENDS_MULTIPLAYER" -> {
                opponentView?.visibility = android.view.View.VISIBLE
                roomCode   = intent.getStringExtra("roomCode")
                targetWord = intent.getStringExtra("targetWord")
                if (roomCode.isNullOrBlank() || targetWord.isNullOrBlank()) {
                    Toast.makeText(this, "Missing multiplayer data", Toast.LENGTH_SHORT).show()
                    finish(); return
                }
                startFriendsMultiplayer(roomCode!!, targetWord!!)
            }
            else -> {
                // DAILY
                opponentView?.visibility = android.view.View.GONE
                lifecycleScope.launch {
                    viewModel.setModeDaily()
                    viewModel.loadDailyWord()
                }
            }
        }

        // Wire hint buttons visibility/behavior based on the resolved mode
        wireHintButtonsForMode(playMode)
    }

    // -------------------------
    // MODE HANDLERS
    // -------------------------

    private fun startSpeedle(seconds: Int) {
        viewModel.startSpeedleSession(seconds)
    }

    /** On-device AI race (same word, first to solve). */
    private fun startAiMultiplayer() {
        val words = try {
            assets.open("wordlist_en_5.txt")
                .bufferedReader().readLines()
                .map { it.trim().uppercase() }.filter { it.length == 5 }
        } catch (_: Exception) { emptyList() }
        val localTarget = if (words.isNotEmpty()) words.random() else "CRANE"

        viewModel.startLocalAiMatch(5)

        opponentView?.visibility = android.view.View.VISIBLE
        opponentView?.bind(OpponentProgress(status = "Ready", row = 0))

        val diffName = intent.getStringExtra("aiDifficulty") ?: "MEDIUM"
        val diff = AiDifficulty.valueOf(diffName)
        val ai = AiOpponent(
            appContext = applicationContext,
            scope = lifecycleScope,
            difficulty = diff,
            targetWord = localTarget,
            wordLength = 5,
            onProgress = { guess, fb, row ->
                opponentView?.bind(
                    OpponentProgress(
                        lastGuess = guess, lastFeedback = fb, row = row, status = "Thinking…"
                    )
                )
                if (fb.all { it == "G" } && viewModel.gameState.value == GameState.PLAYING) {
                    androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("AI wins ")
                        .setMessage("The word was $localTarget.")
                        .setPositiveButton("OK") { d, _ -> d.dismiss(); finish() }
                        .show()
                }
            },
            onWin = { /* handled above */ },
            playerRowProvider = { viewModel.currentRow() }
        )
        opponentLoop = ai
        ai.start()

        btnEnter.setOnClickListener {
            val guess = viewModel.getCurrentRowGuess(5)
            if (guess == null) {
                Toast.makeText(this, "Not enough letters.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val fb = LocalWordJudge.feedback(guess, localTarget)
            val won = viewModel.applyLocalFeedbackAndAdvance(fb)
            if (won) {
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("You win 🎉")
                    .setMessage("Great job! The word was $localTarget.")
                    .setPositiveButton("OK") { d, _ -> d.dismiss(); finish() }
                    .show()
            } else if (viewModel.gameState.value == GameState.LOST) {
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("AI wins 🧠")
                    .setMessage("The word was $localTarget.")
                    .setPositiveButton("OK") { d, _ -> d.dismiss(); finish() }
                    .show()
            }
        }
    }

    /** Show/hide and wire hint buttons per mode. */
    private fun wireHintButtonsForMode(mode: String) {
        // clear previous listeners
        btnHintDefinition.setOnClickListener(null)
        btnHintSynonym.setOnClickListener(null)

        when (mode) {
            "SPEEDLE" -> {
                hintDefinitionContainer.visibility = android.view.View.VISIBLE
                hintSynonymContainer.visibility    = android.view.View.GONE

                btnHintDefinition.setOnClickListener {
                    androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("Use hint?")
                        .setMessage("Reveal the definition (−10s). Continue?")
                        .setPositiveButton("Use hint") { d, _ ->
                            lastHintTitle = "Hint: Definition"
                            viewModel.useSpeedleDefinitionHint()
                            d.dismiss()
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }
            }
            "AI_MULTIPLAYER" -> {
                hintDefinitionContainer.visibility = android.view.View.GONE
                hintSynonymContainer.visibility    = android.view.View.GONE
            }
            "FRIENDS_MULTIPLAYER", "DAILY" -> {
                hintDefinitionContainer.visibility = android.view.View.VISIBLE
                hintSynonymContainer.visibility    = android.view.View.VISIBLE

                btnHintDefinition.setOnClickListener {
                    lastHintTitle = "Hint: Definition"
                    viewModel.revealDefinitionHint()
                }
                btnHintSynonym.setOnClickListener {
                    lastHintTitle = "Hint: Synonym"
                    viewModel.revealSynonymHint()
                }
            }
            else -> {
                hintDefinitionContainer.visibility = android.view.View.VISIBLE
                hintSynonymContainer.visibility    = android.view.View.VISIBLE
                btnHintDefinition.setOnClickListener {
                    lastHintTitle = "Hint: Definition"
                    viewModel.revealDefinitionHint()
                }
                btnHintSynonym.setOnClickListener {
                    lastHintTitle = "Hint: Synonym"
                    viewModel.revealSynonymHint()
                }
            }
        }
    }


    /** Friends race via Firestore events. */
    private fun startFriendsMultiplayer(code: String, target: String) {
        val repo = MultiplayerRepository()
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: UUID.randomUUID().toString()

        val len = target.length.coerceIn(3, 7)
        lifecycleScope.launch {
            viewModel.setModeDaily()
            viewModel.resetBoard(len)
        }

        opponentView?.bind(OpponentProgress(status = "Waiting…", row = 0))

        friendEventsJob?.cancel()
        friendEventsJob = lifecycleScope.launch {
            repo.observeEvents(code).collect { ev ->
                if (ev.userId != uid) {
                    opponentView?.bind(
                        OpponentProgress(
                            lastGuess = ev.guess,
                            lastFeedback = ev.feedback,
                            row = ev.row,
                            status = "Playing"
                        )
                    )
                    if (ev.feedback.all { it == "G" } && viewModel.gameState.value == GameState.PLAYING) {
                        androidx.appcompat.app.AlertDialog.Builder(this@MainActivity)
                            .setTitle("Opponent wins 🧑‍🤝‍🧑")
                            .setMessage("The word was $target.")
                            .setPositiveButton("OK") { d, _ -> d.dismiss(); finish() }
                            .show()
                    }
                }
            }
        }

        btnEnter.setOnClickListener {
            val guess = viewModel.getCurrentRowGuess(len)
            if (guess == null) {
                Toast.makeText(this, "Not enough letters.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val fb = LocalWordJudge.feedback(guess, target)
            val row = viewModel.currentRow()
            val won = viewModel.applyLocalFeedbackAndAdvance(fb)

            lifecycleScope.launch {
                try {
                    repo.postGuess(code, uid, guess, fb, row)
                } catch (_: Exception) {
                    Toast.makeText(this@MainActivity, "Couldn’t send move (offline?)", Toast.LENGTH_SHORT).show()
                }
            }

            if (won) {
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("You win 🎉")
                    .setMessage("Great job! The word was $target.")
                    .setPositiveButton("OK") { d, _ -> d.dismiss(); finish() }
                    .show()
            } else if (viewModel.gameState.value == GameState.LOST) {
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Opponent wins 🧑‍🤝‍🧑")
                    .setMessage("The word was $target.")
                    .setPositiveButton("OK") { d, _ -> d.dismiss(); finish() }
                    .show()
            }
        }
    }

    // -------------------------
    // Shared UI bits
    // -------------------------

    private fun setupKeyboardListeners() {
        val letterKeys = listOf(
            R.id.btnQ, R.id.btnW, R.id.btnE, R.id.btnR, R.id.btnT, R.id.btnY, R.id.btnU, R.id.btnI, R.id.btnO, R.id.btnP,
            R.id.btnA, R.id.btnS, R.id.btnD, R.id.btnF, R.id.btnG, R.id.btnH, R.id.btnJ, R.id.btnK, R.id.btnL,
            R.id.btnZ, R.id.btnX, R.id.btnC, R.id.btnV, R.id.btnB, R.id.btnN, R.id.btnM
        )
        letterKeys.forEach { id ->
            findViewById<Button>(id)?.setOnClickListener { b ->
                val letter = (b as Button).text.toString().first()
                viewModel.handleLetterInput(letter)
            }
        }
        btnEnter.setOnClickListener { viewModel.processGuess() }
        btnBackspace.setOnClickListener { viewModel.deleteLetter() }
    }

    private fun showEndSummaryDialog(summary: EndGameSummary) {
        val title = if (summary.won) "You won! 🎉" else "Nice try!"
        val defLine  = summary.definition?.let { "Definition: $it" } ?: "Definition: (not available)"
        val synLine  = summary.synonym?.let { "Synonym: $it" } ?: "Synonym: (not available)"
        val wordLine = summary.word?.let { "Word: ${it.uppercase()}" }

        val parts = mutableListOf<String>()
        if (wordLine != null) parts += wordLine
        parts += defLine
        parts += synLine

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(parts.joinToString("\n\n"))
            .setPositiveButton("OK") { d, _ -> d.dismiss() }
            .show()
    }

    private fun showDailyEndDialog(summary: EndGameSummary) {
        val title = if (summary.won) "You solved today's Wordle! 🎉" else "Daily attempt over!"
        val wordLine = summary.word?.let { "Word: ${it.uppercase()}" } ?: "Word: (unavailable)"
        val defLine  = summary.definition?.let { "Definition: $it" } ?: "Definition: (not available)"
        val synLine  = summary.synonym?.let { "Synonym: $it" } ?: "Synonym: (not available)"

        val message = listOf(wordLine, defLine, synLine, "See you again tomorrow!")
            .joinToString("\n\n")

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK") { d, _ -> d.dismiss() }
            .show()
    }

    override fun onStop() {
        opponentLoop?.stop()
        super.onStop()
    }

    override fun onDestroy() {
        opponentLoop?.stop()
        friendEventsJob?.cancel()
        super.onDestroy()
    }
}
