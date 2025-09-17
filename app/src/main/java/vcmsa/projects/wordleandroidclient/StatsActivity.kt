package vcmsa.projects.wordleandroidclient


import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class StatsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // R.layout.activity_stats will be created next
        setContentView(R.layout.activity_stats)

        // Setup the action bar for navigation back to the dashboard
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Game Statistics"

        // TODO: Future: Fetch and display actual stats (Games Played, Win Rate, etc.)
    }

    // Handles the back button in the action bar
    override fun onSupportNavigateUp(): Boolean {
        finish() // Closes this activity and returns to the previous one (Dashboard)
        return true
    }
}
