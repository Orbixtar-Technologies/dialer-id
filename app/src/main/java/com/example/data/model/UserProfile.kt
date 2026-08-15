package com.example.data.model

import com.google.firebase.database.IgnoreExtraProperties
import com.google.firebase.database.PropertyName

/**
 * BalanceCache model matching Firebase Realtime Database schema:
 * { "balance": 0.3923, "currency": "USD", "updatedAt": 1786685279 }
 */
@IgnoreExtraProperties
data class BalanceCache(
    var balance: Double = 0.00,
    var currency: String = "USD",
    var updatedAt: Long = System.currentTimeMillis()
)

/**
 * SipConfig for the local SIP stack. The password field is local-only and must
 * never be written to Firebase RTDB.
 */
@IgnoreExtraProperties
data class SipConfig(
    var callerId: String = "",
    var deviceId: String = "",
    var host: String = "",
    var password: String = "",
    var port: Int = 5060,
    var username: String = "",
    var updatedAt: Long = System.currentTimeMillis()
) {
    fun hasUsableCredentials(): Boolean {
        return host.isNotBlank() && username.isNotBlank() && password.isNotBlank()
    }

    fun registrationFingerprint(): String {
        return "$host|$port|$username|$password"
    }

    fun toRemoteMap(): Map<String, Any?> {
        return mapOf(
            "callerId" to callerId,
            "deviceId" to deviceId,
            "host" to host,
            "port" to port,
            "username" to username,
            "updatedAt" to updatedAt
        )
    }
}

/**
 * UserProfile data model representing real true user identity,
 * credit balance, verified caller ID, SIP trunk configuration, and real-time synchronization
 * with Firebase Realtime Database.
 */
