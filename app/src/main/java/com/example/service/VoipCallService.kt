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

class VoipCallService : Service() {

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

                val notification = buildCallNotification(destination, callerId)
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

    private fun teardownAndStop() {
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

    private fun buildCallNotification(destination: String, callerId: String): Notification {
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

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Active Call • $destination")
            .setContentText("Transmitting as: $callerId")
            .setContentIntent(contentPendingIntent)
            .setOngoing(true)
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
