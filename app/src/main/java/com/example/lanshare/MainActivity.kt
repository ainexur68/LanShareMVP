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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    private val notifyPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
    private val pickFiles = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        acceptUris(uris)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ContextCompat.startForegroundService(this, Intent(this, TransferService::class.java))
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notifyPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        consumeShareIntent(intent)
        setContent { LanShareApp() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        consumeShareIntent(intent)
    }

    private fun consumeShareIntent(intent: Intent?) {
        val action = intent?.action ?: return
        val uris: List<Uri> = when (action) {
            Intent.ACTION_SEND -> {
                val uri = if (Build.VERSION.SDK_INT >= 33) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                }
                listOfNotNull(uri)
            }
            Intent.ACTION_SEND_MULTIPLE -> if (Build.VERSION.SDK_INT >= 33) {
                intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java) ?: emptyList()
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM) ?: emptyList()
            }
            else -> emptyList()
        }
        acceptUris(uris)
    }

    private fun acceptUris(uris: List<Uri>) {
        if (uris.isEmpty()) return
        AppState.sharedFiles.clear()
        uris.forEach { uri ->
            runCatching {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            AppState.sharedFiles.add(FileUtil.meta(contentResolver, uri))
        }
    }

    @Composable
    private fun LanShareApp() {
        val colors = lightColorScheme(
            primary = Color(0xFF2563EB),
            onPrimary = Color.White,
            secondary = Color(0xFF0F766E),
            background = Color(0xFFF5F7FB),
            surface = Color.White,
            surfaceVariant = Color(0xFFEFF6FF),
            onSurface = Color(0xFF102A43),
            onSurfaceVariant = Color(0xFF627D98)
        )
        MaterialTheme(colorScheme = colors) {
            Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFF5F7FB)) {
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    val wide = maxWidth >= 720.dp
                    if (wide) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(24.dp),
                            horizontalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            HomePane(Modifier.weight(1f))
                            TransferPane(Modifier.weight(1f))
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            item { HomePane(Modifier.fillMaxWidth()) }
                            item { TransferPane(Modifier.fillMaxWidth()) }
                        }
                    }
                }
                IncomingDialog()
            }
        }
    }

    @Composable
    private fun HomePane(modifier: Modifier) {
        Card(
            modifier = modifier,
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "LanShare",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text("同一局域网，直接传文件")
                    }
                    Spacer(Modifier.width(12.dp))
                    FilledTonalButton(
                        modifier = Modifier.heightIn(min = 48.dp),
                        onClick = { pickFiles.launch(arrayOf("*/*")) }
                    ) {
                        Text("选择文件")
                    }
                }

                Text(
                    "也可以从文件管理器或相册的系统“分享”菜单直接进入。",
                    style = MaterialTheme.typography.bodySmall
                )

                if (AppState.sharedFiles.isNotEmpty()) {
                    Text("待发送", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AppState.sharedFiles.forEach {
                                Text(it.name + "  ·  " + humanSize(it.size))
                            }
                        }
                    }
                }

                Text("附近设备", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (AppState.peers.isEmpty()) {
                    Text(
                        "正在发现设备… 如果网络屏蔽组播，可使用下方手动地址。",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                AppState.peers.forEach { peer ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(peer.alias, fontWeight = FontWeight.SemiBold)
                                Text(
                                    peer.host + ":" + peer.port,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Button(
                                modifier = Modifier.heightIn(min = 48.dp),
                                enabled = AppState.sharedFiles.isNotEmpty() && peer.token.isNotBlank(),
                                onClick = {
                                    TransferClient(this@MainActivity)
                                        .send(peer, AppState.sharedFiles.toList())
                                }
                            ) {
                                Text("发送")
                            }
                        }
                    }
                }
                ManualIpRow()
            }
        }
    }

    @Composable
    private fun ManualIpRow() {
        var endpoint by remember { mutableStateOf("") }
        var token by remember { mutableStateOf("") }
        var error by remember { mutableStateOf("") }

        Text("手动连接", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        OutlinedTextField(
            value = endpoint,
            onValueChange = { endpoint = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("设备地址（IP:端口）") },
            placeholder = { Text("例如 192.168.1.20:53318") },
            singleLine = true
        )
        OutlinedTextField(
            value = token,
            onValueChange = { token = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("访问口令") },
            placeholder = { Text("接收设备“本机接收”卡片中的口令") },
            singleLine = true
        )
        OutlinedButton(
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            onClick = {
                runCatching {
                    val parsed = PeerEndpoint.parse(endpoint)
                    require(token.trim().isNotEmpty()) { "请输入访问口令" }
                    AppState.upsertPeer(
                        Peer(
                            alias = parsed.host,
                            host = parsed.host,
                            port = parsed.port,
                            fingerprint = "manual:" + endpoint.trim(),
                            token = token.trim()
                        )
                    )
                    error = ""
                }.onFailure { error = it.message ?: "地址格式无效" }
            }
        ) {
            Text("添加设备")
        }
        if (error.isNotBlank()) {
            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }

    @Composable
    private fun TransferPane(modifier: Modifier) {
        val state = AppState.transfer.value
        val endpoint = AppState.localEndpoint.value
        Card(
            modifier = modifier,
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("本机接收", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                if (endpoint.token.isBlank()) {
                    Text("正在启动局域网服务…")
                } else {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("自动发现已开启", color = Color(0xFF1D4ED8), fontWeight = FontWeight.SemiBold)
                            Text("服务端口：" + endpoint.port)
                            Text("访问口令：" + endpoint.token)
                            Text(
                                "手动连接时，把本机 IP、端口和口令发给对方。",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                HorizontalDivider()
                Text("传输状态", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(stageLabel(state.stage), color = stageColor(state.stage), fontWeight = FontWeight.SemiBold)
                if (state.fileName.isNotBlank()) Text(state.fileName)
                if (state.total > 0) {
                    LinearProgressIndicator(
                        progress = { (state.sent.toFloat() / state.total).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(humanSize(state.sent) + " / " + humanSize(state.total))
                }
                if (state.message.isNotBlank()) Text(state.message)
                Text("接收目录：Download/LanShare", style = MaterialTheme.typography.bodySmall)
                Text("完成条件：接收端 SHA-256 与发送端一致。", style = MaterialTheme.typography.bodySmall)
            }
        }
    }

    @Composable
    private fun IncomingDialog() {
        val offer = AppState.incoming.value ?: return
        AlertDialog(
            onDismissRequest = {},
            title = { Text("接收文件？") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("来自 " + offer.senderAlias)
                    offer.files.forEach { Text(it.name + " · " + humanSize(it.size)) }
                    Text(
                        "确认后才会开始传输，文件会先写入临时目录并完成 SHA-256 校验。",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    offer.approval.complete(true)
                    AppState.incoming.value = null
                }) {
                    Text("接收")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    offer.approval.complete(false)
                    AppState.incoming.value = null
                }) {
                    Text("拒绝")
                }
            }
        )
    }

    private fun stageLabel(stage: TransferStage): String = when (stage) {
        TransferStage.IDLE -> "等待操作"
        TransferStage.PREPARING -> "准备中"
        TransferStage.WAITING_APPROVAL -> "等待接收确认"
        TransferStage.TRANSFERRING -> "传输中"
        TransferStage.VERIFYING -> "校验中"
        TransferStage.COMPLETE -> "已完成"
        TransferStage.FAILED -> "失败"
        TransferStage.CANCELLED -> "已取消"
    }

    private fun stageColor(stage: TransferStage): Color = when (stage) {
        TransferStage.COMPLETE -> Color(0xFF15803D)
        TransferStage.FAILED, TransferStage.CANCELLED -> Color(0xFFB91C1C)
        TransferStage.TRANSFERRING, TransferStage.VERIFYING -> Color(0xFF2563EB)
        else -> Color(0xFF627D98)
    }

    private fun humanSize(n: Long): String = when {
        n < 0 -> "未知大小"
        n < 1024 -> n.toString() + " B"
        n < 1024 * 1024 -> "%.1f KB".format(n / 1024.0)
        n < 1024L * 1024 * 1024 -> "%.1f MB".format(n / 1024.0 / 1024.0)
        else -> "%.2f GB".format(n / 1024.0 / 1024.0 / 1024.0)
    }
}
