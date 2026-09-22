package com.example.lanshare.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QrScannerLifecycleTest {
    @Test
    fun doesNotBindWhenScannerIsDisposedBeforeProviderReady() {
        val gate = QrScannerBindingGate()
        gate.dispose()
        var bindCalls = 0

        val camera = gate.withActive {
            bindCalls += 1
            Any()
        }

        assertNull(camera)
        assertEquals(0, bindCalls)
    }
}
