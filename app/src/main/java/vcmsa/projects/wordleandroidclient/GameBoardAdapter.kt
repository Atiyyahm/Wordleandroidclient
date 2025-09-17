// Make sure this is in your project's main package
package vcmsa.projects.wordleandroidclient

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import vcmsa.projects.wordleandroidclient.R // Explicit import for the R class

class GameBoardAdapter(private val letters: List<String>) :
    RecyclerView.Adapter<GameBoardAdapter.LetterBlockViewHolder>() {

    class LetterBlockViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val letterTextView: TextView = view.findViewById(R.id.tvLetterBlock)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LetterBlockViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_letter_block, parent, false)
        return LetterBlockViewHolder(view)
    }

    override fun onBindViewHolder(holder: LetterBlockViewHolder, position: Int) {
        holder.letterTextView.text = letters[position]
    }

    override fun getItemCount(): Int {
        return letters.size
    }
}

