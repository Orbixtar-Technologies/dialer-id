package com.example.util

import java.security.MessageDigest

/**
 * RFC 2617 / RFC 2069 HTTP Digest (MD5) helpers used by the isolated custom registrar.
 */
object SipDigestAuth {
    fun md5Hex(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun computeResponse(
        user: String,
        realm: String,
        pass: String,
        method: String,
        uri: String,
        nonce: String,
        qop: String = "",
        cnonce: String = "",
        nc: String = ""
    ): String {
        val ha1 = md5Hex("$user:$realm:$pass")
        val ha2 = md5Hex("$method:$uri")
        return if (qop.contains("auth", ignoreCase = true)) {
            md5Hex("$ha1:$nonce:$nc:$cnonce:auth:$ha2")
        } else {
            md5Hex("$ha1:$nonce:$ha2")
        }
    }
}
