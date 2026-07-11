package com.simplestsoft.twostrokecalc.ui.util

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.imeNestedScroll
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
fun Modifier.keyboardAwareScroll(scrollState: ScrollState): Modifier =
    imePadding()
        .verticalScroll(scrollState)
        .imeNestedScroll()