@IgnoreExtraProperties
data class UserProfile(
    @get:PropertyName("uid")
    @set:PropertyName("uid")
    var uid: String = GUEST_UID,

    @get:PropertyName("displayName")
    @set:PropertyName("displayName")
    var displayName: String = "Guest Operator",

    @get:PropertyName("email")
    @set:PropertyName("email")
    var email: String = "operator@dialerid.secure",

    @get:PropertyName("phoneNumber")
    @set:PropertyName("phoneNumber")
    var phoneNumber: String = "",

    @get:PropertyName("photoUrl")
    @set:PropertyName("photoUrl")
    var photoUrl: String? = null,

    @get:PropertyName("creditBalance")
    @set:PropertyName("creditBalance")
    var creditBalance: Double = 0.00,

    @get:PropertyName("currency")
    @set:PropertyName("currency")
    var currency: String = "USD",

    @get:PropertyName("selectedCallerId")
    @set:PropertyName("selectedCallerId")
    var selectedCallerId: String = "",

    @get:PropertyName("accountType")
    @set:PropertyName("accountType")
    var accountType: String = "Enterprise VoIP Trunk",

    @get:PropertyName("accountRole")
    @set:PropertyName("accountRole")
    var accountRole: String = "Verified Operator",

    @get:PropertyName("organization")
    @set:PropertyName("organization")
    var organization: String = "Secure Telecom Network",

    @get:PropertyName("presence")
    @set:PropertyName("presence")
    var presence: String = "Online & Ready",

    @get:PropertyName("networkStatus")
    @set:PropertyName("networkStatus")
    var networkStatus: String = "Realtime DB Connected",

    @get:PropertyName("isVerified")
    @set:PropertyName("isVerified")
    var isVerified: Boolean = false,

    @get:PropertyName("isEncrypted")
    @set:PropertyName("isEncrypted")
    var isEncrypted: Boolean = false,

    @get:PropertyName("audioQuality")
    @set:PropertyName("audioQuality")
    var audioQuality: String = "Standard Line",

    @get:PropertyName("preferredCodec")
    @set:PropertyName("preferredCodec")
    var preferredCodec: String = "G711_AUTO",

    @get:PropertyName("callsCount")
    @set:PropertyName("callsCount")
    var callsCount: Int = 0,

    @get:PropertyName("totalMinutes")
    @set:PropertyName("totalMinutes")
    var totalMinutes: Int = 0,

    @get:PropertyName("sipConfig")
    @set:PropertyName("sipConfig")
    var sipConfig: SipConfig? = null,

    @get:PropertyName("isCloudSynced")
    @set:PropertyName("isCloudSynced")
    var isCloudSynced: Boolean = false,

    @get:PropertyName("createdAt")
    @set:PropertyName("createdAt")
    var createdAt: Long = System.currentTimeMillis(),

    @get:PropertyName("lastSyncTimestamp")
    @set:PropertyName("lastSyncTimestamp")
    var lastSyncTimestamp: Long = 0L,

    @get:PropertyName("lastUpdated")
    @set:PropertyName("lastUpdated")
    var lastUpdated: Long = System.currentTimeMillis()
) {
    val isGuest: Boolean
        get() = uid.isBlank() || uid == GUEST_UID
    /**
     * Converts this UserProfile instance to a Map conforming to Firebase Realtime Database schema.
     */
    fun toMap(): Map<String, Any?> {
        return Companion.toMap(this)
    }

    companion object {
        const val GUEST_UID = "guest_operator_001"
        /**
         * Serializes a UserProfile object into a Map for Realtime Database write/update operations.
         */
        fun toMap(profile: UserProfile): Map<String, Any?> {
            val profileMap = mapOf(
                "display_name" to profile.displayName,
                "displayName" to profile.displayName,
                "email" to profile.email,
                "createdAt" to profile.createdAt
            )

            val balanceCacheMap = mapOf(
                "balance" to profile.creditBalance,
                "currency" to profile.currency,
                "updatedAt" to System.currentTimeMillis()
            )

            val rootMap = mutableMapOf<String, Any?>(
                "uid" to profile.uid,
                "profile" to profileMap,
                "balanceCache" to balanceCacheMap,
                "selectedCallerId" to profile.selectedCallerId,
                "displayName" to profile.displayName,
                "email" to profile.email,
                "phoneNumber" to profile.phoneNumber,
                "photoUrl" to profile.photoUrl,
                "creditBalance" to profile.creditBalance,
                "accountType" to profile.accountType,
                "accountRole" to profile.accountRole,
                "organization" to profile.organization,
                "presence" to profile.presence,
                "networkStatus" to profile.networkStatus,
                "isVerified" to profile.isVerified,
                "isEncrypted" to profile.isEncrypted,
                "audioQuality" to profile.audioQuality,
                "preferredCodec" to profile.preferredCodec,
                "callsCount" to profile.callsCount,
                "totalMinutes" to profile.totalMinutes,
                "isCloudSynced" to profile.isCloudSynced,
                "createdAt" to profile.createdAt,
                "lastSyncTimestamp" to profile.lastSyncTimestamp,
                "lastUpdated" to System.currentTimeMillis()
            )

            profile.sipConfig?.let { sip ->
                rootMap["sip"] = sip.toRemoteMap()
            }

            return rootMap
        }

        /**
         * Deserializes a Realtime Database snapshot map dynamically into a UserProfile instance,
         * gracefully parsing nested `profile`, `balanceCache`, `sip`, and top-level fields.
         */
        @Suppress("UNCHECKED_CAST")
        fun fromMap(map: Map<String, Any?>, uid: String = ""): UserProfile {
            val resolvedUid = uid.ifBlank { (map["uid"] as? String) ?: GUEST_UID }

            // Extract nested profile object if present
            val nestedProfile = map["profile"] as? Map<String, Any?>

            val displayName = (nestedProfile?.get("display_name") as? String)
                ?: (nestedProfile?.get("displayName") as? String)
                ?: (map["displayName"] as? String)
                ?: (map["display_name"] as? String)
                ?: "Operator"

            val email = (nestedProfile?.get("email") as? String)
                ?: (map["email"] as? String)
                ?: "operator@dialerid.secure"

            val rawCreatedAt = (nestedProfile?.get("createdAt") as? Number)?.toLong()
                ?: (map["createdAt"] as? Number)?.toLong()
                ?: System.currentTimeMillis()
            // If createdAt is in unix seconds (< 10000000000), convert to millis
            val createdAt = if (rawCreatedAt in 1..9999999999L) rawCreatedAt * 1000L else rawCreatedAt

            // Extract nested balanceCache object if present
            val nestedBalance = map["balanceCache"] as? Map<String, Any?>
            val rawBalance = (nestedBalance?.get("balance"))
                ?: map["creditBalance"]
                ?: map["balance"]

            val balance = when (rawBalance) {
                is Number -> rawBalance.toDouble()
                is String -> rawBalance.toDoubleOrNull() ?: 0.00
                else -> 0.00
            }

            val currency = (nestedBalance?.get("currency") as? String)
                ?: (map["currency"] as? String)
                ?: "USD"

            val selectedCallerId = (map["selectedCallerId"] as? String)
                ?: (map["selected_caller_id"] as? String)
                ?: ""

            // Extract SIP configuration if present
            val nestedSip = map["sip"] as? Map<String, Any?>
            val sipConfig = if (nestedSip != null) {
                val portVal = when (val p = nestedSip["port"]) {
                    is Number -> p.toInt()
                    is String -> p.toIntOrNull() ?: 5060
                    else -> 5060
                }
                SipConfig(
                    callerId = (nestedSip["callerId"] as? String) ?: selectedCallerId,
                    deviceId = (nestedSip["deviceId"]?.toString()) ?: "",
                    host = (nestedSip["host"] as? String) ?: "",
                    password = "",
                    port = portVal,
                    username = (nestedSip["username"] as? String) ?: "",
                    updatedAt = (nestedSip["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
                )
            } else null

            val calls = when (val value = map["callsCount"]) {
                is Number -> value.toInt()
                is String -> value.toIntOrNull() ?: 0
                else -> 0
            }

            val minutes = when (val value = map["totalMinutes"]) {
                is Number -> value.toInt()
                is String -> value.toIntOrNull() ?: 0
                else -> 0
            }

            return UserProfile(
                uid = resolvedUid,
                displayName = displayName,
                email = email,
                phoneNumber = (map["phoneNumber"] as? String) ?: "",
                photoUrl = map["photoUrl"] as? String,
                creditBalance = balance,
                currency = currency,
                selectedCallerId = selectedCallerId,
                accountType = (map["accountType"] as? String) ?: "Enterprise VoIP Trunk",
                accountRole = (map["accountRole"] as? String) ?: "Verified Operator",
                organization = (map["organization"] as? String) ?: "Secure Telecom Network",
                presence = (map["presence"] as? String) ?: "Online & Ready",
                networkStatus = (map["networkStatus"] as? String) ?: "Realtime DB Synced",
                isVerified = (map["isVerified"] as? Boolean) ?: false,
                isEncrypted = (map["isEncrypted"] as? Boolean) ?: false,
                audioQuality = (map["audioQuality"] as? String) ?: "Standard Line",
                preferredCodec = (map["preferredCodec"] as? String) ?: "G711_AUTO",
                callsCount = calls,
                totalMinutes = minutes,
                sipConfig = sipConfig,
                isCloudSynced = true,
                createdAt = createdAt,
                lastSyncTimestamp = System.currentTimeMillis(),
                lastUpdated = (map["lastUpdated"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }
    }
}
