package vcmsa.projects.wordleandroidclient

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class ProfileActivity : AppCompatActivity() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val repo by lazy { ProfileRepository() }

    private lateinit var btnBackButton: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        supportActionBar?.hide()

        // 1) Load and show name + username
        val tvFullName = findViewById<TextView>(R.id.tvFullName)
        val tvUsername = findViewById<TextView>(R.id.tvUsername)
        btnBackButton = findViewById(R.id.btnBack)

        btnBackButton.setOnClickListener {
            startActivity(Intent(this, DashboardActivity::class.java))
        }

        lifecycleScope.launch {
            try {
                val profile = repo.getMyProfile()
                val u = auth.currentUser

                val fullName = when {
                    !profile?.fullName.isNullOrBlank() -> profile!!.fullName
                    !u?.displayName.isNullOrBlank()    -> u!!.displayName
                    !u?.email.isNullOrBlank()          -> u!!.email
                    else                               -> "Player"
                } ?: "Player"

                val derivedHandle = u?.email?.substringBefore('@') ?: ""
                val username = profile?.username?.takeIf { !it.isNullOrBlank() } ?: derivedHandle

                tvFullName.text = fullName
                tvUsername.text = if (username.isNotBlank()) "@$username" else ""
            } catch (_: Exception) {
                val u = auth.currentUser
                tvFullName.text = u?.displayName ?: (u?.email ?: "Player")
                tvUsername.text = ""
            }
        }

        // 2) Row clicks
        findViewById<View>(R.id.rowStats).setOnClickListener {
            showComingSoon("Stats")
        }
        findViewById<View>(R.id.rowBadges).setOnClickListener {
            startActivity(Intent(this, BadgesActivity::class.java))
        }
        findViewById<View>(R.id.rowSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        findViewById<View>(R.id.rowLogout).setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, WelcomeActivity::class.java))
            finishAffinity()
        }
    }

    private fun showComingSoon(feature: String) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(feature)
            .setMessage("Coming soon")
            .setPositiveButton("OK", null)
            .show()
    }
}
