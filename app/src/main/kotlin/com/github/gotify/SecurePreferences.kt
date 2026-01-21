package com.github.gotify

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

internal class SecurePreferences(context: Context) {
    private val encryptedPreferences: SharedPreferences

    init {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        encryptedPreferences = EncryptedSharedPreferences.create(
            context,
            "gotify_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun getToken(): String? = encryptedPreferences.getString("token", null)

    fun setToken(value: String?) {
        encryptedPreferences.edit { putString("token", value) }
    }

    fun getClientCertPassword(): String? = encryptedPreferences.getString("clientCertPass", null)

    fun setClientCertPassword(value: String?) {
        encryptedPreferences.edit { putString("clientCertPass", value) }
    }

    fun clear() {
        encryptedPreferences.edit { clear() }
    }
}
