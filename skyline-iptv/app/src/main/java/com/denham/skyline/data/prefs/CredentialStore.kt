package com.denham.skyline.data.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.denham.skyline.core.PortalAddress
import com.denham.skyline.core.XtreamAccount

/**
 * Credentials at rest: AES-256 EncryptedSharedPreferences with the key in the
 * Android Keystore. Note the honest limitation — Xtream sends the username
 * and password in the query string of every request, so at-rest encryption
 * only protects the copy on this device.
 */
class CredentialStore(context: Context) {

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "skyline_credentials",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    fun save(account: XtreamAccount) {
        prefs.edit()
            .putString(KEY_SCHEME, account.portal.scheme)
            .putString(KEY_HOST, account.portal.host)
            .putInt(KEY_PORT, account.portal.port)
            .putString(KEY_USERNAME, account.username)
            .putString(KEY_PASSWORD, account.password)
            .apply()
    }

    fun load(): XtreamAccount? {
        val host = prefs.getString(KEY_HOST, null) ?: return null
        val username = prefs.getString(KEY_USERNAME, null) ?: return null
        val password = prefs.getString(KEY_PASSWORD, null) ?: return null
        return XtreamAccount(
            portal = PortalAddress(
                scheme = prefs.getString(KEY_SCHEME, "http") ?: "http",
                host = host,
                port = prefs.getInt(KEY_PORT, 80),
            ),
            username = username,
            password = password,
        )
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val KEY_SCHEME = "scheme"
        const val KEY_HOST = "host"
        const val KEY_PORT = "port"
        const val KEY_USERNAME = "username"
        const val KEY_PASSWORD = "password"
    }
}
