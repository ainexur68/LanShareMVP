package com.example.lanshare

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PairingPayloadTest {
    @Test
    fun roundTripPreservesConnectionDetails() {
        val original = PairingPayload(
            host = "192.168.10.21",
            port = 53318,
            token = "0482",
            fingerprint = "device:abc/123",
            alias = "客厅 Pixel 9"
        )

        assertEquals(original, PairingPayload.parse(original.encode()))
    }

    @Test
    fun rejectsForeignQrCode() {
        assertThrows(IllegalArgumentException::class.java) {
            PairingPayload.parse("https://example.com/pair?host=192.168.1.2")
        }
    }

    @Test
    fun rejectsInvalidTokenAndAddress() {
        assertThrows(IllegalArgumentException::class.java) {
            PairingPayload.parse(
                "lanshare://pair?v=1&host=example.com&port=53317&token=12&fp=device&alias=Phone"
            )
        }
    }
}
