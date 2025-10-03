package vcmsa.projects.wordleandroidclient

import android.content.Context
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import java.util.Locale


object StatsManager {
    private const val PREF = "wordrush_stats"
    private const val K_PLAYED = "played"
    private const val K_WINS = "wins"
    private const val K_LOSSES = "losses"
    private const val K_CUR_STREAK = "currentStreak"
    private const val K_MAX_STREAK = "maxStreak"
    private const val K_LAST_PLAYED = "lastPlayedIso"

    fun recordGame(ctx: Context, won: Boolean) {
        val sp = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val played = sp.getInt(K_PLAYED, 0) + 1
        val wins = sp.getInt(K_WINS, 0) + if (won) 1 else 0
        val losses = sp.getInt(K_LOSSES, 0) + if (won) 0 else 1

        val currentStreakBefore = sp.getInt(K_CUR_STREAK, 0)
        val currentStreak = if (won) currentStreakBefore + 1 else 0
        val maxStreak = maxOf(sp.getInt(K_MAX_STREAK, 0), currentStreak)

        sp.edit()
            .putInt(K_PLAYED, played)
            .putInt(K_WINS, wins)
            .putInt(K_LOSSES, losses)
            .putInt(K_CUR_STREAK, currentStreak)
            .putInt(K_MAX_STREAK, maxStreak)
            .putString(K_LAST_PLAYED, getTodayIso())
            .apply()
    }

    fun getStats(ctx: Context): Stats {
        val sp = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val played = sp.getInt(K_PLAYED, 0)
        val wins = sp.getInt(K_WINS, 0)
        val losses = sp.getInt(K_LOSSES, 0)
        val winRate = if (played > 0) ((wins * 100.0 / played) + 0.5).toInt() else 0
        return Stats(
            played = played,
            wins = wins,
            losses = losses,
            currentStreak = sp.getInt(K_CUR_STREAK, 0),
            maxStreak = sp.getInt(K_MAX_STREAK, 0),
            lastPlayedIso = sp.getString(K_LAST_PLAYED, null),
            winRate = winRate
        )
    }

    private fun getTodayIso(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }
}
