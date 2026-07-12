package com.simplestsoft.twostrokecalc.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simplestsoft.twostrokecalc.data.preferences.PreferencesManager
import com.simplestsoft.twostrokecalc.data.sync.UserSettingsFirestoreSync
import com.simplestsoft.twostrokecalc.notifications.MaintenanceNotificationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val notificationHelper: MaintenanceNotificationHelper,
    private val preferencesManager: PreferencesManager,
    private val userSettingsFirestoreSync: UserSettingsFirestoreSync,
) : ViewModel() {

    private val _permissionGranted = MutableStateFlow(notificationHelper.canPost())
    val permissionGranted: StateFlow<Boolean> = _permissionGranted

    fun refreshPermissionState() {
        _permissionGranted.value = notificationHelper.canPost()
    }

    fun onPermissionResult(granted: Boolean) {
        refreshPermissionState()
        viewModelScope.launch {
            preferencesManager.setNotificationsEnabled(granted)
            userSettingsFirestoreSync.persistNotificationsEnabled(granted)
        }
    }

    fun syncPushStateAfterSettingsReturn() {
        refreshPermissionState()
        viewModelScope.launch {
            val granted = notificationHelper.canPost()
            preferencesManager.setNotificationsEnabled(granted)
            userSettingsFirestoreSync.persistNotificationsEnabled(granted)
        }
    }
}
