package com.personal.wanandroid.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
fun WanTheme(
    palette: WanPalette = WanPalette.SLATE_BLUE,
    dark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = remember(palette, dark) { wanColorScheme(palette, dark) }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = WanTypography,
        shapes = WanShapes,
        content = content
    )
}
