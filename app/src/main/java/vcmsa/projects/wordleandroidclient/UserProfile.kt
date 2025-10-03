package vcmsa.projects.wordleandroidclient

data class UserProfile(
    val uid: String = "",
    val fullName: String? = null,
    val email: String? = null,
    val username: String? = null,
    val photoUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)