package com.example.lanshare

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PeerEndpointTest {
    @Test
    fun parsesExplicitHostAndPort() {
        assertEquals(
            PeerEndpoint("192.168.1.20", 53318),
            PeerEndpoint.parse("192.168.1.20:53318")
        )
    }

    @Test
    fun usesProtocolDefaultPortWhenPortIsOmitted() {
        assertEquals(
            PeerEndpoint("192.168.1.20", TransferProtocol.DEFAULT_PORT),
            PeerEndpoint.parse("192.168.1.20")
        )
    }

    @Test
    fun rejectsInvalidPort() {
        assertThrows(IllegalArgumentException::class.java) {
            PeerEndpoint.parse("192.168.1.20:99999")
        }
    }
}
