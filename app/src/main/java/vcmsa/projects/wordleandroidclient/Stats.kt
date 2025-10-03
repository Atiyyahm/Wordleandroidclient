package vcmsa.projects.wordleandroidclient

data class Stats(
    val played: Int,
    val wins: Int,
    val losses: Int,
    val currentStreak: Int,
    val maxStreak: Int,
    val lastPlayedIso: String?,
    val winRate: Int
)