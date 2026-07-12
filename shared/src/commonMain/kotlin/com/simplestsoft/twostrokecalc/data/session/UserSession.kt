package com.simplestsoft.twostrokecalc.data.session

class UserSession {
    @Volatile
    private var userId: String? = null

    fun setUserId(id: String?) {
        userId = id
    }

    fun getUserId(): String? = userId
}
