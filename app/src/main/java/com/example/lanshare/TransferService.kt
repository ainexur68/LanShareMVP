package com.example.lanshare

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class TransferService : Service() {
    private lateinit var server: TransferServer
    private lateinit var discovery: DiscoveryManager

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(1001, notification("LanShare 正在等待附近设备"))
        server = TransferServer(this)
        discovery = DiscoveryManager(this)
        server.start(); discovery.start()
    }

    override fun onDestroy() { discovery.stop(); server.stop(); super.onDestroy() }
    override fun onBind(intent: Intent?): IBinder? = null

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val c = NotificationChannel("transfer", "局域网传输", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(c)
        }
    }

    private fun notification(text: String): Notification = NotificationCompat.Builder(this, "transfer")
        .setSmallIcon(android.R.drawable.stat_sys_upload)
        .setContentTitle("LanShare")
        .setContentText(text)
        .setOngoing(true)
        .build()
}
