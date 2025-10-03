package vcmsa.projects.wordleandroidclient.multiplayer

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import vcmsa.projects.wordleandroidclient.MainActivity
import vcmsa.projects.wordleandroidclient.R

class WaitingRoomActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_waiting_room)

        val mode = intent.getStringExtra("mode") ?: "HOST"
        val code = intent.getStringExtra("roomCode") ?: "------"

        findViewById<TextView>(R.id.tvRoomCode).text = code
        findViewById<TextView>(R.id.tvStatus).text =
            if (mode == "HOST") "Waiting for friend to join…" else "Connecting to room…"

        // TODO Firestore wiring:
        //  - HOST: create room doc with code; listen for guest join → start countdown → launch game.
        //  - GUEST: try to join room; if full/not found → show error; on countdown → launch game.

        findViewById<android.view.View>(R.id.btnFakeStart).setOnClickListener {
            // Temporary: jump to MainActivity as the actual play surface,
            // passing "FRIENDS_MULTIPLAYER" so we can render the opponent widget.
            startActivity(Intent(this, MainActivity::class.java).apply {
                putExtra("mode", "FRIENDS_MULTIPLAYER")
                putExtra("roomCode", code)
            })
            finish()
        }

        findViewById<android.view.View>(R.id.btnCancel).setOnClickListener {
            // TODO: mark room cancelled / leave gracefully
            Toast.makeText(this, "Room closed", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
