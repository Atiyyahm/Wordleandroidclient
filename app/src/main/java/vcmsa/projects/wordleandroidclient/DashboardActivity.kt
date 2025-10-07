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
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch


class DashboardActivity : AppCompatActivity() {

    // --- Auth ---
    private val auth by lazy { FirebaseAuth.getInstance() }

    // --- Views ---
    private lateinit var tvGreeting: TextView
    private lateinit var chipStreak: TextView
    private lateinit var tvDailyCountdown: TextView
    private lateinit var bottomNav: BottomNavigationView

    private lateinit var cardDaily: View

    // --- Countdown to daily reset ---
    private var countdown: CountDownTimer? = null

    override fun onStart() {
        super.onStart()
        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        // Re-check after coming back from a game
        refreshDailyCardState()
    }

    /** Server+local check, then wire the card’s UI/behavior. */
    private fun refreshDailyCardState() {
        lifecycleScope.launch {
            var played = false
            try {
                val resp = vcmsa.projects.wordleandroidclient.api.RetrofitClient.wordService.getToday()
                val meta = resp.body()

                if (meta != null) {
                    if (auth.currentUser != null) {
                        // Trust server normally, but confirm if it says "played"
                        played = if (meta.played) {
                            val confirm = vcmsa.projects.wordleandroidclient.api.RetrofitClient
                                .wordService.getMyResult(meta.date, meta.lang)
                            confirm.isSuccessful // 200 only when a result actually exists
                        } else false
                    } else {
                        // unsigned: local fallback
                        val last = vcmsa.projects.wordleandroidclient.data.SettingsStore
                            .getLastPlayedDate(this@DashboardActivity)
                        played = (last == meta.date)
                    }
                }
            } catch (_: Exception) {
                // offline: only unsigned users have a local signal
                if (auth.currentUser == null) {
                    val last = vcmsa.projects.wordleandroidclient.data.SettingsStore
                        .getLastPlayedDate(this@DashboardActivity)
                    played = (last == getTodayIso())
                } else {
                    played = false
                }
            }
            applyDailyCardState(played)
        }
    }


    /** Visually dim + show popup if played; else launch Daily game. */
    private fun applyDailyCardState(played: Boolean) {
        if (played) {
            cardDaily.alpha = 0.6f
            cardDaily.setOnClickListener {
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Daily played")
                    .setMessage("You’ve already played today. Come back tomorrow!")
                    .setPositiveButton("OK", null)
                    .show()
            }
        } else {
            cardDaily.alpha = 1f
            cardDaily.setOnClickListener {
                // Launch Daily mode normally
                startActivity(Intent(this, MainActivity::class.java))
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)
        supportActionBar?.hide()

        // --- Bind views ---
        tvGreeting = findViewById(R.id.tvGreeting)
        chipStreak = findViewById(R.id.chipStreak)
        tvDailyCountdown = findViewById(R.id.tvDailyCountdown)
        bottomNav = findViewById(R.id.bottomNav)
        cardDaily = findViewById(R.id.cardDaily)

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

        findViewById<View>(R.id.qaHowTo).setOnClickListener { showHowToDialog() }

        findViewById<View>(R.id.qaSpeedle).setOnClickListener {
            bottomNav.selectedItemId = R.id.nav_speedle
        }
        findViewById<View>(R.id.qaMultiplayer).setOnClickListener {
            bottomNav.selectedItemId = R.id.nav_multiplayer
        }
        findViewById<View>(R.id.qaLeaderboard).setOnClickListener {
            showComingSoon("Leaderboard")
        }
        findViewById<View>(R.id.qaStats).setOnClickListener {
            showComingSoon("Stats")
        }
        findViewById<View>(R.id.qaHowTo).setOnClickListener { showHowToDialog() }


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
                    startActivity(Intent(this, vcmsa.projects.wordleandroidclient.multiplayer.PlayWithAIActivity::class.java))
                    true
                }

                R.id.nav_leaderboard -> {
                    showComingSoon("Leaderboard")
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

        // First wiring of the Daily card
        refreshDailyCardState()
    }

    private fun showComingSoon(feature: String) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("$feature")
            .setMessage("Coming soon ")
            .setPositiveButton("OK", null)
            .show()
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

    private fun showHowToDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_how_to_play, null)
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(view)
            .setPositiveButton("Got it") { d, _ -> d.dismiss() }
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
