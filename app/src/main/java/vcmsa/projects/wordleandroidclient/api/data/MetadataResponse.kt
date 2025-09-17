package vcmsa.projects.wordleandroidclient.api.data

data class MetadataResponse(
    // Date of the puzzle, useful for display purposes
    val date: String,

    // The required length of the word (should be 5 for classic Wordle)
    val length: Int
)
