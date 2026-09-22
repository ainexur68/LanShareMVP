package com.example.lanshare.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
    val listHeight = minOf(files.size.coerceAtLeast(1) * 52, 390).dp
    val dialogHeight = fileDialogHeightDp(files.size).dp
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth().widthIn(max = 840.dp).height(dialogHeight),
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(32.dp),
                shadowElevation = 18.dp
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 24.dp, top = 8.dp, end = 12.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("待发送文件", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Rounded.Close, contentDescription = "关闭", tint = Muted, modifier = Modifier.size(30.dp))
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().height(listHeight).padding(horizontal = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        items(files, key = { it.uri.toString() }) { file ->
                            FileListRow(file.name, file.size, file.mime)
                            HorizontalDivider(color = Divider)
                        }
                    }

                    Surface(color = SoftBlue.copy(alpha = 0.72f), modifier = Modifier.fillMaxWidth()) {
                        Text(
                            fileQueueSummary(files.size, files.sumOf { it.size.coerceAtLeast(0) }),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            color = Muted,
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FileListRow(name: String, size: Long, mime: String?) {
    val kind = fileKindFor(name, mime)
    Row(
        modifier = Modifier.fillMaxWidth().height(52.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(48.dp),
            color = fileKindBackground(kind),
            shape = RoundedCornerShape(13.dp)
        ) {
            Icon(
                fileKindIcon(kind),
                contentDescription = null,
                tint = fileKindTint(kind),
                modifier = Modifier.padding(10.dp).size(28.dp)
            )
        }
        Column(modifier = Modifier.padding(start = 14.dp).weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(humanSize(size), style = MaterialTheme.typography.bodyLarge, color = Muted)
        }
    }
}

internal fun fileKindIcon(kind: FileKind): ImageVector = when (kind) {
    FileKind.IMAGE -> Icons.Rounded.Image
    FileKind.VIDEO -> Icons.Rounded.PlayArrow
    FileKind.PDF -> Icons.Rounded.PictureAsPdf
    FileKind.GENERIC -> Icons.AutoMirrored.Rounded.InsertDriveFile
}

internal fun fileKindBackground(kind: FileKind): Color = when (kind) {
    FileKind.IMAGE -> Color(0xFFE5F1FF)
    FileKind.VIDEO -> Color(0xFFF0E8FF)
    FileKind.PDF -> Color(0xFFFFE8EB)
    FileKind.GENERIC -> SoftBlue
}

internal fun fileKindTint(kind: FileKind): Color = when (kind) {
    FileKind.IMAGE -> LanBlue
    FileKind.VIDEO -> Color(0xFF7043D8)
    FileKind.PDF -> Color(0xFFE6263B)
    FileKind.GENERIC -> Muted
}

@Composable
internal fun PeerPickerDialog(
    peers: List<Peer>,
    selected: Peer?,
    onSelect: (Peer) -> Unit,
    onOpenConnect: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    DetailsDialog(title = "选择设备", onDismiss = onDismiss) {
        if (peers.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("暂未发现设备", color = Ink)
                Text("请重新扫描，或使用扫码连接。", color = Muted, modifier = Modifier.padding(top = 6.dp))
                if (onOpenConnect != null) {
                    TextButton(
                        onClick = {
                            onDismiss()
                            onOpenConnect()
                        },
                        modifier = Modifier.padding(top = 8.dp).heightIn(min = 48.dp)
                    ) { Text("连接设备") }
                }
            }
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
            modifier = Modifier.fillMaxWidth().padding(20.dp).widthIn(max = 520.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 12.dp
        ) {
            Column(modifier = Modifier.padding(22.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "关闭", tint = Muted)
                    }
                }
                content()
            }
        }
    }
}
