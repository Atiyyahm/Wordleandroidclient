package vcmsa.projects.wordleandroidclient.api.data

data class GuessResponse(
    val guess: String,
    val feedback: List<String>, // List of codes: "G", "Y", or "B"
    val won: Boolean
)
