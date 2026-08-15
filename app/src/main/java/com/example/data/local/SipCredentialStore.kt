package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Local cache of the SIP password. Firebase RTDB users/{uid}/sip is the source of
 * truth; this store fills the gap when the cloud password is missing after an
 * older client deleted it.
 */
class SipCredentialStore(context: Context) {

    private val prefs: SharedPreferences = createPrefs(context.applicationContext)

    fun savePassword(uid: String, password: String) {
        if (uid.isBlank() || uid == GUEST_UID) {
            clear(uid)
            return
        }
        try {
            prefs.edit().putString(keyFor(uid), password).apply()
        } catch (e: Exception) {
            Log.w(TAG, "Save SIP password cache failed: ${e.message}")
        }
    }

    fun getPassword(uid: String): String {
        if (uid.isBlank() || uid == GUEST_UID) return ""
        return try {
            prefs.getString(keyFor(uid), "") ?: ""
        } catch (e: Exception) {
            Log.w(TAG, "Read SIP password cache failed: ${e.message}")
            ""
        }
    }

    fun clear(uid: String) {
        try {
            prefs.edit().remove(keyFor(uid)).apply()
        } catch (e: Exception) {
            Log.w(TAG, "Clear SIP password cache failed: ${e.message}")
        }
    }

    fun clearAll() {
        try {
            prefs.edit().clear().apply()
        } catch (e: Exception) {
            Log.w(TAG, "Clear all SIP password cache failed: ${e.message}")
        }
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
