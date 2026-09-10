package com.personal.wanandroid.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object WanSpacing {
    val small = 8.dp
    val medium = 12.dp
    val page = 16.dp
    val section = 24.dp
}

enum class WanPalette { INK_TEAL, SLATE_BLUE, WARM_AMBER, BERRY_ROSE }

@Immutable
data class WanPaletteSwatches(val primary: Color, val secondary: Color, val container: Color)

private data class AccentRoles(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondary: Color,
    val onSecondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val tertiary: Color,
    val onTertiary: Color,
    val tertiaryContainer: Color,
    val onTertiaryContainer: Color
)

private data class PaletteDefinition(val light: AccentRoles, val dark: AccentRoles)

private val InkTeal = PaletteDefinition(
    light = AccentRoles(
        Color(0xFF006B5B), Color.White, Color(0xFF8EF7DC), Color(0xFF00201A),
        Color(0xFF4A635C), Color.White, Color(0xFFCCE8DF), Color(0xFF06201A),
        Color(0xFF456179), Color.White, Color(0xFFCCE5FF), Color(0xFF001E30)
    ),
    dark = AccentRoles(
        Color(0xFF82D5C2), Color(0xFF00382F), Color(0xFF005045), Color(0xFFA3F2DD),
        Color(0xFFB0CCC3), Color(0xFF1C352F), Color(0xFF334C46), Color(0xFFCCE8DF),
        Color(0xFFACCAE5), Color(0xFF143348), Color(0xFF2C4960), Color(0xFFCCE5FF)
    )
)

private val SlateBlue = PaletteDefinition(
    light = AccentRoles(
        Color(0xFF315F84), Color.White, Color(0xFFCEE5FF), Color(0xFF0B1D2A),
        Color(0xFF50606F), Color.White, Color(0xFFD3E5F6), Color(0xFF0D1D29),
        Color(0xFF65587A), Color.White, Color(0xFFEBDCFF), Color(0xFF201A2D)
    ),
    dark = AccentRoles(
        Color(0xFF9CCAFA), Color(0xFF003353), Color(0xFF164766), Color(0xFFCEE5FF),
        Color(0xFFB7C9D9), Color(0xFF22323F), Color(0xFF394956), Color(0xFFD3E5F6),
        Color(0xFFCEC0E8), Color(0xFF362B49), Color(0xFF4D4261), Color(0xFFEBDCFF)
    )
)

private val WarmAmber = PaletteDefinition(
    light = AccentRoles(
        Color(0xFF795900), Color.White, Color(0xFFFFDF90), Color(0xFF261A00),
        Color(0xFF6B5D3F), Color.White, Color(0xFFF4E0BB), Color(0xFF241A04),
        Color(0xFF4B6548), Color.White, Color(0xFFCDEBC5), Color(0xFF09210A)
    ),
    dark = AccentRoles(
        Color(0xFFEFC15C), Color(0xFF402D00), Color(0xFF5C4300), Color(0xFFFFDF90),
        Color(0xFFD7C4A0), Color(0xFF3B2F15), Color(0xFF52462A), Color(0xFFF4E0BB),
        Color(0xFFB1CFA9), Color(0xFF1E371E), Color(0xFF344D33), Color(0xFFCDEBC5)
    )
)

private val BerryRose = PaletteDefinition(
    light = AccentRoles(
        Color(0xFF8A3F62), Color.White, Color(0xFFFFD9E5), Color(0xFF3A071F),
        Color(0xFF74565F), Color.White, Color(0xFFFFD9E1), Color(0xFF2B151C),
        Color(0xFF7C5635), Color.White, Color(0xFFFFDCC0), Color(0xFF2E1500)
    ),
    dark = AccentRoles(
        Color(0xFFFFAFCE), Color(0xFF53112F), Color(0xFF6D2949), Color(0xFFFFD9E5),
        Color(0xFFE3BDC7), Color(0xFF422931), Color(0xFF5A3F47), Color(0xFFFFD9E1),
        Color(0xFFEFBF94), Color(0xFF48290C), Color(0xFF623F20), Color(0xFFFFDCC0)
    )
)

