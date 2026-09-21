package com.example.lanshare

class TransferProgress(
    private val intervalNanos: Long = 150_000_000L,
    private val nowNanos: () -> Long = System::nanoTime
) {
    private var startedAt = nowNanos()
    private var lastPublishedAt = startedAt
    private var lastPublishedBytes = 0L

    fun snapshot(bytes: Long, total: Long, force: Boolean = false): Snapshot? {
        val now = nowNanos()
        if (!force && now - lastPublishedAt < intervalNanos) return null
        val elapsed = (now - lastPublishedAt).coerceAtLeast(1L)
        val delta = (bytes - lastPublishedBytes).coerceAtLeast(0L)
        val instantRate = delta * 1_000_000_000L / elapsed
        val totalElapsed = (now - startedAt).coerceAtLeast(1L)
        val averageRate = bytes.coerceAtLeast(0L) * 1_000_000_000L / totalElapsed
        val rate = if (instantRate > 0) instantRate else averageRate
        val remaining = (total - bytes).coerceAtLeast(0L)
        val eta = if (rate > 0) (remaining + rate - 1) / rate else null
        lastPublishedAt = now
        lastPublishedBytes = bytes
        return Snapshot(rate, eta)
    }

    data class Snapshot(val bytesPerSecond: Long, val etaSeconds: Long?)
}
