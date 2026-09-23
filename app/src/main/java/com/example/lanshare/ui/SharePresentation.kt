package com.example.lanshare.ui

import com.example.lanshare.SharedFile

internal enum class ShareLayoutState {
    EMPTY,
    SELECTED
}

internal fun shareLayoutState(fileCount: Int): ShareLayoutState =
    if (fileCount > 0) ShareLayoutState.SELECTED else ShareLayoutState.EMPTY

internal fun shareActionLabel(fileCount: Int, peerAlias: String?): String = when {
    peerAlias.isNullOrBlank() -> "选择设备"
    fileCount > 0 -> "发送 $fileCount 个文件"
    else -> "发送到 $peerAlias"
}

internal fun shareTransferSummary(files: List<SharedFile>): String = shareTransferSummary(
    totalSize = files.sumOf { it.size.coerceAtLeast(0) },
    hasUnknownSize = files.any { it.size < 0 }
)

internal fun shareTransferSummary(totalSize: Long, hasUnknownSize: Boolean): String {
    val sizeLabel = if (hasUnknownSize) {
        "大小未知"
    } else {
        humanSize(totalSize)
    }
    return "共 $sizeLabel · 局域网传输"
}

internal data class ShareFooterSpec(
    val showSummary: Boolean,
    val topSpacingDp: Int,
    val summarySlotHeightDp: Int,
    val bottomSpacingDp: Int
)

internal fun shareFooterSpec(fileCount: Int): ShareFooterSpec = ShareFooterSpec(
    showSummary = fileCount > 0,
    topSpacingDp = 28,
    summarySlotHeightDp = 32,
    bottomSpacingDp = 10
)

internal fun shareContentNeedsScroll(expanded: Boolean, availableHeightDp: Int): Boolean =
    expanded && availableHeightDp < 520
