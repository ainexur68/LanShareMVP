package com.example.lanshare.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.lanshare.AppState
import com.example.lanshare.Peer
import com.example.lanshare.R
import com.example.lanshare.SharedFile
import com.example.lanshare.TransferClient
import com.example.lanshare.requestDiscoveryRefresh
import androidx.compose.ui.platform.LocalContext

@Composable
internal fun SharePane(
    modifier: Modifier,
    showConnectAction: Boolean,
    onOpenConnect: () -> Unit,
    onPickFiles: () -> Unit,
    expanded: Boolean = false
) {
    val context = LocalContext.current
    val files = AppState.sharedFiles.toList()
    val selectedPeer = AppState.selectedPeer()
    val availablePeers = AppState.peers.toList()
    var showPeers by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        val horizontalPadding = if (expanded) 24.dp else 20.dp
        val contentMaxWidth = if (expanded) 384.dp else 608.dp
        val selectedPanelHeight = (88.dp + 76.dp * files.size).coerceIn(158.dp, 292.dp)
        val footerSpec = shareFooterSpec(files.size)
        val contentNeedsScroll = shareContentNeedsScroll(expanded, maxHeight.value.toInt())
        val contentScrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = contentMaxWidth)
                .padding(horizontal = horizontalPadding, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ShareHeader(
                showConnectAction = showConnectAction,
                onOpenConnect = onOpenConnect,
                onShowHelp = { showHelp = true }
            )

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                val contentModifier = if (contentNeedsScroll) {
                    Modifier.fillMaxWidth().verticalScroll(contentScrollState)
                } else {
                    Modifier.fillMaxSize()
                }
                when (shareLayoutState(files.size)) {
                    ShareLayoutState.EMPTY -> EmptyShareState(
                        modifier = contentModifier,
                        scrollable = contentNeedsScroll,
                        expanded = expanded,
                        peer = selectedPeer,
                        availableCount = availablePeers.size,
                        onPickFiles = onPickFiles,
                        onChoosePeer = { showPeers = true },
                        onRefresh = { requestDiscoveryRefresh(context) }
                    )

                    ShareLayoutState.SELECTED -> SelectedShareState(
                        modifier = contentModifier,
                        scrollable = contentNeedsScroll,
                        files = files,
                        panelHeight = selectedPanelHeight,
                        peer = selectedPeer,
                        availableCount = availablePeers.size,
                        onAddFiles = onPickFiles,
                        onChoosePeer = { showPeers = true },
                        onRemoveFile = { file ->
                            AppState.sharedFiles.removeAll { it.uri == file.uri }
                        }
                    )
                }
            }

            ShareFooter(
                spec = footerSpec,
                enabled = files.isNotEmpty() && selectedPeer?.token?.isNotBlank() == true,
                label = shareActionLabel(files.size, selectedPeer?.alias),
                summary = if (footerSpec.showSummary) shareTransferSummary(files) else null,
                onClick = {
                    selectedPeer?.let { TransferClient(context).send(it, files) }
                }
            )
        }
    }

    if (showPeers) {
        PeerPickerDialog(
            peers = availablePeers,
            selected = selectedPeer,
            onSelect = {
                AppState.selectedPeerFingerprint.value = it.fingerprint
                showPeers = false
            },
            onOpenConnect = if (showConnectAction) {
                {
                    showPeers = false
                    onOpenConnect()
                }
            } else null,
            onDismiss = { showPeers = false }
        )
    }
    if (showHelp) {
        DetailsDialog(title = "分享文件", onDismiss = { showHelp = false }) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("先选择一个或多个文件，再选择同一局域网内的设备发送。")
                Text("发送完成前会保留原始 content:// 文件，接收端校验通过后才会保存。", color = Muted)
            }
        }
    }
}

