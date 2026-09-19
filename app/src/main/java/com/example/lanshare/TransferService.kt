package com.example.lanshare

import android.app.*
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import java.io.IOException

class TransferService : Service() {
    private var server: TransferServer? = null
    private var discovery: DiscoveryManager? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(1001, notification("LanShare 正在等待附近设备"))
        val token = AccessToken.create()
        var activeServer: TransferServer? = null
        var activePort = TransferProtocol.DEFAULT_PORT
        for (offset in 0..10) {
            val candidate = TransferServer(this, TransferProtocol.DEFAULT_PORT + offset, token)
            try {
                activePort = candidate.start()
                activeServer = candidate
                break
            } catch (_: IOException) {
                candidate.stop()
            }
        }
        if (activeServer == null) {
            stopSelf()
            return
        }
        server = activeServer
        AppState.localEndpoint.value = LocalEndpoint(activePort, token)
        discovery = DiscoveryManager(this, activePort, token).also { it.start() }
    }

    override fun onDestroy() {
        discovery?.stop()
        server?.stop()
        AppState.localEndpoint.value = LocalEndpoint()
        super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder? = null

    private fun createChannel() {
        val c = NotificationChannel("transfer", "局域网传输", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(c)
    }

    private fun notification(text: String): Notification = NotificationCompat.Builder(this, "transfer")
        .setSmallIcon(android.R.drawable.stat_sys_upload)
        .setContentTitle("LanShare")
        .setContentText(text)
        .setOngoing(true)
        .build()
}
