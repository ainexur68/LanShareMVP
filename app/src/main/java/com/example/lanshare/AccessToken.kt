package com.example.lanshare

import java.security.MessageDigest
import java.security.SecureRandom

/** Short-lived access token advertised only to devices on the current LAN. */
object AccessToken {
    private val random = SecureRandom()

    fun create(): String {
        return random.nextInt(10_000).toString().padStart(4, '0')
    }

    fun matches(expected: String?, provided: String?): Boolean {
        if (expected.isNullOrBlank() || provided.isNullOrBlank()) return false
        return MessageDigest.isEqual(
            expected.toByteArray(Charsets.UTF_8),
            provided.toByteArray(Charsets.UTF_8)
        )
    }
}
