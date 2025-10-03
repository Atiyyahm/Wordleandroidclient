package vcmsa.projects.wordleandroidclient

enum class TileState {
    EMPTY,     // no letter yet
    FILLED,    // letter typed but not submitted
    CORRECT,   // G (green)
    PRESENT,   // Y (yellow)
    ABSENT     // B (grey)
}