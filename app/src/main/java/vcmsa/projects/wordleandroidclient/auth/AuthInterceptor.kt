package vcmsa.projects.wordleandroidclient.auth

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.TimeUnit

class AuthInterceptor : Interceptor {

    companion object {
        @Volatile private var lastUid: String? = null
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val req = chain.request()
        val user = FirebaseAuth.getInstance().currentUser ?: return chain.proceed(req)

        val forceRefresh = if (lastUid != user.uid) {
            lastUid = user.uid
            true                 // first request after switching accounts -> force new token
        } else {
            false
        }

        return try {
            val token = Tasks.await(
                user.getIdToken(forceRefresh),    // <— key change
                3, TimeUnit.SECONDS
            )?.token

            val out = if (!token.isNullOrBlank()) {
                req.newBuilder().addHeader("Authorization", "Bearer $token").build()
            } else req

            chain.proceed(out)
        } catch (_: Exception) {
            chain.proceed(req) // fail open (no token)
        }
    }
}
