// WordleViewModelFactory.kt
package vcmsa.projects.wordleandroidclient

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import vcmsa.projects.wordleandroidclient.api.SpeedleApiService
import vcmsa.projects.wordleandroidclient.api.WordApiService

class WordleViewModelFactory(
    private val appContext: Context,
    private val wordApi: WordApiService,
    private val speedleApi: SpeedleApiService
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WordleViewModel::class.java)) {
            return WordleViewModel(
                wordApi = wordApi,
                speedleApi = speedleApi
            ).apply {
                // inject app context into VM so SpeedleStatsManager can use it
                this.appContext = appContext
            } as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
