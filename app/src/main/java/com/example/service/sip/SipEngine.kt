package com.example.service.sip

import android.content.Context
import android.util.Log
import com.example.data.model.SipConfig
import com.example.util.PhoneNumberSanitizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.linphone.core.AVPFMode
import org.linphone.core.Account
import org.linphone.core.Call
import org.linphone.core.Core
import org.linphone.core.CoreListenerStub
import org.linphone.core.Factory
import org.linphone.core.LogLevel
import org.linphone.core.MediaEncryption
import org.linphone.core.RegistrationState
import org.linphone.core.TransportType

data class SipTestResult(
    val isSuccess: Boolean,
    val statusCode: Int,
    val message: String,
    val latencyMs: Long,
    val serverBanner: String = ""
)

interface SipCallEventListener {
    fun onConnecting(details: String)
    fun onRegistering(host: String)
    fun onRegistered(username: String)
    fun onRinging(remoteTag: String?)
    fun onConnected(audioCodec: String, localRtpPort: Int, remoteRtpPort: Int)
    fun onError(errorCode: Int, message: String)
    fun onEnded(reason: String)
    fun onAudioStatsUpdated(latencyMs: Int, packetsSent: Long, packetsReceived: Long)
}

/**
 * Single Linphone stack for REGISTER + INVITE.
 * Custom UDP registrar ([SipRegistrationEngine]) is isolated and is not the source of truth.
 *
 * Media: SRTP is offered and preferred. TLS transport is not forced because typical
 * UDP:5060 trunks (including user-entered sip.sipup.org) will fail a TLS handshake.
 * UI must only claim encryption when [isCurrentCallEncrypted] is true.
 */
