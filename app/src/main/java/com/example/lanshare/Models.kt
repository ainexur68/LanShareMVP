package com.example.lanshare

import android.net.Uri

data class Peer(
    val alias: String,
    val host: String,
    val port: Int,
    val fingerprint: String,
    val token: String = "",
    val lastSeenMs: Long = System.currentTimeMillis()
)

data class SharedFile(
    val uri: Uri,
    val name: String,
    val size: Long,
    val mime: String?,
    val sha256: String? = null
)

enum class TransferStage { IDLE, PREPARING, WAITING_APPROVAL, TRANSFERRING, VERIFYING, COMPLETE, FAILED, CANCELLED }

data class TransferUiState(
    val stage: TransferStage = TransferStage.IDLE,
    val fileName: String = "",
    val sent: Long = 0,
    val total: Long = 0,
    val message: String = ""
)

data class LocalEndpoint(
    val port: Int = TransferProtocol.DEFAULT_PORT,
    val token: String = ""
)
