package vcmsa.projects.wordleandroidclient.multiplayer

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import vcmsa.projects.wordleandroidclient.R

class MultiplayerModeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_multiplayer_mode)

        findViewById<android.view.View>(R.id.btnPlayAI).setOnClickListener {
            startActivity(Intent(this, PlayWithAIActivity::class.java))
        }
        findViewById<android.view.View>(R.id.btnPlayFriends).setOnClickListener {
            startActivity(Intent(this, PlayWithFriendsActivity::class.java))
        }
    }
}
