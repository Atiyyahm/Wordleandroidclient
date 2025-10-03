package vcmsa.projects.wordleandroidclient

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class BadgesActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_badges) // create a simple grid later
        supportActionBar?.hide()
    }
}
