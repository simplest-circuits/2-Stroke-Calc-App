package com.simplestsoft.twostrokecalc.ui.components

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.simplestsoft.twostrokecalc.R
import com.simplestsoft.twostrokecalc.domain.model.ProModuleId
import com.simplestsoft.twostrokecalc.domain.model.labelRes
import com.simplestsoft.twostrokecalc.domain.model.overviewDescriptionRes
import com.simplestsoft.twostrokecalc.ui.billing.BillingViewModel
import com.simplestsoft.twostrokecalc.ui.util.findComponentActivity

@Composable
fun ProUpsellDialog(
    module: ProModuleId?,
    isAuthenticated: Boolean,
    onDismiss: () -> Unit,
    onSignInRequired: () -> Unit,
    billingViewModel: BillingViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val activity = context.findComponentActivity()
    val billingState by billingViewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(billingState.errorMessage) {
        val errorKey = billingState.errorMessage ?: return@LaunchedEffect
        val message = when (errorKey) {
            "sign_in_required" -> context.getString(R.string.settings_pro_sign_in_required)
            "billing_unavailable" -> context.getString(R.string.settings_pro_billing_unavailable)
            "product_unavailable" -> context.getString(R.string.settings_pro_product_unavailable)
            "verification_failed" -> context.getString(R.string.settings_pro_verification_failed)
            else -> errorKey
        }
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        billingViewModel.clearError()
    }

    fun startPurchase() {
        if (!isAuthenticated) {
            onDismiss()
            onSignInRequired()
            return
        }
        activity?.let { billingViewModel.launchProPurchase(it) }
        onDismiss()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (module != null) {
                    stringResource(module.labelRes())
                } else {
                    stringResource(R.string.pro_upsell_title)
                },
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (module != null) {
                    Text(
                        text = stringResource(module.overviewDescriptionRes()),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Text(
                    text = stringResource(R.string.pro_upsell_message),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.textSecondary(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { startPurchase() },
                enabled = !billingState.isPurchasing,
            ) {
                if (billingState.isPurchasing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    val price = billingState.priceFormatted
                    Text(
                        if (price != null) {
                            stringResource(R.string.settings_buy_pro_with_price, price)
                        } else {
                            stringResource(R.string.settings_buy_pro)
                        },
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}
