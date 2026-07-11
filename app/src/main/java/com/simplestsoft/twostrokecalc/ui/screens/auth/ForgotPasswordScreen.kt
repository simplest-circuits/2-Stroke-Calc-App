package com.simplestsoft.twostrokecalc.ui.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imeNestedScroll
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.simplestsoft.twostrokecalc.R
import com.simplestsoft.twostrokecalc.ui.components.AppOutlinedTextField
import com.simplestsoft.twostrokecalc.ui.theme.authSubtitle
import com.simplestsoft.twostrokecalc.ui.theme.authTitle

@Composable
fun ForgotPasswordScreen(
    loading: Boolean,
    error: String?,
    success: Boolean,
    embedded: Boolean = false,
    onSend: (String) -> Unit,
    onBack: () -> Unit,
) {
    var email by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .imeNestedScroll()
            .padding(
                if (embedded) {
                    PaddingValues(top = 8.dp, bottom = 24.dp)
                } else {
                    PaddingValues(24.dp)
                },
            ),
        verticalArrangement = if (embedded) Arrangement.Top else Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            stringResource(R.string.forgot_password_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.authTitle,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.forgot_password_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.authSubtitle,
        )
        Spacer(Modifier.height(24.dp))
        AppOutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = stringResource(R.string.email_label),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        )
        error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        if (success) {
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.email_sent_message))
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { onSend(email) },
            enabled = !loading && email.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (loading) CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.height(24.dp))
            else Text(stringResource(R.string.send_link))
        }
        TextButton(onClick = onBack) {
            Text(stringResource(R.string.back_to_login))
        }
    }
}
