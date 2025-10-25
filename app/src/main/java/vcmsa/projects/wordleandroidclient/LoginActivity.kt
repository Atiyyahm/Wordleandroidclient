package vcmsa.projects.wordleandroidclient

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import vcmsa.projects.wordleandroidclient.UserProfile
import vcmsa.projects.wordleandroidclient.auth.BiometricHelper

class LoginActivity : AppCompatActivity() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var btnSignup: Button
    private lateinit var btnGoogle: View
    private lateinit var progress: ProgressBar

    private lateinit var btnBiometric: Button

    private val googleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        lifecycleScope.launch {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account.idToken
                if (idToken.isNullOrBlank()) {
                    toast("Google sign-in failed: idToken null. Check SHA-1/256 + web client id.")
                    return@launch
                }
                val cred = GoogleAuthProvider.getCredential(idToken, null)
                auth.signInWithCredential(cred).await()
                ensureProfile()
                goToDashboard()
            } catch (e: ApiException) {
                // Common: 10 (DEVELOPER_ERROR), 12500 (config), 7 (network), 12501 (canceled)
                toast("Google sign-in error: status=${e.statusCode}")
            } catch (e: Exception) {
                toast("Google sign-in error: ${e.localizedMessage}")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // If already signed in, skip
        FirebaseAuth.getInstance().currentUser?.let {
            startActivity(Intent(this, DashboardActivity::class.java))
            finish(); return
        }

        setContentView(R.layout.activity_login)
        supportActionBar?.hide()

        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        btnSignup = findViewById(R.id.btnSignup)
        btnGoogle = findViewById(R.id.btnGoogle)
        btnBiometric = findViewById(R.id.btnBiometric)
        progress = findViewById(R.id.progress)

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val pass = etPassword.text.toString()
            if (email.isEmpty() || pass.isEmpty()) { toast("Enter email and password"); return@setOnClickListener }
            lifecycleScope.launch {
                setBusy(true)
                try {
                    auth.signInWithEmailAndPassword(email, pass).await()
                    ensureProfile()
                    toast("Next time you can use biometrics to log in faster")
                    goToDashboard()
                } catch (e: Exception) {
                    toast("Login failed: ${e.localizedMessage}")
                } finally { setBusy(false) }
            }
        }

        btnSignup.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
            finish()
        }

        btnGoogle.setOnClickListener { startGoogle() }

        // --- Biometric quick login (only show if supported & returning user) ---
        if (BiometricHelper.canAuthenticate(this) && auth.currentUser != null) {
            btnBiometric.visibility = View.VISIBLE
            btnBiometric.setOnClickListener {
                BiometricHelper.showPrompt(
                    this,
                    title = "Quick Login",
                    subtitle = "Use fingerprint or face unlock to continue",
                    onSuccess = {
                        goToDashboard()
                    },
                    onFailure = {
                        toast("Authentication failed")
                    }
                )
            }
        } else {
            btnBiometric.visibility = View.GONE
        }
    }


    private fun startGoogle() {
        //  Request the ID token so account.idToken is not null
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)) // from google-services.json
            .requestEmail()
            .build()
        val client = GoogleSignIn.getClient(this, gso)
        googleLauncher.launch(client.signInIntent)
    }

    private suspend fun ensureProfile() {
        val u = auth.currentUser ?: return
        val docRef = db.collection("profiles").document(u.uid)
        val doc = docRef.get().await()
        if (!doc.exists()) {
            val profile = UserProfile(
                uid = u.uid,
                fullName = u.displayName,
                email = u.email,
                username = null, // user can set later
                photoUrl = u.photoUrl?.toString()
            )
            docRef.set(profile).await()
            // Optionally set usernameLower later when username chosen
        }
    }

    private fun goToDashboard() {
        startActivity(Intent(this, DashboardActivity::class.java))
        finish()
    }

    private fun setBusy(b: Boolean) { progress.visibility = if (b) View.VISIBLE else View.GONE }
    private fun toast(m: String) = Toast.makeText(this, m, Toast.LENGTH_SHORT).show()
}
