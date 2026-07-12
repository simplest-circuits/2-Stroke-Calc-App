package com.simplestsoft.twostrokecalc.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object AppColors {
    @Composable
    fun isDarkTheme(): Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f

    @Composable
    fun background(): Color = MaterialTheme.colorScheme.background

    @Composable
    fun surface(): Color = MaterialTheme.colorScheme.surface

    @Composable
    fun surfaceElevated(): Color = MaterialTheme.colorScheme.surfaceVariant

    @Composable
    fun primaryBlue(): Color = MaterialTheme.colorScheme.primary

    @Composable
    fun textPrimary(): Color = MaterialTheme.colorScheme.onBackground

    @Composable
    fun textSecondary(): Color = MaterialTheme.colorScheme.onSurfaceVariant

    @Composable
    fun iconContainerBackground(): Color = MaterialTheme.colorScheme.primaryContainer

    @Composable
    fun iconContainerForeground(): Color = MaterialTheme.colorScheme.primary

    @Composable
    fun borderSubtle(): Color = MaterialTheme.colorScheme.outline.copy(
        alpha = if (isDarkTheme()) 0.55f else 0.28f,
    )

    @Composable
    fun settingsIconBackground(): Color = if (isDarkTheme()) {
        surfaceElevated()
    } else {
        iconContainerBackground()
    }

    @Composable
    fun accentSurfaceBackground(): Color = if (isDarkTheme()) {
        primaryBlue().copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }

    @Composable
    fun primaryGlowModifier(
        shape: Shape,
        modifier: Modifier = Modifier,
        elevation: Dp? = null,
        spotAlpha: Float? = null,
    ): Modifier {
        val glowElevation = elevation ?: if (isDarkTheme()) 8.dp else 4.dp
        val glowSpotAlpha = spotAlpha ?: if (isDarkTheme()) 0.42f else 0.24f
        val gold = primaryBlue()
        return modifier.shadow(
            elevation = glowElevation,
            shape = shape,
            spotColor = gold.copy(alpha = glowSpotAlpha),
            ambientColor = gold.copy(alpha = glowSpotAlpha * 0.45f),
            clip = false,
        )
    }

    @Composable
    fun calculatorIconGlowModifier(modifier: Modifier = Modifier): Modifier = primaryGlowModifier(
        shape = CircleShape,
        modifier = modifier,
        elevation = if (isDarkTheme()) 6.dp else 3.dp,
        spotAlpha = if (isDarkTheme()) 0.5f else 0.3f,
    )

    @Composable
    fun accentSurfaceModifier(
        shape: RoundedCornerShape,
        modifier: Modifier = Modifier,
    ): Modifier = primaryGlowModifier(
        shape = shape,
        modifier = modifier,
        elevation = if (isDarkTheme()) 10.dp else 5.dp,
        spotAlpha = if (isDarkTheme()) 0.5f else 0.3f,
    ).then(
        if (isDarkTheme()) {
            Modifier.border(1.dp, primaryBlue().copy(alpha = 0.35f), shape)
        } else {
            Modifier.border(1.dp, primaryBlue().copy(alpha = 0.22f), shape)
        },
    )

    @Composable
    fun cardBorderModifier(
        shape: RoundedCornerShape,
        modifier: Modifier = Modifier,
    ): Modifier = primaryGlowModifier(
        shape = shape,
        modifier = modifier,
        elevation = if (isDarkTheme()) 6.dp else 3.dp,
        spotAlpha = if (isDarkTheme()) 0.38f else 0.2f,
    ).border(
        width = 1.dp,
        color = if (isDarkTheme()) {
            borderSubtle()
        } else {
            primaryBlue().copy(alpha = 0.18f)
        },
        shape = shape,
    )
}