private fun WanPalette.definition(): PaletteDefinition = when (this) {
    WanPalette.INK_TEAL -> InkTeal
    WanPalette.SLATE_BLUE -> SlateBlue
    WanPalette.WARM_AMBER -> WarmAmber
    WanPalette.BERRY_ROSE -> BerryRose
}

fun wanColorScheme(palette: WanPalette, dark: Boolean): ColorScheme {
    val definition = palette.definition()
    return if (dark) darkScheme(definition) else lightScheme(definition)
}

fun WanPalette.swatches(dark: Boolean): WanPaletteSwatches {
    val scheme = wanColorScheme(this, dark)
    return WanPaletteSwatches(scheme.primary, scheme.secondary, scheme.primaryContainer)
}

private fun lightScheme(definition: PaletteDefinition): ColorScheme {
    val accent = definition.light
    val dark = definition.dark
    return lightColorScheme(
        primary = accent.primary,
        onPrimary = accent.onPrimary,
        primaryContainer = accent.primaryContainer,
        onPrimaryContainer = accent.onPrimaryContainer,
        inversePrimary = dark.primary,
        secondary = accent.secondary,
        onSecondary = accent.onSecondary,
        secondaryContainer = accent.secondaryContainer,
        onSecondaryContainer = accent.onSecondaryContainer,
        tertiary = accent.tertiary,
        onTertiary = accent.onTertiary,
        tertiaryContainer = accent.tertiaryContainer,
        onTertiaryContainer = accent.onTertiaryContainer,
        background = LightNeutral.background,
        onBackground = LightNeutral.onBackground,
        surface = LightNeutral.surface,
        onSurface = LightNeutral.onSurface,
        surfaceVariant = LightNeutral.surfaceVariant,
        onSurfaceVariant = LightNeutral.onSurfaceVariant,
        surfaceTint = accent.primary,
        inverseSurface = LightNeutral.inverseSurface,
        inverseOnSurface = LightNeutral.inverseOnSurface,
        error = LightNeutral.error,
        onError = LightNeutral.onError,
        errorContainer = LightNeutral.errorContainer,
        onErrorContainer = LightNeutral.onErrorContainer,
        outline = LightNeutral.outline,
        outlineVariant = LightNeutral.outlineVariant,
        scrim = Color.Black,
        surfaceBright = LightNeutral.surfaceBright,
        surfaceContainer = LightNeutral.surfaceContainer,
        surfaceContainerHigh = LightNeutral.surfaceContainerHigh,
        surfaceContainerHighest = LightNeutral.surfaceContainerHighest,
        surfaceContainerLow = LightNeutral.surfaceContainerLow,
        surfaceContainerLowest = LightNeutral.surfaceContainerLowest,
        surfaceDim = LightNeutral.surfaceDim,
        primaryFixed = accent.primaryContainer,
        primaryFixedDim = dark.primary,
        onPrimaryFixed = accent.onPrimaryContainer,
        onPrimaryFixedVariant = dark.primaryContainer,
        secondaryFixed = accent.secondaryContainer,
        secondaryFixedDim = dark.secondary,
        onSecondaryFixed = accent.onSecondaryContainer,
        onSecondaryFixedVariant = dark.secondaryContainer,
        tertiaryFixed = accent.tertiaryContainer,
        tertiaryFixedDim = dark.tertiary,
        onTertiaryFixed = accent.onTertiaryContainer,
        onTertiaryFixedVariant = dark.tertiaryContainer
    )
}

