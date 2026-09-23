package com.example.lanshare.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.lanshare.AppState
import com.example.lanshare.ReceiveDirectory
import com.example.lanshare.R
import com.example.lanshare.TransferDirection
import com.example.lanshare.TransferStage
import com.example.lanshare.TransferUiState

@Composable
internal fun TransferCapsule(state: TransferUiState, modifier: Modifier, onClick: () -> Unit) {
    val progress = if (state.total > 0) (state.sent.toFloat() / state.total).coerceIn(0f, 1f) else 0f
    val iconPainter = when {
        state.stage == TransferStage.COMPLETE -> rememberVectorPainter(Icons.Rounded.CheckCircle)
        state.stage == TransferStage.FAILED -> painterResource(R.drawable.ic_error)
        state.direction == TransferDirection.SEND -> painterResource(R.drawable.ic_upload)
        else -> painterResource(R.drawable.ic_download)
    }
    Surface(
        modifier = modifier.fillMaxWidth().widthIn(max = 560.dp).clickable(onClick = onClick),
        color = Ink,
        contentColor = Color.White,
        shape = RoundedCornerShape(22.dp),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = iconPainter,
                contentDescription = null,
                tint = if (state.stage == TransferStage.COMPLETE) Color(0xFF71D99A) else Color.White,
                modifier = Modifier.size(24.dp)
            )
            Column(modifier = Modifier.padding(horizontal = 12.dp).weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        state.fileName.ifBlank { stageLabel(state.stage) },
                        modifier = Modifier.weight(1f),
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(fileCountLabel(state), style = MaterialTheme.typography.bodySmall, color = Color(0xFFCBD5E1))
                }
                if (state.stage == TransferStage.TRANSFERRING || state.stage == TransferStage.VERIFYING) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF7AA7FF),
                        trackColor = Color(0xFF334155)
                    )
                }
                Row {
                    Text(statusSummary(state), style = MaterialTheme.typography.bodySmall, color = Color(0xFFCBD5E1))
                }
            }
            Icon(painter = painterResource(R.drawable.ic_chevron_right), contentDescription = "查看传输详情", tint = Color(0xFFCBD5E1))
        }
    }
}

@Composable
internal fun TransferDetailsDialog(
    state: TransferUiState,
    onOpenReceiveDirectory: () -> Unit,
    onDismiss: () -> Unit
) {
    DetailsDialog(title = "传输详情", onDismiss = onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(stageLabel(state.stage), fontWeight = FontWeight.SemiBold, color = stageColor(state.stage))
            if (state.fileName.isNotBlank()) {
                Text(state.fileName, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            if (state.total > 0) {
                LinearProgressIndicator(
                    progress = { (state.sent.toFloat() / state.total).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text("${humanSize(state.sent)} / ${humanSize(state.total)}", modifier = Modifier.weight(1f), color = Muted)
                    Text("${((state.sent * 100) / state.total).coerceIn(0, 100)}%", color = Muted)
                }
            }
            Surface(color = AppBackground, shape = RoundedCornerShape(18.dp)) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DetailRow("速度", speedLabel(state.bytesPerSecond))
                    DetailRow("剩余时间", etaLabel(state.etaSeconds).ifBlank { "计算中" })
                    DetailRow("文件进度", fileCountLabel(state))
                    if (state.message.isNotBlank()) DetailRow("状态", state.message)
                }
            }
            if (state.direction == TransferDirection.RECEIVE && state.stage == TransferStage.COMPLETE) {
                OutlinedButton(
                    onClick = onOpenReceiveDirectory,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                ) { Text("打开 ${ReceiveDirectory.RELATIVE_PATH}") }
            }
            if (state.stage == TransferStage.COMPLETE || state.stage == TransferStage.FAILED || state.stage == TransferStage.CANCELLED) {
                OutlinedButton(
                    onClick = {
                        AppState.transfer.value = TransferUiState()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                ) { Text("关闭状态") }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(label, color = Muted, modifier = Modifier.weight(1f))
        Text(value, fontWeight = FontWeight.Medium)
    }
}

private fun fileCountLabel(state: TransferUiState): String = if (state.totalFiles > 0) {
    "${state.completedFiles.coerceAtMost(state.totalFiles)} / ${state.totalFiles} 个文件"
} else ""

private fun statusSummary(state: TransferUiState): String = when (state.stage) {
    TransferStage.TRANSFERRING -> listOf(speedLabel(state.bytesPerSecond), etaLabel(state.etaSeconds))
        .filter { it.isNotBlank() }.joinToString(" · ")
    else -> state.message.ifBlank { stageLabel(state.stage) }
}

private fun stageLabel(stage: TransferStage): String = when (stage) {
    TransferStage.IDLE -> "等待操作"
    TransferStage.PREPARING -> "准备中"
    TransferStage.WAITING_APPROVAL -> "等待接收确认"
    TransferStage.TRANSFERRING -> "传输中"
    TransferStage.VERIFYING -> "校验中"
    TransferStage.COMPLETE -> "已完成"
    TransferStage.FAILED -> "传输失败"
    TransferStage.CANCELLED -> "已取消"
}

private fun stageColor(stage: TransferStage): Color = when (stage) {
    TransferStage.COMPLETE -> Color(0xFF15803D)
    TransferStage.FAILED, TransferStage.CANCELLED -> Color(0xFFB91C1C)
    else -> LanBlue
}
