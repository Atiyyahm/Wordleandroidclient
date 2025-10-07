package vcmsa.projects.wordleandroidclient

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import vcmsa.projects.wordleandroidclient.data.SettingsStore

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Read the saved theme once at startup and apply before any Activity draws.
        val isDark = runBlocking { SettingsStore.darkThemeFlow(this@MyApp).first() }
        AppCompatDelegate.setDefaultNightMode(
            if (isDark) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
    }
}
