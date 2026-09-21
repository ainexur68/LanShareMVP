package com.example.lanshare.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.lanshare.AppState
import com.example.lanshare.Peer
import com.example.lanshare.TransferStage

private enum class CompactPage { SHARE, CONNECT }

@Composable
fun LanShareApp(
    onPickFiles: () -> Unit,
    onOpenReceiveDirectory: () -> Unit
) {
    LanShareTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = AppBackground) {
            var page by rememberSaveable { mutableStateOf(CompactPage.SHARE) }
            var showFiles by remember { mutableStateOf(false) }
            var showTransfer by remember { mutableStateOf(false) }
            val transfer = AppState.transfer.value
            val transferVisible = transfer.stage != TransferStage.IDLE

            BoxWithConstraints(Modifier.fillMaxSize()) {
                val expanded = maxWidth >= 720.dp
                val contentPadding = if (expanded) 24.dp else 18.dp
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(contentPadding)
                        .padding(bottom = if (transferVisible) 82.dp else 0.dp)
                ) {
                    if (expanded) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            SharePane(
                                modifier = Modifier.weight(1f),
                                showConnectAction = false,
                                onOpenConnect = {},
                                onPickFiles = onPickFiles,
                                onShowFiles = { showFiles = true }
                            )
                            ConnectionPane(
                                modifier = Modifier.weight(1f),
                                showBack = false,
                                onBack = {},
                                onPaired = ::selectPeer
                            )
                        }
                    } else if (page == CompactPage.SHARE) {
                        SharePane(
                            modifier = Modifier.fillMaxSize(),
                            showConnectAction = true,
                            onOpenConnect = { page = CompactPage.CONNECT },
                            onPickFiles = onPickFiles,
                            onShowFiles = { showFiles = true }
                        )
                    } else {
                        ConnectionPane(
                            modifier = Modifier.fillMaxSize(),
                            showBack = true,
                            onBack = { page = CompactPage.SHARE },
                            onPaired = { peer ->
                                selectPeer(peer)
                                page = CompactPage.SHARE
                            }
                        )
                    }
                }

                if (transferVisible) {
                    TransferCapsule(
                        state = transfer,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 18.dp, vertical = 16.dp),
                        onClick = { showTransfer = true }
                    )
                }
            }

            IncomingDialog()
            if (showFiles) FileListDialog(onDismiss = { showFiles = false })
            if (showTransfer) {
                TransferDetailsDialog(
                    state = transfer,
                    onOpenReceiveDirectory = onOpenReceiveDirectory,
                    onDismiss = { showTransfer = false }
                )
            }
        }
    }
}

private fun selectPeer(peer: Peer) {
    AppState.upsertPeer(peer)
    AppState.selectedPeerFingerprint.value = peer.fingerprint
}
