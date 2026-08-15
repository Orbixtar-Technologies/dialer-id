package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.data.local.AppDatabase
import com.example.data.local.CallLogDao
import com.example.data.local.CallerIdDao
import com.example.data.local.SipCredentialStore
import com.example.data.model.CallLogItem
import com.example.data.model.CallStatus
import com.example.data.model.CallerIdItem
import com.example.data.model.SipConfig
import com.example.data.model.UserProfile
import com.example.ui.common.CountryUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class DialerRepository(
    private val callerIdDao: CallerIdDao,
    private val callLogDao: CallLogDao,
    private val credentialStore: SipCredentialStore
) {
    private val repositoryScope = CoroutineScope(Dispatchers.IO)
    private var databaseListener: ValueEventListener? = null
    private var currentListeningRef: DatabaseReference? = null

    // Reactive user profile state
    private val _userProfile = MutableStateFlow(UserProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    val allCallerIds: Flow<List<CallerIdItem>> = callerIdDao.getAllCallerIds()
    val allCallLogs: Flow<List<CallLogItem>> = callLogDao.getAllCallLogs()

    init {
        // Observe primary caller ID and sync to user profile if empty
        repositoryScope.launch {
            allCallerIds.collect { list ->
                val primary = list.find { it.isPrimary } ?: list.firstOrNull()
                if (primary != null && _userProfile.value.selectedCallerId.isEmpty()) {
                    _userProfile.value = _userProfile.value.copy(
                        selectedCallerId = primary.phoneNumber
                    )
                }
            }
        }

        // Listen to Firebase Auth state for dynamic Realtime Database sync
        try {
            val auth = FirebaseAuth.getInstance()
            auth.addAuthStateListener { firebaseAuth ->
                val user = firebaseAuth.currentUser
                if (user != null) {
                    bindUser(user)
                } else {
                    unbindUser()
                }
            }
        } catch (e: Exception) {
            Log.w("DialerRepository", "Firebase Auth listener initialization notice: ${e.message}")
        }
    }

    private fun getDatabaseInstance(): FirebaseDatabase {
        return try {
            FirebaseDatabase.getInstance("https://dialerid-default-rtdb.firebaseio.com")
        } catch (e: Exception) {
            FirebaseDatabase.getInstance()
        }
    }

    fun bindUser(user: FirebaseUser) {
        val uid = user.uid
        val name = user.displayName?.ifBlank { null } ?: user.email?.substringBefore("@")?.replaceFirstChar { it.uppercase() } ?: "Operator"
        val email = user.email ?: "operator@dialerid.secure"
        val photoUrl = user.photoUrl?.toString()
        val phone = user.phoneNumber ?: ""

        _userProfile.value = _userProfile.value.copy(
            uid = uid,
            displayName = name,
            email = email,
            phoneNumber = phone,
            photoUrl = photoUrl,
            networkStatus = "Realtime DB Synced"
        )

        // Attach Realtime Database listener for dynamic live synchronization
        attachDatabaseListener(uid, name, email, phone, photoUrl)
    }

    fun unbindUser() {
        if (currentListeningRef != null && databaseListener != null) {
            currentListeningRef?.removeEventListener(databaseListener!!)
        }
        databaseListener = null
        currentListeningRef = null

        credentialStore.clearAll()
        _userProfile.value = UserProfile(
            uid = UserProfile.GUEST_UID,
            displayName = "Guest Operator",
            email = "operator@dialerid.secure",
            phoneNumber = "",
            photoUrl = null,
            creditBalance = 0.00,
            sipConfig = null,
            isVerified = false,
            isEncrypted = false,
            audioQuality = "Standard Line",
            isCloudSynced = false
        )
    }

    private fun attachDatabaseListener(
        uid: String,
        fallbackName: String,
        fallbackEmail: String,
        fallbackPhone: String,
        photoUrl: String?
    ) {
        if (currentListeningRef != null && databaseListener != null) {
            currentListeningRef?.removeEventListener(databaseListener!!)
        }

        try {
            val db = getDatabaseInstance()
            val userRef = db.getReference("users").child(uid)
            currentListeningRef = userRef

            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        @Suppress("UNCHECKED_CAST")
                        val map = snapshot.value as? Map<String, Any?>
                        if (map != null) {
                            processRealtimeDatabaseSnapshot(map, uid, photoUrl)
                        }
                    } else {
                        // Check if data is stored directly at root (for databases where user root is top-level)
                        db.getReference().child(uid).addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(rootSnapshot: DataSnapshot) {
                                if (rootSnapshot.exists()) {
                                    @Suppress("UNCHECKED_CAST")
                                    val rootMap = rootSnapshot.value as? Map<String, Any?>
                                    if (rootMap != null) {
                                        processRealtimeDatabaseSnapshot(rootMap, uid, photoUrl)
                                        return
                                    }
                                }

                                // Initialize new user entry in Realtime Database conforming to JSON schema
                                val initialProfile = UserProfile(
                                    uid = uid,
                                    displayName = fallbackName,
                                    email = fallbackEmail,
                                    phoneNumber = fallbackPhone,
                                    photoUrl = photoUrl,
                                    creditBalance = _userProfile.value.creditBalance,
                                    selectedCallerId = _userProfile.value.selectedCallerId,
                                    accountType = "Enterprise VoIP Trunk",
                                    accountRole = "Verified Operator",
                                    organization = "Secure Telecom Network",
                                    presence = "Online & Ready",
                                    networkStatus = "Realtime DB Synced",
                                    isVerified = false,
                                    isEncrypted = false,
                                    audioQuality = "Standard Line",
                                    callsCount = 0,
                                    totalMinutes = 0,
                                    createdAt = System.currentTimeMillis(),
                                    lastSyncTimestamp = System.currentTimeMillis(),
                                    lastUpdated = System.currentTimeMillis(),
                                    isCloudSynced = true
                                )
                                userRef.setValue(initialProfile.toMap())
                                    .addOnSuccessListener {
                                        _userProfile.value = initialProfile
                                    }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Log.w("DialerRepository", "Root snapshot check notice: ${error.message}")
                            }
                        })
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w("DialerRepository", "Realtime Database sync error: ${error.message}")
                }
            }

            databaseListener = listener
            userRef.addValueEventListener(listener)
        } catch (e: Exception) {
            Log.w("DialerRepository", "Realtime Database initialization error: ${e.message}")
        }
    }

    /**
     * Dynamically processes all nodes from the Realtime Database snapshot:
     * - balanceCache { balance, currency, updatedAt }
     * - profile { display_name, email, createdAt }
     * - selectedCallerId
     * - sip { callerId, deviceId, host, password, port, username, updatedAt }
     * - callerIds [ { caller_id, host, id, label, port, username } ]
     * - callLogs { id: { callerId, destination, durationSeconds, endedAt, id, status } }
     */
    @Suppress("UNCHECKED_CAST")
    private fun processRealtimeDatabaseSnapshot(
        map: Map<String, Any?>,
        uid: String,
        photoUrl: String?
    ) {
        val parsedProfile = UserProfile.fromMap(map, uid)

        // 1. Synchronize Caller IDs dynamically into Room
        val rawCallerIds = map["callerIds"]
        val callerIdList = mutableListOf<CallerIdItem>()

        if (rawCallerIds is List<*>) {
            rawCallerIds.filterIsInstance<Map<String, Any?>>().forEachIndexed { index, cidMap ->
                val phone = (cidMap["caller_id"] as? String)
                    ?: (cidMap["callerId"] as? String)
                    ?: (cidMap["phoneNumber"] as? String)
                    ?: ""
                if (phone.isNotBlank()) {
                    val idVal = cidMap["id"]?.toString() ?: "cid_$index"
                    val label = (cidMap["label"] as? String) ?: "Identity #${index + 1}"
                    val host = (cidMap["host"] as? String) ?: ""
                    val port = cidMap["port"]?.toString() ?: ""
                    val username = (cidMap["username"] as? String) ?: ""
                    val isPrimary = phone.trim() == parsedProfile.selectedCallerId.trim() || index == 0

                    callerIdList.add(
                        CallerIdItem(
                            id = idVal,
                            phoneNumber = phone,
                            label = label,
                            isPrimary = isPrimary,
                            isVerified = (cidMap["isVerified"] as? Boolean) ?: false,
                            countryCode = CountryUtils.estimateRateForNumber(phone).first.code,
                            host = host,
                            port = port,
                            username = username
                        )
                    )
                }
            }
        } else if (rawCallerIds is Map<*, *>) {
            rawCallerIds.forEach { (key, value) ->
                if (value is Map<*, *>) {
                    val cidMap = value as Map<String, Any?>
                    val phone = (cidMap["caller_id"] as? String)
                        ?: (cidMap["callerId"] as? String)
                        ?: (cidMap["phoneNumber"] as? String)
                        ?: ""
                    if (phone.isNotBlank()) {
                        val idVal = cidMap["id"]?.toString() ?: key.toString()
                        val label = (cidMap["label"] as? String) ?: "Identity $idVal"
                        val host = (cidMap["host"] as? String) ?: ""
                        val port = cidMap["port"]?.toString() ?: ""
                        val username = (cidMap["username"] as? String) ?: ""
                        val isPrimary = phone.trim() == parsedProfile.selectedCallerId.trim()

                        callerIdList.add(
                            CallerIdItem(
                                id = idVal,
                                phoneNumber = phone,
                                label = label,
                                isPrimary = isPrimary,
                                isVerified = (cidMap["isVerified"] as? Boolean) ?: false,
                                countryCode = CountryUtils.estimateRateForNumber(phone).first.code,
                                host = host,
                                port = port,
                                username = username
                            )
                        )
                    }
                }
            }
        }

        if (callerIdList.isNotEmpty()) {
            repositoryScope.launch {
                callerIdDao.clearPrimaryFlags()
                callerIdDao.insertAll(callerIdList)
            }
        }

        // 2. Synchronize Call Logs dynamically into Room
        val rawCallLogs = map["callLogs"]
        val callLogsList = mutableListOf<CallLogItem>()
        var computedTotalDurationSeconds = 0

        if (rawCallLogs is Map<*, *>) {
            rawCallLogs.forEach { (key, value) ->
                if (value is Map<*, *>) {
                    val logMap = value as Map<String, Any?>
                    val logId = (logMap["id"] as? String) ?: key.toString()
                    val dest = (logMap["destination"] as? String)
                        ?: (logMap["destinationNumber"] as? String)
                        ?: ""
                    val cidUsed = (logMap["callerId"] as? String)
                        ?: (logMap["callerIdUsed"] as? String)
                        ?: parsedProfile.selectedCallerId

                    val duration = when (val d = logMap["durationSeconds"]) {
                        is Number -> d.toInt()
                        is String -> d.toIntOrNull() ?: 0
                        else -> 0
                    }
                    computedTotalDurationSeconds += duration

                    val rawEndedAt = (logMap["endedAt"] as? Number)?.toLong()
                        ?: (logMap["timestamp"] as? Number)?.toLong()
                        ?: System.currentTimeMillis()
                    val timestamp = if (rawEndedAt in 1..9999999999L) rawEndedAt * 1000L else rawEndedAt

                    val statusStr = (logMap["status"] as? String)?.uppercase() ?: "DISCONNECTED"
                    val status = when (statusStr) {
                        "DISCONNECTED", "COMPLETED" -> CallStatus.COMPLETED
                        "FAILED" -> CallStatus.FAILED
                        "CANCELLED" -> CallStatus.CANCELLED
                        "NO_ANSWER" -> CallStatus.NO_ANSWER
                        else -> CallStatus.COMPLETED
                    }

                    val (country, rate) = CountryUtils.estimateRateForNumber(dest)
                    val minutes = Math.ceil(duration / 60.0).toInt()
                    val cost = Math.round(minutes * rate * 100.0) / 100.0

                    callLogsList.add(
                        CallLogItem(
                            id = logId,
                            destinationNumber = dest,
                            callerIdUsed = cidUsed,
                            countryName = country.name,
                            status = status,
                            timestamp = timestamp,
                            durationSeconds = duration,
                            billingRatePerMin = rate,
                            totalCost = cost
                        )
                    )
                }
            }
        } else if (rawCallLogs is List<*>) {
            rawCallLogs.filterIsInstance<Map<String, Any?>>().forEachIndexed { index, logMap ->
                val logId = (logMap["id"] as? String) ?: "log_$index"
                val dest = (logMap["destination"] as? String)
                    ?: (logMap["destinationNumber"] as? String)
                    ?: ""
                val cidUsed = (logMap["callerId"] as? String)
                    ?: (logMap["callerIdUsed"] as? String)
                    ?: parsedProfile.selectedCallerId

                val duration = when (val d = logMap["durationSeconds"]) {
                    is Number -> d.toInt()
                    is String -> d.toIntOrNull() ?: 0
                    else -> 0
                }
                computedTotalDurationSeconds += duration

                val rawEndedAt = (logMap["endedAt"] as? Number)?.toLong()
                    ?: (logMap["timestamp"] as? Number)?.toLong()
                    ?: System.currentTimeMillis()
                val timestamp = if (rawEndedAt in 1..9999999999L) rawEndedAt * 1000L else rawEndedAt

                val statusStr = (logMap["status"] as? String)?.uppercase() ?: "DISCONNECTED"
                val status = when (statusStr) {
                    "DISCONNECTED", "COMPLETED" -> CallStatus.COMPLETED
                    "FAILED" -> CallStatus.FAILED
                    "CANCELLED" -> CallStatus.CANCELLED
                    "NO_ANSWER" -> CallStatus.NO_ANSWER
                    else -> CallStatus.COMPLETED
                }

                val (country, rate) = CountryUtils.estimateRateForNumber(dest)
                val minutes = Math.ceil(duration / 60.0).toInt()
                val cost = Math.round(minutes * rate * 100.0) / 100.0

                callLogsList.add(
                    CallLogItem(
                        id = logId,
                        destinationNumber = dest,
                        callerIdUsed = cidUsed,
                        countryName = country.name,
                        status = status,
                        timestamp = timestamp,
                        durationSeconds = duration,
                        billingRatePerMin = rate,
                        totalCost = cost
                    )
                )
            }
        }

        if (callLogsList.isNotEmpty()) {
            repositoryScope.launch {
                callLogsList.forEach { log ->
                    callLogDao.insertCallLog(log)
                }
            }
        }

        // Update profile state with computed logs count and total minutes if present
        val effectiveCallsCount = if (callLogsList.isNotEmpty()) callLogsList.size else parsedProfile.callsCount
        val effectiveTotalMinutes = if (callLogsList.isNotEmpty()) {
            Math.ceil(computedTotalDurationSeconds / 60.0).toInt()
        } else {
            parsedProfile.totalMinutes
        }

        val isSignedInUser = uid.isNotEmpty() && uid != UserProfile.GUEST_UID
        val localPassword = if (isSignedInUser) credentialStore.getPassword(uid) else ""
        val mergedSip = parsedProfile.sipConfig?.withResolvedPassword(localPassword)
        if (isSignedInUser) {
            val resolvedPassword = mergedSip?.password.orEmpty()
            if (resolvedPassword.isNotBlank() && resolvedPassword != localPassword) {
                credentialStore.savePassword(uid, resolvedPassword)
            }
        }

        _userProfile.value = parsedProfile.copy(
            photoUrl = parsedProfile.photoUrl ?: photoUrl,
            sipConfig = mergedSip,
            callsCount = effectiveCallsCount,
            totalMinutes = effectiveTotalMinutes,
            isCloudSynced = true,
            lastSyncTimestamp = System.currentTimeMillis()
        )
    }

    private fun syncToRealtimeDatabase(updates: Map<String, Any?>) {
        val uid = _userProfile.value.uid
        if (uid.isNotEmpty() && uid != UserProfile.GUEST_UID) {
            try {
                val db = getDatabaseInstance()
                val mergedUpdates = updates.toMutableMap().apply {
                    put("lastUpdated", System.currentTimeMillis())
                    put("lastSyncTimestamp", System.currentTimeMillis())
                    put("isCloudSynced", true)
                }
                db.getReference("users").child(uid).updateChildren(mergedUpdates)
                    .addOnSuccessListener {
                        _userProfile.value = _userProfile.value.copy(
                            isCloudSynced = true,
                            lastSyncTimestamp = System.currentTimeMillis()
                        )
                    }
            } catch (e: Exception) {
                Log.w("DialerRepository", "Sync to Realtime Database failed: ${e.message}")
            }
        }
    }

    fun updateProfile(
        displayName: String,
        phoneNumber: String,
        organization: String,
        accountRole: String
    ) {
        val updated = _userProfile.value.copy(
            displayName = displayName.trim(),
            phoneNumber = phoneNumber.trim(),
            organization = organization.trim(),
            accountRole = accountRole.trim(),
            lastUpdated = System.currentTimeMillis()
        )
        _userProfile.value = updated
        syncToRealtimeDatabase(
            mapOf(
                "profile" to mapOf(
                    "display_name" to updated.displayName,
                    "displayName" to updated.displayName,
                    "email" to updated.email,
                    "createdAt" to updated.createdAt
                ),
                "displayName" to updated.displayName,
                "phoneNumber" to updated.phoneNumber,
                "organization" to updated.organization,
                "accountRole" to updated.accountRole
            )
        )
    }

    fun updateSelectedCallerId(phoneNumber: String) {
        _userProfile.value = _userProfile.value.copy(selectedCallerId = phoneNumber)
        syncToRealtimeDatabase(mapOf("selectedCallerId" to phoneNumber))
    }

    fun updateSipConfig(sipConfig: SipConfig) {
        val uid = _userProfile.value.uid
        if (uid.isNotEmpty() && uid != UserProfile.GUEST_UID) {
            credentialStore.savePassword(uid, sipConfig.password)
        }
        _userProfile.value = _userProfile.value.copy(
            sipConfig = sipConfig,
            lastUpdated = System.currentTimeMillis()
        )
        syncToRealtimeDatabase(
            mapOf("sip" to sipConfig.toRemoteMap())
        )
    }

    fun updatePreferredCodec(codec: String) {
        _userProfile.value = _userProfile.value.copy(preferredCodec = codec)
        syncToRealtimeDatabase(mapOf("preferredCodec" to codec))
    }

    fun addCredit(amount: Double) {
        if (!BuildConfig.DEBUG) {
            Log.w("DialerRepository", "Client credit add is disabled in release builds")
            return
        }
        val newBalance = (_userProfile.value.creditBalance + amount).coerceAtLeast(0.0)
        _userProfile.value = _userProfile.value.copy(creditBalance = newBalance)
        syncToRealtimeDatabase(
            mapOf(
                "balanceCache" to mapOf(
                    "balance" to newBalance,
                    "currency" to _userProfile.value.currency,
                    "updatedAt" to (System.currentTimeMillis() / 1000)
                ),
                "creditBalance" to newBalance
            )
        )
    }

    fun deductCredit(amount: Double) {
        val newBalance = (_userProfile.value.creditBalance - amount).coerceAtLeast(0.0)
        _userProfile.value = _userProfile.value.copy(creditBalance = newBalance)
        syncToRealtimeDatabase(
            mapOf(
                "balanceCache" to mapOf(
                    "balance" to newBalance,
                    "currency" to _userProfile.value.currency,
                    "updatedAt" to (System.currentTimeMillis() / 1000)
                ),
                "creditBalance" to newBalance
            )
        )
    }

    fun setDisplayName(name: String, email: String) {
        _userProfile.value = _userProfile.value.copy(
            displayName = name,
            email = email
        )
        syncToRealtimeDatabase(
            mapOf(
                "profile" to mapOf(
                    "display_name" to name,
                    "displayName" to name,
                    "email" to email,
                    "createdAt" to _userProfile.value.createdAt
                ),
                "displayName" to name,
                "email" to email
            )
        )
    }

    suspend fun addCallerId(
        phoneNumber: String,
        label: String,
        isPrimary: Boolean,
        countryCode: String = "US",
        host: String = "",
        port: String = "5060",
        username: String = ""
    ) {
        if (isPrimary) {
            callerIdDao.clearPrimaryFlags()
        }
        val generatedId = "10" + (1000..9999).random()
        val item = CallerIdItem(
            id = generatedId,
            phoneNumber = phoneNumber.trim(),
            label = label.trim().ifEmpty { "Caller ID" },
            isPrimary = isPrimary,
            isVerified = false,
            countryCode = countryCode,
            host = host,
            port = port,
            username = username.ifEmpty { generatedId }
        )
        callerIdDao.insertCallerId(item)
        if (isPrimary) {
            updateSelectedCallerId(item.phoneNumber)
        }

        // Push to Realtime Database callerIds array/map
        val uid = _userProfile.value.uid
        if (uid.isNotEmpty() && uid != UserProfile.GUEST_UID) {
            try {
                val db = getDatabaseInstance()
                val cidMap = mapOf(
                    "caller_id" to item.phoneNumber,
                    "callerId" to item.phoneNumber,
                    "host" to item.host,
                    "id" to (item.id.toIntOrNull() ?: item.id),
                    "label" to item.label,
                    "port" to item.port,
                    "username" to item.username
                )
                db.getReference("users").child(uid).child("callerIds").child(item.id).setValue(cidMap)
            } catch (e: Exception) {
                Log.w("DialerRepository", "Caller ID Realtime DB push error: ${e.message}")
            }
        }
    }

    suspend fun setPrimaryCallerId(id: String, phoneNumber: String) {
        callerIdDao.clearPrimaryFlags()
        callerIdDao.setPrimary(id)
        updateSelectedCallerId(phoneNumber)
    }

    suspend fun deleteCallerId(item: CallerIdItem) {
        callerIdDao.deleteCallerId(item)
        val uid = _userProfile.value.uid
        if (uid.isNotEmpty() && uid != UserProfile.GUEST_UID) {
            try {
                val db = getDatabaseInstance()
                db.getReference("users").child(uid).child("callerIds").child(item.id).removeValue()
            } catch (e: Exception) {
                Log.w("DialerRepository", "Caller ID Realtime DB delete error: ${e.message}")
            }
        }
    }

    suspend fun logCall(
        destinationNumber: String,
        callerIdUsed: String,
        countryName: String,
        status: CallStatus,
        durationSeconds: Int,
        billingRatePerMin: Double
    ) {
        val totalCost = if (durationSeconds > 0) {
            val minutes = Math.ceil(durationSeconds / 60.0).toInt()
            Math.round(minutes * billingRatePerMin * 100.0) / 100.0
        } else {
            0.00
        }

        val logId = UUID.randomUUID().toString().replace("-", "").take(12)
        val log = CallLogItem(
            id = logId,
            destinationNumber = destinationNumber,
            callerIdUsed = callerIdUsed,
            countryName = countryName,
            status = status,
            durationSeconds = durationSeconds,
            billingRatePerMin = billingRatePerMin,
            totalCost = totalCost
        )
        callLogDao.insertCallLog(log)

        if (totalCost > 0.0) {
            deductCredit(totalCost)
        }

        // Increment lifetime statistics & push callLog to Realtime Database
        val addedMinutes = Math.ceil(durationSeconds / 60.0).toInt()
        val newCallsCount = _userProfile.value.callsCount + 1
        val newTotalMinutes = _userProfile.value.totalMinutes + addedMinutes
        _userProfile.value = _userProfile.value.copy(
            callsCount = newCallsCount,
            totalMinutes = newTotalMinutes
        )

        val uid = _userProfile.value.uid
        if (uid.isNotEmpty() && uid != UserProfile.GUEST_UID) {
            try {
                val db = getDatabaseInstance()
                val callLogMap = mapOf(
                    "id" to logId,
                    "callerId" to callerIdUsed,
                    "destination" to destinationNumber,
                    "durationSeconds" to durationSeconds,
                    "endedAt" to (System.currentTimeMillis() / 1000),
                    "status" to (if (status == CallStatus.COMPLETED) "DISCONNECTED" else status.name)
                )
                db.getReference("users").child(uid).child("callLogs").child(logId).setValue(callLogMap)
                db.getReference("users").child(uid).updateChildren(
                    mapOf(
                        "callsCount" to newCallsCount,
                        "totalMinutes" to newTotalMinutes,
                        "lastSyncTimestamp" to System.currentTimeMillis()
                    )
                )
            } catch (e: Exception) {
                Log.w("DialerRepository", "Call Log Realtime DB push error: ${e.message}")
            }
        }
    }

    suspend fun clearCallHistory() {
        callLogDao.clearAll()
        val uid = _userProfile.value.uid
        if (uid.isNotEmpty() && uid != UserProfile.GUEST_UID) {
            try {
                val db = getDatabaseInstance()
                db.getReference("users").child(uid).child("callLogs").removeValue()
            } catch (e: Exception) {
                Log.w("DialerRepository", "Clear Call Logs Realtime DB error: ${e.message}")
            }
        }
    }

    suspend fun clearHistory() {
        clearCallHistory()
    }

    companion object {
        @Volatile
        private var INSTANCE: DialerRepository? = null

        fun getInstance(context: Context): DialerRepository {
            return INSTANCE ?: synchronized(this) {
                val db = AppDatabase.getDatabase(context)
                val instance = DialerRepository(
                    callerIdDao = db.callerIdDao(),
                    callLogDao = db.callLogDao(),
                    credentialStore = SipCredentialStore.getInstance(context)
                )
                INSTANCE = instance
                instance
            }
        }
    }
}
