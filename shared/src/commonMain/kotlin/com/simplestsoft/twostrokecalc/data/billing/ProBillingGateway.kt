package com.simplestsoft.twostrokecalc.data.billing

interface ProBillingGateway {
    suspend fun restorePurchases()
}
