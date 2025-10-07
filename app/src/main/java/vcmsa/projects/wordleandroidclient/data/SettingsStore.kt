// app/src/main/java/vcmsa/projects/wordleandroidclient/data/SettingsStore.kt
package vcmsa.projects.wordleandroidclient.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("wordle_prefs")

object SettingsStore {
    // -------- App settings --------
    private val DARK_THEME = booleanPreferencesKey("dark_theme")
    private val HAPTICS    = booleanPreferencesKey("haptics")
    private val SOUNDS     = booleanPreferencesKey("sounds")

    fun darkThemeFlow(ctx: Context): Flow<Boolean> =
        ctx.dataStore.data.map { it[DARK_THEME] ?: false }

    fun hapticsFlow(ctx: Context): Flow<Boolean> =
        ctx.dataStore.data.map { it[HAPTICS] ?: true }

    fun soundsFlow(ctx: Context): Flow<Boolean> =
        ctx.dataStore.data.map { it[SOUNDS] ?: true }

    suspend fun setDarkTheme(ctx: Context, v: Boolean) {
        ctx.dataStore.edit { it[DARK_THEME] = v }
    }
    suspend fun setHaptics(ctx: Context, v: Boolean) {
        ctx.dataStore.edit { it[HAPTICS] = v }
    }
    suspend fun setSounds(ctx: Context, v: Boolean) {
        ctx.dataStore.edit { it[SOUNDS] = v }
    }

    // -------- Daily lock + saved game state (PER USER tracking) --------
    private val LAST_PLAYED_DATE   = stringPreferencesKey("last_played_date")
    private val LAST_PLAYED_USER   = stringPreferencesKey("last_played_user")  // NEW: Track which user played
    private val LAST_GUESSES       = stringPreferencesKey("last_game_guesses")
    private val LAST_FEEDBACK_ROWS = stringPreferencesKey("last_game_feedback_rows")

    /**
     * Store that a specific user played on a specific date.
     * This allows different users to play on the same device.
     */
    suspend fun setLastPlayedDate(ctx: Context, date: String, userId: String) {
        ctx.dataStore.edit {
            it[LAST_PLAYED_DATE] = date
            it[LAST_PLAYED_USER] = userId
        }
    }

    /**
     * Check if THIS specific user has already played on this date.
     * Returns true only if the stored date matches AND the stored user matches.
     */
    suspend fun hasUserPlayedToday(ctx: Context, date: String, userId: String): Boolean {
        val prefs = ctx.dataStore.data.first()
        val lastDate = prefs[LAST_PLAYED_DATE]
        val lastUser = prefs[LAST_PLAYED_USER]

        return lastDate == date && lastUser == userId
    }

    /**
     * Legacy function for backwards compatibility (unsigned users or old code).
     * Returns the last played date without checking user.
     */
    suspend fun getLastPlayedDate(ctx: Context): String? =
        ctx.dataStore.data.map { it[LAST_PLAYED_DATE] }.first()

    /**
     * Save the local board so we can render it later if user isn't signed in.
     * We store compact strings:
     *  - guesses: "CRANE|BREAD|SMILE"
     *  - feedbackRows: each row like "AYAAG", joined as "AYAAG|GAGAA|GYYGA"
     */
    suspend fun saveLastGameState(
        ctx: Context,
        guesses: List<String>,
        feedbackRows: List<List<String>>
    ) {
        val guessesStr = guesses.joinToString("|") { it.uppercase() }
        val feedbackStr = feedbackRows.joinToString("|") { row ->
            row.joinToString("") { it } // "GYAAY"
        }
        ctx.dataStore.edit {
            it[LAST_GUESSES] = guessesStr
            it[LAST_FEEDBACK_ROWS] = feedbackStr
        }
    }

    /**
     * Load the last saved local board.
     * @return Pair<guesses, feedbackRows> or null if nothing saved.
     */
    suspend fun getLastGameState(ctx: Context): Pair<List<String>, List<List<String>>>? {
        val prefs = ctx.dataStore.data.first()
        val g = prefs[LAST_GUESSES]
        val f = prefs[LAST_FEEDBACK_ROWS]
        if (g.isNullOrBlank() || f.isNullOrBlank()) return null

        val guesses = g.split("|").filter { it.isNotBlank() }

        val feedbackRows: List<List<String>> = f.split("|").map { row ->
            // row is like "GYAAY" -> ["G","Y","A","A","Y"]
            row.trim().map { it.toString() }
        }

        return guesses to feedbackRows
    }

    /** Clear the saved local board (use if you ever want to reset). */
    suspend fun clearLastGameState(ctx: Context) {
        ctx.dataStore.edit {
            it.remove(LAST_GUESSES)
            it.remove(LAST_FEEDBACK_ROWS)
        }
    }
}