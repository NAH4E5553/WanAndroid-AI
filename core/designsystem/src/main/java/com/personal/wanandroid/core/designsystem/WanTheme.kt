package com.personal.wanandroid.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Adapted from CoolMall's visual tokens. See NOTICE.md.
object WanSpacing {
    val small = 8.dp
    val medium = 12.dp
    val page = 16.dp
    val section = 24.dp
}

private val LightColors = lightColorScheme(
    primary = Color(0xFF465CFF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE6EAFF),
    onPrimaryContainer = Color(0xFF132984),
    background = Color(0xFFF1F4FA),
    onBackground = Color(0xFF181818),
    surface = Color.White,
    onSurface = Color(0xFF181818),
    surfaceVariant = Color(0xFFE8ECF5),
    onSurfaceVariant = Color(0xFF515B70)
)
private val DarkColors = darkColorScheme(
    primary = Color(0xFFBAC3FF),
    onPrimary = Color(0xFF122784),
    primaryContainer = Color(0xFF293A9E),
    onPrimaryContainer = Color(0xFFE0E4FF),
    background = Color(0xFF111111),
    onBackground = Color(0xFFE4E4E8),
    surface = Color(0xFF1B1B1B),
    onSurface = Color(0xFFE4E4E8),
    surfaceVariant = Color(0xFF292D39),
    onSurfaceVariant = Color(0xFFBCC3D4)
)

@Composable
fun WanTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = Typography(),
        shapes = Shapes(
            small = RoundedCornerShape(8.dp),
            medium = RoundedCornerShape(12.dp),
            large = RoundedCornerShape(24.dp)
        ),
        content = content
    )
}
