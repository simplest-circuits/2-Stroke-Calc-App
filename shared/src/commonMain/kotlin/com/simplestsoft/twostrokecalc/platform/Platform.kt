package com.simplestsoft.twostrokecalc.platform

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
