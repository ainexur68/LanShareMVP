package com.example.lanshare.ui

/**
 * The reference screen keeps both connection choices compact and visually equivalent.
 * The final size is still clamped by the available pane in the composable.
 */
internal fun connectionCardSizeDp(expanded: Boolean): Int = if (expanded) 320 else 244

@Suppress("UNUSED_PARAMETER")
internal fun initialScannerEnabled(cameraPermissionGranted: Boolean): Boolean = false
