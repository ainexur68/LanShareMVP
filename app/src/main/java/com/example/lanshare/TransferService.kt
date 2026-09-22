package com.example.lanshare

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import java.io.IOException

internal const val ACTION_REFRESH_DISCOVERY = "com.example.lanshare.action.REFRESH_DISCOVERY"

internal fun requestDiscoveryRefresh(context: Context) {
    AppState.refreshPeers()
    ContextCompat.startForegroundService(
        context,
        Intent(context, TransferService::class.java).setAction(ACTION_REFRESH_DISCOVERY)
    )
}

class TransferService : Service() {
    private var server: TransferServer? = null
    private var discovery: DiscoveryManager? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(1001, notification("LocalShare 正在等待附近设备"))
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

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // The refresh request is consumed by DiscoveryManager through AppState's counter.
        return START_STICKY
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
        .setContentTitle("LocalShare")
        .setContentText(text)
        .setOngoing(true)
        .build()
}
