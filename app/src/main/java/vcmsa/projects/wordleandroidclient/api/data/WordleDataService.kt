package vcmsa.projects.wordleandroidclient.api.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// The class that handles executing the sequential API calls and combining the data.
class WordleDataService(private val api: WordleWordsApi) {

    // Exposing the required data as flows
    private val _targetWordEntry = MutableStateFlow<WordleEntry?>(null)
    val targetWordEntry: StateFlow<WordleEntry?> = _targetWordEntry

    // This list remains empty until you source a full dictionary file separately,
    // as the API does not provide a list of all valid guess words.
    private val _validWords = MutableStateFlow<List<String>>(emptyList())
    val validWords: StateFlow<List<String>> = _validWords

    /**
     * Executes the sequential API calls to fetch the Word, Definition, and Synonym.
     */
    suspend fun loadDailyWord() {
        try {
            // STEP 1: Get the random word
            val word = api.fetchRandomFiveLetterWord().uppercase()

            // STEP 2: Get the definition
            val definitionResult = api.fetchBestDefinitionForWord(word)
            val definition = definitionResult?.definition

            // STEP 3: Get the synonyms (hint)
            val synonymList = api.fetchBestSynonymForWord(word)
            val bestSynonymHint = synonymList?.firstOrNull()

            // STEP 4: Combine into the final entry and update the state flow
            val finalEntry = WordleEntry(
                word = word,
                definition = definition,
                synonymHint = bestSynonymHint
            )
            _targetWordEntry.value = finalEntry

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}