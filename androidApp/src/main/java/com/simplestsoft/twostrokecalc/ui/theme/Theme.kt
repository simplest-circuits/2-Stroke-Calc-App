package com.simplestsoft.twostrokecalc.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.simplestsoft.twostrokecalc.domain.model.ThemeMode

private val AppMaterialShapes = Shapes(
    extraSmall = RoundedCornerShape(ContainerCornerRadius),
    small = RoundedCornerShape(ContainerCornerRadius),
    medium = RoundedCornerShape(ContainerCornerRadius),
    large = RoundedCornerShape(ContainerCornerRadius),
    extraLarge = RoundedCornerShape(ContainerCornerRadius),
)

private val Gold = Color(0xFFD4AF37)
private val BrightGold = Color(0xFFE8C547)

private val LightColors = lightColorScheme(
    primary = Gold,
    onPrimary = Color(0xFF1F1708),
    primaryContainer = Color(0xFFFFF3D6),
    onPrimaryContainer = Color(0xFF4A3808),
    secondary = Color(0xFFA67C52),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF0E4D4),
    onSecondaryContainer = Color(0xFF3D2E1F),
    tertiary = Color(0xFFB8860B),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFECB8),
    onTertiaryContainer = Color(0xFF5C4308),
    background = Color(0xFFFAF7F2),
    onBackground = Color(0xFF1F1B16),
    surface = Color(0xFFFFFDF9),
    onSurface = Color(0xFF1F1B16),
    surfaceVariant = Color(0xFFF0EBE3),
    onSurfaceVariant = Color(0xFF5C5548),
    outline = Color(0xFFC4B8A8),
    outlineVariant = Color(0xFFDDD5C8),
)

private val DarkColors = darkColorScheme(
    primary = BrightGold,
    onPrimary = Color(0xFF1A1508),
    primaryContainer = Color(0xFF4F3D1A),
    onPrimaryContainer = Color(0xFFF5E6B8),
    secondary = Color(0xFFC4A882),
    onSecondary = Color(0xFF1A1508),
    secondaryContainer = Color(0xFF423725),
    onSecondaryContainer = Color(0xFFE8D8B8),
    tertiary = Color(0xFFF0D060),
    onTertiary = Color(0xFF1A1508),
    tertiaryContainer = Color(0xFF4F4525),
    onTertiaryContainer = Color(0xFFF5E8B8),
    background = Color(0xFF0F0D0A),
    onBackground = Color(0xFFF5F0E8),
    surface = Color(0xFF1F1D19),
    onSurface = Color(0xFFF5F0E8),
    surfaceVariant = Color(0xFF2F2B25),
    onSurfaceVariant = Color(0xFFB8AFA0),
    outline = Color(0xFF5C5548),
    outlineVariant = Color(0xFF423D35),
)

@Composable
fun AppTheme(
    themeMode: ThemeMode,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colorScheme = if (dark) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppMaterialShapes,
        content = content,
    )
}