@Composable
private fun ShareHeader(
    showConnectAction: Boolean,
    onOpenConnect: () -> Unit,
    onShowHelp: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "分享文件",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Ink
        )
        Spacer(Modifier.weight(1f))
        if (showConnectAction) {
            IconButton(onClick = onOpenConnect) {
                Icon(painter = painterResource(R.drawable.ic_qr_code_scanner), contentDescription = "扫码连接", tint = LanBlue, modifier = Modifier.size(25.dp))
            }
        }
        IconButton(onClick = onShowHelp) {
            Icon(painter = painterResource(R.drawable.ic_help_outline), contentDescription = "分享帮助", tint = Muted, modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
private fun EmptyShareState(
    modifier: Modifier,
    scrollable: Boolean,
    expanded: Boolean,
    peer: Peer?,
    availableCount: Int,
    onPickFiles: () -> Unit,
    onChoosePeer: () -> Unit,
    onRefresh: () -> Unit
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(if (expanded) 16.dp else 24.dp))
        FlowHint()
        Spacer(Modifier.height(if (expanded) 16.dp else 24.dp))

        Surface(
            modifier = Modifier.fillMaxWidth().height(if (expanded) 204.dp else 220.dp),
            color = SoftBlue,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Divider)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 10.dp)) {
                ShareOptionRow(
                    modifier = Modifier.weight(1f),
                    icon = R.drawable.ic_description,
                    iconTint = LanBlue,
                    title = "选择文件",
                    subtitle = "尚未选择文件",
                    trailing = { Icon(painter = painterResource(R.drawable.ic_chevron_right), contentDescription = null, tint = Muted, modifier = Modifier.size(28.dp)) },
                    onClick = onPickFiles
                )
                HorizontalDivider(color = Divider)
                ShareOptionRow(
                    modifier = Modifier.weight(1f),
                    icon = R.drawable.ic_phone_android,
                    iconTint = if (peer == null) Muted else LanBlue,
                    title = peer?.alias ?: "未发现设备",
                    subtitle = if (peer == null) {
                        if (availableCount == 0) "点击选择或连接设备" else "$availableCount 台设备可用"
                    } else "同一局域网",
                    trailing = {
                        if (peer == null) {
                            Icon(painter = painterResource(R.drawable.ic_chevron_right), contentDescription = null, tint = Muted, modifier = Modifier.size(28.dp))
                        } else {
                            SelectionCheck()
                        }
                    },
                    onClick = onChoosePeer
                )
            }
        }

        TextButton(onClick = onRefresh, modifier = Modifier.padding(top = 10.dp).heightIn(min = 48.dp)) {
            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(23.dp))
            Text("重新扫描设备", modifier = Modifier.padding(start = 8.dp), style = MaterialTheme.typography.titleMedium)
        }

        if (!scrollable) Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun SelectedShareState(
    modifier: Modifier,
    scrollable: Boolean,
    files: List<SharedFile>,
    panelHeight: androidx.compose.ui.unit.Dp,
    peer: Peer?,
    availableCount: Int,
    onAddFiles: () -> Unit,
    onChoosePeer: () -> Unit,
    onRemoveFile: (SharedFile) -> Unit
) {
    Column(modifier = modifier) {
        Spacer(Modifier.height(12.dp))
        Surface(
            modifier = Modifier.fillMaxWidth().height(panelHeight),
            color = SoftBlue,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Divider)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(68.dp).padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconBubble(R.drawable.ic_description, LanBlue)
                    Text(
                        "已选 ${files.size} 个文件",
                        modifier = Modifier.padding(start = 14.dp).weight(1f),
                        style = MaterialTheme.typography.titleLarge,
                        color = Ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    TextButton(onClick = onAddFiles, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text("添加", style = MaterialTheme.typography.titleMedium)
                    }
                }
                HorizontalDivider(color = Divider)
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 2.dp)
                ) {
                    itemsIndexed(files, key = { _, file -> file.uri.toString() }) { index, file ->
                        SelectedFileRow(file = file, onRemove = { onRemoveFile(file) })
                        if (index < files.lastIndex) HorizontalDivider(color = Divider)
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Text("发送到", style = MaterialTheme.typography.titleLarge, color = Ink)
        Spacer(Modifier.height(8.dp))
        ShareDeviceRow(
            peer = peer,
            availableCount = availableCount,
            onClick = onChoosePeer
        )
        if (!scrollable) Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun ShareFooter(
    spec: ShareFooterSpec,
    enabled: Boolean,
    label: String,
    summary: String?,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = spec.topSpacingDp.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ShareButton(enabled = enabled, label = label, onClick = onClick)
        Box(
            modifier = Modifier.fillMaxWidth().height(spec.summarySlotHeightDp.dp),
            contentAlignment = Alignment.Center
        ) {
            if (spec.showSummary && summary != null) {
                Text(
                    summary,
                    color = Muted,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(Modifier.height(spec.bottomSpacingDp.dp))
    }
}

@Composable
private fun FlowHint() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("文件", color = Muted, style = MaterialTheme.typography.titleMedium)
        Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, tint = Muted, modifier = Modifier.padding(horizontal = 14.dp).size(24.dp))
        Text("设备", color = Muted, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun ShareOptionRow(
    modifier: Modifier,
    icon: Int,
    iconTint: Color,
    title: String,
    subtitle: String,
    trailing: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBubble(icon, iconTint)
        Column(modifier = Modifier.padding(start = 14.dp).weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        trailing()
    }
}

@Composable
private fun ShareDeviceRow(peer: Peer?, availableCount: Int, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().heightIn(min = 76.dp).clickable(onClick = onClick),
        color = SoftBlue,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, Divider)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBubble(R.drawable.ic_phone_android, if (peer == null) Muted else LanBlue)
            Column(modifier = Modifier.padding(start = 14.dp).weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    peer?.alias ?: "未发现设备",
                    style = MaterialTheme.typography.titleMedium,
                    color = Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    if (peer == null) {
                        if (availableCount == 0) "选择或连接设备" else "$availableCount 台设备可用"
                    } else "同一局域网",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (peer == null) {
                Icon(painter = painterResource(R.drawable.ic_chevron_right), contentDescription = "选择设备", tint = Muted, modifier = Modifier.size(28.dp))
            } else {
                SelectionCheck()
            }
        }
    }
}

@Composable
private fun SelectedFileRow(file: SharedFile, onRemove: () -> Unit) {
    val kind = fileKindFor(file.name, file.mime)
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(modifier = Modifier.size(48.dp), color = fileKindBackground(kind), shape = RoundedCornerShape(13.dp)) {
            Icon(painter = painterResource(fileKindIcon(kind)), contentDescription = null, tint = fileKindTint(kind), modifier = Modifier.padding(10.dp).size(28.dp))
        }
        Column(modifier = Modifier.padding(start = 14.dp).weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(file.name, style = MaterialTheme.typography.titleMedium, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(humanSize(file.size), style = MaterialTheme.typography.bodyLarge, color = Muted, maxLines = 1)
        }
        IconButton(onClick = onRemove) {
            Surface(color = PaleBlue, shape = CircleShape) {
                Icon(Icons.Rounded.Close, contentDescription = "删除 ${file.name}", tint = Muted, modifier = Modifier.padding(9.dp).size(22.dp))
            }
        }
    }
}

@Composable
private fun IconBubble(icon: Int, tint: Color) {
    Surface(color = PaleBlue, shape = CircleShape, modifier = Modifier.size(64.dp)) {
        Icon(painter = painterResource(icon), contentDescription = null, tint = tint, modifier = Modifier.padding(16.dp).size(32.dp))
    }
}

@Composable
private fun SelectionCheck() {
    Surface(color = LanBlue, shape = CircleShape, modifier = Modifier.size(48.dp)) {
        Icon(Icons.Rounded.Check, contentDescription = "已选择", tint = Color.White, modifier = Modifier.padding(11.dp).size(26.dp))
    }
}

@Composable
private fun ShareButton(enabled: Boolean, label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(30.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = LanBlue,
            disabledContainerColor = ColorDisabledBlue,
            disabledContentColor = ColorDisabledContent
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp, pressedElevation = 0.dp)
    ) {
        Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = null, modifier = Modifier.size(26.dp))
        Text(label, modifier = Modifier.padding(start = 10.dp), style = MaterialTheme.typography.titleMedium, color = Color.White)
    }
}

private val ColorDisabledBlue = Color(0xFFB8CFF4)
private val ColorDisabledContent = Color(0xFFF7FAFF)
