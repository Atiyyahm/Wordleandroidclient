package vcmsa.projects.wordleandroidclient.multiplayer

enum class AiDifficulty { EASY, MEDIUM, HARD }

enum class MatchPhase { WAITING, COUNTDOWN, PLAYING, FINISHED, CANCELLED }



data class EndMatchSummary(
    val won: Boolean,
    val yourGuesses: Int,
    val opponentGuesses: Int?,
    val definition: String? = null,
    val synonym: String? = null
)
