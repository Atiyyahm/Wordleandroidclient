package vcmsa.projects.wordleandroidclient

data class DailyWordMetadata(
    val date: String,
    val lang: String,
    val length: Int,
    val mode: String,
    val source: String,
    val createdAt: String,
    val hasDefinition: Boolean,
    val hasSynonym: Boolean
)
