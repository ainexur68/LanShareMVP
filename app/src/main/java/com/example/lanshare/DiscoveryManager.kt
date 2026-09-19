package com.example.lanshare

import android.content.Context
import android.net.wifi.WifiManager
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.MulticastSocket
import java.util.concurrent.atomic.AtomicBoolean

class DiscoveryManager(
    private val context: Context,
    private val servicePort: Int,
    private val accessToken: String,
    private val discoveryPort: Int = TransferProtocol.DEFAULT_PORT
) {
    private val running = AtomicBoolean(false)
    private var lock: WifiManager.MulticastLock? = null
    private val group = InetAddress.getByName(TransferProtocol.DISCOVERY_GROUP)
    private val fingerprint = DeviceIdentity.fingerprint(context)

    fun start() {
        if (!running.compareAndSet(false, true)) return
        lock = (context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager)
            .createMulticastLock("lanshare-discovery").apply { setReferenceCounted(false); acquire() }
        Thread(::listenLoop, "lanshare-discovery-listen").start()
        Thread(::announceLoop, "lanshare-discovery-announce").start()
    }

    fun stop() {
        running.set(false)
        lock?.let { if (it.isHeld) it.release() }
    }

    fun addManual(host: String, port: Int = servicePort, token: String = accessToken, alias: String = host) {
        AppState.upsertPeer(Peer(alias, host, port, "manual:" + host + ":" + port, token))
    }

    private fun payload(announce: Boolean): ByteArray = JSONObject()
        .put("alias", DeviceIdentity.alias(context))
        .put("version", "lanshare-0.1")
        .put("deviceModel", android.os.Build.MODEL)
        .put("deviceType", "mobile")
        .put("fingerprint", fingerprint)
        .put("port", servicePort)
        .put("token", accessToken)
        .put("protocol", "http")
        .put("announce", announce)
        .toString().toByteArray()

    private fun announceLoop() {
        MulticastSocket().use { socket ->
            while (running.get()) {
                val bytes = payload(true)
                runCatching { socket.send(DatagramPacket(bytes, bytes.size, group, discoveryPort)) }
                Thread.sleep(3_000)
            }
        }
    }

    private fun listenLoop() {
        MulticastSocket(discoveryPort).use { socket ->
            socket.reuseAddress = true
            socket.joinGroup(group)
            socket.soTimeout = 2_000
            val buf = ByteArray(16 * 1024)
            while (running.get()) {
                try {
                    val packet = DatagramPacket(buf, buf.size)
                    socket.receive(packet)
                    val json = JSONObject(String(packet.data, packet.offset, packet.length))
                    val fp = json.optString("fingerprint")
                    if (fp.isBlank() || fp == fingerprint) continue
                    AppState.upsertPeer(
                        Peer(
                            alias = json.optString("alias", packet.address.hostAddress ?: "Device"),
                            host = packet.address.hostAddress ?: continue,
                            port = json.optInt("port", TransferProtocol.DEFAULT_PORT),
                            fingerprint = fp,
                            token = json.optString("token", "")
                        )
                    )
                    if (json.optBoolean("announce", false)) {
                        val bytes = payload(false)
                        socket.send(DatagramPacket(bytes, bytes.size, packet.address, discoveryPort))
                    }
                } catch (_: java.net.SocketTimeoutException) {
                } catch (_: Exception) {
                }
            }
            runCatching { socket.leaveGroup(group) }
        }
    }
}
