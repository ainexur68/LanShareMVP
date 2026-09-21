package com.example.lanshare

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiscoveryProtocolTest {
    @Test
    fun acceptsCurrentProtocolVersion() {
        assertTrue(DiscoveryProtocol.isCompatible("lanshare-0.2"))
    }

    @Test
    fun rejectsLegacyProtocolVersion() {
        assertFalse(DiscoveryProtocol.isCompatible("lanshare-0.1"))
    }

    @Test
    fun rejectsMissingProtocolVersion() {
        assertFalse(DiscoveryProtocol.isCompatible(""))
    }
}
