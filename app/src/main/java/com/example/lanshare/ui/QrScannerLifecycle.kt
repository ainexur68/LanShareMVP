package com.example.lanshare.ui

internal class QrScannerBindingGate {
    private val lock = Any()
    private var active = true

    fun isActive(): Boolean = synchronized(lock) { active }

    fun dispose() {
        synchronized(lock) {
            active = false
        }
    }

    fun <T> withActive(block: () -> T): T? = synchronized(lock) {
        if (active) block() else null
    }
}
