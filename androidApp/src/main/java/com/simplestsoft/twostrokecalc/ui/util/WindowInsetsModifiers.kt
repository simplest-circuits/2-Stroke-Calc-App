package com.simplestsoft.twostrokecalc.ui.util

import android.view.ViewTreeObserver
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

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

/**
 * IME visibility without reading Compose [androidx.compose.foundation.layout.WindowInsets.ime]
 * during composition.
 *
 * Reading animated IME inset values recomposes the whole tree on every keyboard frame and, when
 * combined with nested inset paddings, can explode into recursive [UnionInsets.equals] work (ANR).
 * This only flips when visibility changes.
 */
@Composable
fun rememberIsImeVisible(): Boolean {
    val view = LocalView.current
    var visible by remember(view) {
        mutableStateOf(
            ViewCompat.getRootWindowInsets(view)?.isVisible(WindowInsetsCompat.Type.ime()) == true,
        )
    }
    DisposableEffect(view) {
        val listener = ViewTreeObserver.OnGlobalLayoutListener {
            val next =
                ViewCompat.getRootWindowInsets(view)?.isVisible(WindowInsetsCompat.Type.ime()) == true
            if (visible != next) {
                visible = next
            }
        }
        view.viewTreeObserver.addOnGlobalLayoutListener(listener)
        onDispose {
            view.viewTreeObserver.removeOnGlobalLayoutListener(listener)
        }
    }
    return visible
}
