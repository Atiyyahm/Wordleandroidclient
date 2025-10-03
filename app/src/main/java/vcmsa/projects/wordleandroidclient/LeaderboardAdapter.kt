package vcmsa.projects.wordleandroidclient

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import vcmsa.projects.wordleandroidclient.api.LeaderboardEntry

class LeaderboardAdapter(
    private var items: List<LeaderboardEntry> = emptyList()
) : RecyclerView.Adapter<LeaderboardAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvRank: TextView = v.findViewById(R.id.tvRank)
        val tvName: TextView = v.findViewById(R.id.tvName)
        val tvSub: TextView = v.findViewById(R.id.tvSub)
        val tvScore: TextView = v.findViewById(R.id.tvScore)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_leaderboard_row, parent, false)
        return VH(v)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(h: VH, i: Int) {
        val e = items[i]
        h.tvRank.text = e.rank.toString()
        h.tvName.text = e.displayName
        h.tvScore.text = e.score.toString()
        h.tvSub.text = "Score • ${e.guessesUsed} guesses • ${e.timeRemainingSec}s left"
    }

    fun submit(newItems: List<LeaderboardEntry>) {
        items = newItems
        notifyDataSetChanged()
    }
}
