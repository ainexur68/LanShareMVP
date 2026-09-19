package com.example.lanshare

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessTokenTest {
    @Test
    fun createsACompactTokenThatCanBeVerified() {
        val token = AccessToken.create()

        assertTrue(token.matches(Regex("[0-9]{4}")))
        assertTrue(AccessToken.matches(token, token))
        assertFalse(AccessToken.matches(token, if (token == "0000") "0001" else "0000"))
    }
}
