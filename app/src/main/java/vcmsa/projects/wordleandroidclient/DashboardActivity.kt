package vcmsa.projects.wordleandroidclient

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.RadioGroup
import android.widget.TextView
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
import kotlinx.coroutines.tasks.await
import vcmsa.projects.wordleandroidclient.multiplayer.PlayWithAIActivity

class DashboardActivity : AppCompatActivity() {

    private val auth by lazy { FirebaseAuth.getInstance() }

    private lateinit var tvGreeting: TextView
    private lateinit var chipStreak: TextView
    private lateinit var tvDailyCountdown: TextView
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var btnAI: Button
    private lateinit var cardDaily: View

    private var countdown: CountDownTimer? = null

    override fun onStart() {
        super.onStart()
        Log.e("DASHBOARD", "===== onStart called =====")
        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        refreshDailyCardState()
    }

    override fun onResume() {
        super.onResume()
        Log.e("DASHBOARD", "===== onResume called =====")
        refreshDailyCardState()
    }

    private fun refreshDailyCardState() {
        Log.e("DASHBOARD", "refreshDailyCardState started")
        lifecycleScope.launch {
            var played = false
            val today = getTodayIso()

            try {
                val user = auth.currentUser
                if (user != null) {
                    // 1. Quick local check first (instant)
                    val localPlayed = vcmsa.projects.wordleandroidclient.data.SettingsStore
                        .hasUserPlayedToday(this@DashboardActivity, today, user.uid)

                    Log.e("DASHBOARD", "Local check: played=$localPlayed")

                    if (localPlayed) {
                        played = true
                        Log.e("DASHBOARD", "Using local result: played=true")
                    } else {
                        // 2. Check Firestore directly
                        Log.e("DASHBOARD", "Checking Firestore...")
                        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        val docId = "${today}_en-ZA_${user.uid}"

                        try {
                            val doc = db.collection("results").document(docId).get().await()
                            val firestorePlayed = doc.exists()
                            Log.e("DASHBOARD", "Firestore says: played=$firestorePlayed")

                            if (firestorePlayed) {
                                // Sync local storage
                                vcmsa.projects.wordleandroidclient.data.SettingsStore
                                    .setLastPlayedDate(this@DashboardActivity, today, user.uid)
                                played = true
                                Log.e("DASHBOARD", "Synced local from Firestore")
                            } else {
                                played = false
                            }
                        } catch (e: Exception) {
                            Log.e("DASHBOARD", "Firestore error: ${e.message}")
                            // On error, trust local
                            played = localPlayed
                        }
                    }
                } else {
                    // Unsigned user: only local
                    val lastPlayed = vcmsa.projects.wordleandroidclient.data.SettingsStore
                        .getLastPlayedDate(this@DashboardActivity)
                    played = (lastPlayed == today)
                    Log.e("DASHBOARD", "Anonymous user: played=$played")
                }
            } catch (e: Exception) {
                Log.e("DASHBOARD", "Error: ${e.message}", e)
                played = false
            }

            Log.e("DASHBOARD", "***** FINAL: played=$played *****")
            applyDailyCardState(played)
        }
    }
    private fun applyDailyCardState(played: Boolean) {
        Log.e("DASHBOARD", "applyDailyCardState: played=$played")
        if (played) {
            Log.e("DASHBOARD", "Setting card to DISABLED state (alpha=0.6)")
            cardDaily.alpha = 0.6f
            cardDaily.setOnClickListener {
                Log.e("DASHBOARD", "Disabled card clicked - showing dialog")
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Daily played")
                    .setMessage("You've already played today. Come back tomorrow!")
                    .setPositiveButton("OK", null)
                    .show()
            }
        } else {
            Log.e("DASHBOARD", "Setting card to ENABLED state (alpha=1.0)")
            cardDaily.alpha = 1f
            cardDaily.setOnClickListener {
                Log.e("DASHBOARD", "Enabled card clicked - launching game")
                startActivity(Intent(this, MainActivity::class.java))
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)
        supportActionBar?.hide()

        tvGreeting = findViewById(R.id.tvGreeting)
        chipStreak = findViewById(R.id.chipStreak)
        tvDailyCountdown = findViewById(R.id.tvDailyCountdown)
        bottomNav = findViewById(R.id.bottomNav)
        cardDaily = findViewById(R.id.cardDaily)
        btnAI = findViewById(R.id.btnAI)

        val user = auth.currentUser
        val fallback = user?.displayName ?: user?.email?.substringBefore("@") ?: "Player"
        tvGreeting.text = "Welcome back, $fallback"

        val stats = StatsManager.getStats(this)
        chipStreak.text = if (stats.currentStreak > 0) {
            "🔥 ${stats.currentStreak}-day streak"
        } else {
            "Start a streak today"
        }

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

        btnAI.setOnClickListener {
            startActivity(Intent(this, PlayWithAIActivity::class.java))
        }

        val rg = findViewById<RadioGroup>(R.id.rgSpeedle).apply {
            check(R.id.rb90)
        }

        findViewById<View>(R.id.cardSpeedle).setOnClickListener {
            val seconds = when (rg.checkedRadioButtonId) {
                R.id.rb60  -> 60
                R.id.rb120 -> 120
                else       -> 90
            }
            startActivity(
                Intent(this, MainActivity::class.java).apply {
                    putExtra("mode", "SPEEDLE")
                    putExtra("seconds", seconds)
                }
            )
        }

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_speedle -> {
                    showSpeedleDurationChooser()
                    true
                }
                R.id.nav_multiplayer -> {
                    startActivity(Intent(this, PlayWithAIActivity::class.java))
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

        startResetCountdown()
        refreshDailyCardState()
    }

    private fun showComingSoon(feature: String) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("$feature")
            .setMessage("Coming soon")
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
                        putExtra("mode", "SPEEDLE")
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

    private fun getTodayIso(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date())
    }
}