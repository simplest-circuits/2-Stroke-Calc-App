package com.simplestsoft.twostrokecalc.data.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.google.firebase.auth.FirebaseAuth
import com.simplestsoft.twostrokecalc.billing.ProProduct
import com.simplestsoft.twostrokecalc.data.config.ProAccessRepository
import com.simplestsoft.twostrokecalc.data.preferences.PreferencesManager
import com.simplestsoft.twostrokecalc.data.remote.AccountApiService
import com.simplestsoft.twostrokecalc.domain.model.remote.VerifyProPurchaseRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class BillingState(
    val isReady: Boolean = false,
    val priceFormatted: String? = null,
    val isPurchasing: Boolean = false,
    val errorMessage: String? = null,
)

@Singleton
class BillingRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val accountApiService: AccountApiService,
    private val preferencesManager: PreferencesManager,
    private val proAccessRepository: ProAccessRepository,
    private val firebaseAuth: FirebaseAuth,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val connectMutex = Mutex()
    private var productDetails: ProductDetails? = null

    private val _state = MutableStateFlow(BillingState())
    val state: StateFlow<BillingState> = _state.asStateFlow()

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { purchase ->
                    scope.launch { handlePurchase(purchase) }
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                _state.update { it.copy(isPurchasing = false, errorMessage = null) }
            }
            else -> {
                _state.update {
                    it.copy(
                        isPurchasing = false,
                        errorMessage = billingResult.debugMessage.ifBlank {
                            "Billing error ${billingResult.responseCode}"
                        },
                    )
                }
            }
        }
    }

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build(),
        )
        .build()

    fun start() {
        scope.launch { ensureConnected() }
    }

    fun restorePurchases() {
        scope.launch {
            if (!ensureConnected()) return@launch
            val purchases = queryActivePurchases() ?: return@launch
            purchases.filter { it.products.contains(ProProduct.PRODUCT_ID) }
                .forEach { handlePurchase(it) }
        }
    }

    fun launchProPurchase(activity: Activity) {
        if (firebaseAuth.currentUser == null) {
            _state.update { it.copy(errorMessage = "sign_in_required") }
            return
        }
        scope.launch {
            _state.update { it.copy(isPurchasing = true, errorMessage = null) }
            if (!ensureConnected()) {
                _state.update { it.copy(isPurchasing = false, errorMessage = "billing_unavailable") }
                return@launch
            }
            val details = productDetails ?: run {
                loadProductDetails()
                productDetails
            }
            if (details == null) {
                _state.update { it.copy(isPurchasing = false, errorMessage = "product_unavailable") }
                return@launch
            }
            val params = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(
                    listOf(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                            .setProductDetails(details)
                            .build(),
                    ),
                )
                .build()
            val result = billingClient.launchBillingFlow(activity, params)
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                _state.update {
                    it.copy(
                        isPurchasing = false,
                        errorMessage = result.debugMessage.ifBlank { "billing_flow_failed" },
                    )
                }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }

    private suspend fun ensureConnected(): Boolean {
        if (billingClient.isReady) return true
        return connectMutex.withLock {
            if (billingClient.isReady) return true
            val connected = suspendCancellableCoroutine { continuation ->
                billingClient.startConnection(
                    object : BillingClientStateListener {
                        override fun onBillingSetupFinished(result: BillingResult) {
                            val ok = result.responseCode == BillingClient.BillingResponseCode.OK
                            if (ok) {
                                scope.launch { loadProductDetails() }
                            }
                            continuation.resume(ok)
                        }

                        override fun onBillingServiceDisconnected() {
                            _state.update { it.copy(isReady = false) }
                        }
                    },
                )
            }
            _state.update { it.copy(isReady = connected) }
            connected
        }
    }

    private suspend fun loadProductDetails() {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(ProProduct.PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build(),
                ),
            )
            .build()
        val result = billingClient.queryProductDetails(params)
        if (result.billingResult.responseCode != BillingClient.BillingResponseCode.OK) return
        val details = result.productDetailsList?.firstOrNull() ?: return
        productDetails = details
        val offer = details.oneTimePurchaseOfferDetails
        _state.update { it.copy(priceFormatted = offer?.formattedPrice) }
    }

    private suspend fun queryActivePurchases(): List<Purchase>? {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        val result = billingClient.queryPurchasesAsync(params)
        return if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            result.purchasesList
        } else {
            null
        }
    }

    private suspend fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return
        if (!purchase.products.contains(ProProduct.PRODUCT_ID)) return
        if (firebaseAuth.currentUser == null) {
            _state.update { it.copy(isPurchasing = false, errorMessage = "sign_in_required") }
            return
        }
        runCatching {
            accountApiService.verifyProPurchase(
                VerifyProPurchaseRequest(
                    purchaseToken = purchase.purchaseToken,
                    productId = ProProduct.PRODUCT_ID,
                    packageName = context.packageName,
                ),
            )
        }.onFailure { error ->
            _state.update {
                it.copy(
                    isPurchasing = false,
                    errorMessage = error.message ?: "verification_failed",
                )
            }
            return
        }
        if (!purchase.isAcknowledged) {
            val ackParams = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            billingClient.acknowledgePurchase(ackParams)
        }
        preferencesManager.setIsPro(true)
        proAccessRepository.applyUserEntitlements(isPro = true, isAdmin = preferencesManager.getIsAdmin())
        _state.update { it.copy(isPurchasing = false, errorMessage = null) }
    }
}
