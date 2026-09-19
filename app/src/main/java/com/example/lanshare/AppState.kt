package com.example.lanshare

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf

object AppState {
    val peers = mutableStateListOf<Peer>()
    val incoming = mutableStateOf<IncomingOffer?>(null)
    val transfer = mutableStateOf(TransferUiState())
    val sharedFiles = mutableStateListOf<SharedFile>()
    val localEndpoint = mutableStateOf(LocalEndpoint())

    @Synchronized
    fun upsertPeer(peer: Peer) {
        val index = peers.indexOfFirst { it.fingerprint == peer.fingerprint }
        if (index >= 0) peers[index] = peer else peers.add(peer)
        val cutoff = System.currentTimeMillis() - 15_000
        peers.removeAll { it.lastSeenMs < cutoff }
    }
}

data class IncomingOffer(
    val sessionId: String,
    val senderAlias: String,
    val senderHost: String,
    val files: List<IncomingFileMeta>,
    val approval: java.util.concurrent.CompletableFuture<Boolean>
)

data class IncomingFileMeta(
    val id: String,
    val name: String,
    val size: Long,
    val mime: String?,
    val sha256: String
)
