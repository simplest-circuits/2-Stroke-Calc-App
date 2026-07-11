package com.simplestsoft.twostrokecalc.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.simplestsoft.twostrokecalc.R
import com.simplestsoft.twostrokecalc.domain.model.CalculatorId
import com.simplestsoft.twostrokecalc.domain.model.ProModuleId
import com.simplestsoft.twostrokecalc.domain.model.remote.AdminSettingsDto
import com.simplestsoft.twostrokecalc.ui.calculator.proModuleAdminOrder
import com.simplestsoft.twostrokecalc.ui.calculator.calculatorDisplayOrder
import com.simplestsoft.twostrokecalc.ui.calculator.tabLabelRes
import com.simplestsoft.twostrokecalc.domain.model.remote.AdminUserDeviceDto
import com.simplestsoft.twostrokecalc.domain.model.remote.AdminUserDto
import com.simplestsoft.twostrokecalc.domain.model.remote.AdminUserVehicleDto
import com.simplestsoft.twostrokecalc.ui.admin.AdminUiState
import com.simplestsoft.twostrokecalc.ui.admin.AdminViewModel
import com.simplestsoft.twostrokecalc.ui.components.AppOutlinedTextField
import com.simplestsoft.twostrokecalc.ui.components.FormFieldDefaults
import com.simplestsoft.twostrokecalc.ui.util.LocalScreenInsets
import com.simplestsoft.twostrokecalc.ui.util.keyboardAwareScroll
import com.simplestsoft.twostrokecalc.ui.util.screenSystemBarPadding
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private data class AdminTab(val titleRes: Int, val icon: ImageVector)

@Composable
fun AdminPanelScreen(viewModel: AdminViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        AdminTab(R.string.users_tab, Icons.Default.Groups),
        AdminTab(R.string.statistics_tab, Icons.Default.Bolt),
        AdminTab(R.string.admin_settings_tab, Icons.Default.ToggleOn),
    )

    LaunchedEffect(tab) {
        when (tab) {
            0 -> viewModel.loadUsers()
            1 -> viewModel.loadStatistics()
            2 -> viewModel.loadSettings()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .screenSystemBarPadding()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        AdminHeroCard(totalUsers = state.statistics?.totalUsers ?: state.users.size, loading = state.loading)
        AdminTabBar(tabs = tabs, selectedIndex = tab, onSelected = { tab = it })
        AdminFeedback(error = state.error, message = state.message, loading = state.loading)
        Box(modifier = Modifier.weight(1f)) {
            when (tab) {
                0 -> AdminUsersTab(state, viewModel)
                1 -> AdminStatsTab(state, viewModel)
                2 -> AdminSettingsTab(state, viewModel)
            }
        }
    }
}

