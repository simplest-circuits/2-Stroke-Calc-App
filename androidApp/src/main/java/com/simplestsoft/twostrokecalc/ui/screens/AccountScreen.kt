package com.simplestsoft.twostrokecalc.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.simplestsoft.twostrokecalc.R
import com.simplestsoft.twostrokecalc.ui.account.AccountViewModel
import com.simplestsoft.twostrokecalc.ui.components.AppColors
import com.simplestsoft.twostrokecalc.ui.components.AppOutlinedTextField
import com.simplestsoft.twostrokecalc.ui.components.FormFieldDefaults
import com.simplestsoft.twostrokecalc.ui.util.keyboardAwareScroll
import com.simplestsoft.twostrokecalc.ui.util.screenSystemBarPadding

@Composable
fun AccountScreen(
    isAdmin: Boolean,
    firebaseAuth: FirebaseAuth,
    displayNameFromPrefs: String?,
    accountEmailFromPrefs: String?,
    onSaveDisplayName: (String) -> Unit,
    onLogout: () -> Unit,
    onAdmin: () -> Unit,
    onBack: (() -> Unit)? = null,
    viewModel: AccountViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val user = firebaseAuth.currentUser
    val canChangePassword = user?.providerData?.any { it.providerId == EmailAuthProvider.PROVIDER_ID } == true
    val shownName = user?.displayName?.takeIf { it.isNotBlank() }
        ?: displayNameFromPrefs?.takeIf { it.isNotBlank() }
        ?: stringResource(R.string.user_fallback_name)
    val shownEmail = accountEmailFromPrefs?.takeIf { it.isNotBlank() }.orEmpty()
        .ifBlank { user?.email.orEmpty() }
    var showLogout by remember { mutableStateOf(false) }
    var showDeleteAccount by remember { mutableStateOf(false) }
    var displayNameInput by remember { mutableStateOf(shownName) }
    var savedMessageVisible by remember { mutableStateOf(false) }
    var currentPassword by rememberSaveable { mutableStateOf("") }
    var newPassword by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(shownName) { displayNameInput = shownName }
    LaunchedEffect(state.accountDeleted) {
        if (state.accountDeleted) {
            viewModel.consumeAccountDeletedFlag()
            onLogout()
        }
    }

    val scrollState = rememberScrollState()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .screenSystemBarPadding()
            .background(AppColors.background()),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .keyboardAwareScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
        onBack?.let { back ->
            TextButton(onClick = back) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                Text(
                    text = stringResource(R.string.settings_title),
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        }
        AccountHeroCard(name = shownName, email = shownEmail)

        AccountSectionCard(title = stringResource(R.string.profile_title), icon = Icons.Default.Person) {
            AppOutlinedTextField(
                value = displayNameInput,
                onValueChange = {
                    displayNameInput = it
                    savedMessageVisible = false
                },
                label = stringResource(R.string.profile_display_name_label),
            )
            Button(
                onClick = {
                    onSaveDisplayName(displayNameInput)
                    savedMessageVisible = true
                },
                enabled = displayNameInput.trim().isNotEmpty() && displayNameInput.trim() != shownName,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.save))
            }
            if (savedMessageVisible) {
                Text(
                    text = stringResource(R.string.profile_saved_success),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        if (canChangePassword) {
            AccountSectionCard(title = stringResource(R.string.change_password), icon = Icons.Default.Lock) {
                AppOutlinedTextField(
                    value = currentPassword,
                    onValueChange = { currentPassword = it },
                    label = stringResource(R.string.current_password),
                    visualTransformation = PasswordVisualTransformation(),
                )
                AppOutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = stringResource(R.string.new_password),
                    visualTransformation = PasswordVisualTransformation(),
                )
                Button(
                    onClick = { viewModel.changePassword(currentPassword, newPassword) },
                    enabled = !state.loading,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.save))
                }
                if (state.loading) {
                    CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
                }
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (state.passwordChanged) {
                    Text(
                        text = stringResource(R.string.password_changed_success),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        if (isAdmin) {
            OutlinedButton(onClick = onAdmin, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.AdminPanelSettings, contentDescription = null)
                Text(
                    text = stringResource(R.string.admin_panel_title),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }

        OutlinedButton(
            onClick = { showLogout = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
        ) {
            Icon(Icons.Default.Logout, contentDescription = null)
            Text(text = stringResource(R.string.logout_label), modifier = Modifier.padding(start = 8.dp))
        }

        OutlinedButton(
            onClick = { showDeleteAccount = true },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.deleteInProgress,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
        ) {
            Icon(Icons.Default.DeleteForever, contentDescription = null)
            Text(text = stringResource(R.string.delete_account_label), modifier = Modifier.padding(start = 8.dp))
        }
        }
    }

    if (showLogout) {
        AlertDialog(
            onDismissRequest = { showLogout = false },
            title = { Text(stringResource(R.string.logout_dialog_title)) },
            text = { Text(stringResource(R.string.logout_dialog_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showLogout = false
                    onLogout()
                }) { Text(stringResource(R.string.logout_confirm_button)) }
            },
            dismissButton = {
                TextButton(onClick = { showLogout = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }

    if (showDeleteAccount) {
        AlertDialog(
            onDismissRequest = { showDeleteAccount = false },
            title = { Text(stringResource(R.string.delete_account_title)) },
            text = { Text(stringResource(R.string.delete_account_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteAccount = false
                        viewModel.deleteAccount()
                    },
                    enabled = !state.deleteInProgress,
                ) {
                    Text(stringResource(R.string.delete_account_confirm_button))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccount = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun AccountHeroCard(name: String, email: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (email.isNotBlank()) {
                Text(
                    text = email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun AccountSectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(FormFieldDefaults.fieldSpacing),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(32.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null)
                    }
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 10.dp),
                )
            }
            content()
        }
    }
}