private fun darkScheme(definition: PaletteDefinition): ColorScheme {
    val accent = definition.dark
    val light = definition.light
    return darkColorScheme(
        primary = accent.primary,
        onPrimary = accent.onPrimary,
        primaryContainer = accent.primaryContainer,
        onPrimaryContainer = accent.onPrimaryContainer,
        inversePrimary = light.primary,
        secondary = accent.secondary,
        onSecondary = accent.onSecondary,
        secondaryContainer = accent.secondaryContainer,
        onSecondaryContainer = accent.onSecondaryContainer,
        tertiary = accent.tertiary,
        onTertiary = accent.onTertiary,
        tertiaryContainer = accent.tertiaryContainer,
        onTertiaryContainer = accent.onTertiaryContainer,
        background = DarkNeutral.background,
        onBackground = DarkNeutral.onBackground,
        surface = DarkNeutral.surface,
        onSurface = DarkNeutral.onSurface,
        surfaceVariant = DarkNeutral.surfaceVariant,
        onSurfaceVariant = DarkNeutral.onSurfaceVariant,
        surfaceTint = accent.primary,
        inverseSurface = DarkNeutral.inverseSurface,
        inverseOnSurface = DarkNeutral.inverseOnSurface,
        error = DarkNeutral.error,
        onError = DarkNeutral.onError,
        errorContainer = DarkNeutral.errorContainer,
        onErrorContainer = DarkNeutral.onErrorContainer,
        outline = DarkNeutral.outline,
        outlineVariant = DarkNeutral.outlineVariant,
        scrim = Color.Black,
        surfaceBright = DarkNeutral.surfaceBright,
        surfaceContainer = DarkNeutral.surfaceContainer,
        surfaceContainerHigh = DarkNeutral.surfaceContainerHigh,
        surfaceContainerHighest = DarkNeutral.surfaceContainerHighest,
        surfaceContainerLow = DarkNeutral.surfaceContainerLow,
        surfaceContainerLowest = DarkNeutral.surfaceContainerLowest,
        surfaceDim = DarkNeutral.surfaceDim,
        primaryFixed = light.primaryContainer,
        primaryFixedDim = accent.primary,
        onPrimaryFixed = light.onPrimaryContainer,
        onPrimaryFixedVariant = accent.primaryContainer,
        secondaryFixed = light.secondaryContainer,
        secondaryFixedDim = accent.secondary,
        onSecondaryFixed = light.onSecondaryContainer,
        onSecondaryFixedVariant = accent.secondaryContainer,
        tertiaryFixed = light.tertiaryContainer,
        tertiaryFixedDim = accent.tertiary,
        onTertiaryFixed = light.onTertiaryContainer,
        onTertiaryFixedVariant = accent.tertiaryContainer
    )
}

private data class NeutralRoles(
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val onSurface: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color,
    val inverseSurface: Color,
    val inverseOnSurface: Color,
    val error: Color,
    val onError: Color,
    val errorContainer: Color,
    val onErrorContainer: Color,
    val outline: Color,
    val outlineVariant: Color,
    val surfaceBright: Color,
    val surfaceContainer: Color,
    val surfaceContainerHigh: Color,
    val surfaceContainerHighest: Color,
    val surfaceContainerLow: Color,
    val surfaceContainerLowest: Color,
    val surfaceDim: Color
)

private val LightNeutral = NeutralRoles(
    Color(0xFFF7F9FC), Color(0xFF191C1F), Color(0xFFF7F9FC), Color(0xFF191C1F),
    Color(0xFFDEE3E8), Color(0xFF42474D), Color(0xFF2E3135), Color(0xFFEFF1F5),
    Color(0xFFBA1A1A), Color.White, Color(0xFFFFDAD6), Color(0xFF410002),
    Color(0xFF72777D), Color(0xFFC2C7CD), Color(0xFFF7F9FC), Color(0xFFEBEEF2),
    Color(0xFFE5E9EC), Color(0xFFDFE3E7), Color(0xFFF1F4F7), Color.White,
    Color(0xFFD8DADF)
)

private val DarkNeutral = NeutralRoles(
    Color(0xFF111417), Color(0xFFE1E2E6), Color(0xFF111417), Color(0xFFE1E2E6),
    Color(0xFF42474D), Color(0xFFC2C7CD), Color(0xFFE1E2E6), Color(0xFF2E3135),
    Color(0xFFFFB4AB), Color(0xFF690005), Color(0xFF93000A), Color(0xFFFFDAD6),
    Color(0xFF8C9197), Color(0xFF42474D), Color(0xFF37393D), Color(0xFF1D2024),
    Color(0xFF282A2E), Color(0xFF333539), Color(0xFF191C20), Color(0xFF0C0F12),
    Color(0xFF111417)
)

@Composable
fun WanTheme(
    palette: WanPalette = WanPalette.SLATE_BLUE,
    dark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = wanColorScheme(palette, dark),
        typography = Typography(),
        shapes = Shapes(
            small = RoundedCornerShape(8.dp),
            medium = RoundedCornerShape(12.dp),
            large = RoundedCornerShape(24.dp)
        ),
        content = content
    )
}
