package vcmsa.projects.wordleandroidclient.multiplayer

import android.content.Intent
import android.os.Bundle
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity
import vcmsa.projects.wordleandroidclient.R

class PlayWithAIActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_play_with_ai)

        findViewById<android.view.View>(R.id.btnBack).setOnClickListener { finish() }

        val rg = findViewById<RadioGroup>(R.id.rgDifficulty).apply {
            check(R.id.rbMedium) // default
        }

        findViewById<android.view.View>(R.id.btnStartAi).setOnClickListener {
            val diff = when (rg.checkedRadioButtonId) {
                R.id.rbEasy -> AiDifficulty.EASY
                R.id.rbHard -> AiDifficulty.HARD
                else -> AiDifficulty.MEDIUM
            }
            // For v1 we’ll launch your existing MainActivity with flags,
            // and later we’ll switch to a dedicated MultiplayerGameActivity.
            startActivity(Intent(this, vcmsa.projects.wordleandroidclient.MainActivity::class.java).apply {
                putExtra("mode", "AI_MULTIPLAYER")
                putExtra("aiDifficulty", diff.name)
            })
        }
    }
}
