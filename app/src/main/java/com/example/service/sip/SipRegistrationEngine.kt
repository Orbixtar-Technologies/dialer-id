package com.example.service.sip

import android.content.Context
import android.util.Log
import com.example.data.model.SipConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import com.example.util.SipDigestAuth
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Isolated custom UDP REGISTER client. Not the source of truth for "Registered"
 * or placeCall() gating — Linphone [SipEngine] owns that. Kept for digest tests
 * and diagnostics only.
 *
 * Core Features:
 * - RFC 3261 REGISTER transaction lifecycle & MD5 Digest Authentication (RFC 2617 / RFC 2069).
 * - Automatic parsing of 200 OK responses with server-granted Expiry extraction (Contact expires / Expires header).
 * - Proactive lease renewal before expiry (adaptive refresh at 85% of granted duration).
 * - SIP NAT Pinhole maintenance: Periodic keep-alive ping loop (CRLF / short OPTIONS) every 25 seconds.
 * - Dynamic network adaptation & exponential backoff on transient connection failures.
 * - Graceful unregistration (Expires: 0) on account logout or teardown.
 */
class SipRegistrationEngine(
    private val context: Context
) {
    private val tag = "SipRegistrationEngine"
    private val engineScope = CoroutineScope(Dispatchers.IO + Job())

    private val _registrationState = MutableStateFlow(SipRegistrationState())
    val registrationState: StateFlow<SipRegistrationState> = _registrationState.asStateFlow()

    private var sipSocket: DatagramSocket? = null
    private val isRunning = AtomicBoolean(false)
    private val isRegistering = AtomicBoolean(false)

    private var currentConfig: SipConfig? = null
    private var currentCallId = ""
    private var currentFromTag = ""
    private var cSeq = 100
    private var localIpAddress = "127.0.0.1"
    private var localSipPort = 5060

    private var keepAliveJob: Job? = null
    private var expiryTickerJob: Job? = null
    private var refreshScheduleJob: Job? = null

    // Digest caching
    private var cachedRealm: String = ""
    private var cachedNonce: String = ""
    private var cachedQop: String = ""
    private var cachedOpaque: String = ""
    private var nonceCount = 0

    companion object {
        const val DEFAULT_REQUESTED_EXPIRES = 300 // 5 minutes
        const val KEEP_ALIVE_INTERVAL_MS = 25_000L // 25 seconds for NAT pinhole
    }

    /**
     * Resolves the best local routable IPv4 address on the active network interface.
     */
    fun getLocalIp(targetHost: String? = null): String {
        try {
            if (!targetHost.isNullOrBlank()) {
                val targetAddr = InetAddress.getByName(targetHost)
                DatagramSocket().use { tempSocket ->
                    tempSocket.connect(targetAddr, 5060)
                    val local = tempSocket.localAddress
                    if (local is Inet4Address && !local.isLoopbackAddress && !local.isAnyLocalAddress) {
                        return local.hostAddress ?: "127.0.0.1"
                    }
                }
            }
        } catch (e: Exception) {
            Log.d(tag, "Local IP socket route lookup: ${e.message}")
        }

        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (iface.isLoopback || !iface.isUp) continue

                val addresses = iface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (addr is Inet4Address && !addr.isLoopbackAddress && !addr.isLinkLocalAddress) {
                        val hostAddr = addr.hostAddress
                        if (!hostAddr.isNullOrBlank() && hostAddr != "0.0.0.0") {
                            return hostAddr
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(tag, "Network interface scan notice: ${e.message}")
        }

        return "127.0.0.1"
    }

    /**
     * Initiates SIP registration with the target SIP server.
     */
    fun register(sipConfig: SipConfig, requestedExpires: Int = DEFAULT_REQUESTED_EXPIRES) {
        if (!sipConfig.hasUsableCredentials()) {
            _registrationState.value = _registrationState.value.copy(
                status = RegistrationStatus.FAILED,
                statusCode = 0,
                statusMessage = "SIP credentials not configured",
                lastError = "Host, username, and password are required"
            )
            return
        }
        currentConfig = sipConfig
        val host = sipConfig.host.trim()
        val port = if (sipConfig.port > 0) sipConfig.port else 5060
        val username = sipConfig.username.trim()

        isRunning.set(true)
        isRegistering.set(true)

        _registrationState.value = _registrationState.value.copy(
            status = RegistrationStatus.REGISTERING,
            username = username,
            host = host,
            port = port,
            expiresSeconds = requestedExpires,
            statusMessage = "Connecting to $host:$port ($username)...",
            lastError = null
        )

        engineScope.launch {
            try {
                localIpAddress = withContext(Dispatchers.IO) { getLocalIp(host) }
                if (currentCallId.isBlank()) {
                    currentCallId = UUID.randomUUID().toString().replace("-", "") + "@" + localIpAddress
                }
                if (currentFromTag.isBlank()) {
                    currentFromTag = "reg_" + (10000..99999).random()
                }

                initSocketIfNeeded()

                val targetServer = withContext(Dispatchers.IO) {
                    try {
                        InetAddress.getByName(host)
                    } catch (e: Exception) {
                        Log.w(tag, "DNS resolution fallback: ${e.message}")
                        null
                    }
                }

                executeRegisterTransaction(
                    host = host,
                    port = port,
                    targetServer = targetServer,
                    username = username,
                    password = sipConfig.password,
                    requestedExpires = requestedExpires
                )

            } catch (e: Exception) {
                Log.e(tag, "SIP Registration exception: ${e.message}", e)
                handleRegistrationFailure(500, "Registration Error: ${e.message}")
            } finally {
                isRegistering.set(false)
            }
        }
    }

    /**
     * Executes the RFC 3261 REGISTER transaction: sends initial REGISTER,
     * handles 401/407 challenges with Digest authentication, and processes 200 OK.
     */
    private suspend fun executeRegisterTransaction(
        host: String,
        port: Int,
        targetServer: InetAddress?,
        username: String,
        password: String,
        requestedExpires: Int
    ) {
        val startTime = System.currentTimeMillis()
        val socket = sipSocket ?: return

        // 1. If we have cached digest credentials, send authenticated REGISTER directly, otherwise send initial
        cSeq++
        val authHeader = if (cachedRealm.isNotBlank() && cachedNonce.isNotBlank()) {
            nonceCount++
            val nc = String.format("%08x", nonceCount)
            val cnonce = UUID.randomUUID().toString().take(8)
            val uri = "sip:$host"
            val responseHash = SipDigestAuth.computeResponse(
                user = username,
                realm = cachedRealm,
                pass = password,
                method = "REGISTER",
                uri = uri,
                nonce = cachedNonce,
                qop = cachedQop,
                cnonce = cnonce,
                nc = nc
            )
            buildDigestHeader(
                isProxy = false,
                username = username,
                realm = cachedRealm,
                nonce = cachedNonce,
                uri = uri,
                response = responseHash,
                qop = cachedQop,
                cnonce = cnonce,
                nc = nc,
                opaque = cachedOpaque
            )
        } else null

        val registerMsg = buildSipRegister(
            host = host,
            username = username,
            callId = currentCallId,
            fromTag = currentFromTag,
            cSeq = cSeq,
            expires = requestedExpires,
            authHeader = authHeader
        )

        if (targetServer != null) {
            sendSipMessage(registerMsg, targetServer, port)
        }

        // 2. Listen for response
        val buffer = ByteArray(4096)
        val packet = DatagramPacket(buffer, buffer.size)
        var transactionComplete = false
        var challengeAttempts = 0

        while (isRunning.get() && !transactionComplete && challengeAttempts < 2) {
            try {
                socket.soTimeout = 4500
                socket.receive(packet)
                val responseStr = String(packet.data, 0, packet.length)
                Log.d(tag, "SIP Register Response:\n$responseStr")

                val statusLine = responseStr.lines().firstOrNull() ?: ""
                val statusCode = statusLine.split(" ").getOrNull(1)?.toIntOrNull() ?: 0
                val elapsed = System.currentTimeMillis() - startTime

                when (statusCode) {
                    100, 180, 183 -> {
                        _registrationState.value = _registrationState.value.copy(
                            statusMessage = "SIP $statusCode Trying..."
                        )
                    }
                    401, 407 -> {
                        challengeAttempts++
                        val authHeaderKey = if (statusCode == 401) "WWW-Authenticate:" else "Proxy-Authenticate:"
                        val challenge = extractHeaderValue(responseStr, authHeaderKey)
                        cachedRealm = extractParam(challenge, "realm")
                        cachedNonce = extractParam(challenge, "nonce")
                        cachedQop = extractParam(challenge, "qop")
                        cachedOpaque = extractParam(challenge, "opaque")
                        nonceCount = 1

                        val nc = "00000001"
                        val cnonce = UUID.randomUUID().toString().take(8)
                        val uri = "sip:$host"

                        val responseHash = SipDigestAuth.computeResponse(
                            user = username,
                            realm = cachedRealm,
                            pass = password,
                            method = "REGISTER",
                            uri = uri,
                            nonce = cachedNonce,
                            qop = cachedQop,
                            cnonce = cnonce,
                            nc = nc
                        )

                        val digestHeader = buildDigestHeader(
                            isProxy = (statusCode == 407),
                            username = username,
                            realm = cachedRealm,
                            nonce = cachedNonce,
                            uri = uri,
                            response = responseHash,
                            qop = cachedQop,
                            cnonce = cnonce,
                            nc = nc,
                            opaque = cachedOpaque
                        )

                        cSeq++
                        val authRegisterMsg = buildSipRegister(
                            host = host,
                            username = username,
                            callId = currentCallId,
                            fromTag = currentFromTag,
                            cSeq = cSeq,
                            expires = requestedExpires,
                            authHeader = digestHeader
                        )

                        if (targetServer != null) {
                            sendSipMessage(authRegisterMsg, targetServer, port)
                        }
                    }
                    200 -> {
                        transactionComplete = true
                        handleRegistrationSuccess(responseStr, host, port, username, requestedExpires, elapsed)
                        return
                    }
                    403 -> {
                        transactionComplete = true
                        handleRegistrationFailure(403, "Forbidden: Invalid SIP trunk credentials or unauthorized host")
                        return
                    }
                    else -> {
                        if (statusCode >= 400) {
                            transactionComplete = true
                            handleRegistrationFailure(statusCode, "SIP Error $statusCode: ${statusLine.substringAfter(statusCode.toString()).trim()}")
                            return
                        }
                    }
                }

            } catch (e: java.net.SocketTimeoutException) {
                Log.w(tag, "SIP REGISTER timed out; treating as failure")
                transactionComplete = true
                handleRegistrationFailure(408, "REGISTER timed out")
                return
            }
        }
    }

    /**
     * Processes 200 OK response:
     * - Parses granted `Expires` parameter from `Contact` or `Expires` header.
     * - Records timestamps.
     * - Schedules re-registration before expiry.
     * - Starts NAT keep-alive loop.
     */
    private fun handleRegistrationSuccess(
        responseStr: String,
        host: String,
        port: Int,
        username: String,
        requestedExpires: Int,
        elapsedMs: Long
    ) {
        val grantedExpires = parseExpires(responseStr, requestedExpires)
        val serverBanner = extractHeaderValue(responseStr, "Server:")
        val now = System.currentTimeMillis()
        val expiresAt = now + (grantedExpires * 1000L)
        // Refresh proactively at 85% of granted lease (or 20s before expiry)
        val refreshLeadTime = (grantedExpires * 0.15).coerceIn(10.0, 45.0).toLong()
        val refreshIntervalSec = (grantedExpires - refreshLeadTime).coerceAtLeast(15)
        val nextRefreshAt = now + (refreshIntervalSec * 1000L)

        Log.i(tag, "SIP Registration SUCCESS (200 OK): username=$username, expires=${grantedExpires}s, server=$serverBanner, nextRefresh in ${refreshIntervalSec}s")

        _registrationState.value = _registrationState.value.copy(
            status = RegistrationStatus.REGISTERED,
            username = username,
            host = host,
            port = port,
            localIp = localIpAddress,
            localPort = localSipPort,
            expiresSeconds = grantedExpires,
            registeredAt = now,
            expiresAt = expiresAt,
            nextRefreshAt = nextRefreshAt,
            secondsRemaining = grantedExpires,
            serverBanner = serverBanner,
            statusCode = 200,
            statusMessage = "Registered with $host (Lease: ${grantedExpires}s)",
            lastError = null,
            roundTripLatencyMs = elapsedMs,
            retryCount = 0,
            isKeepAliveActive = true
        )

        // Start Keep-Alive Pinhole Loop
        startKeepAliveLoop(host, port)

        // Start Countdown Ticker
        startExpiryTicker(expiresAt)

        // Schedule Automatic Re-Registration before lease expiration
        scheduleRefresh(refreshIntervalSec)
    }

    private fun handleRegistrationFailure(statusCode: Int, errorMessage: String) {
        val currentRetry = _registrationState.value.retryCount + 1
        Log.w(tag, "SIP Registration FAILED ($statusCode): $errorMessage (retry=$currentRetry)")

        _registrationState.value = _registrationState.value.copy(
            status = if (statusCode == 403) RegistrationStatus.FAILED else RegistrationStatus.FAILED,
            statusCode = statusCode,
            statusMessage = errorMessage,
            lastError = errorMessage,
            retryCount = currentRetry,
            isKeepAliveActive = false
        )

        // Stop keep-alive and ticker
        stopKeepAliveLoop()
        stopExpiryTicker()

        // Auto-retry with backoff even on 403 Forbidden (in case credentials/network fix themselves)
        if (isRunning.get()) {
            val backoffSec = when (currentRetry) {
                1 -> 5L
                2 -> 10L
                3 -> 20L
                else -> 30L
            }
            engineScope.launch {
                Log.d(tag, "Scheduling SIP registration retry in ${backoffSec}s...")
                delay(backoffSec * 1000L)
                if (isRunning.get()) {
                    currentConfig?.let { register(it, DEFAULT_REQUESTED_EXPIRES) }
                }
            }
        }
    }

    /**
     * Starts the periodic NAT pinhole keep-alive loop.
     * Sends periodic double-CRLF ("\r\n\r\n") or short SIP ping packets every 25 seconds.
     */
    private fun startKeepAliveLoop(host: String, port: Int) {
        keepAliveJob?.cancel()
        keepAliveJob = engineScope.launch {
            val targetAddr = try {
                InetAddress.getByName(host)
            } catch (e: Exception) {
                null
            }

            while (isActive && isRunning.get() && _registrationState.value.status == RegistrationStatus.REGISTERED) {
                delay(KEEP_ALIVE_INTERVAL_MS)
                if (!isActive || !isRunning.get()) break

                try {
                    val socket = sipSocket
                    if (socket != null && !socket.isClosed && targetAddr != null) {
                        // Double CRLF keep-alive ping (RFC 5626)
                        val pingBytes = "\r\n\r\n".toByteArray(Charsets.UTF_8)
                        val packet = DatagramPacket(pingBytes, pingBytes.size, targetAddr, port)
                        socket.send(packet)

                        val now = System.currentTimeMillis()
                        val currentPings = _registrationState.value.keepAlivePingsSent + 1
                        _registrationState.value = _registrationState.value.copy(
                            lastKeepAliveAt = now,
                            keepAlivePingsSent = currentPings,
                            isKeepAliveActive = true
                        )
                        Log.d(tag, "Sent SIP NAT Pinhole Keep-Alive ping #$currentPings to $host:$port")
                    }
                } catch (e: Exception) {
                    Log.d(tag, "Keep-alive ping notice: ${e.message}")
                }
            }
        }
    }

    private fun stopKeepAliveLoop() {
        keepAliveJob?.cancel()
        keepAliveJob = null
    }

    /**
     * Starts a 1-second interval ticker to update `secondsRemaining` in the state flow.
     */
    private fun startExpiryTicker(expiresAt: Long) {
        expiryTickerJob?.cancel()
        expiryTickerJob = engineScope.launch {
            while (isActive && isRunning.get() && _registrationState.value.status == RegistrationStatus.REGISTERED) {
                delay(1000)
                val remainingSec = ((expiresAt - System.currentTimeMillis()) / 1000).toInt().coerceAtLeast(0)
                _registrationState.value = _registrationState.value.copy(
                    secondsRemaining = remainingSec
                )
                if (remainingSec <= 0) {
                    _registrationState.value = _registrationState.value.copy(
                        status = RegistrationStatus.EXPIRED,
                        statusMessage = "Registration lease expired, re-authenticating..."
                    )
                    break
                }
            }
        }
    }

    private fun stopExpiryTicker() {
        expiryTickerJob?.cancel()
        expiryTickerJob = null
    }

    /**
     * Schedules the automatic lease refresh before expiration.
     */
    private fun scheduleRefresh(intervalSec: Long) {
        refreshScheduleJob?.cancel()
        refreshScheduleJob = engineScope.launch {
            delay(intervalSec * 1000L)
            if (isActive && isRunning.get()) {
                Log.i(tag, "SIP Lease refresh threshold reached (${intervalSec}s elapsed). Triggering proactive re-registration...")
                currentConfig?.let {
                    register(it, _registrationState.value.expiresSeconds)
                }
            }
        }
    }

    /**
     * Manually triggers an immediate registration refresh.
     */
    /** Test/diagnostic hook: REGISTER timeout is always a failure, never a synthesized 200 OK. */
    fun markRegisterTimeout() {
        handleRegistrationFailure(408, "REGISTER timed out")
    }

    fun refreshNow() {
        currentConfig?.let {
            register(it, _registrationState.value.expiresSeconds.coerceAtLeast(DEFAULT_REQUESTED_EXPIRES))
        }
    }

    /**
     * Unregisters the account from the SIP server (RFC 3261: REGISTER with Expires: 0).
     */
    fun unregister() {
        if (!isRunning.getAndSet(false)) return

        stopKeepAliveLoop()
        stopExpiryTicker()
        refreshScheduleJob?.cancel()

        _registrationState.value = _registrationState.value.copy(
            status = RegistrationStatus.UNREGISTERING,
            statusMessage = "Unregistering SIP Trunk...",
            isKeepAliveActive = false
        )

        engineScope.launch {
            try {
                val config = currentConfig
                if (config != null) {
                    val host = config.host
                    val port = if (config.port > 0) config.port else 5060
                    val username = config.username
                    val targetServer = try { InetAddress.getByName(host) } catch (e: Exception) { null }

                    cSeq++
                    val unregMsg = buildSipRegister(
                        host = host,
                        username = username,
                        callId = currentCallId,
                        fromTag = currentFromTag,
                        cSeq = cSeq,
                        expires = 0,
                        authHeader = null
                    )

                    if (targetServer != null && sipSocket != null) {
                        sendSipMessage(unregMsg, targetServer, port)
                    }
                }
            } catch (e: Exception) {
                Log.d(tag, "Unregister send error: ${e.message}")
            } finally {
                cleanUpSocket()
                _registrationState.value = _registrationState.value.copy(
                    status = RegistrationStatus.UNREGISTERED,
                    statusMessage = "Unregistered",
                    secondsRemaining = 0,
                    isKeepAliveActive = false
                )
            }
        }
    }

    fun destroy() {
        unregister()
    }

    private fun initSocketIfNeeded() {
        if (sipSocket == null || sipSocket!!.isClosed) {
            sipSocket = DatagramSocket().apply {
                soTimeout = 4000
            }
            localSipPort = sipSocket?.localPort ?: 5060
        }
    }

    private fun cleanUpSocket() {
        try {
            sipSocket?.close()
        } catch (e: Exception) {}
        sipSocket = null
    }

    // --- SIP Message Builders & Parsers ---

    private fun buildSipRegister(
        host: String,
        username: String,
        callId: String,
        fromTag: String,
        cSeq: Int,
        expires: Int,
        authHeader: String?
    ): String {
        val branch = "z9hG4bK" + UUID.randomUUID().toString().take(12)
        val sb = StringBuilder()
        sb.append("REGISTER sip:$host SIP/2.0\r\n")
        sb.append("Via: SIP/2.0/UDP $localIpAddress:$localSipPort;branch=$branch;rport\r\n")
        sb.append("Max-Forwards: 70\r\n")
        sb.append("From: <sip:$username@$host>;tag=$fromTag\r\n")
        sb.append("To: <sip:$username@$host>\r\n")
        sb.append("Call-ID: $callId\r\n")
        sb.append("CSeq: $cSeq REGISTER\r\n")
        sb.append("Contact: <sip:$username@$localIpAddress:$localSipPort;transport=udp>;expires=$expires\r\n")
        sb.append("Expires: $expires\r\n")
        sb.append("User-Agent: DialerID-SIP/1.0 (Android Telecom)\r\n")
        sb.append("Allow: INVITE, ACK, CANCEL, OPTIONS, BYE, REFER, NOTIFY, MESSAGE, SUBSCRIBE, INFO\r\n")
        if (!authHeader.isNullOrBlank()) {
            sb.append("$authHeader\r\n")
        }
        sb.append("Content-Length: 0\r\n\r\n")
        return sb.toString()
    }

    private fun parseExpires(response: String, fallback: Int): Int {
        try {
            // Check Contact header parameter: Contact: <sip:...>;expires=120
            val contactHeader = extractHeaderValue(response, "Contact:")
            if (contactHeader.contains("expires=")) {
                val match = Regex("""expires=(\d+)""").find(contactHeader)
                if (match != null) {
                    val exp = match.groupValues[1].toIntOrNull()
                    if (exp != null && exp > 0) return exp
                }
            }
            // Check Expires header
            val expiresHeader = extractHeaderValue(response, "Expires:")
            val exp = expiresHeader.trim().toIntOrNull()
            if (exp != null && exp > 0) return exp
        } catch (e: Exception) {
            Log.d(tag, "parseExpires error: ${e.message}")
        }
        return fallback
    }

    private fun sendSipMessage(message: String, target: InetAddress, port: Int) {
        try {
            val bytes = message.toByteArray(Charsets.UTF_8)
            val packet = DatagramPacket(bytes, bytes.size, target, port)
            sipSocket?.send(packet)
            Log.d(tag, "Sent SIP REGISTER packet to $target:$port (${bytes.size} bytes)")
        } catch (e: Exception) {
            Log.w(tag, "sendSipMessage error: ${e.message}")
        }
    }

    // --- Digest Authentication (RFC 2617 / RFC 2069) ---

    private fun buildDigestHeader(
        isProxy: Boolean,
        username: String,
        realm: String,
        nonce: String,
        uri: String,
        response: String,
        qop: String,
        cnonce: String,
        nc: String,
        opaque: String
    ): String {
        val prefix = if (isProxy) "Proxy-Authorization: Digest " else "Authorization: Digest "
        val sb = StringBuilder(prefix)
        sb.append("username=\"$username\", ")
        sb.append("realm=\"$realm\", ")
        sb.append("nonce=\"$nonce\", ")
        sb.append("uri=\"$uri\", ")
        sb.append("response=\"$response\", ")
        sb.append("algorithm=MD5")

        if (qop.contains("auth", ignoreCase = true)) {
            sb.append(", qop=auth, nc=$nc, cnonce=\"$cnonce\"")
        }
        if (opaque.isNotBlank()) {
            sb.append(", opaque=\"$opaque\"")
        }
        return sb.toString()
    }

    private fun extractHeaderValue(message: String, headerPrefix: String): String {
        return message.lines()
            .firstOrNull { it.startsWith(headerPrefix, ignoreCase = true) }
            ?.substringAfter(":")
            ?.trim() ?: ""
    }

    private fun extractParam(headerValue: String, paramName: String): String {
        val regex = Regex("""$paramName="?([^",\r\n]+)"?""", RegexOption.IGNORE_CASE)
        val match = regex.find(headerValue)
        return match?.groupValues?.getOrNull(1)?.trim() ?: ""
    }
}
