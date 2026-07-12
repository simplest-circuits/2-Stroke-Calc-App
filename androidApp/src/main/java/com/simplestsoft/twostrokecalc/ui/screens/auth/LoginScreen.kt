package com.simplestsoft.twostrokecalc.ui.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.annotation.StringRes
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.simplestsoft.twostrokecalc.ui.components.AppOutlinedTextField
import com.simplestsoft.twostrokecalc.ui.components.ButtonLabel
import com.simplestsoft.twostrokecalc.ui.components.FormFieldDefaults
import com.simplestsoft.twostrokecalc.ui.components.FlowBodyText
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.simplestsoft.twostrokecalc.R
import com.simplestsoft.twostrokecalc.ui.theme.authSubtitle
import com.simplestsoft.twostrokecalc.ui.theme.authTitle

@Composable
fun LoginScreen(
    loading: Boolean,
    error: String?,
    @StringRes subtitleRes: Int = R.string.login_subtitle,
    showHeader: Boolean = true,
    embedded: Boolean = false,
    onLogin: (String, String) -> Unit,
    onGoogle: () -> Unit,
    onForgot: () -> Unit,
    onRegister: () -> Unit,
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
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
        if (showHeader) {
            Text(
                stringResource(R.string.login_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.authTitle,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                maxLines = 2,
                softWrap = true,
            )
            Spacer(Modifier.height(8.dp))
            FlowBodyText(
                text = stringResource(subtitleRes),
                color = MaterialTheme.colorScheme.authSubtitle,
            )
            Spacer(Modifier.height(24.dp))
        }
        AppOutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = stringResource(R.string.email_label),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        )
        Spacer(Modifier.height(FormFieldDefaults.fieldSpacing))
        AppOutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = stringResource(R.string.password_label),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        )
        error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { onLogin(email, password) },
            enabled = !loading,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
        ) {
            if (loading) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    modifier = Modifier.height(24.dp),
                )
            } else {
                ButtonLabel(stringResource(R.string.login_button))
            }
        }
        TextButton(onClick = onForgot) {
            Text(stringResource(R.string.forgot_password))
        }
        Button(onClick = { onGoogle() }, enabled = !loading, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.sign_in_google))
        }
        TextButton(onClick = onRegister) {
            Text(stringResource(R.string.no_account_register))
        }
    }
}
