package com.example.lanshare.ui

import java.util.Locale

internal fun humanSize(bytes: Long): String = when {
    bytes < 0 -> "未知大小"
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
    bytes < 1024L * 1024 * 1024 -> String.format(Locale.US, "%.1f MB", bytes / 1024.0 / 1024.0)
    else -> String.format(Locale.US, "%.2f GB", bytes / 1024.0 / 1024.0 / 1024.0)
}

internal fun speedLabel(bytesPerSecond: Long): String = if (bytesPerSecond <= 0) {
    "正在测速"
} else {
    "${humanSize(bytesPerSecond)}/s"
}

internal fun etaLabel(seconds: Long?): String = when {
    seconds == null -> ""
    seconds < 60 -> "约 ${seconds.coerceAtLeast(1)} 秒"
    seconds < 3600 -> "约 ${(seconds + 59) / 60} 分钟"
    else -> "约 ${(seconds + 3599) / 3600} 小时"
}
