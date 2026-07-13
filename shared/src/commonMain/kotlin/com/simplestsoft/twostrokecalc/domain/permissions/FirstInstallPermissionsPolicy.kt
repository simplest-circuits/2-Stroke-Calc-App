package com.simplestsoft.twostrokecalc.domain.permissions

object FirstInstallPermissionsPolicy {
    fun isNotificationStepNeeded(canPostNotifications: Boolean): Boolean = !canPostNotifications

    fun isFlowNeeded(canPostNotifications: Boolean): Boolean =
        isNotificationStepNeeded(canPostNotifications)
}
