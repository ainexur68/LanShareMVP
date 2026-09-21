package com.example.lanshare

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TransferProgressTest {
    @Test
    fun throttlesUpdatesAndCalculatesRateAndEta() {
        var now = 0L
        val progress = TransferProgress(intervalNanos = 100L) { now }

        now = 50L
        assertNull(progress.snapshot(500, 1_500))

        now = 100L
        val snapshot = progress.snapshot(1_000, 2_000)!!
        assertEquals(10_000_000_000L, snapshot.bytesPerSecond)
        assertEquals(1L, snapshot.etaSeconds)
    }
}
