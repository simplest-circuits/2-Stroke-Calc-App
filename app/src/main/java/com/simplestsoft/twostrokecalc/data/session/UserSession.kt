package com.simplestsoft.twostrokecalc.data.session

import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserSession @Inject constructor() {
    private val userIdRef = AtomicReference<String?>(null)

    fun setUserId(id: String?) {
        userIdRef.set(id)
    }

    fun getUserId(): String? = userIdRef.get()
}