class SipEngine private constructor(
    private val context: Context
) {
    private val tag = "SipEngine"
    private val engineJob = SupervisorJob()
    private val engineScope = CoroutineScope(Dispatchers.Main + engineJob)
    private var iterateJob: Job? = null
    private var core: Core? = null
    private var currentCall: Call? = null
    private var listener: SipCallEventListener? = null

    private var pendingCallDestination: String? = null
    private var pendingCallConfig: SipConfig? = null
    private var pendingCallCodec: G711CodecType? = null
    private var currentSipConfig: SipConfig? = null
    private var lastRegisterFingerprint: String? = null

    private val _registrationState = MutableStateFlow(SipRegistrationState())
    val registrationState: StateFlow<SipRegistrationState> = _registrationState.asStateFlow()

    val isRegistered: Boolean
        get() = _registrationState.value.isRegistered

    fun isCurrentCallEncrypted(): Boolean {
        val encryption = currentCall?.currentParams?.mediaEncryption ?: return false
        return encryption != MediaEncryption.None
    }

    fun setListener(listener: SipCallEventListener?) {
        this.listener = listener
    }

    private fun executePendingCall() {
        val dest = pendingCallDestination ?: return
        val config = pendingCallConfig ?: return
        pendingCallDestination = null

        val sanitized = PhoneNumberSanitizer.sanitizeDestination(dest)
        if (sanitized == null) {
            listener?.onError(400, "Invalid destination number")
            return
        }

        val c = core ?: return
        val callAddress = c.createAddress("sip:$sanitized@${config.host}")
        val callParams = c.createCallParams(null)

        val desiredIdentity = "sip:${config.username}@${config.host}"
        val account = c.accountList.find { it.params?.identityAddress?.asStringUriOnly() == desiredIdentity } ?: c.defaultAccount
        callParams?.account = account

        callParams?.setAudioEnabled(true)
        callParams?.setVideoEnabled(false)
        // Offer SRTP; do not require it so UDP trunks that lack SRTP still connect.
        callParams?.setMediaEncryption(MediaEncryption.SRTP)
        callParams?.setAvpfEnabled(false)
        callParams?.setCapabilityNegotiationsEnabled(false)
        callParams?.setCapabilityNegotiationReinviteEnabled(false)

        if (callAddress != null && callParams != null) {
            currentCall = c.inviteAddressWithParams(callAddress, callParams)
        } else {
            listener?.onError(500, "Failed to create call parameters")
        }
    }

    companion object {
        private val _lastSdpDiagnosticDump = MutableStateFlow<SdpDiagnosticDump?>(null)
        val lastSdpDiagnosticDump: StateFlow<SdpDiagnosticDump?> = _lastSdpDiagnosticDump

        @Volatile
        private var INSTANCE: SipEngine? = null

        fun getInstance(context: Context): SipEngine {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SipEngine(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val coreListener = object : CoreListenerStub() {
        override fun onCallStateChanged(
            core: Core,
            call: Call,
            state: Call.State,
            message: String
        ) {
            Log.i(tag, "Call state changed: $state - $message")
            when (state) {
                Call.State.OutgoingProgress -> listener?.onConnecting(message)
                Call.State.OutgoingRinging -> listener?.onRinging(null)
                Call.State.Connected -> {
                    val codec = call.currentParams?.usedAudioPayloadType?.mimeType ?: "Unknown"
                    listener?.onConnected(codec, 0, 0)
                }
                Call.State.Error -> listener?.onError(500, message)
                Call.State.End, Call.State.Released -> listener?.onEnded(message)
                else -> {}
            }
        }

        override fun onAccountRegistrationStateChanged(
            core: Core,
            account: Account,
            state: RegistrationState,
            message: String
        ) {
            Log.i(tag, "Registration state: $state")
            val username = account.params?.identityAddress?.username ?: currentSipConfig?.username.orEmpty()
            val host = account.params?.serverAddress?.domain ?: currentSipConfig?.host.orEmpty()
            when (state) {
                RegistrationState.Progress -> {
                    _registrationState.value = _registrationState.value.copy(
                        status = RegistrationStatus.REGISTERING,
                        username = username,
                        host = host,
                        port = currentSipConfig?.port ?: 5060,
                        statusMessage = "Registering with $host...",
                        lastError = null
                    )
                    listener?.onRegistering(host)
                }
                RegistrationState.Ok -> {
                    _registrationState.value = _registrationState.value.copy(
                        status = RegistrationStatus.REGISTERED,
                        username = username,
                        host = host,
                        port = currentSipConfig?.port ?: 5060,
                        statusCode = 200,
                        statusMessage = "Registered with $host",
                        lastError = null,
                        serverBanner = "Linphone",
                        isKeepAliveActive = true
                    )
                    listener?.onRegistered(username)
                    executePendingCall()
                }
                RegistrationState.Failed -> {
                    _registrationState.value = _registrationState.value.copy(
                        status = RegistrationStatus.FAILED,
                        username = username,
                        host = host,
                        statusCode = 401,
                        statusMessage = message.ifBlank { "Registration failed" },
                        lastError = message,
                        isKeepAliveActive = false
                    )
                    listener?.onError(401, message)
                }
                RegistrationState.Cleared, RegistrationState.None -> {
                    if (_registrationState.value.status != RegistrationStatus.FAILED) {
                        _registrationState.value = _registrationState.value.copy(
                            status = RegistrationStatus.UNREGISTERED,
                            statusMessage = "Unregistered",
                            isKeepAliveActive = false
                        )
                    }
                }
                else -> {}
            }
        }
    }

    init {
        try {
            val factory = Factory.instance()

            val loggingService = factory.loggingService
            loggingService?.domain = "Linphone"
            loggingService?.setLogLevel(LogLevel.Message)

            core = factory.createCore(null, null, context)
            core?.setUserAgent("LinphoneAndroid", "5.2.0")

            val config = core?.config
            config?.setInt("rtp", "rtcp_xr_enabled", 0)
            config?.setInt("rtp", "rtcp_fb_implicit_rtcp_sync", 0)
            config?.setInt("sip", "use_rtcp", 0)
            config?.setInt("sip", "use_avpf", 0)
            config?.setInt("video", "capture", 0)
            config?.setInt("video", "display", 0)
            config?.setInt("video", "enabled", 0)

            val vp = core?.videoActivationPolicy
            vp?.setAutomaticallyAccept(false)
            vp?.setAutomaticallyInitiate(false)

            core?.setVideoCaptureEnabled(false)
            core?.setVideoDisplayEnabled(false)

            core?.audioPayloadTypes?.forEach { pt ->
                val mime = pt.mimeType.uppercase()
                if (mime == "PCMU" || mime == "PCMA" || mime == "OPUS" || mime.contains("TELEPHONE-EVENT")) {
                    pt.enable(true)
                } else {
                    pt.enable(false)
                }
            }

            // Prefer SRTP when the peer supports it. Do not mandate it: many UDP
            // registrars (user-configured sip.sipup.org:5060) have no TLS listener
            // and will reject a mandatory-SRTP / TLS-only INVITE.
            core?.setMediaEncryption(MediaEncryption.SRTP)
            core?.setMediaEncryptionMandatory(false)

            core?.addListener(coreListener)
            core?.start()
            core?.setNetworkReachable(true)

            iterateJob = engineScope.launch {
                while (isActive) {
                    core?.iterate()
                    delay(20)
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to initialize Linphone Core", e)
        }
    }

    /**
     * Registers with Linphone. Cancels in-flight account setup first.
     * No-ops when host/user/password are unchanged.
     */
    fun register(sipConfig: SipConfig, force: Boolean = false) {
        if (!sipConfig.hasUsableCredentials()) {
            lastRegisterFingerprint = null
            currentSipConfig = null
            _registrationState.value = SipRegistrationState(
                status = RegistrationStatus.UNREGISTERED,
                statusMessage = "SIP credentials not configured",
                lastError = "Enter host, username, and password in Settings"
            )
            return
        }

        val fingerprint = sipConfig.registrationFingerprint()
        if (!force && fingerprint == lastRegisterFingerprint && _registrationState.value.isRegistered) {
            return
        }

        lastRegisterFingerprint = fingerprint
        currentSipConfig = sipConfig
        applyAccount(sipConfig, enableRegister = true)
    }

    fun refreshNow() {
        val config = currentSipConfig
        if (config == null || !config.hasUsableCredentials()) {
            _registrationState.value = _registrationState.value.copy(
                status = RegistrationStatus.UNREGISTERED,
                statusMessage = "SIP credentials not configured"
            )
            return
        }
        val account = core?.defaultAccount
        if (account != null) {
            core?.refreshRegisters()
        } else {
            register(config, force = true)
        }
    }

    fun unregister() {
        lastRegisterFingerprint = null
        currentSipConfig = null
        pendingCallDestination = null
        try {
            core?.clearAccounts()
            core?.clearProxyConfig()
            core?.clearAllAuthInfo()
        } catch (e: Exception) {
            Log.d(tag, "Unregister notice: ${e.message}")
        }
        _registrationState.value = SipRegistrationState(
            status = RegistrationStatus.UNREGISTERED,
            statusMessage = "Unregistered"
        )
    }

    fun startOutboundCall(
        sipConfig: SipConfig,
        destination: String,
        preferredCodec: G711CodecType?
    ) {
        val sanitized = PhoneNumberSanitizer.sanitizeDestination(destination)
        if (sanitized == null) {
            listener?.onError(400, "Invalid destination number")
            return
        }
        if (!sipConfig.hasUsableCredentials()) {
            listener?.onError(401, "SIP credentials not configured")
            return
        }

        val c = core ?: return
        applyAccount(sipConfig, enableRegister = true)
        lastRegisterFingerprint = sipConfig.registrationFingerprint()
        currentSipConfig = sipConfig

        c.setIpv6Enabled(false)
        c.setUseRfc2833ForDtmf(true)
        c.setUseInfoForDtmf(false)
        c.setAvpfMode(AVPFMode.Disabled)
        c.setMediaEncryption(MediaEncryption.SRTP)

        val targetMime = when (preferredCodec) {
            G711CodecType.PCMA -> "PCMA"
            G711CodecType.PCMU -> "PCMU"
            else -> "PCMU"
        }

        c.audioPayloadTypes?.forEach { pt ->
            val mime = pt.mimeType.uppercase()
            if (mime == targetMime || mime.contains("TELEPHONE-EVENT")) {
                pt.enable(true)
            } else {
                pt.enable(false)
            }
        }

        c.videoPayloadTypes?.forEach { pt ->
            pt.enable(false)
        }

        val natPolicy = c.createNatPolicy()
        if (natPolicy != null) {
            natPolicy.setIceEnabled(false)
            natPolicy.setStunEnabled(false)
            natPolicy.setTurnEnabled(false)
            c.setNatPolicy(natPolicy)
        }

        pendingCallDestination = sanitized
        pendingCallConfig = sipConfig
        pendingCallCodec = preferredCodec

        val desiredIdentity = "sip:${sipConfig.username}@${sipConfig.host}"
        val account = c.accountList.find { it.params?.identityAddress?.asStringUriOnly() == desiredIdentity } ?: c.defaultAccount
        if (account != null && account.state == RegistrationState.Ok) {
            executePendingCall()
        } else if (account == null) {
            listener?.onError(500, "Failed to create SIP account")
        }
    }

    suspend fun testSipConnection(sipConfig: SipConfig): SipTestResult = withContext(Dispatchers.IO) {
        if (!sipConfig.hasUsableCredentials()) {
            return@withContext SipTestResult(false, 0, "Host, username, and password are required", 0)
        }
        val start = System.currentTimeMillis()
        withContext(Dispatchers.Main) {
            register(sipConfig, force = true)
        }
        val observed = withTimeoutOrNull(12_000) {
            registrationState.first {
                it.status == RegistrationStatus.REGISTERED || it.status == RegistrationStatus.FAILED
            }
        }
        val elapsed = System.currentTimeMillis() - start
        when (observed?.status) {
            RegistrationStatus.REGISTERED -> SipTestResult(
                isSuccess = true,
                statusCode = observed.statusCode.takeIf { it > 0 } ?: 200,
                message = observed.statusMessage.ifBlank { "REGISTER succeeded" },
                latencyMs = elapsed,
                serverBanner = observed.serverBanner.ifBlank { "Linphone" }
            )
            RegistrationStatus.FAILED -> SipTestResult(
                isSuccess = false,
                statusCode = observed.statusCode.takeIf { it > 0 } ?: 401,
                message = observed.lastError ?: observed.statusMessage,
                latencyMs = elapsed,
                serverBanner = observed.serverBanner
            )
            else -> SipTestResult(false, 408, "REGISTER timed out", elapsed)
        }
    }

    fun stopCall(reason: String = "Call Ended") {
        currentCall?.terminate()
        currentCall = null
    }

    fun sendDtmfTone(digit: Char) {
        currentCall?.sendDtmf(digit)
    }

    fun updateDiagnosticDump(dump: SdpDiagnosticDump) {
        _lastSdpDiagnosticDump.value = dump
    }

    fun destroy() {
        iterateJob?.cancel()
        iterateJob = null
        try {
            currentCall?.terminate()
            core?.stop()
        } catch (e: Exception) {
            Log.d(tag, "Core stop notice: ${e.message}")
        }
        core = null
        engineJob.cancel()
        synchronized(SipEngine::class.java) {
            if (INSTANCE === this) {
                INSTANCE = null
            }
        }
    }

    private fun applyAccount(sipConfig: SipConfig, enableRegister: Boolean) {
        val c = core ?: return
        c.clearAccounts()
        c.clearProxyConfig()
        c.clearAllAuthInfo()

        val authInfo = Factory.instance().createAuthInfo(
            sipConfig.username,
            null,
            sipConfig.password,
            null,
            null,
            sipConfig.host
        )
        c.addAuthInfo(authInfo)

        val accountParams = c.createAccountParams() ?: return
        val identity = Factory.instance().createAddress("sip:${sipConfig.username}@${sipConfig.host}")
        accountParams.setIdentityAddress(identity)

        val serverAddr = Factory.instance().createAddress("sip:${sipConfig.host}:${sipConfig.port}")
        // UDP:5060 is the common registrar transport. Forcing TLS here would break
        // existing sip.sipup.org-style trunks that do not listen on 5061.
        serverAddr?.transport = TransportType.Udp
        accountParams.setServerAddress(serverAddr)
        accountParams.setOutboundProxyEnabled(true)
        accountParams.setRegisterEnabled(enableRegister)

        accountParams.setAvpfMode(AVPFMode.Disabled)
        accountParams.setRtpBundleEnabled(false)
        accountParams.setQualityReportingEnabled(false)
        accountParams.setPushNotificationAllowed(false)
        accountParams.setRemotePushNotificationAllowed(false)

        val natPolicy = c.createNatPolicy()
        if (natPolicy != null) {
            natPolicy.setIceEnabled(false)
            natPolicy.setStunEnabled(false)
            natPolicy.setTurnEnabled(false)
            accountParams.setNatPolicy(natPolicy)
        }

        val account = c.createAccount(accountParams)
        c.addAccount(account)
        c.defaultAccount = account

        if (enableRegister) {
            _registrationState.value = _registrationState.value.copy(
                status = RegistrationStatus.REGISTERING,
                username = sipConfig.username,
                host = sipConfig.host,
                port = sipConfig.port,
                statusMessage = "Registering with ${sipConfig.host}...",
                lastError = null
            )
        }
    }
}
