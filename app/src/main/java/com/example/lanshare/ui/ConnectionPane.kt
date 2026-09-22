package com.example.lanshare.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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
    onPaired: (Peer) -> Unit,
    expanded: Boolean = false
) {
    val context = LocalContext.current
    val endpoint = AppState.localEndpoint.value
    var localHost by remember { mutableStateOf(NetworkAddress.localIpv4(context)) }

    DisposableEffect(context) {
        val stopObserving = NetworkAddress.observe(context) { localHost = it }
        onDispose(stopObserving)
    }

    val payload = remember(endpoint, localHost) {
        val host = localHost
        if (host != null && endpoint.token.isNotBlank()) {
            PairingPayload(
                host = host,
                port = endpoint.port,
                token = endpoint.token,
                fingerprint = DeviceIdentity.fingerprint(context),
                alias = DeviceIdentity.alias(context)
            )
        } else null
    }
    var scanning by remember { mutableStateOf(initialScannerEnabled(cameraPermissionGranted = false)) }
    var scanError by remember { mutableStateOf("") }
    var showManual by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }
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

    Surface(modifier = modifier, color = MaterialTheme.colorScheme.surface) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val horizontalPadding = if (expanded) 24.dp else 20.dp
            val requestedCardSize = connectionCardSizeDp(expanded).dp
            val maxCardWidth = (maxWidth - horizontalPadding * 2f).coerceAtLeast(0.dp)
            val maxCardHeight = ((maxHeight - 196.dp).coerceAtLeast(0.dp) / 2f)
            val cardSize = minOf(requestedCardSize, maxCardWidth, maxCardHeight)
            val qrSize = (cardSize * 0.54f).coerceAtMost(168.dp)
            val deviceLabel = if (endpoint.token.isBlank()) {
                DeviceIdentity.alias(context)
            } else {
                "${DeviceIdentity.alias(context)} · ${endpoint.token}"
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = horizontalPadding, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    if (showBack) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回", tint = Ink)
                        }
                    }
                    Text(
                        "连接设备",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = if (showBack) 2.dp else 0.dp)
                    )
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { showHelp = true }) {
                        Icon(
                            Icons.AutoMirrored.Rounded.HelpOutline,
                            contentDescription = "连接帮助",
                            tint = Muted,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(Modifier.height(if (expanded) 12.dp else 28.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(modifier = Modifier.size(cardSize)) {
                        if (scanning) {
                            QrScanner(
                                modifier = Modifier.fillMaxSize(),
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
                                onError = { scanError = it },
                                onToggle = { scanning = false }
                            )
                        } else {
                            ScannerPlaceholder(
                                modifier = Modifier.fillMaxSize(),
                                onClick = ::beginScan,
                                error = scanError
                            )
                        }
                    }

                    Spacer(Modifier.height(if (expanded) 10.dp else 18.dp))
                    ConnectionDivider()
                    Spacer(Modifier.height(if (expanded) 10.dp else 18.dp))

                    QrCodeCard(
                        modifier = Modifier.size(cardSize),
                        cardSize = cardSize,
                        payload = payload,
                        qrSize = qrSize,
                        deviceLabel = deviceLabel
                    )
                }

                TextButton(
                    onClick = { showManual = true },
                    modifier = Modifier.padding(top = if (expanded) 8.dp else 12.dp)
                ) {
                    Text("手动连接", style = MaterialTheme.typography.titleMedium)
                    Icon(
                        Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.padding(start = 2.dp).size(24.dp)
                    )
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
    if (showHelp) {
        DetailsDialog(title = "如何连接", onDismiss = { showHelp = false }) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("扫描对方设备上的 LanShare 二维码即可建立局域网连接。")
                Text("也可以让对方扫描本机二维码，或使用底部的手动连接。", color = Muted)
            }
        }
    }
}

@Composable
private fun ConnectionDivider() {
    Row(
        modifier = Modifier.width(180.dp).height(30.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = Divider)
        Text(
            "或",
            modifier = Modifier.padding(horizontal = 12.dp),
            color = Muted,
            style = MaterialTheme.typography.titleMedium
        )
        HorizontalDivider(modifier = Modifier.weight(1f), color = Divider)
    }
}

@Composable
private fun QrCodeCard(
    modifier: Modifier,
    cardSize: Dp,
    payload: PairingPayload?,
    qrSize: Dp,
    deviceLabel: String
) {
    Surface(
        modifier = modifier,
        color = Color.White,
        shape = RoundedCornerShape(26.dp),
        border = BorderStroke(1.dp, Divider),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (payload != null) {
                val bitmap = remember(payload) { QrCodeGenerator.create(payload.encode(), 512) }
                val qrFrameSize = (qrSize + 12.dp).coerceAtMost((cardSize - 32.dp).coerceAtLeast(0.dp))
                Surface(
                    modifier = Modifier.size(qrFrameSize),
                    color = Color.White,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, Divider)
                ) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "本机连接二维码",
                        modifier = Modifier.fillMaxSize().padding(6.dp)
                    )
                }
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.PhoneAndroid, contentDescription = null, tint = Ink, modifier = Modifier.size(22.dp))
                    Text(
                        deviceLabel,
                        modifier = Modifier.padding(start = 8.dp),
                        color = Ink,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
                Icon(Icons.Rounded.Devices, contentDescription = "本机连接信息", tint = PaleBlue, modifier = Modifier.size(46.dp))
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
                shape = RoundedCornerShape(18.dp)
            ) { Text("连接设备") }
        }
    }
}
