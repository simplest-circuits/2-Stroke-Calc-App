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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.simplestsoft.twostrokecalc.ui.components.AppOutlinedTextField
import com.simplestsoft.twostrokecalc.ui.components.FormFieldDefaults
import com.simplestsoft.twostrokecalc.ui.components.FlowBodyText
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.simplestsoft.twostrokecalc.R
import com.simplestsoft.twostrokecalc.ui.theme.authSubtitle
import com.simplestsoft.twostrokecalc.ui.theme.authTitle

@Composable
fun RegisterScreen(
    loading: Boolean,
    error: String?,
    embedded: Boolean = false,
    onRegister: (email: String, password: String, displayName: String?) -> Unit,
    onLogin: () -> Unit,
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var mismatch by remember { mutableStateOf(false) }
    var emailInvalid by remember { mutableStateOf(false) }
    val emailOk = email.contains('@') && email.substringAfter('@', "").isNotBlank()

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
            stringResource(R.string.register_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.authTitle,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            maxLines = 2,
            softWrap = true,
        )
        Spacer(Modifier.height(8.dp))
        FlowBodyText(
            text = stringResource(R.string.register_subtitle),
            color = MaterialTheme.colorScheme.authSubtitle,
        )
        Spacer(Modifier.height(24.dp))
        AppOutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = stringResource(R.string.name_optional),
        )
        Spacer(Modifier.height(FormFieldDefaults.fieldSpacing))
        AppOutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                emailInvalid = false
            },
            label = stringResource(R.string.email_label),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        )
        Spacer(Modifier.height(FormFieldDefaults.fieldSpacing))
        AppOutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = stringResource(R.string.password_min_hint),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        )
        Spacer(Modifier.height(FormFieldDefaults.fieldSpacing))
        AppOutlinedTextField(
            value = confirm,
            onValueChange = { confirm = it },
            label = stringResource(R.string.password_confirm),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        )
        error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        if (emailInvalid) {
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.email_invalid), color = MaterialTheme.colorScheme.error)
        }
        if (mismatch) {
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.passwords_do_not_match), color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                if (!emailOk) {
                    emailInvalid = true
                    mismatch = false
                    return@Button
                }
                emailInvalid = false
                if (password != confirm) {
                    mismatch = true
                    return@Button
                }
                mismatch = false
                onRegister(email.trim(), password, name.ifBlank { null })
            },
            enabled = !loading && emailOk && password.length >= 6 && confirm.length >= 6,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (loading) CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.height(24.dp))
            else Text(stringResource(R.string.create_account))
        }
        TextButton(onClick = onLogin) {
            Text(stringResource(R.string.already_registered))
        }
    }
}
