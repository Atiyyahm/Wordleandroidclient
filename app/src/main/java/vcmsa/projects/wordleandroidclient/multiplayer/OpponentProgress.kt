package vcmsa.projects.wordleandroidclient.multiplayer

data class OpponentProgress(
    val lastGuess: String? = null,
    val lastFeedback: List<String>? = null, // ["G","Y","A"]
    val row: Int = 0, // 0-based
    val status: String = "Ready"
)