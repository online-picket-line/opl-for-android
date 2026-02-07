package com.onlinepicketline.opl.ui

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.onlinepicketline.opl.data.api.ApiClient
import com.onlinepicketline.opl.data.repository.OplRepository
import com.onlinepicketline.opl.databinding.ActivitySettingsBinding
import com.onlinepicketline.opl.util.SecureStorage
import com.onlinepicketline.opl.BuildConfig

/**
 * Settings screen for app configuration.
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Settings"

        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        loadSettings()
        setupListeners()
    }

    private fun loadSettings() {
        binding.notificationsToggle.isChecked = prefs.getBoolean("notifications_enabled", true)
        binding.autoBlockToggle.isChecked = prefs.getBoolean("auto_block", false)
        binding.gpsAlertsToggle.isChecked = prefs.getBoolean("gps_alerts", true)
        binding.versionText.text = "Version ${BuildConfig.VERSION_NAME}"
        binding.apiKeyStatus.text = if (SecureStorage.hasApiKey(this)) "API key configured" else "No API key"
    }

    private fun setupListeners() {
        binding.notificationsToggle.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("notifications_enabled", isChecked).apply()
        }

        binding.autoBlockToggle.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("auto_block", isChecked).apply()
        }

        binding.gpsAlertsToggle.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("gps_alerts", isChecked).apply()
        }

        binding.clearCacheButton.setOnClickListener {
            OplRepository(this).clearCache()
            Toast.makeText(this, "Cache cleared", Toast.LENGTH_SHORT).show()
        }

        binding.resetApiKeyButton.setOnClickListener {
            SecureStorage.clearApiKey(this)
            ApiClient.reset()
            binding.apiKeyStatus.text = "No API key"
            Toast.makeText(this, "API key removed. Restart app to reconfigure.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
