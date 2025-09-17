package vcmsa.projects.wordleandroidclient


import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import vcmsa.projects.wordleandroidclient.api.data.WordleDataService

/**
 * Factory class required to instantiate WordleViewModel with a WordleDataService dependency.
 */
class WordleViewModelFactory(
    private val dataService: WordleDataService
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WordleViewModel::class.java)) {
            return WordleViewModel(dataService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}