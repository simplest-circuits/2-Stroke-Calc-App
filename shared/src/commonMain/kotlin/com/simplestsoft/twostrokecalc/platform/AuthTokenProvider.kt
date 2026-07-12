package com.simplestsoft.twostrokecalc.platform

expect suspend fun fetchAuthIdToken(forceRefresh: Boolean = false): String?

expect fun currentAuthUserId(): String?
