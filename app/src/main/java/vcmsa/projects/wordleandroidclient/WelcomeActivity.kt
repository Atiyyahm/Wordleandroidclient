package vcmsa.projects.wordleandroidclient

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class WelcomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // If already signed in, bypass welcome
        FirebaseAuth.getInstance().currentUser?.let {
            startActivity(Intent(this, DashboardActivity::class.java))
            finish(); return
        }

        setContentView(R.layout.activity_welcome)
        supportActionBar?.hide()

        findViewById<Button>(R.id.btnGetStarted).setOnClickListener {
            startActivity(Intent(this, EntryChoiceActivity::class.java))
            finish()
        }
    }
}
