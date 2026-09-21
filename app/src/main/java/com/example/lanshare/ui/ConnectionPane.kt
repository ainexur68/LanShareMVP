package com.example.lanshare.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.core.content.ContextCompat
import com.example.lanshare.AppState
import com.example.lanshare.DeviceIdentity
import com.example.lanshare.NetworkAddress
import com.example.lanshare.PairingPayload
import com.example.lanshare.Peer
import com.example.lanshare.PeerEndpoint
import com.example.lanshare.QrCodeGenerator

@Composable
internal fun ConnectionPane(
    modifier: Modifier,
    showBack: Boolean,
    onBack: () -> Unit,
    onPaired: (Peer) -> Unit
) {
    val context = LocalContext.current
    val endpoint = AppState.localEndpoint.value
    val localHost = remember(endpoint) { NetworkAddress.localIpv4() }
    val payload = remember(endpoint, localHost) {
        if (localHost != null && endpoint.token.isNotBlank()) {
            PairingPayload(
                host = localHost,
                port = endpoint.port,
                token = endpoint.token,
                fingerprint = DeviceIdentity.fingerprint(context),
                alias = DeviceIdentity.alias(context)
            )
        } else null
    }
    var scanning by remember { mutableStateOf(false) }
    var scanError by remember { mutableStateOf("") }
    var showManual by remember { mutableStateOf(false) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        scanning = granted
        if (!granted) scanError = "需要相机权限才能扫码连接"
    }

    fun beginScan() {
        scanError = ""
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            scanning = true
        } else {
            permission.launch(Manifest.permission.CAMERA)
        }
    }

    Surface(modifier = modifier, color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(28.dp)) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val qrSize = if (maxHeight < 660.dp) 138.dp else 176.dp
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    if (showBack) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Rounded.ArrowBack, contentDescription = "返回")
                        }
                    }
                    Text(
                        "连接设备",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = if (showBack) 2.dp else 0.dp)
                    )
                }

                if (scanning) {
                    QrScanner(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        onValue = { raw ->
                            runCatching { PairingPayload.parse(raw) }
                                .onSuccess {
                                    scanning = false
                                    onPaired(it.toPeer())
                                }
                                .onFailure {
                                    scanError = it.message ?: "无法识别二维码"
                                    scanning = false
                                }
                        },
                        onError = { scanError = it }
                    )
                    OutlinedButton(onClick = { scanning = false }, modifier = Modifier.fillMaxWidth()) {
                        Text("取消扫码")
                    }
                } else {
                    Button(
                        onClick = ::beginScan,
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Icon(Icons.Rounded.QrCodeScanner, contentDescription = null)
                        Text("扫描对方二维码", modifier = Modifier.padding(start = 8.dp))
                    }

                    if (scanError.isNotBlank()) {
                        Text(scanError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Text("让对方扫描", style = MaterialTheme.typography.labelLarge, color = Muted)
                        if (payload != null) {
                            val bitmap = remember(payload) { QrCodeGenerator.create(payload.encode(), 512) }
                            Surface(color = androidx.compose.ui.graphics.Color.White, shape = RoundedCornerShape(20.dp)) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "本机连接二维码",
                                    modifier = Modifier.size(qrSize).padding(8.dp)
                                )
                            }
                            Text(
                                DeviceIdentity.alias(context),
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text("连接码 ${endpoint.token}", color = Muted, style = MaterialTheme.typography.bodySmall)
                        } else {
                            Surface(color = AppBackground, shape = RoundedCornerShape(20.dp)) {
                                Text(
                                    "正在准备本机连接信息…\n请确认已连接 Wi-Fi",
                                    modifier = Modifier.size(qrSize).padding(20.dp),
                                    textAlign = TextAlign.Center,
                                    color = Muted
                                )
                            }
                        }
                    }

                    NearbyPeers(
                        peers = AppState.peers.take(2),
                        onSelect = onPaired
                    )

                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { showManual = true }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                        Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("无法扫码？手动连接", modifier = Modifier.padding(start = 7.dp))
                    }
                }
            }
        }
    }

    if (showManual) {
        ManualConnectDialog(
            onConnected = {
                showManual = false
                onPaired(it)
            },
            onDismiss = { showManual = false }
        )
    }
}

@Composable
private fun NearbyPeers(peers: List<Peer>, onSelect: (Peer) -> Unit) {
    if (peers.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("附近设备", style = MaterialTheme.typography.labelLarge, color = Muted)
        peers.forEach { peer ->
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { onSelect(peer) }.padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Devices, contentDescription = null, tint = LanBlue)
                Text(peer.alias, modifier = Modifier.padding(start = 10.dp).weight(1f), maxLines = 1)
                Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = Muted)
            }
        }
    }
}

@Composable
private fun ManualConnectDialog(onConnected: (Peer) -> Unit, onDismiss: () -> Unit) {
    var endpoint by remember { mutableStateOf("") }
    var token by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    DetailsDialog(title = "手动连接", onDismiss = onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = endpoint,
                onValueChange = { endpoint = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("IP 地址与端口") },
                placeholder = { Text("192.168.1.20:53317") },
                singleLine = true
            )
            OutlinedTextField(
                value = token,
                onValueChange = { token = it.filter(Char::isDigit).take(4) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("4 位连接码") },
                singleLine = true
            )
            if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            Button(
                onClick = {
                    runCatching {
                        val parsed = PeerEndpoint.parse(endpoint)
                        require(token.matches(Regex("[0-9]{4}"))) { "请输入 4 位数字连接码" }
                        Peer(
                            alias = parsed.host,
                            host = parsed.host,
                            port = parsed.port,
                            fingerprint = "manual:${parsed.host}:${parsed.port}",
                            token = token
                        )
                    }.onSuccess(onConnected).onFailure { error = it.message ?: "连接信息无效" }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) { Text("连接设备") }
        }
    }
}
