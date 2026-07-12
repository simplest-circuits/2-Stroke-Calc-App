package com.simplestsoft.twostrokecalc.ui.pro

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf

val LocalCalculatorEditingEnabled = staticCompositionLocalOf { true }

val LocalVehicleEditingEnabled = staticCompositionLocalOf { true }

fun requireProEdit(
    canEdit: Boolean,
    onEditLocked: () -> Unit,
    action: () -> Unit,
) {
    if (canEdit) {
        action()
    } else {
        onEditLocked()
    }
}
