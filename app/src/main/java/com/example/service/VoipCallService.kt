package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class VoipCallService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var callStateJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var audioManager: AudioManager? = null

    companion object {
        const val CHANNEL_ID = "dialerid_voip_calls"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START_CALL = "com.example.service.ACTION_START_CALL"
        const val ACTION_END_CALL = "com.example.service.ACTION_END_CALL"
        const val ACTION_STOP_SERVICE = "com.example.service.ACTION_STOP_SERVICE"
        const val ACTION_TOGGLE_MUTE = "com.example.service.ACTION_TOGGLE_MUTE"
        const val ACTION_TOGGLE_SPEAKER = "com.example.service.ACTION_TOGGLE_SPEAKER"

        const val EXTRA_DESTINATION = "extra_destination"
        const val EXTRA_CALLER_ID = "extra_caller_id"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "DialerID:VoipCallWakeLock").apply {
            setReferenceCounted(false)
        }

        callStateJob = serviceScope.launch {
            CallManager.getInstance(this@VoipCallService).callState.collectLatest { info ->
                if (info.phase != CallPhase.IDLE) {
                    updateNotification(info)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START_CALL
        val destination = intent?.getStringExtra(EXTRA_DESTINATION) ?: "Active Call"
        val callerId = intent?.getStringExtra(EXTRA_CALLER_ID) ?: "Outbound Caller ID"

        when (action) {
            ACTION_START_CALL -> {
                wakeLock?.acquire(3600_000L)
                try {
                    audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION
                } catch (e: Exception) {
                    // Audio mode
                }

                val currentCall = CallManager.getInstance(this).callState.value
                val notification = buildCallNotification(currentCall.takeIf { it.destinationNumber.isNotBlank() }
                    ?: ActiveCallInfo(destinationNumber = destination, callerIdUsed = callerId, phase = CallPhase.INITIALIZING))

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE or ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
                    )
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
            }
            ACTION_END_CALL -> {
                CallManager.getInstance(this).endCall("Ended from notification")
                teardownAndStop()
            }
            ACTION_STOP_SERVICE -> {
                teardownAndStop()
            }
            ACTION_TOGGLE_MUTE -> {
                CallManager.getInstance(this).toggleMute()
            }
            ACTION_TOGGLE_SPEAKER -> {
                CallManager.getInstance(this).toggleSpeaker()
            }
        }
        return START_NOT_STICKY
    }

    private fun updateNotification(info: ActiveCallInfo) {
        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(NOTIFICATION_ID, buildCallNotification(info))
    }

    private fun teardownAndStop() {
        callStateJob?.cancel()
        try {
            audioManager?.mode = AudioManager.MODE_NORMAL
        } catch (e: Exception) {
            // Reset mode
        }
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "DialerID Active Call",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Ongoing phone call notifications"
                setShowBadge(true)
                enableVibration(false)
                setSound(null, null)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildCallNotification(info: ActiveCallInfo): Notification {
        val contentIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this, 0, contentIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val endCallIntent = Intent(this, VoipCallService::class.java).apply {
            action = ACTION_END_CALL
        }
        val endCallPendingIntent = PendingIntent.getService(
            this, 1, endCallIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val title = when (info.phase) {
            CallPhase.INITIALIZING -> "Initializing Line • ${info.destinationNumber}"
            CallPhase.DIALING -> "Dialing • ${info.destinationNumber}"
            CallPhase.CONNECTING -> "Connecting (100 Trying) • ${info.destinationNumber}"
            CallPhase.RINGING -> "Ringing Destination (180) • ${info.destinationNumber}"
            CallPhase.EARLY_MEDIA -> "Session Progress (183) • ${info.destinationNumber}"
            CallPhase.CONNECTED -> "Connecting Audio • ${info.destinationNumber}"
            CallPhase.ACTIVE -> "Active Call (${info.formattedDuration}) • ${info.destinationNumber}"
            CallPhase.ON_HOLD -> "Call On Hold • ${info.destinationNumber}"
            CallPhase.ENDING -> "Ending Call • ${info.destinationNumber}"
            CallPhase.ENDED -> "Call Ended • ${info.destinationNumber}"
            CallPhase.IDLE -> "DialerID Call"
        }

        val content = when (info.phase) {
            CallPhase.ACTIVE -> "${info.audioCodec} • Transmitting as: ${info.callerIdUsed}"
            CallPhase.RINGING, CallPhase.EARLY_MEDIA -> "Destination ringing • Caller ID: ${info.callerIdUsed}"
            CallPhase.ON_HOLD -> "Call is paused on hold"
            CallPhase.ENDED -> info.endReason
            else -> if (info.statusMessage.isNotBlank()) info.statusMessage else "Connecting line via ${info.sipHost}..."
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(content)
            .setContentIntent(contentPendingIntent)
            .setOngoing(info.isCallActive)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "End Call",
                endCallPendingIntent
            )
            .build()
    }

    override fun onDestroy() {
        callStateJob?.cancel()
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
        try {
            audioManager?.mode = AudioManager.MODE_NORMAL
        } catch (e: Exception) {
            // Mode reset
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
