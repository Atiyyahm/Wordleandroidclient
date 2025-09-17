package vcmsa.projects.wordleandroidclient

import com.google.gson.annotations.SerializedName

data class WordResponse(
    @SerializedName("word")
    val word: String
)
