package vcmsa.projects.wordleandroidclient.api.data


// --- 1. DATA MODELS ---

/**
 * Represents the structured definition data returned by the API's definition endpoint.
 */
data class WordleDefinition(
    val partOfSpeech: String?,
    val definition: String
)

/**
 * Represents the final, compiled entry containing all the daily word data.
 * This is the object your service builds by making three separate API calls.
 */
data class WordleEntry(
    val word: String,
    val definition: String?, // The definition (for post-game summary)
    val synonymHint: String? // The single best synonym (for hint)
)

// --- 2. API INTERFACE ---

/**
 * Defines the contract for your Retrofit API client.
 * This directly maps to the three separate functions defined in your wordsApi.js file.
 */
interface WordleWordsApi {

    // Maps to fetchRandomFiveLetterWord() -> returns a single word string
    suspend fun fetchRandomFiveLetterWord(): String

    // Maps to fetchBestDefinitionForWord() -> returns the Definition object
    suspend fun fetchBestDefinitionForWord(word: String): WordleDefinition?

    // Maps to fetchBestSynonymForWord() -> returns a list of synonyms
    suspend fun fetchBestSynonymForWord(word: String): List<String>?
}