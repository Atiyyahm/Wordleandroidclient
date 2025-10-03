package vcmsa.projects.wordleandroidclient

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import vcmsa.projects.wordleandroidclient.api.RetrofitClient
import java.text.SimpleDateFormat
import java.util.*

class LeaderboardActivity : AppCompatActivity() {

    private lateinit var adapter: LeaderboardAdapter
    private lateinit var progress: ProgressBar
    private lateinit var tvTitle: TextView
    private lateinit var tvEmpty: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_leaderboard)

        tvTitle = findViewById(R.id.tvTitle)
        progress = findViewById(R.id.progress)
        tvEmpty = findViewById(R.id.tvEmpty)

        val rv = findViewById<RecyclerView>(R.id.rvLeaderboard)
        rv.layoutManager = LinearLayoutManager(this)
        adapter = LeaderboardAdapter()
        rv.adapter = adapter

        // Inputs (can be passed via Dashboard/BottomNav)
        val date = intent.getStringExtra("date") ?: getTodayIso()  // "YYYY-MM-DD"
        val duration = intent.getIntExtra("duration", 90)          // 60 / 90 / 120

        tvTitle.text = "Speedle Leaderboard — $date • ${duration}s"

        loadLeaderboard(date, duration)
    }

    private fun loadLeaderboard(date: String, duration: Int) {
        progress.visibility = View.VISIBLE
        tvEmpty.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.speedleService.leaderboard(date, duration)
                progress.visibility = View.GONE

                val rows = resp.body()
                if (resp.isSuccessful && rows != null) {
                    if (rows.isEmpty()) {
                        tvEmpty.visibility = View.VISIBLE
                        adapter.submit(emptyList())
                    } else {
                        tvEmpty.visibility = View.GONE
                        adapter.submit(rows)
                    }
                } else {
                    tvEmpty.visibility = View.VISIBLE
                    Toast.makeText(this@LeaderboardActivity, "Couldn’t load leaderboard", Toast.LENGTH_SHORT).show()
                }
            } catch (_: Exception) {
                progress.visibility = View.GONE
                tvEmpty.visibility = View.VISIBLE
                Toast.makeText(this@LeaderboardActivity, "Network error", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

// API-24–safe ISO date helper
fun getTodayIso(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return sdf.format(Date())
}
