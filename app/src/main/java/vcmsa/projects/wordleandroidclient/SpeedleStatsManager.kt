package vcmsa.projects.wordleandroidclient

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SpeedleRun(
    val dateISO: String,
    val won: Boolean,
    val durationSec: Int,
    val timeRemainingSec: Int,
    val guessesUsed: Int,
    val wordId: String?
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("dateISO", dateISO)
            put("won", won)
            put("durationSec", durationSec)
            put("timeRemainingSec", timeRemainingSec)
            put("guessesUsed", guessesUsed)
            put("wordId", wordId ?: JSONObject.NULL)
        }
    }

    companion object {
        fun fromJson(obj: JSONObject): SpeedleRun {
            return SpeedleRun(
                dateISO = obj.optString("dateISO"),
                won = obj.optBoolean("won"),
                durationSec = obj.optInt("durationSec"),
                timeRemainingSec = obj.optInt("timeRemainingSec"),
                guessesUsed = obj.optInt("guessesUsed"),
                wordId = obj.optString("wordId", null)
            )
        }
    }
}

object SpeedleStatsManager {
    private const val PREFS_NAME = "speedle_stats"
    private const val KEY_RUNS = "runs"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /** Save a completed run (append to history). */
    fun recordRun(
        context: Context,
        won: Boolean,
        durationSec: Int,
        timeRemainingSec: Int,
        guessesUsed: Int,
        wordId: String?
    ) {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        val nowIso = sdf.format(Date())

        val run = SpeedleRun(
            dateISO = nowIso,
            won = won,
            durationSec = durationSec,
            timeRemainingSec = timeRemainingSec,
            guessesUsed = guessesUsed,
            wordId = wordId
        )

        val prefs = getPrefs(context)
        val arr = JSONArray(prefs.getString(KEY_RUNS, "[]"))
        arr.put(run.toJson())

        prefs.edit().putString(KEY_RUNS, arr.toString()).apply()
    }

    /** Get full run history. */
    fun getRuns(context: Context): List<SpeedleRun> {
        val prefs = getPrefs(context)
        val arr = JSONArray(prefs.getString(KEY_RUNS, "[]"))
        val result = mutableListOf<SpeedleRun>()
        for (i in 0 until arr.length()) {
            result.add(SpeedleRun.fromJson(arr.getJSONObject(i)))
        }
        return result
    }

    /** Clear history (for debugging/reset). */
    fun clear(context: Context) {
        getPrefs(context).edit().remove(KEY_RUNS).apply()
    }
}
