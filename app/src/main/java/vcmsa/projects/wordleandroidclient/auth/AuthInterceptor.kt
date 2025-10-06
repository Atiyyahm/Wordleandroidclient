package vcmsa.projects.wordleandroidclient.auth

import com.google.firebase.auth.FirebaseAuth
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()

        val user = FirebaseAuth.getInstance().currentUser
        val token = try {

            user?.getIdToken(false)?.result?.token
                ?: user?.getIdToken(true)?.result?.token
        } catch (_: Exception) { null }

        val req = if (token != null) {
            original.newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else original

        return chain.proceed(req)
    }
}
