package com.zekkers.watthome.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

class TokenStore private constructor(context: Context) {
    private val prefs: SharedPreferences = encryptedPrefs(context.applicationContext)

    fun hasToken(): Boolean = readToken().isNullOrBlank().not()

    fun readToken(): String? {
        val value = prefs.getString(KEY_TOKEN, null)?.trim().orEmpty()
        return value.takeIf { it.isNotEmpty() }
    }

    fun saveToken(raw: String) {
        val token = normalize(raw) ?: throw IllegalArgumentException("Paste a GivEnergy API token first")
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    fun clearToken() {
        prefs.edit().remove(KEY_TOKEN).apply()
    }

    companion object {
        private const val PREFS_NAME = "watt_home_token"
        private const val KEY_TOKEN = "givenergy_bearer"

        fun normalize(raw: String): String? {
            var token = raw.trim()
            if (token.startsWith("Bearer ", ignoreCase = true)) {
                token = token.substring(7).trim()
            }
            return token.takeIf { it.isNotEmpty() }
        }

        private fun encryptedPrefs(context: Context): SharedPreferences {
            val masterKey = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            return EncryptedSharedPreferences.create(
                PREFS_NAME,
                masterKey,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }

        @Volatile
        private var instance: TokenStore? = null

        fun get(context: Context): TokenStore {
            return instance ?: synchronized(this) {
                instance ?: TokenStore(context).also { instance = it }
            }
        }
    }
}
