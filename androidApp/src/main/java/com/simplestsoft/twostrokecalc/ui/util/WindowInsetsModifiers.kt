package com.simplestsoft.twostrokecalc.ui.util

import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier

data class ScreenInsetsConfig(
    val statusBarHandledByChrome: Boolean = false,
    val navigationBarHandledByChrome: Boolean = false,
)

val LocalScreenInsets = compositionLocalOf { ScreenInsetsConfig() }

fun Modifier.edgeToEdgeContentPadding(
    applyStatusBar: Boolean = true,
    applyNavigationBar: Boolean = true,
): Modifier = this
    .then(if (applyStatusBar) Modifier.statusBarsPadding() else Modifier)
    .then(if (applyNavigationBar) Modifier.navigationBarsPadding() else Modifier)

@Composable
fun Modifier.screenSystemBarPadding(): Modifier {
    val config = LocalScreenInsets.current
    return edgeToEdgeContentPadding(
        applyStatusBar = !config.statusBarHandledByChrome,
        applyNavigationBar = !config.navigationBarHandledByChrome,
    )
}
