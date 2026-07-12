package com.simplestsoft.twostrokecalc.ui.billing

import android.app.Activity
import androidx.lifecycle.ViewModel
import com.simplestsoft.twostrokecalc.data.billing.BillingRepository
import com.simplestsoft.twostrokecalc.data.billing.BillingState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class BillingViewModel @Inject constructor(
    private val billingRepository: BillingRepository,
) : ViewModel() {
    val state: StateFlow<BillingState> = billingRepository.state

    fun launchProPurchase(activity: Activity) = billingRepository.launchProPurchase(activity)

    fun restorePurchases() = billingRepository.restorePurchases()

    fun clearError() = billingRepository.clearError()
}