@Composable
private fun AdminHeroCard(totalUsers: Int, loading: Boolean) {
    val titleShownInTopBar = LocalScreenInsets.current.statusBarHandledByChrome
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(modifier = Modifier.size(40.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.AdminPanelSettings,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (!titleShownInTopBar) {
                    Text(
                        stringResource(R.string.admin_panel_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        stringResource(R.string.admin_panel_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.76f),
                    )
                }
                Text(
                    stringResource(R.string.admin_users_count, totalUsers),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Text(
                if (loading) stringResource(R.string.admin_loading_label) else stringResource(R.string.admin_ready_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
            )
        }
    }
}

@Composable
private fun AdminTabBar(tabs: List<AdminTab>, selectedIndex: Int, onSelected: (Int) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
        ScrollableTabRow(selectedTabIndex = selectedIndex, edgePadding = 8.dp, divider = {}) {
            tabs.forEachIndexed { index, adminTab ->
                Tab(
                    selected = selectedIndex == index,
                    onClick = { onSelected(index) },
                    modifier = Modifier.height(52.dp),
                    text = {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(adminTab.icon, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text(stringResource(adminTab.titleRes), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun AdminFeedback(error: String?, message: String?, loading: Boolean) {
    val text = error ?: message ?: return
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = if (error != null) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (error == null && loading) {
                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
            }
            Text(text, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun AdminUsersTab(state: AdminUiState, vm: AdminViewModel) {
    var selectedUserId by remember { mutableStateOf<String?>(null) }
    val selectedUser = state.users.firstOrNull { it.id == selectedUserId }
    Box(Modifier.fillMaxSize()) {
        if (selectedUser != null) {
            Column(
                modifier = Modifier.fillMaxSize().keyboardAwareScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { selectedUserId = null }
                        .padding(horizontal = 4.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        stringResource(R.string.settings_general_back),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                UserCard(selectedUser, vm, actionsEnabled = !state.loading)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(state.users, key = { it.id }) { user ->
                    UserListRow(user = user, onClick = { selectedUserId = user.id })
                }
            }
        }
        if (state.loading) CircularProgressIndicator(Modifier.align(Alignment.Center))
    }
}

@Composable
private fun UserListRow(user: AdminUserDto, onClick: () -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Person, contentDescription = null)
            Column(
                modifier = Modifier.weight(1f).padding(start = 12.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    user.displayName?.takeIf { it.isNotBlank() } ?: user.email ?: user.id,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    user.email ?: user.id,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                user.device?.appVersionName?.takeIf { it.isNotBlank() }?.let { version ->
                    Text(
                        text = stringResource(R.string.admin_user_device_app_version_short, version),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = stringResource(R.string.admin_user_vehicle_count, user.vehicleCount ?: 0),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(user.role ?: "USER", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun UserCard(user: AdminUserDto, vm: AdminViewModel, actionsEnabled: Boolean) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val nextRole = if (user.role == "ADMIN") "USER" else "ADMIN"
    val displayName = user.displayName?.takeIf { it.isNotBlank() } ?: user.email ?: user.id

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Surface(
                        modifier = Modifier.size(56.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(30.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Default.Email,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                user.email ?: user.id,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                AdminUserStatusBadges(user = user)
            }
        }

        AdminSectionCard(stringResource(R.string.admin_user_account_section), Icons.Default.AdminPanelSettings) {
            AdminUserDetailRow(
                label = stringResource(R.string.admin_role_label),
                value = user.role ?: "USER",
            )
            user.phoneNumber?.takeIf { it.isNotBlank() }?.let { phone ->
                AdminUserDetailRow(
                    label = stringResource(R.string.admin_user_phone_label),
                    value = phone,
                )
            }
            AdminUserDetailRow(
                label = stringResource(R.string.admin_user_email_verified_label),
                value = if (user.emailVerified == true) {
                    stringResource(R.string.admin_value_yes)
                } else {
                    stringResource(R.string.admin_value_no)
                },
            )
            AdminUserDetailRow(
                label = stringResource(R.string.admin_user_auth_disabled_label),
                value = if (user.authDisabled == true) {
                    stringResource(R.string.admin_value_yes)
                } else {
                    stringResource(R.string.admin_value_no)
                },
            )
            AdminUserDetailRow(
                label = stringResource(R.string.admin_user_providers_label),
                value = user.providerIds.orEmpty().takeIf { it.isNotEmpty() }?.joinToString(", ")
                    ?: stringResource(R.string.admin_user_unknown),
            )
            OutlinedButton(
                onClick = { vm.updateRole(user.id, nextRole) },
                enabled = actionsEnabled,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.admin_switch_role_to, nextRole))
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            AdminUserToggleRow(
                label = stringResource(R.string.admin_status_label),
                checked = user.active == true,
                onCheckedChange = { vm.setUserActive(user.id, it) },
                enabled = actionsEnabled,
            )
            AdminUserToggleRow(
                label = stringResource(R.string.admin_ban_status_label),
                checked = user.banned == true,
                onCheckedChange = { vm.setUserBanned(user.id, it) },
                enabled = actionsEnabled,
            )
            AdminUserToggleRow(
                label = stringResource(R.string.admin_pro_status_label),
                checked = user.isPro == true,
                onCheckedChange = { vm.setUserPro(user.id, it) },
                enabled = actionsEnabled,
            )
        }

        AdminSectionCard(stringResource(R.string.admin_user_stats_heading), Icons.Default.TwoWheeler) {
            AdminUserStatsSection(user = user)
        }

        AdminSectionCard(stringResource(R.string.admin_user_device_heading), Icons.Default.PhoneAndroid) {
            AdminUserDeviceInfoSection(device = user.device)
        }

        AdminSectionCard(stringResource(R.string.admin_user_actions_section), Icons.Default.LockReset) {
            OutlinedButton(
                onClick = { vm.resetPassword(user.id) },
                enabled = actionsEnabled,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.LockReset, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(
                    stringResource(R.string.admin_reset_password_action),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            OutlinedButton(
                onClick = { showDeleteDialog = true },
                enabled = actionsEnabled,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.error,
                )
                Text(
                    stringResource(R.string.admin_delete_user),
                    modifier = Modifier.padding(start = 8.dp),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.admin_delete_user)) },
            text = { Text(stringResource(R.string.admin_delete_user_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    vm.deleteUser(user.id)
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdminUserStatusBadges(user: AdminUserDto) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AdminUserStatusChip(
            label = user.role ?: "USER",
            color = if (user.role == "ADMIN") {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
        AdminUserStatusChip(
            label = if (user.active == true) {
                stringResource(R.string.admin_user_active)
            } else {
                stringResource(R.string.admin_user_inactive)
            },
            color = if (user.active == true) {
                MaterialTheme.colorScheme.tertiary
            } else {
                MaterialTheme.colorScheme.error
            },
        )
        if (user.banned == true) {
            AdminUserStatusChip(
                label = stringResource(R.string.admin_user_banned),
                color = MaterialTheme.colorScheme.error,
            )
        }
        AdminUserStatusChip(
            label = stringResource(
                if (user.isPro == true) R.string.pro_badge_label else R.string.free_badge_label,
            ),
            color = if (user.isPro == true) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

@Composable
private fun AdminUserStatusChip(label: String, color: Color) {
    Surface(shape = RoundedCornerShape(8.dp), color = color.copy(alpha = 0.12f)) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = color,
        )
    }
}

@Composable
private fun AdminUserDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

@Composable
private fun AdminUserToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

@Composable
private fun AdminUserDeviceInfoSection(device: AdminUserDeviceDto?) {
    if (device == null) {
        Text(
            stringResource(R.string.admin_user_device_unavailable),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }

    val modelLine = listOfNotNull(device.deviceManufacturer, device.deviceModel)
        .joinToString(" ")
        .takeIf { it.isNotBlank() }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        modelLine?.let {
            AdminUserDetailRow(
                label = stringResource(R.string.admin_device_label_model),
                value = it,
            )
        }
        device.androidVersion?.let { version ->
            val sdk = device.androidSdk?.toIntOrNull()
            AdminUserDetailRow(
                label = "Android",
                value = if (sdk != null) "$version (API $sdk)" else version,
            )
        }
        device.appVersionName?.let { versionName ->
            val versionCode = device.appVersionCode
            AdminUserDetailRow(
                label = "App",
                value = if (!versionCode.isNullOrBlank()) "$versionName ($versionCode)" else versionName,
            )
        }
        val width = device.screenWidthPx?.toIntOrNull()
        val height = device.screenHeightPx?.toIntOrNull()
        val dpi = device.screenDensityDpi?.toIntOrNull()
        if (width != null && height != null && dpi != null) {
            AdminUserDetailRow(
                label = "Display",
                value = "${width}x$height, $dpi dpi",
            )
        }
        device.locale?.takeIf { it.isNotBlank() }?.let {
            AdminUserDetailRow(label = "Locale", value = it)
        }
        device.buildType?.takeIf { it.isNotBlank() }?.let {
            AdminUserDetailRow(label = "Build", value = it)
        }
        AdminUserDetailRow(
            label = stringResource(R.string.admin_user_push_enabled_label),
            value = if (device.pushEnabled == true) {
                stringResource(R.string.admin_value_yes)
            } else {
                stringResource(R.string.admin_value_no)
            },
        )
        AdminUserDetailRow(
            label = stringResource(R.string.admin_user_push_token_label),
            value = if (device.hasFcmToken == true) {
                stringResource(R.string.admin_value_yes)
            } else {
                stringResource(R.string.admin_value_no)
            },
        )
        formatAdminTimestamp(device.updatedAt)?.let {
            AdminUserDetailRow(
                label = stringResource(R.string.admin_user_device_updated_label),
                value = it,
            )
        }
    }
}

@Composable
private fun AdminUserStatsSection(user: AdminUserDto) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        AdminUserDetailRow(
            label = stringResource(R.string.admin_user_vehicles_label),
            value = (user.vehicleCount ?: 0).toString(),
        )
        AdminUserVehiclePreviewSection(
            vehicles = user.vehicles.orEmpty(),
            totalCount = user.vehicleCount ?: 0,
        )
        formatAdminTimestamp(user.createdAt)?.let { createdAt ->
            AdminUserDetailRow(
                label = stringResource(R.string.admin_user_registered_label),
                value = createdAt,
            )
        }
        AdminUserDetailRow(
            label = stringResource(R.string.admin_user_last_sign_in_label),
            value = formatAdminTimestamp(user.lastSignInAt)
                ?: stringResource(R.string.admin_user_unknown),
        )
        formatAdminTimestamp(user.lastRefreshAt)?.let { lastRefresh ->
            AdminUserDetailRow(
                label = stringResource(R.string.admin_user_last_refresh_label),
                value = lastRefresh,
            )
        }
        formatAdminTimestamp(user.updatedAt)?.let { updatedAt ->
            AdminUserDetailRow(
                label = stringResource(R.string.admin_user_profile_updated_label),
                value = updatedAt,
            )
        }
        formatAdminTimestamp(user.lastPasswordResetAt)?.let { resetAt ->
            AdminUserDetailRow(
                label = stringResource(R.string.admin_user_password_reset_label),
                value = resetAt,
            )
        }
        formatAdminTimestamp(user.bannedAt)?.let { bannedAt ->
            AdminUserDetailRow(
                label = stringResource(R.string.admin_user_banned_at_label),
                value = bannedAt,
            )
        }
        user.proPurchase?.let { purchase ->
            purchase.productId?.takeIf { it.isNotBlank() }?.let { productId ->
                AdminUserDetailRow(
                    label = stringResource(R.string.admin_user_pro_product_label),
                    value = productId,
                )
            }
            purchase.orderId?.takeIf { it.isNotBlank() }?.let { orderId ->
                AdminUserDetailRow(
                    label = stringResource(R.string.admin_user_pro_order_label),
                    value = orderId,
                )
            }
            formatAdminTimestamp(purchase.verifiedAt)?.let { verifiedAt ->
                AdminUserDetailRow(
                    label = stringResource(R.string.admin_user_pro_verified_at_label),
                    value = verifiedAt,
                )
            }
        }
    }
}

@Composable
private fun AdminUserVehiclePreviewSection(vehicles: List<AdminUserVehicleDto>, totalCount: Int) {
    if (totalCount <= 0) return
    val preview = vehicles.take(5)
    if (preview.isEmpty()) {
        Text(
            text = stringResource(R.string.admin_user_vehicle_preview_unavailable),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = stringResource(R.string.admin_user_vehicle_preview_label),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
        )
        preview.forEachIndexed { index, vehicle ->
            val baseLabel = vehicle.name?.takeIf { it.isNotBlank() } ?: listOfNotNull(
                vehicle.brand?.takeIf { it.isNotBlank() },
                vehicle.model?.takeIf { it.isNotBlank() },
            ).joinToString(" ").ifBlank { stringResource(R.string.admin_user_unknown) }
            val details = buildList {
                vehicle.year?.takeIf { it.isNotBlank() }?.let { add(it) }
                vehicle.currentOdometerKm?.takeIf { it.isNotBlank() }?.let { add("$it km") }
                vehicle.currentOperatingHours?.takeIf { it.isNotBlank() }?.let { add("$it h") }
            }.joinToString(" · ")
            val line = if (details.isBlank()) {
                "${index + 1}. $baseLabel"
            } else {
                "${index + 1}. $baseLabel · $details"
            }
            Text(
                text = line,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (totalCount > preview.size) {
            Text(
                text = stringResource(
                    R.string.admin_user_vehicle_preview_more,
                    totalCount - preview.size,
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AdminStatsTab(state: AdminUiState, vm: AdminViewModel) {
    val stats = state.statistics
    Column(
        modifier = Modifier.fillMaxSize().keyboardAwareScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.statistics_tab), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            IconButton(onClick = { vm.loadStatistics() }) {
                Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.admin_refresh_statistics))
            }
        }
        CompactStatRow(stringResource(R.string.total_users), (stats?.totalUsers ?: 0).toString(), Icons.Default.Groups)
        CompactStatRow(stringResource(R.string.active_sessions), (stats?.activeSessions ?: 0).toString(), Icons.Default.Bolt)
        CompactStatRow(stringResource(R.string.new_today), (stats?.newToday ?: 0).toString(), Icons.Default.PersonAdd)
        CompactStatRow(stringResource(R.string.storage_usage), "${stats?.storageUsageMb ?: 0.0} MB", Icons.Default.Storage)
        CompactStatRow(stringResource(R.string.admin_admin_users), (stats?.adminUsers ?: 0).toString(), Icons.Default.AdminPanelSettings)
        CompactStatRow(stringResource(R.string.admin_banned_users), (stats?.bannedUsers ?: 0).toString(), Icons.Default.Delete)
        CompactStatRow(stringResource(R.string.admin_inactive_users), (stats?.inactiveUsers ?: 0).toString(), Icons.Default.ToggleOn)
    }
}

@Composable
private fun CompactStatRow(title: String, value: String, icon: ImageVector) {
    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Text(title, style = MaterialTheme.typography.bodyMedium)
                Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun AdminSettingsTab(state: AdminUiState, vm: AdminViewModel) {
    var maintenance by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf(false) }
    var demoVehiclesEnabled by remember { mutableStateOf(true) }
    var calculatorAvailability by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    var proModules by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    var pushTitle by remember { mutableStateOf("") }
    var pushBody by remember { mutableStateOf("") }

    state.settings?.let { settings ->
        LaunchedEffect(settings) {
            maintenance = settings.maintenanceMode
            email = settings.emailNotifications
            demoVehiclesEnabled = settings.demoVehiclesEnabled
            calculatorAvailability = defaultCalculatorAvailability(settings.calculatorAvailability)
            proModules = defaultProModules(settings.proModules)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().keyboardAwareScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        AdminSectionCard(stringResource(R.string.admin_settings_tab), Icons.Default.ToggleOn) {
            SettingSwitchRow(stringResource(R.string.maintenance_mode), maintenance) { maintenance = it }
            SettingSwitchRow(stringResource(R.string.email_notifications_enable), email) { email = it }
            SettingSwitchRow(stringResource(R.string.admin_demo_vehicles_global_toggle), demoVehiclesEnabled) {
                demoVehiclesEnabled = it
            }
            Button(
                onClick = {
                    vm.saveSettings(
                        AdminSettingsDto(
                            maintenanceMode = maintenance,
                            debugMode = state.settings?.debugMode ?: false,
                            emailNotifications = email,
                            demoVehiclesEnabled = demoVehiclesEnabled,
                            calculatorAvailability = calculatorAvailability,
                            proModules = proModules,
                        ),
                    )
                },
                enabled = !state.loading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.save))
            }
            OutlinedButton(onClick = { vm.triggerWalkthroughForCurrentDevice() }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.admin_trigger_walkthrough))
            }
        }
        AdminSectionCard(stringResource(R.string.admin_pro_modules_section_title), Icons.Default.Star) {
            Text(
                stringResource(R.string.admin_pro_modules_section_hint),
                style = MaterialTheme.typography.bodySmall,
            )
            proModuleAdminOrder.forEach { moduleId ->
                val requiresPro = proModules[moduleId.name] == true
                SettingSwitchRow(
                    label = stringResource(moduleId.labelRes()),
                    checked = requiresPro,
                ) { checked ->
                    proModules = proModules.toMutableMap().apply {
                        put(moduleId.name, checked)
                    }
                }
            }
            Button(
                onClick = {
                    vm.saveSettings(
                        AdminSettingsDto(
                            maintenanceMode = maintenance,
                            debugMode = state.settings?.debugMode ?: false,
                            emailNotifications = email,
                            demoVehiclesEnabled = demoVehiclesEnabled,
                            calculatorAvailability = calculatorAvailability,
                            proModules = proModules,
                        ),
                    )
                },
                enabled = !state.loading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.admin_pro_modules_save))
            }
        }
        AdminSectionCard(stringResource(R.string.admin_calculators_section_title), Icons.Default.Speed) {
            Text(
                stringResource(R.string.admin_calculators_section_hint),
                style = MaterialTheme.typography.bodySmall,
            )
            calculatorDisplayOrder.forEach { calculatorId ->
                val enabled = calculatorAvailability[calculatorId.name] != false
                SettingSwitchRow(
                    label = stringResource(calculatorId.tabLabelRes()),
                    checked = enabled,
                ) { checked ->
                    calculatorAvailability = calculatorAvailability.toMutableMap().apply {
                        put(calculatorId.name, checked)
                    }
                }
            }
            Button(
                onClick = {
                    vm.saveSettings(
                        AdminSettingsDto(
                            maintenanceMode = maintenance,
                            debugMode = state.settings?.debugMode ?: false,
                            emailNotifications = email,
                            demoVehiclesEnabled = demoVehiclesEnabled,
                            calculatorAvailability = calculatorAvailability,
                            proModules = proModules,
                        ),
                    )
                },
                enabled = !state.loading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.admin_calculators_save))
            }
        }
        AdminSectionCard(stringResource(R.string.admin_demo_vehicles_section_title), Icons.Default.TwoWheeler) {
            Text(stringResource(R.string.admin_demo_vehicles_section_hint), style = MaterialTheme.typography.bodySmall)
            OutlinedButton(
                onClick = { vm.seedDemoVehicles() },
                enabled = !state.loading && demoVehiclesEnabled,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.admin_demo_vehicles_create))
            }
        }
        AdminSectionCard(stringResource(R.string.admin_push_section_title), Icons.Default.Email) {
            Text(stringResource(R.string.admin_push_section_hint), style = MaterialTheme.typography.bodySmall)
            AppOutlinedTextField(
                value = pushTitle,
                onValueChange = { pushTitle = it },
                label = stringResource(R.string.admin_push_title_label),
            )
            AppOutlinedTextField(
                value = pushBody,
                onValueChange = { pushBody = it },
                label = stringResource(R.string.admin_push_body_label),
                singleLine = false,
                minLines = 2,
                maxLines = 6,
                compactHeight = false,
            )
            Button(onClick = { vm.sendPushNotification(pushTitle, pushBody) }, enabled = !state.loading, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.admin_push_send_all))
            }
        }
    }
}

@Composable
private fun AdminSectionCard(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(FormFieldDefaults.fieldSpacing)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null)
                Text(title, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 8.dp))
            }
            content()
        }
    }
}

@Composable
private fun SettingSwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private fun defaultCalculatorAvailability(stored: Map<String, Boolean>): Map<String, Boolean> =
    calculatorDisplayOrder.associate { id ->
        id.name to (stored[id.name] != false)
    }

private fun defaultProModules(stored: Map<String, Boolean>): Map<String, Boolean> =
    proModuleAdminOrder.associate { id ->
        id.name to when {
            stored.containsKey(id.name) -> stored[id.name] == true
            id in ProModuleId.defaultProModules -> true
            else -> false
        }
    }

private fun formatAdminTimestamp(raw: String?): String? {
    if (raw.isNullOrBlank()) return null
    val pattern = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm", Locale.getDefault())
    return runCatching {
        Instant.parse(raw).atZone(ZoneId.systemDefault()).format(pattern)
    }.getOrNull() ?: runCatching {
        ZonedDateTime.parse(raw, DateTimeFormatter.RFC_1123_DATE_TIME)
            .withZoneSameInstant(ZoneId.systemDefault())
            .format(pattern)
    }.getOrNull()
}
