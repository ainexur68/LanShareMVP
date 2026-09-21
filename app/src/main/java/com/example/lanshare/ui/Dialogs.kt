package com.example.lanshare.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.lanshare.AppState
import com.example.lanshare.Peer

@Composable
internal fun FileListDialog(onDismiss: () -> Unit) {
    val files = AppState.sharedFiles.toList()
    DetailsDialog(title = "待发送文件", onDismiss = onDismiss) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(files, key = { it.uri.toString() }) { file ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(file.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(file.mime ?: "文件", style = MaterialTheme.typography.bodySmall, color = Muted)
                    }
                    Text(humanSize(file.size), color = Muted, style = MaterialTheme.typography.bodySmall)
                }
                HorizontalDivider(color = Divider)
            }
        }
        Text(
            "共 ${files.size} 项 · ${humanSize(files.sumOf { it.size.coerceAtLeast(0) })}",
            modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
            color = Muted,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
internal fun PeerPickerDialog(
    peers: List<Peer>,
    selected: Peer?,
    onSelect: (Peer) -> Unit,
    onDismiss: () -> Unit
) {
    DetailsDialog(title = "选择设备", onDismiss = onDismiss) {
        if (peers.isEmpty()) {
            Text("暂未发现设备，请返回后使用扫码连接。", color = Muted, modifier = Modifier.padding(vertical = 28.dp))
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
                items(peers, key = { it.fingerprint }) { peer ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onSelect(peer) }.padding(vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Devices, contentDescription = null, tint = LanBlue)
                        Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                            Text(peer.alias, fontWeight = FontWeight.SemiBold)
                            Text("${peer.host}:${peer.port}", style = MaterialTheme.typography.bodySmall, color = Muted)
                        }
                        if (peer.fingerprint == selected?.fingerprint) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = "已选择", tint = LanBlue)
                        }
                    }
                    HorizontalDivider(color = Divider)
                }
            }
        }
    }
}

@Composable
internal fun IncomingDialog() {
    val offer = AppState.incoming.value ?: return
    AlertDialog(
        onDismissRequest = {},
        title = { Text("接收文件？") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("来自 ${offer.senderAlias}")
                LazyColumn(modifier = Modifier.heightIn(max = 260.dp)) {
                    items(offer.files, key = { it.id }) { file ->
                        Text("${file.name} · ${humanSize(file.size)}", modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
                Text("确认后开始传输，校验通过才会保存到下载目录。", style = MaterialTheme.typography.bodySmall, color = Muted)
            }
        },
        confirmButton = {
            Button(onClick = {
                offer.approval.complete(true)
                AppState.incoming.value = null
            }) { Text("接收") }
        },
        dismissButton = {
            TextButton(onClick = {
                offer.approval.complete(false)
                AppState.incoming.value = null
            }) { Text("拒绝") }
        }
    )
}

@Composable
internal fun DetailsDialog(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(24.dp).widthIn(max = 520.dp),
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(22.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text("关闭") }
                }
                content()
            }
        }
    }
}
