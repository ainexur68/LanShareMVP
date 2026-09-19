package com.example.lanshare

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    private val notifyPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ContextCompat.startForegroundService(this, Intent(this, TransferService::class.java))
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notifyPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        consumeShareIntent(intent)
        setContent { LanShareApp() }
    }

    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); consumeShareIntent(intent) }

    private fun consumeShareIntent(intent: Intent?) {
        val action = intent?.action ?: return
        val uris: List<Uri> = when (action) {
            Intent.ACTION_SEND -> listOfNotNull(intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java))
            Intent.ACTION_SEND_MULTIPLE -> intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java) ?: emptyList()
            else -> emptyList()
        }
        if (uris.isEmpty()) return
        AppState.sharedFiles.clear()
        uris.forEach { uri ->
            runCatching { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            AppState.sharedFiles.add(FileUtil.meta(contentResolver, uri))
        }
    }

    @Composable
    private fun LanShareApp() {
        MaterialTheme(colorScheme = lightColorScheme()) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceVariant) {
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    val wide = maxWidth >= 720.dp
                    if (wide) Row(Modifier.fillMaxSize().padding(24.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        HomePane(Modifier.weight(1f)); TransferPane(Modifier.weight(1f))
                    } else LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        item { HomePane(Modifier.fillMaxWidth()) }; item { TransferPane(Modifier.fillMaxWidth()) }
                    }
                }
                IncomingDialog()
            }
        }
    }

    @Composable
    private fun HomePane(modifier: Modifier) = Card(modifier, shape = RoundedCornerShape(28.dp)) {
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("LanShare", style = MaterialTheme.typography.headlineMedium)
            Text("同一局域网内，选择附近设备即可发送。也可以从系统“分享”菜单直接进入。")
            if (AppState.sharedFiles.isNotEmpty()) {
                Text("待发送", style = MaterialTheme.typography.titleMedium)
                AppState.sharedFiles.forEach { Text("${it.name}  ·  ${humanSize(it.size)}") }
            }
            Text("附近设备", style = MaterialTheme.typography.titleMedium)
            if (AppState.peers.isEmpty()) Text("正在发现设备… 如果车机屏蔽组播，可使用手动 IP。")
            AppState.peers.forEach { peer ->
                FilledTonalButton(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = AppState.sharedFiles.isNotEmpty(),
                    onClick = { TransferClient(this@MainActivity).send(peer, AppState.sharedFiles.toList()) }
                ) { Text("${peer.alias}  ·  ${peer.host}") }
            }
            ManualIpRow()
        }
    }

    @Composable
    private fun ManualIpRow() {
        var ip by remember { mutableStateOf("") }
        OutlinedTextField(ip, { ip = it }, modifier = Modifier.fillMaxWidth(), label = { Text("手动 IP") }, singleLine = true)
        OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = { if (ip.isNotBlank()) AppState.upsertPeer(Peer(ip, ip.trim(), 53317, "manual:${ip.trim()}")) }) { Text("添加设备") }
    }

    @Composable
    private fun TransferPane(modifier: Modifier) = Card(modifier, shape = RoundedCornerShape(28.dp)) {
        val state = AppState.transfer.value
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("传输状态", style = MaterialTheme.typography.titleLarge)
            Text(state.stage.name)
            if (state.fileName.isNotBlank()) Text(state.fileName)
            if (state.total > 0) {
                LinearProgressIndicator(progress = { (state.sent.toFloat() / state.total).coerceIn(0f,1f) }, modifier = Modifier.fillMaxWidth())
                Text("${humanSize(state.sent)} / ${humanSize(state.total)}")
            }
            if (state.message.isNotBlank()) Text(state.message)
            Text("接收目录：Download/LanShare", style = MaterialTheme.typography.bodySmall)
            Text("完成条件：接收端 SHA-256 与发送端一致。", style = MaterialTheme.typography.bodySmall)
        }
    }

    @Composable
    private fun IncomingDialog() {
        val offer = AppState.incoming.value ?: return
        AlertDialog(
            onDismissRequest = {},
            title = { Text("接收文件？") },
            text = { Column { Text("来自 ${offer.senderAlias}"); offer.files.forEach { Text("${it.name} · ${humanSize(it.size)}") } } },
            confirmButton = { Button(onClick = { offer.approval.complete(true); AppState.incoming.value = null }) { Text("接收") } },
            dismissButton = { TextButton(onClick = { offer.approval.complete(false); AppState.incoming.value = null }) { Text("拒绝") } }
        )
    }

    private fun humanSize(n: Long): String = when {
        n < 0 -> "未知大小"
        n < 1024 -> "$n B"
        n < 1024*1024 -> "%.1f KB".format(n/1024.0)
        n < 1024L*1024*1024 -> "%.1f MB".format(n/1024.0/1024.0)
        else -> "%.2f GB".format(n/1024.0/1024.0/1024.0)
    }
}
