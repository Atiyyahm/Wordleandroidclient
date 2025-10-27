package vcmsa.projects.wordleandroidclient.leaderboard

import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import vcmsa.projects.wordleandroidclient.R

class LeaderboardActivity : AppCompatActivity() {

    private val repo by lazy { ResultsLeaderboardRepo(FirebaseFirestore.getInstance()) }
    private lateinit var adapter: LeaderboardAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_leaderboard)
        supportActionBar?.hide()

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        val rv = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvLeaderboard)
        adapter = LeaderboardAdapter()
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        lifecycleScope.launch {
            try {
                val top = repo.fetchDailyLeaderboard(limit = 50)
                adapter.submit(top)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
