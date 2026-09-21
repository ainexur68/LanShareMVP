package com.example.lanshare.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

internal val LanBlue = Color(0xFF246BFD)
internal val Ink = Color(0xFF14233A)
internal val Muted = Color(0xFF66758C)
internal val AppBackground = Color(0xFFF6F8FC)
internal val SoftBlue = Color(0xFFEAF1FF)
internal val Divider = Color(0xFFE6EAF0)

@Composable
internal fun LanShareTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = LanBlue,
            onPrimary = Color.White,
            secondary = Color(0xFF0D7C74),
            background = AppBackground,
            surface = Color.White,
            surfaceVariant = SoftBlue,
            onSurface = Ink,
            onSurfaceVariant = Muted,
            outline = Divider
        ),
        content = content
    )
}
