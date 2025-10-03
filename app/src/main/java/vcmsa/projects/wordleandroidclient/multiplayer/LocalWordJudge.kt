package vcmsa.projects.wordleandroidclient.multiplayer

/**
 * Wordle-style feedback without calling the server.
 * Returns a list like ["G","Y","A", ...] for Green/Yellow/Absent.
 */
object LocalWordJudge {

    fun feedback(guess: String, target: String): List<String> {
        val g = guess.uppercase()
        val t = target.uppercase()
        require(g.length == t.length) { "Guess and target lengths must match." }

        val res = MutableList(g.length) { "A" } // default ABSENT
        val avail = IntArray(26)

        // count letters in target
        for (i in t.indices) {
            val idx = t[i] - 'A'
            if (idx in 0..25) avail[idx]++
        }

        // first pass: greens
        for (i in g.indices) {
            if (g[i] == t[i]) {
                res[i] = "G"
                val idx = g[i] - 'A'
                if (idx in 0..25) avail[idx]--
            }
        }

        // second pass: yellows
        for (i in g.indices) {
            if (res[i] == "G") continue
            val idx = g[i] - 'A'
            if (idx !in 0..25) continue
            if (avail[idx] > 0) {
                res[i] = "Y"
                avail[idx]--
            }
        }
        return res
    }
}
