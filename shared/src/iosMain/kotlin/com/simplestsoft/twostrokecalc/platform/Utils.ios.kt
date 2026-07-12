package com.simplestsoft.twostrokecalc.platform

import platform.Foundation.NSUUID

actual fun randomUUID(): String = NSUUID().UUIDString()
