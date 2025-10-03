package vcmsa.projects.wordleandroidclient.multiplayer

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.ImageView
import android.widget.LinearLayout
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

        // Subtext & chips
        chipsContainer.removeAllViews()
        if (model.lastGuess == null || model.lastFeedback == null) {
            tvSub.isVisible = true
            tvSub.text = context.getString(R.string.no_guess_yet)
            return
        }

        tvSub.isVisible = true
        tvSub.text = "Last: ${model.lastGuess}"

        // Build 5 chips with background by feedback code
        val letters = model.lastGuess.toCharArray().map { it.toString() }
        letters.forEachIndexed { i, ch ->
            val code = model.lastFeedback.getOrNull(i) ?: "A"
            val chip = buildChip(ch, code)
            chipsContainer.addView(chip)
        }
    }

    private fun buildChip(letter: String, code: String): TextView {
        val tv = TextView(context)
        tv.text = letter
        tv.setTextColor(resources.getColor(android.R.color.white, null))
        tv.textSize = 14f
        tv.setPadding(14, 8, 14, 8)
        tv.setBackgroundResource(
            when (code) {
                "G" -> R.drawable.bg_chip_green
                "Y" -> R.drawable.bg_chip_yellow
                else -> R.drawable.bg_chip_absent
            }
        )
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        lp.marginEnd = 8
        tv.layoutParams = lp
        return tv
    }
}
