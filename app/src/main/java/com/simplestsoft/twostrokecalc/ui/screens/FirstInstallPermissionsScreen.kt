package com.simplestsoft.twostrokecalc.ui.screens

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.simplestsoft.twostrokecalc.R
import com.simplestsoft.twostrokecalc.ui.components.AppColors
import com.simplestsoft.twostrokecalc.ui.notifications.NotificationViewModel
import com.simplestsoft.twostrokecalc.ui.util.findComponentActivity

@Composable
fun FirstInstallPermissionsScreen(
    onComplete: () -> Unit,
    notificationViewModel: NotificationViewModel = hiltViewModel(
        LocalContext.current.findComponentActivity() ?: error("Activity required"),
    ),
) {
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        notificationViewModel.onPermissionResult(granted)
        onComplete()
    }

    fun finishNotificationsStep(granted: Boolean) {
        notificationViewModel.onPermissionResult(granted)
        onComplete()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.background())
            .padding(horizontal = 28.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        PermissionPromptContent(
            icon = Icons.Default.Notifications,
            title = stringResource(R.string.first_install_notifications_title),
            body = stringResource(R.string.first_install_notifications_body),
            stepLabel = stringResource(
                R.string.first_install_step_counter,
                1,
                1,
            ),
            allowLabel = stringResource(R.string.first_install_allow),
            skipLabel = stringResource(R.string.first_install_skip),
            onAllow = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(
                        android.Manifest.permission.POST_NOTIFICATIONS,
                    )
                } else {
                    finishNotificationsStep(granted = true)
                }
            },
            onSkip = {
                finishNotificationsStep(granted = false)
            },
        )
    }
}

@Composable
private fun PermissionPromptContent(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    body: String,
    stepLabel: String,
    allowLabel: String,
    skipLabel: String,
    onAllow: () -> Unit,
    onSkip: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp),
        )
        Text(
            text = stepLabel,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 20.dp),
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onAllow,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(6.dp),
        ) {
            Text(allowLabel)
        }
        TextButton(
            onClick = onSkip,
            modifier = Modifier.padding(top = 8.dp),
        ) {
            Text(skipLabel)
        }
    }
}
