package com.simplestsoft.twostrokecalc.ui.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import com.simplestsoft.twostrokecalc.ui.util.edgeToEdgeContentPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.simplestsoft.twostrokecalc.R
import com.simplestsoft.twostrokecalc.ui.components.FlowBodyText
import com.simplestsoft.twostrokecalc.ui.navigation.AuthNavHost

@Composable
fun AuthGateDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    allowDismiss: Boolean = true,
    @StringRes titleRes: Int = R.string.auth_gate_title_default,
    @StringRes subtitleRes: Int = R.string.auth_gate_subtitle_default,
) {
    if (!visible) return
    Dialog(
        onDismissRequest = { if (allowDismiss) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .edgeToEdgeContentPadding()
                    .padding(horizontal = 24.dp),
            ) {
                if (allowDismiss) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(top = 8.dp),
                    ) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cancel))
                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                ) {
                    Text(
                        text = stringResource(titleRes),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2,
                        softWrap = true,
                    )
                    FlowBodyText(
                        text = stringResource(subtitleRes),
                        modifier = Modifier.padding(top = 8.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                AuthNavHost(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    subtitleRes = subtitleRes,
                    showHeader = false,
                    embedded = true,
                )
            }
        }
    }
}
