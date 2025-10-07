package vcmsa.projects.wordleandroidclient.multiplayer

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import vcmsa.projects.wordleandroidclient.R

class OpponentProgressView @JvmOverloads constructor(
    ctx: Context, attrs: AttributeSet? = null
) : FrameLayout(ctx, attrs) {

    private val tvStatus: TextView
    private val tvRow: TextView
    private val tvSub: TextView
    private val chipsContainer: LinearLayout
    private val imgAvatar: ImageView

    init {
        LayoutInflater.from(ctx).inflate(R.layout.view_opponent_progress, this, true)
        tvStatus = findViewById(R.id.tvStatus)
        tvRow = findViewById(R.id.tvRow)
        tvSub = findViewById(R.id.tvSub)
        chipsContainer = findViewById(R.id.chipsContainer)
        imgAvatar = findViewById(R.id.imgAvatar)
    }

    fun bind(model: OpponentProgress) {
        tvStatus.text = model.status
        tvRow.text = "Row ${model.row + 1}/6"

        chipsContainer.isVisible = false
        chipsContainer.removeAllViews()

        tvSub.isVisible = true
        tvSub.text = if (model.lastGuess.isNullOrBlank()) {
            context.getString(R.string.no_guess_yet)
        } else {
            "AI guessed: ${model.lastGuess}"
        }
    }
}
