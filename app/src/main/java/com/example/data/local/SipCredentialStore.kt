package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Local-only SIP password store. Passwords are never written to Firebase RTDB.
 */
class SipCredentialStore(context: Context) {

    private val prefs: SharedPreferences = createPrefs(context.applicationContext)

    fun savePassword(uid: String, password: String) {
        if (uid.isBlank() || uid == GUEST_UID) {
            clear(uid)
            return
        }
        prefs.edit().putString(keyFor(uid), password).apply()
    }

    fun getPassword(uid: String): String {
        if (uid.isBlank() || uid == GUEST_UID) return ""
        return prefs.getString(keyFor(uid), "") ?: ""
    }

    fun clear(uid: String) {
        prefs.edit().remove(keyFor(uid)).apply()
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    private fun keyFor(uid: String): String = "sip_password_$uid"

    companion object {
        const val PREFS_NAME = "sip_credentials"
        const val GUEST_UID = "guest_operator_001"
        private const val TAG = "SipCredentialStore"

        @Volatile
        private var INSTANCE: SipCredentialStore? = null

        fun getInstance(context: Context): SipCredentialStore {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SipCredentialStore(context.applicationContext).also { INSTANCE = it }
            }
        }

        private fun createPrefs(context: Context): SharedPreferences {
            return try {
                val masterKey = MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()
                EncryptedSharedPreferences.create(
                    context,
                    PREFS_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            } catch (e: Exception) {
                Log.w(TAG, "EncryptedSharedPreferences unavailable, using private prefs: ${e.message}")
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            }
        }
    }
}
