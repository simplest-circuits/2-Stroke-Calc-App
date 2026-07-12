package com.simplestsoft.twostrokecalc

import com.simplestsoft.twostrokecalc.platform.getPlatform

class Greeting {
    private val platform = getPlatform()

    fun greet(): String = "Hello, ${platform.name}!"
}
