package vcmsa.projects.wordleandroidclient

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// Assuming a ViewHolder structure for a single tile (5x6 grid = 30 tiles total)

// We need a way to track the letter and its state (color: green, yellow, gray)
// For simplicity right now, we'll only pass strings, but you will likely need a data class later.
class GameBoardAdapter(private var letters: List<String>) :
    RecyclerView.Adapter<GameBoardAdapter.LetterBlockViewHolder>() {

    // --- ViewHolder Class ---
    inner class LetterBlockViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Assuming your item_letter_block.xml contains a TextView with ID tvLetter
        val letterTextView: TextView = itemView.findViewById(R.id.tvLetterBlock)
        val blockView: View = itemView // Or a CardView/ConstraintLayout acting as the block

        fun bind(letter: String) {
            letterTextView.text = letter

            // FUTURE: This is where you would set the background color based on the letter state
            // Example:
            // when (state) {
            //     LetterState.CORRECT -> blockView.setBackgroundColor(Color.GREEN)
            //     ...
            // }
        }
    }

    // --- Adapter Overrides ---

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LetterBlockViewHolder {
        // You MUST have a layout file named R.layout.item_letter_block
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_letter_block, parent, false)
        return LetterBlockViewHolder(view)
    }

    override fun onBindViewHolder(holder: LetterBlockViewHolder, position: Int) {
        holder.bind(letters[position])
    }

    override fun getItemCount(): Int = letters.size

    // --- *** REQUIRED NEW FUNCTION FOR DYNAMIC UPDATES *** ---
    /**
     * Updates the data set and notifies the RecyclerView to redraw the grid.
     */
    fun updateLetters(newLetters: List<String>) {
        this.letters = newLetters
        // Tells the RecyclerView to redraw all 30 tiles based on the new data list
        notifyDataSetChanged()
    }
}
