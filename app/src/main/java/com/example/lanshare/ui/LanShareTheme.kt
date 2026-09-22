package com.example.lanshare.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

internal val LanBlue = Color(0xFF1677FF)
internal val LanBlueDark = Color(0xFF0D55B5)
internal val Ink = Color(0xFF07183D)
internal val Muted = Color(0xFF6B7A96)
internal val AppBackground = Color.White
internal val SoftBlue = Color(0xFFF3F7FD)
internal val PaleBlue = Color(0xFFE6F0FF)
internal val Divider = Color(0xFFDFE9F7)
internal val SuccessGreen = Color(0xFF1DAA68)

private val LanShareTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 42.sp,
        letterSpacing = (-0.6).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.3).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 34.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 17.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 15.sp,
        lineHeight = 21.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    )
)

@Composable
internal fun LanShareTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = LanBlue,
            onPrimary = Color.White,
            primaryContainer = PaleBlue,
            onPrimaryContainer = LanBlueDark,
            secondary = LanBlueDark,
            background = AppBackground,
            surface = Color.White,
            surfaceVariant = SoftBlue,
            onSurface = Ink,
            onSurfaceVariant = Muted,
            outline = Divider
        ),
        typography = LanShareTypography,
        content = content
    )
}
