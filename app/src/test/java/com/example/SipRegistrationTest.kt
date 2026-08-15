package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.CallerIdItem
import com.example.data.model.SipConfig
import com.example.service.sip.RegistrationStatus
import com.example.service.sip.SipRegistrationEngine
import com.example.service.sip.SipRegistrationState
import com.example.util.PhoneNumberSanitizer
import com.example.util.SipDigestAuth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SipRegistrationTest {

    private lateinit var context: Context
    private lateinit var engine: SipRegistrationEngine

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        engine = SipRegistrationEngine(context)
    }

    @Test
    fun testInitialRegistrationState() {
        val state = engine.registrationState.value
        assertEquals(RegistrationStatus.UNREGISTERED, state.status)
        assertFalse(state.isRegistered)
        assertEquals(0, state.keepAlivePingsSent)
    }

    @Test
    fun digestAuthUsesRfc2617Md5Vector() {
        val user = "Mufasa"
        val realm = "testrealm@host.com"
        val pass = "Circle Of Life"
        val method = "GET"
        val uri = "/dir/index.html"
        val nonce = "dcd98b7102dd2f0e8b11d0f600bfb0c093"
        val ha1 = SipDigestAuth.md5Hex("$user:$realm:$pass")
        val ha2 = SipDigestAuth.md5Hex("$method:$uri")
        val expected = SipDigestAuth.md5Hex("$ha1:$nonce:$ha2")
        val response = SipDigestAuth.computeResponse(
            user = user,
            realm = realm,
            pass = pass,
            method = method,
            uri = uri,
            nonce = nonce
        )
        assertEquals(32, ha1.length)
        assertEquals(expected, response)
        assertFalse(response.contains(pass))
    }

    @Test
    fun digestAuthWithQopAuth() {
        val response = SipDigestAuth.computeResponse(
            user = "alice",
            realm = "atlanta.com",
            pass = "secret-pass",
            method = "REGISTER",
            uri = "sip:example.test",
            nonce = "abc123",
            qop = "auth",
            cnonce = "0a4f113b",
            nc = "00000001"
        )
        val ha1 = SipDigestAuth.md5Hex("alice:atlanta.com:secret-pass")
        val ha2 = SipDigestAuth.md5Hex("REGISTER:sip:example.test")
        assertEquals(SipDigestAuth.md5Hex("$ha1:abc123:00000001:0a4f113b:auth:$ha2"), response)
        assertFalse(response.contains("secret-pass"))
    }

    @Test
    fun registerTimeoutIsFailedNot200() {
        engine.markRegisterTimeout()
        val state = engine.registrationState.value
        assertEquals(RegistrationStatus.FAILED, state.status)
        assertEquals(408, state.statusCode)
        assertFalse(state.isRegistered)
        assertNotEquals(200, state.statusCode)
    }

    @Test
    fun doesNotRegisterWithBlankPassword() {
        engine.register(
            SipConfig(
                host = "sip.example.test",
                port = 5060,
                username = "operator",
                password = ""
            )
        )
        val state = engine.registrationState.value
        assertFalse(state.isRegistered)
        assertEquals(RegistrationStatus.FAILED, state.status)
        assertTrue(state.lastError?.contains("password", ignoreCase = true) == true)
    }

    @Test
    fun doesNotRegisterWithBlankUsername() {
        engine.register(
            SipConfig(
                host = "sip.example.test",
                port = 5060,
                username = "",
                password = "not-a-live-secret"
            )
        )
        assertFalse(engine.registrationState.value.isRegistered)
        assertEquals(RegistrationStatus.FAILED, engine.registrationState.value.status)
    }

    @Test
    fun sipConfigWithoutCredentialsIsNotUsable() {
        val empty = SipConfig()
        assertFalse(empty.hasUsableCredentials())
        assertTrue(
            SipConfig(host = "sip.example.test", username = "user", password = "local-only").hasUsableCredentials()
        )
    }

    @Test
    fun sipConfigNeedsPasswordWhenIdentityExistsWithoutSecret() {
        val migrated = SipConfig(host = "sip.example.test", username = "operator", password = "")
        assertTrue(migrated.hasIdentity())
        assertTrue(migrated.needsPassword())
        assertFalse(migrated.hasUsableCredentials())
    }

    @Test
    fun fromMapReadsSipPasswordFromRealtimeDatabase() {
        val profile = com.example.data.model.UserProfile.fromMap(
            mapOf(
                "sip" to mapOf(
                    "host" to "sip.example.test",
                    "port" to 5060,
                    "username" to "operator",
                    "password" to "rtdb-secret",
                    "deviceId" to "dev-1",
                    "callerId" to "+15551212"
                )
            ),
            uid = "firebase_uid_123"
        )
        val sip = profile.sipConfig
        assertTrue(sip != null && sip.hasUsableCredentials())
        assertEquals("sip.example.test", sip?.host)
        assertEquals("operator", sip?.username)
        assertEquals("rtdb-secret", sip?.password)
        assertEquals("dev-1", sip?.deviceId)
        assertEquals("+15551212", sip?.callerId)
    }

    @Test
    fun toRemoteMapWritesSipPasswordForExistingDataModel() {
        val remote = SipConfig(
            host = "sip.example.test",
            port = 5060,
            username = "operator",
            password = "rtdb-secret",
            deviceId = "dev-1",
            callerId = "+15551212"
        ).toRemoteMap()
        assertEquals("rtdb-secret", remote["password"])
        assertEquals("sip.example.test", remote["host"])
        assertEquals("operator", remote["username"])
        assertEquals("dev-1", remote["deviceId"])
    }

    @Test
    fun resolvedPasswordPrefersRealtimeDatabaseThenLocalCache() {
        val fromCloud = SipConfig(host = "sip.example.test", username = "operator", password = "cloud-pass")
            .withResolvedPassword("local-pass")
        assertEquals("cloud-pass", fromCloud.password)

        val fromLocal = SipConfig(host = "sip.example.test", username = "operator", password = "")
            .withResolvedPassword("local-pass")
        assertEquals("local-pass", fromLocal.password)
        assertTrue(fromLocal.hasUsableCredentials())

        val empty = SipConfig(host = "sip.example.test", username = "operator", password = "")
            .withResolvedPassword("")
        assertTrue(empty.needsPassword())
    }

    @Test
    fun authErrorMapperDoesNotLabelEveryFailureAsIoError() {
        val unauthorized = com.example.service.sip.SipAuthErrorMapper.map(401, "io error", "IOError")
        assertEquals(401, unauthorized.statusCode)
        assertTrue(unauthorized.message.contains("401"))
        assertFalse(unauthorized.message.equals("io error", ignoreCase = true))

        val forbidden = com.example.service.sip.SipAuthErrorMapper.map(403, "Forbidden", "Forbidden")
        assertEquals(403, forbidden.statusCode)
        assertTrue(forbidden.message.contains("403"))

        val timeout = com.example.service.sip.SipAuthErrorMapper.map(0, "Request timeout", "NoResponse")
        assertEquals(408, timeout.statusCode)

        val ioOnly = com.example.service.sip.SipAuthErrorMapper.map(0, "io error", "IOError")
        assertEquals(0, ioOnly.statusCode)
        assertTrue(ioOnly.message.contains("UDP", ignoreCase = true))
        assertFalse(ioOnly.message.equals("io error", ignoreCase = true))

        val unavailable = com.example.service.sip.SipAuthErrorMapper.map(
            503,
            "io error",
            "IOError",
            "Service Unavailable",
            "Retry-After: 30"
        )
        assertEquals(503, unavailable.statusCode)
        assertEquals(30, unavailable.retryAfterSeconds)
        assertTrue(unavailable.message.contains("503"))
        assertTrue(unavailable.message.contains("30"))
        assertFalse(unavailable.message.equals("io error", ignoreCase = true))
    }

    @Test
    fun destinationSanitizerAllowsDigitsAndPlusOnly() {
        assertEquals("+15551234567", PhoneNumberSanitizer.sanitizeDestination("+15551234567"))
        assertEquals("3200", PhoneNumberSanitizer.sanitizeDestination("3200"))
        assertNull(PhoneNumberSanitizer.sanitizeDestination("sip:evil@host"))
        assertNull(PhoneNumberSanitizer.sanitizeDestination("555-1212;transfer=yes"))
        assertNull(PhoneNumberSanitizer.sanitizeDestination(""))
        assertNull(PhoneNumberSanitizer.sanitizeDestination("++1555"))
        assertEquals("15551212", PhoneNumberSanitizer.sanitizeDestination("1 (555) 1212"))
        assertEquals("+15551212", PhoneNumberSanitizer.filterDialInput("tel:+1 (555) 1212"))
    }

    @Test
    fun callerIdIsNotAutoVerified() {
        val item = CallerIdItem(
            id = "cid_test",
            phoneNumber = "+15551234567",
            label = "Desk"
        )
        assertFalse(item.isVerified)
        assertFalse(PhoneNumberSanitizer.isValidCallerId("abc"))
        assertTrue(PhoneNumberSanitizer.isValidCallerId("+15551234567"))
    }

    @Test
    fun testRegistrationStateFormatting() {
        val registeredState = SipRegistrationState(
            status = RegistrationStatus.REGISTERED,
            host = "sip.example.test",
            port = 5060,
            username = "operator",
            expiresSeconds = 300,
            registeredAt = System.currentTimeMillis(),
            expiresAt = System.currentTimeMillis() + 300_000L,
            secondsRemaining = 299,
            isKeepAliveActive = true,
            serverBanner = "Asterisk PBX 18.20.0"
        )

        assertTrue(registeredState.isRegistered)
        assertEquals("Registered (200 OK)", registeredState.formattedStatus)
        assertTrue(registeredState.secondsRemaining in 290..300)
    }

    @Test
    fun testRegistrationFailureState() {
        val failedState = SipRegistrationState(
            status = RegistrationStatus.FAILED,
            statusCode = 403,
            lastError = "403 Forbidden - Invalid Credentials"
        )

        assertFalse(failedState.isRegistered)
        assertEquals("Registration Failed (403)", failedState.formattedStatus)
        assertEquals(403, failedState.statusCode)
    }

    @Test
    fun passwordRequiredStateIsExplicit() {
        val state = SipRegistrationState(
            status = RegistrationStatus.FAILED,
            host = "sip.example.test",
            username = "operator",
            needsPassword = true,
            statusMessage = "SIP password required"
        )
        assertFalse(state.isRegistered)
        assertEquals("SIP password required", state.formattedStatus)
        assertTrue(state.needsPassword)
    }

}
