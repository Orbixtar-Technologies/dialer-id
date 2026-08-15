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

}
