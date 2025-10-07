package vcmsa.projects.wordleandroidclient

import android.os.Bundle
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import vcmsa.projects.wordleandroidclient.data.SettingsStore

class SettingsActivity : AppCompatActivity() {

    private lateinit var swDark: Switch
    private lateinit var swHaptics: Switch

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        supportActionBar?.title = "Settings"

        swDark = findViewById(R.id.swDarkTheme)
        swHaptics = findViewById(R.id.swHaptics)

        // reflect current values
        lifecycleScope.launch {
            SettingsStore.darkThemeFlow(this@SettingsActivity).collectLatest { v ->
                if (swDark.isChecked != v) swDark.isChecked = v
            }
        }
        lifecycleScope.launch {
            SettingsStore.hapticsFlow(this@SettingsActivity).collectLatest { v ->
                if (swHaptics.isChecked != v) swHaptics.isChecked = v
            }
        }

        // save on change
        swDark.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch {
                SettingsStore.setDarkTheme(this@SettingsActivity, isChecked)
                AppCompatDelegate.setDefaultNightMode(
                    if (isChecked) AppCompatDelegate.MODE_NIGHT_YES
                    else AppCompatDelegate.MODE_NIGHT_NO
                )
                // apply immediately
                delegate.applyDayNight()
                recreate() // refresh this screen; others will pick it up when opened
            }
        }

        swHaptics.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch { SettingsStore.setHaptics(this@SettingsActivity, isChecked) }
        }
    }
}
