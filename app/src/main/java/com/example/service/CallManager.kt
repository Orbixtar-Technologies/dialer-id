package com.example.service

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.example.data.model.CallStatus
import com.example.data.repository.DialerRepository
import com.example.service.sip.SipCallEventListener
import com.example.service.sip.SipEngine
import com.example.util.PhoneNumberSanitizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class CallManager private constructor(private val context: Context) : SipCallEventListener {

    private val repository = DialerRepository.getInstance(context)
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var callJob: Job? = null
    private var timerJob: Job? = null
    private var toneGenerator: ToneGenerator? = null

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    val sipEngine: SipEngine = SipEngine.getInstance(context.applicationContext)

    private val _callState = MutableStateFlow(ActiveCallInfo())
    val callState: StateFlow<ActiveCallInfo> = _callState.asStateFlow()

    private val endingCall = AtomicBoolean(false)

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_VOICE_CALL, 70)
        } catch (e: Exception) {
            // ToneGenerator fallback
        }
        sipEngine.setListener(this)
    }

    fun startCall(destinationNumber: String, callerId: String, countryName: String, rate: Double): Boolean {
        val sanitized = PhoneNumberSanitizer.sanitizeDestination(destinationNumber) ?: return false
        val currentProfile = repository.userProfile.value
        val isTestNumber = sanitized == "3200" || sanitized == "444"

        if (currentProfile.creditBalance <= 0.0 && !isTestNumber) {
            return false
        }

        val effectiveSipConfig = currentProfile.sipConfig
        if (effectiveSipConfig == null || effectiveSipConfig.needsPassword()) {
            Log.w("CallManager", "Refusing call: SIP password required")
            return false
        }
        if (!effectiveSipConfig.hasUsableCredentials()) {
            Log.w("CallManager", "Refusing call: SIP credentials are not configured")
            return false
        }
        if (!sipEngine.isRegistered) {
            Log.w("CallManager", "Refusing call: Linphone is not registered")
            return false
        }

        endingCall.set(false)
        callJob?.cancel()
        timerJob?.cancel()

        val selectedOutboundId = callerId.ifEmpty { currentProfile.selectedCallerId }

        val preferredCodec = when (currentProfile.preferredCodec) {
            "G711A" -> com.example.service.sip.G711CodecType.PCMA
            "G711U" -> com.example.service.sip.G711CodecType.PCMU
            else -> null
        }

        val initialCodecName = when (preferredCodec) {
            com.example.service.sip.G711CodecType.PCMA -> "G.711a (PCMA)"
            com.example.service.sip.G711CodecType.PCMU -> "G.711u (PCMU)"
            else -> "G.711 (PCMA/PCMU)"
        }

        _callState.value = ActiveCallInfo(
            destinationNumber = sanitized,
            callerIdUsed = selectedOutboundId,
            countryName = countryName,
            phase = CallPhase.INITIALIZING,
            durationSeconds = 0,
            isMuted = false,
            isSpeakerOn = false,
            isEncrypted = false,
            billingRate = if (isTestNumber) 0.00 else rate,
            dtmfLog = "",
            sipHost = effectiveSipConfig.host,
            audioCodec = initialCodecName,
            latencyMs = 20,
            packetsSent = 0L,
            packetsReceived = 0L,
            statusMessage = "Initializing Call..."
        )

        startVoipService()
        playTone(ToneGenerator.TONE_SUP_CONFIRM, 150)

        scope.launch(Dispatchers.IO) {
            sipEngine.startOutboundCall(
                sipConfig = effectiveSipConfig,
                destination = sanitized,
                preferredCodec = preferredCodec
            )
        }

        return true
    }

    override fun onInitializing(details: String) {
        scope.launch(Dispatchers.Main) {
            if (_callState.value.phase != CallPhase.ENDED) {
                _callState.value = _callState.value.copy(
                    phase = CallPhase.INITIALIZING,
                    statusMessage = details
                )
            }
        }
    }

    override fun onDialing(details: String) {
        scope.launch(Dispatchers.Main) {
            if (_callState.value.phase != CallPhase.ENDED && _callState.value.phase != CallPhase.ACTIVE) {
                _callState.value = _callState.value.copy(
                    phase = CallPhase.DIALING,
                    statusMessage = details
                )
            }
        }
    }

    override fun onConnecting(details: String) {
        scope.launch(Dispatchers.Main) {
            if (_callState.value.phase != CallPhase.ENDED && _callState.value.phase != CallPhase.ACTIVE) {
                _callState.value = _callState.value.copy(
                    phase = CallPhase.CONNECTING,
                    statusMessage = details
                )
            }
        }
    }

    override fun onRegistering(host: String) {
        scope.launch(Dispatchers.Main) {
            _callState.value = _callState.value.copy(
                sipHost = host
            )
        }
    }

    override fun onRegistered(username: String) {
        Log.d("CallManager", "SIP Operator Registered: $username")
    }

    override fun onRinging(remoteTag: String?) {
        scope.launch(Dispatchers.Main) {
            if (_callState.value.phase != CallPhase.ACTIVE && _callState.value.phase != CallPhase.ENDED) {
                _callState.value = _callState.value.copy(
                    phase = CallPhase.RINGING,
                    statusMessage = "Ringing Destination (180 Ringing)..."
                )
                playTone(ToneGenerator.TONE_SUP_RINGTONE, 800)
            }
        }
    }

    override fun onEarlyMedia(details: String) {
        scope.launch(Dispatchers.Main) {
            if (_callState.value.phase != CallPhase.ACTIVE && _callState.value.phase != CallPhase.ENDED) {
                _callState.value = _callState.value.copy(
                    phase = CallPhase.EARLY_MEDIA,
                    statusMessage = details
                )
            }
        }
    }

    override fun onConnected(audioCodec: String, localRtpPort: Int, remoteRtpPort: Int) {
        scope.launch(Dispatchers.Main) {
            if (_callState.value.phase == CallPhase.ENDED) return@launch

            _callState.value = _callState.value.copy(
                phase = CallPhase.CONNECTED,
                audioCodec = audioCodec,
                isEncrypted = sipEngine.isCurrentCallEncrypted(),
                statusMessage = "Connecting Audio Streams..."
            )
        }
    }

    override fun onStreamsRunning(audioCodec: String, localRtpPort: Int, remoteRtpPort: Int) {
        scope.launch(Dispatchers.Main) {
            if (_callState.value.phase == CallPhase.ENDED) return@launch

            vibrate(120)
            val isEncrypted = sipEngine.isCurrentCallEncrypted()
            _callState.value = _callState.value.copy(
                phase = CallPhase.ACTIVE,
                audioCodec = audioCodec,
                isEncrypted = isEncrypted,
                statusMessage = if (isEncrypted) "Secured Line Connected (SRTP)" else "HD Line Connected"
            )

            // Accurate duration timer: starts strictly when RTP streams are active
            timerJob?.cancel()
            timerJob = launch {
                while (isActive && _callState.value.phase == CallPhase.ACTIVE) {
                    delay(1000)
                    val newDuration = _callState.value.durationSeconds + 1
                    _callState.value = _callState.value.copy(durationSeconds = newDuration)
                }
            }
        }
    }

    override fun onHold(isPausedByRemote: Boolean) {
        scope.launch(Dispatchers.Main) {
            if (_callState.value.phase != CallPhase.ENDED) {
                _callState.value = _callState.value.copy(
                    phase = CallPhase.ON_HOLD,
                    isOnHold = true,
                    statusMessage = if (isPausedByRemote) "Call Paused by Remote Party" else "Call On Hold"
                )
            }
        }
    }

    override fun onResumed() {
        scope.launch(Dispatchers.Main) {
            if (_callState.value.phase != CallPhase.ENDED) {
                val isEncrypted = sipEngine.isCurrentCallEncrypted()
                _callState.value = _callState.value.copy(
                    phase = CallPhase.ACTIVE,
                    isOnHold = false,
                    statusMessage = if (isEncrypted) "Secured Line Connected (SRTP)" else "HD Line Connected"
                )
            }
        }
    }

    override fun onTerminating(reason: String) {
        scope.launch(Dispatchers.Main) {
            if (_callState.value.phase != CallPhase.ENDED) {
                _callState.value = _callState.value.copy(
                    phase = CallPhase.ENDING,
                    statusMessage = reason
                )
            }
        }
    }

    override fun onAudioStatsUpdated(latencyMs: Int, packetsSent: Long, packetsReceived: Long) {
        scope.launch(Dispatchers.Main) {
            if (_callState.value.phase == CallPhase.ACTIVE || _callState.value.phase == CallPhase.CONNECTED) {
                _callState.value = _callState.value.copy(
                    latencyMs = latencyMs,
                    packetsSent = packetsSent,
                    packetsReceived = packetsReceived
                )
            }
        }
    }

    override fun onError(errorCode: Int, message: String) {
        scope.launch(Dispatchers.Main) {
            Log.e("CallManager", "SIP Error received: $errorCode -> $message")
            endCall(reason = message, statusCode = errorCode)
        }
    }

    override fun onEnded(reason: String) {
        scope.launch(Dispatchers.Main) {
            endCall(reason = reason.ifBlank { "Call Ended" })
        }
    }

    fun endCall(reason: String = "Call Ended", statusCode: Int = 0) {
        val current = _callState.value
        if (!endingCall.compareAndSet(false, true)) {
            return
        }
        if (current.phase == CallPhase.IDLE || current.phase == CallPhase.ENDED) {
            endingCall.set(false)
            return
        }

        callJob?.cancel()
        timerJob?.cancel()

        scope.launch(Dispatchers.IO) {
            sipEngine.stopCall(reason)
        }

        val finalDuration = current.durationSeconds
        val finalStatus = if (finalDuration > 0) CallStatus.COMPLETED else CallStatus.CANCELLED

        playTone(ToneGenerator.TONE_SUP_BUSY, 300)
        vibrate(150)

        val cleanReason = formatEndReason(reason, statusCode)

        _callState.value = current.copy(
            phase = CallPhase.ENDED,
            endReason = cleanReason,
            statusMessage = cleanReason,
            sipResponseCode = statusCode
        )

        stopVoipService()

        scope.launch(Dispatchers.IO) {
            repository.logCall(
                destinationNumber = current.destinationNumber,
                callerIdUsed = current.callerIdUsed,
                countryName = current.countryName,
                status = finalStatus,
                durationSeconds = finalDuration,
                billingRatePerMin = current.billingRate
            )
        }

        scope.launch {
            delay(2000)
            _callState.value = ActiveCallInfo()
            endingCall.set(false)
        }
    }

    private fun formatEndReason(reason: String, statusCode: Int): String {
        return when {
            statusCode == 486 || reason.contains("busy", ignoreCase = true) -> "Busy Here (486)"
            statusCode == 603 || reason.contains("decline", ignoreCase = true) -> "Call Declined (603)"
            statusCode == 404 || reason.contains("not found", ignoreCase = true) -> "Number Not Found (404)"
            statusCode == 408 || reason.contains("timeout", ignoreCase = true) -> "Request Timeout (408)"
            statusCode == 480 || reason.contains("temporarily unavailable", ignoreCase = true) -> "Temporarily Unavailable (480)"
            statusCode == 488 || reason.contains("not acceptable", ignoreCase = true) -> "Media Not Acceptable (488)"
            statusCode == 503 || reason.contains("service unavailable", ignoreCase = true) -> "Service Unavailable (503)"
            reason.isNotBlank() && !reason.equals("Call terminated", ignoreCase = true) -> reason
            else -> "Call Ended"
        }
    }

    fun toggleMute() {
        val newMute = !_callState.value.isMuted
        _callState.value = _callState.value.copy(isMuted = newMute)
        try {
            audioManager.isMicrophoneMute = newMute
        } catch (e: Exception) {
            // Safety
        }
    }

    fun toggleSpeaker() {
        val newSpeaker = !_callState.value.isSpeakerOn
        _callState.value = _callState.value.copy(isSpeakerOn = newSpeaker)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (newSpeaker) {
                    val speakerDevice = audioManager.availableCommunicationDevices.firstOrNull {
                        it.type == android.media.AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
                    }
                    if (speakerDevice != null) {
                        audioManager.setCommunicationDevice(speakerDevice)
                    } else {
                        @Suppress("DEPRECATION")
                        audioManager.isSpeakerphoneOn = true
                    }
                } else {
                    audioManager.clearCommunicationDevice()
                    @Suppress("DEPRECATION")
                    audioManager.isSpeakerphoneOn = false
                }
            } else {
                @Suppress("DEPRECATION")
                audioManager.isSpeakerphoneOn = newSpeaker
            }
        } catch (e: Exception) {
            try {
                @Suppress("DEPRECATION")
                audioManager.isSpeakerphoneOn = newSpeaker
            } catch (ignored: Exception) {}
        }
    }

    fun sendDtmfTone(digit: Char) {
        val updatedLog = _callState.value.dtmfLog + digit
        _callState.value = _callState.value.copy(dtmfLog = updatedLog)

        sipEngine.sendDtmfTone(digit)

        val toneType = when (digit) {
            '0' -> ToneGenerator.TONE_DTMF_0
            '1' -> ToneGenerator.TONE_DTMF_1
            '2' -> ToneGenerator.TONE_DTMF_2
            '3' -> ToneGenerator.TONE_DTMF_3
            '4' -> ToneGenerator.TONE_DTMF_4
            '5' -> ToneGenerator.TONE_DTMF_5
            '6' -> ToneGenerator.TONE_DTMF_6
            '7' -> ToneGenerator.TONE_DTMF_7
            '8' -> ToneGenerator.TONE_DTMF_8
            '9' -> ToneGenerator.TONE_DTMF_9
            '*' -> ToneGenerator.TONE_DTMF_S
            '#' -> ToneGenerator.TONE_DTMF_P
            else -> ToneGenerator.TONE_PROP_BEEP
        }
        playTone(toneType, 120)
        vibrate(30)
    }

    private fun playTone(toneType: Int, durationMs: Int) {
        try {
            toneGenerator?.startTone(toneType, durationMs)
        } catch (e: Exception) {
            // Ignore sound error
        }
    }

    private fun vibrate(durationMs: Long) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                vibrator?.vibrate(durationMs)
            }
        } catch (e: Exception) {
            // Vibrator fallback
        }
    }

    private fun startVoipService() {
        try {
            val intent = Intent(context, VoipCallService::class.java).apply {
                action = VoipCallService.ACTION_START_CALL
                putExtra(VoipCallService.EXTRA_DESTINATION, _callState.value.destinationNumber)
                putExtra(VoipCallService.EXTRA_CALLER_ID, _callState.value.callerIdUsed)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (e: Exception) {
            // Service startup handle
        }
    }

    private fun stopVoipService() {
        try {
            val intent = Intent(context, VoipCallService::class.java).apply {
                action = VoipCallService.ACTION_STOP_SERVICE
            }
            context.startService(intent)
        } catch (e: Exception) {
            // Service teardown
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: CallManager? = null

        fun getInstance(context: Context): CallManager {
            return INSTANCE ?: synchronized(this) {
                val instance = CallManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
