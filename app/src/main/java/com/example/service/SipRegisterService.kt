package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.SipConfig
import com.example.data.model.UserProfile
import com.example.data.repository.DialerRepository
import com.example.service.sip.RegistrationStatus
import com.example.service.sip.SipEngine
import com.example.service.sip.SipRegistrationState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Foreground keep-alive for Linphone REGISTER. Not a phone-call FGS.
 * Registers only when an authenticated (non-guest) user has real SIP credentials.
 */
class SipRegisterService : Service() {

    private val tag = "SipRegisterService"
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private val binder = LocalBinder()

    private var sipEngine: SipEngine? = null
    private var profileCollectJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var lastRegisterFingerprint: String? = null

    inner class LocalBinder : Binder() {
        fun getService(): SipRegisterService = this@SipRegisterService
    }

    companion object {
        const val CHANNEL_ID = "dialerid_sip_registration"
        const val NOTIFICATION_ID = 1002

        const val ACTION_START_REGISTRATION = "com.example.service.ACTION_START_REGISTRATION"
        const val ACTION_STOP_REGISTRATION = "com.example.service.ACTION_STOP_REGISTRATION"
        const val ACTION_REFRESH_REGISTRATION = "com.example.service.ACTION_REFRESH_REGISTRATION"

        const val EXTRA_HOST = "extra_host"
        const val EXTRA_PORT = "extra_port"
        const val EXTRA_USERNAME = "extra_username"
        const val EXTRA_PASSWORD = "extra_password"

        @Volatile
        private var INSTANCE: SipRegisterService? = null

        fun start(context: Context) {
            try {
                val intent = Intent(context, SipRegisterService::class.java).apply {
                    action = ACTION_START_REGISTRATION
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.w("SipRegisterService", "Service start notice: ${e.message}")
            }
        }

        fun stop(context: Context) {
            try {
                val intent = Intent(context, SipRegisterService::class.java).apply {
                    action = ACTION_STOP_REGISTRATION
                }
                context.startService(intent)
            } catch (e: Exception) {
                Log.w("SipRegisterService", "Service stop notice: ${e.message}")
            }
        }

        fun refresh(context: Context) {
            try {
                val intent = Intent(context, SipRegisterService::class.java).apply {
                    action = ACTION_REFRESH_REGISTRATION
                }
                context.startService(intent)
            } catch (e: Exception) {
                Log.w("SipRegisterService", "Service refresh notice: ${e.message}")
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        INSTANCE = this
        createNotificationChannel()
        // Must promote to FGS before any Linphone/Firebase work. Otherwise
        // startForegroundService() from MainActivity times out while SipEngine
        // constructs the Core on the main thread.
        enterForeground(SipRegistrationState(statusMessage = "Starting SIP registration"))

        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "DialerID:SipRegisterWakeLock")?.apply {
            setReferenceCounted(false)
        }

        sipEngine = SipEngine.getInstance(this)

        val repository = DialerRepository.getInstance(this)
        profileCollectJob = serviceScope.launch {
            repository.userProfile.collectLatest { profile ->
                syncRegistration(profile, explicitConfig = null)
            }
        }

        serviceScope.launch {
            sipEngine?.registrationState?.collectLatest { state ->
                updateNotification(state)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START_REGISTRATION

        when (action) {
            ACTION_START_REGISTRATION -> {
                wakeLock?.acquire(3600_000L)
                enterForeground(
                    sipEngine?.registrationState?.value ?: SipRegistrationState()
                )

                val host = intent?.getStringExtra(EXTRA_HOST)
                val port = intent?.getIntExtra(EXTRA_PORT, 5060) ?: 5060
                val user = intent?.getStringExtra(EXTRA_USERNAME)
                val pass = intent?.getStringExtra(EXTRA_PASSWORD)

                val explicit = if (!host.isNullOrBlank() && !user.isNullOrBlank() && !pass.isNullOrBlank()) {
                    SipConfig(host = host, port = port, username = user, password = pass)
                } else {
                    null
                }
                val profile = DialerRepository.getInstance(this).userProfile.value
                syncRegistration(profile, explicit)
            }

            ACTION_REFRESH_REGISTRATION -> {
                val profile = DialerRepository.getInstance(this).userProfile.value
                val config = usableConfig(profile, null)
                if (config != null) {
                    sipEngine?.refreshNow()
                }
            }

            ACTION_STOP_REGISTRATION -> {
                profileCollectJob?.cancel()
                lastRegisterFingerprint = null
                sipEngine?.unregister()
                if (wakeLock?.isHeld == true) {
                    wakeLock?.release()
                }
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        super.onDestroy()
        profileCollectJob?.cancel()
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
        INSTANCE = null
    }

    val registrationState: StateFlow<SipRegistrationState>?
        get() = sipEngine?.registrationState

    private fun usableConfig(profile: UserProfile, explicit: SipConfig?): SipConfig? {
        if (profile.isGuest) return null
        val candidate = explicit ?: profile.sipConfig
        return candidate?.takeIf { it.hasUsableCredentials() }
    }

    private fun syncRegistration(profile: UserProfile, explicitConfig: SipConfig?) {
        if (profile.isGuest) {
            if (lastRegisterFingerprint != null) {
                lastRegisterFingerprint = null
                sipEngine?.unregister()
            }
            return
        }
        val candidate = explicitConfig ?: profile.sipConfig
        Log.d(
            tag,
            "syncRegistration guest=${profile.isGuest} host=${candidate?.host.orEmpty()} " +
                "user=${candidate?.username.orEmpty()} usable=${candidate?.hasUsableCredentials() == true} " +
                "needsPassword=${candidate?.needsPassword() == true}"
        )
        if (candidate == null) {
            if (lastRegisterFingerprint != null) {
                lastRegisterFingerprint = null
                sipEngine?.unregister()
            }
            return
        }
        if (candidate.needsPassword()) {
            lastRegisterFingerprint = null
            sipEngine?.register(candidate)
            return
        }
        val config = candidate.takeIf { it.hasUsableCredentials() }
        if (config == null) {
            if (lastRegisterFingerprint != null) {
                lastRegisterFingerprint = null
                sipEngine?.unregister()
            }
            return
        }
        val fingerprint = config.registrationFingerprint()
        val status = sipEngine?.registrationState?.value?.status
        if (fingerprint == lastRegisterFingerprint &&
            (status == RegistrationStatus.REGISTERED ||
                status == RegistrationStatus.REGISTERING ||
                status == RegistrationStatus.AUTHENTICATING ||
                status == RegistrationStatus.RETRYING ||
                status == RegistrationStatus.FAILED)
        ) {
            // Engine owns 503/timeout backoff. Do not tear down the account on every profile emit.
            return
        }
        lastRegisterFingerprint = fingerprint
        Log.d(tag, "Registering SIP account ${config.username}@${config.host} via Linphone")
        sipEngine?.register(config)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "SIP Registration",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Maintains SIP registration while credentials are configured"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun enterForeground(state: SipRegistrationState) {
        val notification = buildRegistrationNotification(state)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.w(tag, "Foreground service start warning: ${e.message}")
        }
    }

    private fun updateNotification(state: SipRegistrationState) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.notify(NOTIFICATION_ID, buildRegistrationNotification(state))
    }

    private fun buildRegistrationNotification(state: SipRegistrationState): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val title = when {
            state.needsPassword -> "SIP Password Required"
            state.status == RegistrationStatus.REGISTERED -> "SIP Connected"
            state.status == RegistrationStatus.AUTHENTICATING -> "SIP Authenticating (401)..."
            state.status == RegistrationStatus.REGISTERING -> "Registering SIP..."
            state.status == RegistrationStatus.RETRYING -> "Retrying SIP Connection..."
            state.status == RegistrationStatus.EXPIRED -> "SIP Registration Expired"
            state.status == RegistrationStatus.FAILED -> "SIP Registration Error"
            state.status == RegistrationStatus.UNREGISTERING -> "SIP Disconnecting"
            else -> "SIP Offline"
        }

        val content = when {
            state.isRegistered -> "${state.username}@${state.host}"
            state.needsPassword -> "Re-enter SIP password in Settings"
            state.status == RegistrationStatus.AUTHENTICATING -> "Completing 401 challenge for ${state.username}@${state.host}"
            state.status == RegistrationStatus.RETRYING && state.retryAfterSeconds > 0 -> "Reconnecting in ${state.retryAfterSeconds}s..."
            state.status == RegistrationStatus.RETRYING -> "Reconnecting to ${state.host}..."
            else -> state.statusMessage.ifBlank { "Configure SIP credentials in Settings" }
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
