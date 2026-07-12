package com.simplestsoft.twostrokecalc.ui.permissions

object FirstInstallPermissionsPolicy {

    fun isNotificationStepNeeded(canPostNotifications: Boolean): Boolean = !canPostNotifications

    fun isFlowNeeded(canPostNotifications: Boolean): Boolean =
        isNotificationStepNeeded(canPostNotifications)
}
