package com.simplestsoft.twostrokecalc.platform

import kotlinx.datetime.Clock

actual fun currentTimeMillis(): Long = Clock.System.now().toEpochMilliseconds()
