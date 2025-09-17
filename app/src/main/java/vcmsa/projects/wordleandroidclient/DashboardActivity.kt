package vcmsa.projects.wordleandroidclient



import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

class DashboardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        // Hide the default action bar (optional, based on your design)
        supportActionBar?.hide()

        // Find the "Daily WordRush" card
        val dailyWordleCard: CardView = findViewById(R.id.cvDailyWordle)

        // Set the listener to launch the main game (MainActivity.kt)
        dailyWordleCard.setOnClickListener {
            // Use Intent to switch to the game board activity
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        // Setup listener for the bottom navigation bar
        setupBottomNav()

        // Setup listeners for other tiles (optional)
        findViewById<CardView>(R.id.cvSpeedle).setOnClickListener {
            Toast.makeText(this, "Speedle Mode coming soon!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupBottomNav() {
        val bottomNav = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNavigationView)

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_daily_wordle -> {
                    // This is the Daily Wordle icon, launches MainActivity
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.nav_stats -> {
                    // This is the Stats/Profile icon, assuming it maps to a StatsActivity (if you create it)
                    Toast.makeText(this, "Stats Page Placeholder", Toast.LENGTH_SHORT).show()
                    true
                }
                // Handle other navigation items here
                else -> {
                    Toast.makeText(this, "Navigation Item Selected", Toast.LENGTH_SHORT).show()
                    true
                }
            }
        }
    }
}