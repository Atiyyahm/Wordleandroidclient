package vcmsa.projects.wordleandroidclient

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView

class GameBoardAdapter(
    private var letters: List<String>,
    private var states: List<TileState>
) : RecyclerView.Adapter<GameBoardAdapter.LetterBlockViewHolder>() {

    /** Number of columns in a row (used for stagger timing). Defaults to 5. */
    var wordLength: Int = 5

    /** Remember which positions we’ve already animated (prevents repeat flips). */
    private val animatedPositions = mutableSetOf<Int>()

    inner class LetterBlockViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val letterTextView: TextView = itemView.findViewById(R.id.tvLetterBlock)
        private val blockCard: CardView = itemView as CardView

        fun bindAt(position: Int, letter: String, state: TileState) {
            letterTextView.text = letter

            val (bgColor, fgColor) = when (state) {
                TileState.CORRECT -> Color.parseColor("#6AAA64") to Color.WHITE  // green
                TileState.PRESENT -> Color.parseColor("#C9B458") to Color.WHITE  // yellow
                TileState.ABSENT  -> Color.parseColor("#787C7E") to Color.WHITE  // gray
                TileState.FILLED  -> Color.WHITE to Color.BLACK                   // typed but not submitted
                TileState.EMPTY   -> Color.parseColor("#E6E6E6") to Color.DKGRAY // empty
            }

            // If this tile just transitioned to a final state (G/Y/B), play flip once
            val isFinal = state == TileState.CORRECT || state == TileState.PRESENT || state == TileState.ABSENT
            if (isFinal && animatedPositions.add(position)) {
                // Stagger by column index: 0,1,2,3,4 → 0ms,70ms,140ms,210ms,280ms
                val colIndex = if (wordLength > 0) position % wordLength else 0
                val delay = 70L * colIndex
                flipReveal(blockCard, letterTextView, bgColor, fgColor, delay)
            } else {
                // No animation (or already animated) — just ensure colors are correct
                blockCard.setCardBackgroundColor(bgColor)
                letterTextView.setTextColor(fgColor)
            }
        }

        private fun flipReveal(
            card: CardView,
            tv: TextView,
            bgColor: Int,
            fgColor: Int,
            delay: Long
        ) {
            // Simple two-step "flip": scaleY down, swap color, scaleY up
            card.animate()
                .setStartDelay(delay)
                .scaleY(0f)
                .setDuration(110L)
                .withEndAction {
                    card.setCardBackgroundColor(bgColor)
                    tv.setTextColor(fgColor)
                    card.animate()
                        .scaleY(1f)
                        .setDuration(110L)
                        .start()
                }
                .start()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LetterBlockViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_letter_block, parent, false) as CardView
        return LetterBlockViewHolder(view)
    }

    override fun onBindViewHolder(holder: LetterBlockViewHolder, position: Int) {
        holder.bindAt(position, letters[position], states[position])
    }

    override fun getItemCount(): Int = letters.size

    /** Update just the letters . */
    fun updateLetters(newLetters: List<String>) {
        val sizeChanged = newLetters.size != letters.size
        letters = newLetters
        if (sizeChanged) animatedPositions.clear() // board reset
        notifyDataSetChanged()
    }

    /** Update states without forcing animations (used for non-reveal changes). */
    fun updateStates(newStates: List<TileState>) {
        val sizeChanged = newStates.size != states.size
        states = newStates
        if (sizeChanged) animatedPositions.clear() // board reset
        notifyDataSetChanged()
    }


    fun revealRow(startIndex: Int, len: Int = wordLength) {
        // Allow this row to animate even if some items were previously bound
        for (i in 0 until len) animatedPositions.remove(startIndex + i)
        notifyItemRangeChanged(startIndex, len)
    }
}
