package com.example.lanshare.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.lanshare.AppState
import com.example.lanshare.Peer
import com.example.lanshare.TransferClient

@Composable
internal fun SharePane(
    modifier: Modifier,
    showConnectAction: Boolean,
    onOpenConnect: () -> Unit,
    onPickFiles: () -> Unit,
    onShowFiles: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val selectedPeer = AppState.selectedPeer()
    var showPeers by remember { mutableStateOf(false) }
    val fileCount = AppState.sharedFiles.size
    val totalBytes = AppState.sharedFiles.sumOf { it.size.coerceAtLeast(0) }

    Surface(modifier = modifier, color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(28.dp)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("LanShare", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                if (showConnectAction) {
                    TextButton(onClick = onOpenConnect) {
                        Icon(Icons.Rounded.QrCodeScanner, contentDescription = null, modifier = Modifier.size(20.dp))
                        Text("扫码连接", modifier = Modifier.padding(start = 6.dp))
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("分享文件", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("选择内容和目标设备，即刻发送", color = Muted, style = MaterialTheme.typography.bodyMedium)
            }

            Surface(
                modifier = Modifier.fillMaxWidth().heightIn(min = 138.dp).clickable(onClick = onPickFiles),
                color = SoftBlue,
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize().padding(20.dp)
                ) {
                    Surface(color = MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(18.dp)) {
                        Icon(
                            Icons.Rounded.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(12.dp).size(28.dp)
                        )
                    }
                    Text(
                        if (fileCount == 0) "选择文件" else "继续添加",
                        modifier = Modifier.padding(top = 10.dp),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.UploadFile, contentDescription = null, tint = Muted)
                Text(
                    if (fileCount == 0) "尚未选择文件" else "$fileCount 个文件 · ${humanSize(totalBytes)}",
                    modifier = Modifier.padding(start = 10.dp).weight(1f),
                    color = if (fileCount == 0) Muted else Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (fileCount > 0) TextButton(onClick = onShowFiles) { Text("查看") }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("发送到", style = MaterialTheme.typography.labelLarge, color = Muted)
                Surface(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp).clickable { showPeers = true },
                    color = AppBackground,
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Devices, contentDescription = null, tint = LanBlue)
                        Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                            Text(
                                selectedPeer?.alias ?: "选择设备",
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                if (AppState.peers.isEmpty()) "扫码或等待发现附近设备" else "${AppState.peers.size} 台设备可用",
                                style = MaterialTheme.typography.bodySmall,
                                color = Muted
                            )
                        }
                        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = Muted)
                    }
                }
            }

            Spacer(Modifier.weight(1f))
            Button(
                onClick = { selectedPeer?.let { TransferClient(context).send(it, AppState.sharedFiles.toList()) } },
                enabled = fileCount > 0 && selectedPeer?.token?.isNotBlank() == true,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text(if (selectedPeer == null) "请选择设备" else "发送到 ${selectedPeer.alias}")
            }
        }
    }

    if (showPeers) {
        PeerPickerDialog(
            peers = AppState.peers.toList(),
            selected = selectedPeer,
            onSelect = {
                AppState.selectedPeerFingerprint.value = it.fingerprint
                showPeers = false
            },
            onDismiss = { showPeers = false }
        )
    }
}
