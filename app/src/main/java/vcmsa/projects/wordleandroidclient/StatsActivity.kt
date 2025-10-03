package vcmsa.projects.wordleandroidclient

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class StatsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stats)
        supportActionBar?.hide()

        val stats = StatsManager.getStats(this)

        findViewById<TextView>(R.id.tvPlayed).text         = stats.played.toString()
        findViewById<TextView>(R.id.tvWins).text           = stats.wins.toString()
        findViewById<TextView>(R.id.tvLosses).text         = stats.losses.toString()
        findViewById<TextView>(R.id.tvWinRate).text        = "${stats.winRate}%"
        findViewById<TextView>(R.id.tvCurrentStreak).text  = stats.currentStreak.toString()
        findViewById<TextView>(R.id.tvMaxStreak).text      = stats.maxStreak.toString()
    }
}
