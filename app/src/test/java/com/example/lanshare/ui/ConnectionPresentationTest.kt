package com.example.lanshare.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ConnectionPresentationTest {
    @Test
    fun keepsScannerAndQrCardsAtTheSameCompactSize() {
        assertEquals(244, connectionCardSizeDp(expanded = false))
        assertEquals(
            connectionCardSizeDp(expanded = false),
            connectionCardSizeDp(expanded = false)
        )
        assertEquals(320, connectionCardSizeDp(expanded = true))
    }

    @Test
    fun startsWithCameraClosedEvenWhenPermissionWasPreviouslyGranted() {
        assertFalse(initialScannerEnabled(cameraPermissionGranted = true))
        assertFalse(initialScannerEnabled(cameraPermissionGranted = false))
    }
}
