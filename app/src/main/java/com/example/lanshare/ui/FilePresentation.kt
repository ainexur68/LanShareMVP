package com.example.lanshare.ui

internal enum class FileKind {
    IMAGE,
    VIDEO,
    PDF,
    GENERIC
}

internal fun fileKindFor(name: String, mime: String?): FileKind {
    val normalizedMime = mime.orEmpty().lowercase()
    val extension = name.substringAfterLast('.', missingDelimiterValue = "").lowercase()
    return when {
        normalizedMime.startsWith("image/") || extension in setOf("jpg", "jpeg", "png", "gif", "webp", "heic") -> FileKind.IMAGE
        normalizedMime.startsWith("video/") || extension in setOf("mp4", "mov", "mkv", "webm", "avi") -> FileKind.VIDEO
        normalizedMime == "application/pdf" || extension == "pdf" -> FileKind.PDF
        else -> FileKind.GENERIC
    }
}

internal fun fileQueueSummary(count: Int, totalBytes: Long): String =
    "$count 个文件 · ${humanSize(totalBytes.coerceAtLeast(0))}"

internal fun fileDialogHeightDp(fileCount: Int): Int {
    val listHeight = (fileCount.coerceAtLeast(1) * 52).coerceAtMost(390)
    return listHeight + 132
}
