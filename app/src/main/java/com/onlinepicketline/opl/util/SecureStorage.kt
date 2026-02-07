package com.onlinepicketline.opl.util

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Secure storage for the API key using EncryptedSharedPreferences.
 * The API key is app-specific (not user-specific) and set once during setup.
 */
object SecureStorage {

    private const val FILE_NAME = "opl_secure_prefs"
    private const val KEY_API_KEY = "api_key"

    private fun getPrefs(context: Context) =
        EncryptedSharedPreferences.create(
            context,
            FILE_NAME,
            MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

    fun getApiKey(context: Context): String? =
        getPrefs(context).getString(KEY_API_KEY, null)

    fun setApiKey(context: Context, apiKey: String) {
        getPrefs(context).edit().putString(KEY_API_KEY, apiKey).apply()
    }

    fun hasApiKey(context: Context): Boolean =
        getApiKey(context) != null

    fun clearApiKey(context: Context) {
        getPrefs(context).edit().remove(KEY_API_KEY).apply()
    }
}
