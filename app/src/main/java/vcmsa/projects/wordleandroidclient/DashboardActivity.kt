// app/src/main/java/vcmsa/projects/wordleandroidclient/DashboardActivity.kt
package vcmsa.projects.wordleandroidclient

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.ImageButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class DashboardActivity : AppCompatActivity() {

    // --- Auth ---
    private val auth by lazy { FirebaseAuth.getInstance() }

    // --- Views ---
    private lateinit var tvGreeting: TextView
    private lateinit var chipStreak: TextView
    private lateinit var tvDailyCountdown: TextView
    private lateinit var bottomNav: BottomNavigationView

    // --- Countdown to daily reset ---
    private var countdown: CountDownTimer? = null

    override fun onStart() {
        super.onStart()
        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)
        supportActionBar?.hide()

        // --- Top bar avatar -> Profile ---
        findViewById<ImageButton>(R.id.btnAvatar).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        // --- Bind views ---
        tvGreeting = findViewById(R.id.tvGreeting)
        chipStreak = findViewById(R.id.chipStreak)
        tvDailyCountdown = findViewById(R.id.tvDailyCountdown)
        bottomNav = findViewById(R.id.bottomNav)

        // --- Greeting + streak chip ---
        val user = auth.currentUser
        val fallback = user?.displayName ?: user?.email?.substringBefore("@") ?: "Player"
        tvGreeting.text = "Welcome back, $fallback"

        val stats = StatsManager.getStats(this)
        chipStreak.text = if (stats.currentStreak > 0) {
            "🔥 ${stats.currentStreak}-day streak"
        } else {
            "Start a streak today"
        }

        // --- Daily card -> MainActivity (Daily mode) ---
        findViewById<View>(R.id.cardDaily).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        // --- Quick actions ---
        findViewById<View>(R.id.qaSpeedle).setOnClickListener {
            bottomNav.selectedItemId = R.id.nav_speedle
        }
        findViewById<View>(R.id.qaMultiplayer).setOnClickListener {
            bottomNav.selectedItemId = R.id.nav_multiplayer
        }
        findViewById<View>(R.id.qaLeaderboard).setOnClickListener {
            startActivity(
                Intent(this, LeaderboardActivity::class.java).apply {
                    putExtra("date", getTodayIso()) // "YYYY-MM-DD"
                    putExtra("duration", 90)        // default 90; adjust if you want
                }
            )
        }

        findViewById<View>(R.id.qaHowTo).setOnClickListener {
            Toast.makeText(this, "How to Play coming soon", Toast.LENGTH_SHORT).show()
        }

        // --- Speedle selector on the Speedle card ---
        val rg = findViewById<RadioGroup>(R.id.rgSpeedle).apply {
            check(R.id.rb90) // default selection
        }

        // Tap the Speedle card to start with the selected duration
        findViewById<View>(R.id.cardSpeedle).setOnClickListener {
            val seconds = when (rg.checkedRadioButtonId) {
                R.id.rb60  -> 60
                R.id.rb120 -> 120
                else       -> 90
            }
            startActivity(
                Intent(this, MainActivity::class.java).apply {
                    putExtra("mode", "SPEEDLE")   // <-- uppercase so MainActivity recognizes it
                    putExtra("seconds", seconds)
                }
            )
        }

        // --- Bottom navigation routing ---
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true // already here

                R.id.nav_speedle -> {
                    // Show quick chooser for 60/90/120
                    showSpeedleDurationChooser()
                    true
                }

                R.id.nav_multiplayer -> {
                    // Wire up your multiplayer screen here when ready:
                    // startActivity(Intent(this, vcmsa.projects.wordleandroidclient.multiplayer.WaitingRoomActivity::class.java))
                    Toast.makeText(this, "Multiplayer coming soon", Toast.LENGTH_SHORT).show()
                    true
                }

                R.id.nav_leaderboard -> {
                    startActivity(
                        Intent(this, LeaderboardActivity::class.java).apply {
                            putExtra("date", getTodayIso())
                            putExtra("duration", 90)
                        }
                    )
                    true
                }

                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }

                else -> false
            }
        }
        bottomNav.selectedItemId = R.id.nav_home

        // --- Daily reset countdown (to midnight local) ---
        startResetCountdown()
    }

    private fun showSpeedleDurationChooser() {
        val durations = arrayOf("60 seconds", "90 seconds", "120 seconds")
        val options = intArrayOf(60, 90, 120)
        AlertDialog.Builder(this)
            .setTitle("Play Speedle")
            .setItems(durations) { _, which ->
                startActivity(
                    Intent(this, MainActivity::class.java).apply {
                        putExtra("mode", "SPEEDLE")   // <-- uppercase
                        putExtra("seconds", options[which])
                    }
                )
            }
            .show()
    }

    private fun startResetCountdown() {
        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val target = cal.timeInMillis
        val duration = (target - System.currentTimeMillis()).coerceAtLeast(0)

        countdown?.cancel()
        countdown = object : CountDownTimer(duration, 1000) {
            override fun onTick(ms: Long) {
                val h = TimeUnit.MILLISECONDS.toHours(ms)
                val m = TimeUnit.MILLISECONDS.toMinutes(ms) % 60
                val s = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
                tvDailyCountdown.text = String.format("Resets in %02d:%02d:%02d", h, m, s)
            }

            override fun onFinish() {
                tvDailyCountdown.text = "New puzzle ready!"
            }
        }.start()
    }

    override fun onDestroy() {
        countdown?.cancel()
        super.onDestroy()
    }

    // -------- Helpers --------

    /** Returns today's date as ISO yyyy-MM-dd without needing java.time/desugaring. */
    private fun getTodayIso(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date())
    }
}
