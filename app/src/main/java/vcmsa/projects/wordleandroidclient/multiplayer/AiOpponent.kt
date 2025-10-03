package vcmsa.projects.wordleandroidclient.multiplayer

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.random.Random

class AiOpponent(
    private val appContext: Context,
    private val scope: CoroutineScope,
    private val difficulty: AiDifficulty,
    private val targetWord: String,
    private val wordLength: Int,
    private val onProgress: (guess: String, feedback: List<String>, row: Int) -> Unit,
    private val onWin: (rowsUsed: Int) -> Unit
) {
    private var job: Job? = null
    private var row = 0
    private var candidates: MutableList<String> = mutableListOf()
    private var lastGuess: String? = null

    private fun loadWordList(): List<String> {
        val fileName = "wordlist_en_${wordLength}.txt" // put files in /app/src/main/assets
        return try {
            appContext.assets.open(fileName).bufferedReader().useLines { seq ->
                seq.map { it.trim().uppercase() }
                    .filter { it.length == wordLength && it.all { c -> c in 'A'..'Z' } }
                    .toList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun start() {
        candidates = loadWordList().toMutableList()
        if (candidates.isEmpty()) return

        val (guessDelayMs, strictness) = when (difficulty) {
            AiDifficulty.EASY   -> 2400L to 0.60f
            AiDifficulty.MEDIUM -> 1600L to 0.80f
            AiDifficulty.HARD   -> 900L  to 1.00f
        }

        job = scope.launch {
            while (isActive && row < 6) {
                delay(guessDelayMs)
                if (!isActive) break

                val guess = pickNextGuess(strictness)
                lastGuess = guess
                val fb = LocalWordJudge.feedback(guess, targetWord)
                onProgress(guess, fb, row)

                if (fb.all { it == "G" }) {
                    onWin(row + 1)
                    break
                }

                // filter candidates by feedback
                candidates = candidates.filter { cand ->
                    LocalWordJudge.feedback(guess, cand) == fb
                }.toMutableList()

                row++
            }
        }
    }

    fun stop() { job?.cancel(); job = null }

    private fun pickNextGuess(strictness: Float): String {
        if (candidates.isEmpty()) return lastGuess ?: "RAISE"

        // In HARD we always use the filtered candidate list.
        // In easier modes, sometimes explore a frequent-letters word for coverage.
        val explore = when (difficulty) {
            AiDifficulty.HARD -> false
            else -> Random.nextFloat() > strictness
        }

        return if (!explore) {
            // deterministic-ish: pick middle candidate
            candidates[max(0, candidates.size / 2 - 1)]
        } else {
            // simple letter frequency heuristic
            val scores = IntArray(26)
            candidates.take(500).forEach { w ->
                w.toCharArray().distinct().forEach { c -> scores[c - 'A']++ }
            }
            candidates.maxBy { w ->
                w.toCharArray().distinct().sumOf { scores[it - 'A'] }
            }
        }
    }
}
