package com.example.lanshare

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessTokenTest {
    @Test
    fun createsACompactTokenThatCanBeVerified() {
        val token = AccessToken.create()

        assertTrue(token.matches(Regex("[a-f0-9]{12}")))
        assertTrue(AccessToken.matches(token, token))
        assertFalse(AccessToken.matches(token, "000000000000"))
    }
}
