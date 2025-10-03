package vcmsa.projects.wordleandroidclient.multiplayer

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import vcmsa.projects.wordleandroidclient.R
import kotlin.random.Random

class PlayWithFriendsActivity : AppCompatActivity() {

    private fun randomRoomCode(): String {
        val alphabet = "ABCDEFGHJKMNPQRSTUVWXYZ234567" // Base32 no look-alikes
        return (1..6).map { alphabet[Random.nextInt(alphabet.length)] }.joinToString("")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_play_with_friends)

        val etCode = findViewById<EditText>(R.id.etRoomCode)

        findViewById<android.view.View>(R.id.btnGenerate).setOnClickListener {
            // TODO: call server to choose a random word + create Firestore room.
            val code = randomRoomCode()
            startActivity(Intent(this, WaitingRoomActivity::class.java).apply {
                putExtra("mode", "HOST")
                putExtra("roomCode", code)
            })
        }

        findViewById<android.view.View>(R.id.btnJoin).setOnClickListener {
            val code = etCode.text.toString().trim().uppercase()
            if (code.length != 6) {
                Toast.makeText(this, "Enter a 6-char room code", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            startActivity(Intent(this, WaitingRoomActivity::class.java).apply {
                putExtra("mode", "GUEST")
                putExtra("roomCode", code)
            })
        }
    }
}
