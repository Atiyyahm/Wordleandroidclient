package vcmsa.projects.wordleandroidclient.api

data class DailyWordMetadata(
    val word: String,
    val date: String,
    val lang: String,
    val length: Int,
    val mode: String,
    val source: String,
    val createdAt: String,
    val hasDefinition: Boolean,
    val hasSynonym: Boolean
)
