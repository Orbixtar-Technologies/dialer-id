package com.example.service.sip

import android.content.Context
import android.os.Looper
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
import org.linphone.core.AuthInfo
import org.linphone.core.AuthMethod
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
    @Volatile
    private var coreStarted: Boolean = false
    private var currentCall: Call? = null
    private var listener: SipCallEventListener? = null

    private var pendingCallDestination: String? = null
    private var pendingCallConfig: SipConfig? = null
    private var pendingCallCodec: G711CodecType? = null
    private var currentSipConfig: SipConfig? = null
    private var lastRegisterFingerprint: String? = null
    @Volatile
    private var replacingAccount: Boolean = false
    private var retryJob: Job? = null
    private var nextRetryAtMs: Long = 0L
    private var failCount: Int = 0

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
        override fun onAuthenticationRequested(core: Core, authInfo: AuthInfo, method: AuthMethod) {
            val config = currentSipConfig
            if (config == null || !config.hasUsableCredentials()) {
                Log.w(tag, "401/407 challenge received but SIP password is not configured")
                return
            }
            fillAuthInfo(authInfo, config)
            core.addAuthInfo(authInfo)
            Log.i(tag, "Answered ${method.name} challenge for ${config.username}@${config.host} (realm=${authInfo.realm ?: "*"})")
        }

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
                Call.State.Error -> {
                    val info = call.errorInfo
                    val mapped = SipAuthErrorMapper.map(
                        protocolCode = info?.protocolCode ?: 0,
                        linphoneMessage = message,
                        reasonName = info?.reason?.name.orEmpty()
                    )
                    listener?.onError(mapped.statusCode.takeIf { it > 0 } ?: 500, mapped.message)
                }
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
            if (replacingAccount && state == RegistrationState.Failed) {
                Log.d(tag, "Ignoring registration failure while replacing account: $message")
                return
            }
            Log.i(tag, "Registration state: $state ($message)")
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
                        lastError = null,
                        needsPassword = false
                    )
                    listener?.onRegistering(host)
                }
                RegistrationState.Ok -> {
                    failCount = 0
                    nextRetryAtMs = 0L
                    retryJob?.cancel()
                    retryJob = null
                    val contact = account.contactAddress?.asStringUriOnly().orEmpty()
                    Log.i(tag, "REGISTER 200 OK contact=$contact server=$host")
                    _registrationState.value = _registrationState.value.copy(
                        status = RegistrationStatus.REGISTERED,
                        username = username,
                        host = host,
                        port = currentSipConfig?.port ?: 5060,
                        statusCode = 200,
                        statusMessage = "Registered with $host",
                        lastError = null,
                        retryCount = 0,
                        retryAfterSeconds = 0,
                        serverBanner = "Linphone",
                        isKeepAliveActive = true,
                        needsPassword = false
                    )
                    listener?.onRegistered(username)
                    executePendingCall()
                }
                RegistrationState.Failed -> {
                    val info = account.errorInfo
                    val contact = account.contactAddress?.asStringUriOnly().orEmpty()
                    val server = account.params?.serverAddress?.asStringUriOnly().orEmpty()
                    val transport = account.params?.serverAddress?.transport?.name.orEmpty()
                    val mapped = SipAuthErrorMapper.map(
                        protocolCode = info?.protocolCode ?: 0,
                        linphoneMessage = message,
                        reasonName = info?.reason?.name.orEmpty(),
                        phrase = info?.phrase.orEmpty(),
                        warnings = info?.warnings.orEmpty()
                    )
                    Log.w(
                        tag,
                        "REGISTER failed protocol=${info?.protocolCode ?: 0} phrase=${info?.phrase} " +
                            "reason=${info?.reason?.name} warnings=${info?.warnings} msg=$message " +
                            "server=$server transport=$transport contact=$contact retryAfter=${mapped.retryAfterSeconds}"
                    )
                    val alreadyFailed = _registrationState.value.status == RegistrationStatus.FAILED
                    if (!alreadyFailed) {
                        failCount += 1
                    }
                    _registrationState.value = _registrationState.value.copy(
                        status = RegistrationStatus.FAILED,
                        username = username,
                        host = host,
                        statusCode = mapped.statusCode,
                        statusMessage = mapped.message,
                        lastError = mapped.message,
                        retryCount = failCount,
                        retryAfterSeconds = mapped.retryAfterSeconds,
                        isKeepAliveActive = false,
                        needsPassword = false
                    )
                    if (!alreadyFailed) {
                        currentSipConfig?.let { scheduleRetry(it, mapped.statusCode, mapped.retryAfterSeconds) }
                    }
                    if (pendingCallDestination != null) {
                        pendingCallDestination = null
                        listener?.onError(mapped.statusCode.takeIf { it > 0 } ?: 401, mapped.message)
                    }
                }
                RegistrationState.Cleared, RegistrationState.None -> {
                    if (_registrationState.value.status != RegistrationStatus.FAILED &&
                        !_registrationState.value.needsPassword
                    ) {
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
        // Defer Core.create/start off the constructing call stack. Creating the
        // stack inside MainActivity.onCreate (via ViewModel → CallManager) blocks
        // SipRegisterService.startForeground() and crashes with
        // ForegroundServiceDidNotStartInTimeException. Debug JNI log listeners and
        // auto-iterate + manual iterate also SIGSEGV Linphone 5.3 on device.
        engineScope.launch {
            ensureCore()
        }
    }

    /**
     * Registers with Linphone. Cancels in-flight account setup first.
     * No-ops when host/user/password are unchanged and a REGISTER is already in flight.
     */
    fun register(sipConfig: SipConfig, force: Boolean = false) {
        if (!onEngineThread()) {
            engineScope.launch { register(sipConfig, force) }
            return
        }
        ensureCore()
        if (core == null || !coreStarted) {
            _registrationState.value = SipRegistrationState(
                status = RegistrationStatus.FAILED,
                statusMessage = "SIP engine failed to start",
                lastError = "Linphone Core could not be created"
            )
            return
        }
        if (sipConfig.needsPassword()) {
            lastRegisterFingerprint = null
            currentSipConfig = sipConfig
            publishPasswordRequired(sipConfig)
            return
        }
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
        val status = _registrationState.value.status
        if (!force && fingerprint == lastRegisterFingerprint &&
            (status == RegistrationStatus.REGISTERED || status == RegistrationStatus.REGISTERING)
        ) {
            return
        }
        if (!force && fingerprint == lastRegisterFingerprint &&
            status == RegistrationStatus.FAILED &&
            System.currentTimeMillis() < nextRetryAtMs
        ) {
            Log.d(tag, "Skipping REGISTER; backoff until ${nextRetryAtMs - System.currentTimeMillis()}ms")
            return
        }

        lastRegisterFingerprint = fingerprint
        currentSipConfig = sipConfig
        applyAccount(sipConfig, enableRegister = true, recreate = force)
    }

    fun refreshNow(force: Boolean = false) {
        if (!onEngineThread()) {
            engineScope.launch { refreshNow(force) }
            return
        }
        ensureCore()
        val config = currentSipConfig
        if (config == null || config.needsPassword()) {
            if (config != null) publishPasswordRequired(config)
            else {
                _registrationState.value = _registrationState.value.copy(
                    status = RegistrationStatus.UNREGISTERED,
                    statusMessage = "SIP credentials not configured"
                )
            }
            return
        }
        if (!config.hasUsableCredentials()) {
            _registrationState.value = _registrationState.value.copy(
                status = RegistrationStatus.UNREGISTERED,
                statusMessage = "SIP credentials not configured"
            )
            return
        }
        if (_registrationState.value.status == RegistrationStatus.REGISTERING) {
            return
        }
        if (!force && System.currentTimeMillis() < nextRetryAtMs) {
            Log.d(tag, "Skipping refresh; backoff active")
            return
        }
        if (force) {
            nextRetryAtMs = 0L
            retryJob?.cancel()
        }
        val account = core?.defaultAccount
        if (account != null) {
            core?.refreshRegisters()
        } else {
            register(config, force = true)
        }
    }

    fun unregister() {
        if (!onEngineThread()) {
            engineScope.launch { unregister() }
            return
        }
        if (core == null || !coreStarted) return
        lastRegisterFingerprint = null
        currentSipConfig = null
        pendingCallDestination = null
        retryJob?.cancel()
        retryJob = null
        nextRetryAtMs = 0L
        failCount = 0
        try {
            replacingAccount = true
            core?.clearAccounts()
            core?.clearProxyConfig()
            core?.clearAllAuthInfo()
        } catch (e: Exception) {
            Log.d(tag, "Unregister notice: ${e.message}")
        } finally {
            replacingAccount = false
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
        if (!onEngineThread()) {
            engineScope.launch { startOutboundCall(sipConfig, destination, preferredCodec) }
            return
        }
        ensureCore()
        if (core == null || !coreStarted) {
            listener?.onError(500, "SIP engine failed to start")
            return
        }
        val sanitized = PhoneNumberSanitizer.sanitizeDestination(destination)
        if (sanitized == null) {
            listener?.onError(400, "Invalid destination number")
            return
        }
        if (sipConfig.needsPassword()) {
            publishPasswordRequired(sipConfig)
            listener?.onError(0, "SIP password required")
            return
        }
        if (!sipConfig.hasUsableCredentials()) {
            listener?.onError(0, "SIP credentials not configured")
            return
        }

        val c = core ?: return
        val fingerprint = sipConfig.registrationFingerprint()
        if (fingerprint != lastRegisterFingerprint || !isRegistered) {
            applyAccount(sipConfig, enableRegister = true, recreate = false)
            lastRegisterFingerprint = fingerprint
        }
        currentSipConfig = sipConfig

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
        if (sipConfig.needsPassword()) {
            return@withContext SipTestResult(false, 0, "SIP password required", 0)
        }
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
                statusCode = observed.statusCode,
                message = observed.lastError ?: observed.statusMessage,
                latencyMs = elapsed,
                serverBanner = observed.serverBanner
            )
            else -> SipTestResult(false, 408, "REGISTER timed out", elapsed)
        }
    }

    fun stopCall(reason: String = "Call Ended") {
        if (!onEngineThread()) {
            engineScope.launch { stopCall(reason) }
            return
        }
        currentCall?.terminate()
        currentCall = null
    }

    fun sendDtmfTone(digit: Char) {
        if (!onEngineThread()) {
            engineScope.launch { sendDtmfTone(digit) }
            return
        }
        currentCall?.sendDtmf(digit)
    }

    fun updateDiagnosticDump(dump: SdpDiagnosticDump) {
        _lastSdpDiagnosticDump.value = dump
    }

    fun destroy() {
        coreStarted = false
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

    private fun publishPasswordRequired(sipConfig: SipConfig) {
        _registrationState.value = SipRegistrationState(
            status = RegistrationStatus.FAILED,
            username = sipConfig.username,
            host = sipConfig.host,
            port = sipConfig.port,
            statusCode = 0,
            statusMessage = "SIP password required",
            lastError = "Re-enter the SIP password in Settings, or wait for Firebase credentials to sync.",
            needsPassword = true
        )
    }

    private fun ensureCore() {
        if (!onEngineThread()) {
            engineScope.launch { ensureCore() }
            return
        }
        if (core != null && coreStarted) return
        if (core != null && !coreStarted) {
            try {
                core?.stop()
            } catch (_: Exception) {
            }
            core = null
        }
        createAndStartCore()
    }

    private fun createAndStartCore() {
        try {
            val factory = Factory.instance()

            val loggingService = factory.loggingService
            loggingService?.domain = "Linphone"
            loggingService?.setLogLevel(LogLevel.Warning)

            val created = factory.createCore(null, null, context) ?: return
            created.setUserAgent("LinphoneAndroid", "5.3.0")
            try {
                created.isAutoIterateEnabled = false
            } catch (e: Exception) {
                Log.d(tag, "setAutoIterateEnabled: ${e.message}")
            }

            val config = created.config
            config?.setInt("rtp", "rtcp_xr_enabled", 0)
            config?.setInt("rtp", "rtcp_fb_implicit_rtcp_sync", 0)
            config?.setInt("sip", "use_rtcp", 0)
            config?.setInt("sip", "use_avpf", 0)
            config?.setInt("sip", "use_rfc5626", 0)
            config?.setInt("sip", "ping_with_options", 0)
            config?.setInt("net", "dns_srv_enabled", 0)
            config?.setInt("net", "ipv6_enabled", 0)
            config?.setInt("sip", "prefer_ipv6", 0)
            config?.setInt("video", "capture", 0)
            config?.setInt("video", "display", 0)
            config?.setInt("video", "enabled", 0)

            val vp = created.videoActivationPolicy
            vp?.setAutomaticallyAccept(false)
            vp?.setAutomaticallyInitiate(false)

            created.setVideoCaptureEnabled(false)
            created.setVideoDisplayEnabled(false)
            created.setIpv6Enabled(false)
            try {
                created.setDnsSrvEnabled(false)
            } catch (e: Exception) {
                Log.d(tag, "setDnsSrvEnabled: ${e.message}")
            }

            // UDP only. Default Core transports include TCP/TLS; a 401 retry on TLS
            // is a common sip.sipup.org 503 / "io error" because :5061 is closed.
            val transports = created.transports
            if (transports != null) {
                transports.udpPort = -1
                transports.tcpPort = 0
                transports.tlsPort = 0
                created.transports = transports
            }

            val natPolicy = created.createNatPolicy()
            if (natPolicy != null) {
                natPolicy.setIceEnabled(false)
                natPolicy.setStunEnabled(false)
                natPolicy.setTurnEnabled(false)
                try {
                    natPolicy.setUpnpEnabled(false)
                } catch (_: Exception) {
                }
                created.setNatPolicy(natPolicy)
            }

            created.audioPayloadTypes?.forEach { pt ->
                val mime = pt.mimeType.uppercase()
                if (mime == "PCMU" || mime == "PCMA" || mime == "OPUS" || mime.contains("TELEPHONE-EVENT")) {
                    pt.enable(true)
                } else {
                    pt.enable(false)
                }
            }

            created.setMediaEncryption(MediaEncryption.SRTP)
            created.setMediaEncryptionMandatory(false)
            created.addListener(coreListener)
            created.start()
            created.setIpv6Enabled(false)
            created.setNetworkReachable(true)

            core = created
            coreStarted = true
            if (iterateJob == null) {
                iterateJob = engineScope.launch {
                    while (isActive) {
                        val c = core
                        if (c != null && coreStarted) {
                            try {
                                c.iterate()
                            } catch (e: Exception) {
                                Log.e(tag, "Core.iterate failed", e)
                            }
                        }
                        delay(20)
                    }
                }
            }
            Log.i(tag, "Linphone Core started (autoIterate=off, ipv6=off, udp-only)")
        } catch (e: Exception) {
            coreStarted = false
            core = null
            Log.e(tag, "Failed to initialize Linphone Core", e)
        }
    }

    private fun applyAccount(sipConfig: SipConfig, enableRegister: Boolean, recreate: Boolean = false) {
        val c = core ?: return
        val username = sipConfig.username.trim()
        val host = registrarHost(sipConfig.host)
        val embeddedPort = registrarPort(sipConfig.host)
        val port = when {
            sipConfig.port > 0 -> sipConfig.port
            embeddedPort > 0 -> embeddedPort
            else -> 5060
        }
        val identityUri = "sip:$username@$host"
        // Do not put ;transport=udp in the URI. belle-sip createAddress() often
        // returns a null C++ object for that form; setServerAddress then SIGSEGVs.
        val serverUri = "sip:$host:$port"

        val existing = c.defaultAccount
        val existingIdentity = existing?.params?.identityAddress?.asStringUriOnly().orEmpty()
        val existingServer = existing?.params?.serverAddress?.asString() ?: ""
        val canReuse = existing != null &&
            existingIdentity == identityUri &&
            existingServer.contains(host) &&
            !recreate

        if (canReuse) {
            installAuthInfo(c, sipConfig)
            val cloned = existing.params?.clone()
            if (cloned != null) {
                cloned.setRegisterEnabled(enableRegister)
                cloned.setExpires(300)
                existing.params = cloned
            }
            if (enableRegister) {
                _registrationState.value = _registrationState.value.copy(
                    status = RegistrationStatus.REGISTERING,
                    username = username,
                    host = host,
                    port = port,
                    statusMessage = "Registering with $host...",
                    lastError = null,
                    needsPassword = false
                )
                c.refreshRegisters()
            }
            Log.i(tag, "Reusing Linphone account $identityUri -> $serverUri")
            return
        }

        replacingAccount = true
        try {
            c.clearAccounts()
            c.clearProxyConfig()
            c.clearAllAuthInfo()

            // AuthInfo must exist before the Account is created/enabled so the first
            // 401/407 can be answered. Wildcard + domain-scoped entries cover realm mismatch.
            installAuthInfo(c, sipConfig)

            val accountParams = c.createAccountParams() ?: return
            // Identity must not carry ;transport= — that leaks into Contact and breaks some trunks.
            val identity = createSipAddress(identityUri)
            if (identity == null) {
                Log.e(tag, "Invalid SIP identity $identityUri")
                replacingAccount = false
                _registrationState.value = SipRegistrationState(
                    status = RegistrationStatus.FAILED,
                    username = username,
                    host = host,
                    port = port,
                    lastError = "Invalid SIP identity $identityUri"
                )
                return
            }
            accountParams.setIdentityAddress(identity)

            // Pin UDP on the Address object, not in the URI string.
            val serverAddr = createSipAddress(serverUri) ?: createSipAddress("sip:$host")
            if (serverAddr == null) {
                Log.e(tag, "Invalid SIP server $serverUri")
                replacingAccount = false
                _registrationState.value = SipRegistrationState(
                    status = RegistrationStatus.FAILED,
                    username = username,
                    host = host,
                    port = port,
                    lastError = "Invalid SIP server $serverUri"
                )
                return
            }
            serverAddr.transport = TransportType.Udp
            accountParams.setServerAddress(serverAddr)
            accountParams.setOutboundProxyEnabled(true)
            accountParams.setRegisterEnabled(enableRegister)
            accountParams.setExpires(300)

            accountParams.setAvpfMode(AVPFMode.Disabled)
            accountParams.setRtpBundleEnabled(false)
            accountParams.setQualityReportingEnabled(false)
            accountParams.setPushNotificationAllowed(false)
            accountParams.setRemotePushNotificationAllowed(false)
            try {
                accountParams.setPublishEnabled(false)
            } catch (_: Exception) {
            }

            val natPolicy = c.createNatPolicy()
            if (natPolicy != null) {
                natPolicy.setIceEnabled(false)
                natPolicy.setStunEnabled(false)
                natPolicy.setTurnEnabled(false)
                accountParams.setNatPolicy(natPolicy)
            }

            val account = c.createAccount(accountParams)
            if (account == null) {
                Log.e(tag, "createAccount returned null for $identityUri")
                return
            }
            c.addAccount(account)
            c.defaultAccount = account
            Log.i(tag, "Linphone REGISTER identity=$identityUri server=$serverUri transport=udp expires=300 outboundProxy=true")
        } finally {
            replacingAccount = false
        }

        if (enableRegister) {
            _registrationState.value = _registrationState.value.copy(
                status = RegistrationStatus.REGISTERING,
                username = username,
                host = host,
                port = port,
                statusMessage = "Registering with $host...",
                lastError = null,
                needsPassword = false
            )
        }
    }

    private fun scheduleRetry(sipConfig: SipConfig, statusCode: Int, retryAfter: Int) {
        retryJob?.cancel()
        val backoffSec = when {
            retryAfter > 0 -> retryAfter.coerceIn(5, 120).toLong()
            statusCode == 503 -> (15L * (1 shl failCount.coerceAtMost(3))).coerceAtMost(120L)
            else -> when (failCount) {
                1 -> 8L
                2 -> 16L
                else -> 32L
            }
        }
        nextRetryAtMs = System.currentTimeMillis() + backoffSec * 1000L
        Log.i(tag, "Scheduling REGISTER retry in ${backoffSec}s (status=$statusCode failCount=$failCount)")
        retryJob = engineScope.launch {
            delay(backoffSec * 1000L)
            if (currentSipConfig?.registrationFingerprint() == sipConfig.registrationFingerprint()) {
                refreshNow(force = true)
            }
        }
    }

    private fun installAuthInfo(core: Core, sipConfig: SipConfig) {
        val factory = Factory.instance()
        val username = sipConfig.username.trim()
        val password = sipConfig.password
        val domain = sipConfig.host.trim()
        val deviceId = sipConfig.deviceId.trim()

        // userid defaults to the SIP username. Wildcard realm/domain lets Linphone
        // answer any WWW-Authenticate challenge (401/407) without a realm mismatch.
        core.addAuthInfo(factory.createAuthInfo(username, username, password, null, null, null))
        core.addAuthInfo(factory.createAuthInfo(username, username, password, null, domain, domain))

        if (deviceId.isNotBlank() && deviceId != username) {
            core.addAuthInfo(factory.createAuthInfo(username, deviceId, password, null, null, null))
            core.addAuthInfo(factory.createAuthInfo(deviceId, deviceId, password, null, domain, domain))
        }
    }

    private fun fillAuthInfo(authInfo: AuthInfo, sipConfig: SipConfig) {
        val username = sipConfig.username.trim()
        if (authInfo.username.isNullOrBlank()) {
            authInfo.username = username
        }
        if (authInfo.userid.isNullOrBlank()) {
            authInfo.userid = username
        }
        authInfo.password = sipConfig.password
        if (authInfo.domain.isNullOrBlank()) {
            authInfo.domain = sipConfig.host.trim()
        }
    }

    private fun registrarHost(raw: String): String {
        var host = raw.trim()
            .removePrefix("sip:")
            .removePrefix("sips:")
            .substringBefore("/")
            .substringBefore(";")
            .substringAfter("@")
            .trim()
        // Firebase sip.host is sometimes "1.2.3.4:5060". A second :port
        // ("sip:1.2.3.4:5060:5060;transport=udp") makes createAddress return
        // a null C++ object and setServerAddress SIGSEGVs.
        val afterColon = host.substringAfterLast(':', "")
        if (afterColon.isNotEmpty() && afterColon.all { it.isDigit() }) {
            host = host.substringBeforeLast(':')
        }
        return host
    }

    private fun registrarPort(raw: String): Int {
        val host = raw.trim()
            .removePrefix("sip:")
            .removePrefix("sips:")
            .substringBefore("/")
            .substringBefore(";")
            .substringAfter("@")
            .trim()
        val afterColon = host.substringAfterLast(':', "")
        return afterColon.toIntOrNull() ?: 0
    }

    private fun createSipAddress(uri: String): org.linphone.core.Address? {
        return try {
            core?.createAddress(uri) ?: Factory.instance().createAddress(uri)
        } catch (e: Exception) {
            Log.e(tag, "createAddress failed for $uri: ${e.message}")
            null
        }
    }

    private fun onEngineThread(): Boolean {
        return Looper.myLooper() == Looper.getMainLooper()
    }
}
