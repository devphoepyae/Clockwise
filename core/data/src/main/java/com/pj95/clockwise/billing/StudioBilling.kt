package com.pj95.clockwise.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The single one-time product Chronos Studio sells.
 *
 * It is a non-consumable: buying it once unlocks every premium widget style on
 * this account forever, and the purchase is never consumed.
 */
const val STUDIO_ALL_ACCESS_SKU: String = "chronos_studio_all_access"

/** A short, translatable outcome the gallery can surface after a billing call. */
enum class BillingNotice {
    PURCHASE_COMPLETE,
    RESTORED,
    NOTHING_TO_RESTORE,
    CANCELLED,
    UNAVAILABLE,
    FAILED,
}

/**
 * Everything the Studio purchase card renders.
 *
 * [priceText] comes from Play and is already formatted in the user's own
 * currency; until it arrives the card shows a neutral label rather than a
 * guessed price.
 */
data class BillingUiState(
    val isReady: Boolean = false,
    val isUnavailable: Boolean = false,
    val isWorking: Boolean = false,
    val priceText: String? = null,
    val notice: BillingNotice? = null,
)

/**
 * The billing surface the gallery talks to.
 *
 * Deliberately tiny and Android-`Activity`-shaped only where Play requires it:
 * the ViewModel starts a connection and restores, while the screen hands over
 * its own `Activity` for the purchase dialog.
 */
interface StudioBilling {

    val state: StateFlow<BillingUiState>

    /** Opens the Play connection and loads the product's localised price. */
    fun connect()

    /** Shows Play's purchase sheet. Must be called with a live Activity. */
    fun purchase(activity: Activity)

    /** Re-reads the account's purchases, e.g. after a reinstall. */
    fun restore()

    /** Clears a one-shot notice once the screen has shown it. */
    fun consumeNotice()

    /** Releases the Play connection. */
    fun close()
}

/**
 * Play Billing, wrapped so nothing else in the app imports it.
 *
 * Offline-first is preserved: the purchase is written to Room through
 * [onEntitlement], and from then on the licence table — not Play — decides what
 * the gallery and every pinned widget may show. A device with no Play Store
 * simply reports [BillingUiState.isUnavailable] and keeps every free style.
 */
class PlayBillingClient(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onEntitlement: suspend () -> Unit,
) : StudioBilling {

    private val _state = MutableStateFlow(BillingUiState())
    override val state: StateFlow<BillingUiState> = _state.asStateFlow()

    private var productDetails: ProductDetails? = null
    private var connectionAttempts = 0

    private val purchasesUpdated = PurchasesUpdatedListener { result, purchases ->
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases.orEmpty().forEach(::recordPurchase)
                if (purchases.isNullOrEmpty()) {
                    finishWork(BillingNotice.FAILED)
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED ->
                finishWork(BillingNotice.CANCELLED)
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> restore()
            else -> {
                Log.w(TAG, "Purchase failed: ${result.debugMessage}")
                finishWork(BillingNotice.FAILED)
            }
        }
    }

    private val client: BillingClient by lazy {
        BillingClient.newBuilder(context)
            .setListener(purchasesUpdated)
            .enablePendingPurchases()
            .build()
    }

    override fun connect() {
        if (client.isReady) {
            loadProduct()
            return
        }
        _state.update { it.copy(isWorking = true, isUnavailable = false) }
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    connectionAttempts = 0
                    _state.update {
                        it.copy(isReady = true, isUnavailable = false)
                    }
                    loadProduct()
                    restoreInternal(showNotice = false)
                } else {
                    Log.w(TAG, "Billing setup failed: ${result.debugMessage}")
                    markUnavailable()
                }
            }

            override fun onBillingServiceDisconnected() {
                _state.update { it.copy(isReady = false) }
                // One quiet retry: a transient disconnect at launch is common,
                // and retrying forever would burn battery for no benefit.
                if (connectionAttempts < 1) {
                    connectionAttempts += 1
                    scope.launch {
                        delay(RETRY_DELAY_MILLIS)
                        connect()
                    }
                } else {
                    markUnavailable()
                }
            }
        })
    }

    override fun purchase(activity: Activity) {
        val details = productDetails
        if (!client.isReady || details == null) {
            _state.update { it.copy(isWorking = true) }
            connect()
            finishWork(BillingNotice.UNAVAILABLE)
            return
        }

        _state.update { it.copy(isWorking = true, notice = null) }
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .build(),
                ),
            )
            .build()

        val result = client.launchBillingFlow(activity, params)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            Log.w(TAG, "Purchase sheet could not open: ${result.debugMessage}")
            finishWork(BillingNotice.FAILED)
        }
    }

    override fun restore() {
        if (!client.isReady) {
            connect()
            return
        }
        _state.update { it.copy(isWorking = true, notice = null) }
        restoreInternal(showNotice = true)
    }

    override fun consumeNotice() {
        _state.update { it.copy(notice = null) }
    }

    override fun close() {
        runCatching { client.endConnection() }
        _state.update { it.copy(isReady = false) }
    }

    private fun loadProduct() {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(STUDIO_ALL_ACCESS_SKU)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build(),
                ),
            )
            .build()

        client.queryProductDetailsAsync(params) { result, details ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                productDetails = details.firstOrNull()
                _state.update {
                    it.copy(
                        priceText = productDetails
                            ?.oneTimePurchaseOfferDetails
                            ?.formattedPrice,
                    )
                }
            } else {
                Log.w(TAG, "Product details unavailable: ${result.debugMessage}")
            }
            _state.update { it.copy(isWorking = false) }
        }
    }

    private fun restoreInternal(showNotice: Boolean) {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        client.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                if (showNotice) finishWork(BillingNotice.FAILED)
                return@queryPurchasesAsync
            }

            val owned = purchases.filter { purchase ->
                purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                    purchase.products.contains(STUDIO_ALL_ACCESS_SKU)
            }
            if (owned.isEmpty()) {
                if (showNotice) finishWork(BillingNotice.NOTHING_TO_RESTORE)
                else _state.update { it.copy(isWorking = false) }
                return@queryPurchasesAsync
            }

            owned.forEach(::recordPurchase)
            if (showNotice) finishWork(BillingNotice.RESTORED)
        }
    }

    /**
     * Writes a verified purchase to Room, acknowledging it once.
     *
     * Acknowledgement is required within three days or Play refunds the order,
     * so it is attempted before the local write. If it fails the entitlement is
     * still recorded — the user paid, and the next restore will acknowledge.
     */
    private fun recordPurchase(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return
        if (!purchase.products.contains(STUDIO_ALL_ACCESS_SKU)) return

        if (purchase.isAcknowledged) {
            grantEntitlement(BillingNotice.PURCHASE_COMPLETE)
            return
        }

        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        client.acknowledgePurchase(params) { result ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                Log.w(TAG, "Purchase could not be acknowledged: ${result.debugMessage}")
            }
            grantEntitlement(BillingNotice.PURCHASE_COMPLETE)
        }
    }

    private fun grantEntitlement(notice: BillingNotice) {
        scope.launch {
            runCatching { onEntitlement() }
                .onFailure { error ->
                    Log.w(TAG, "Purchase could not be recorded locally", error)
                    finishWork(BillingNotice.FAILED)
                }
                .onSuccess { finishWork(notice) }
        }
    }

    private fun markUnavailable() {
        _state.update {
            it.copy(
                isReady = false,
                isUnavailable = true,
                isWorking = false,
                notice = BillingNotice.UNAVAILABLE,
            )
        }
    }

    private fun finishWork(notice: BillingNotice) {
        _state.update {
            it.copy(
                isWorking = false,
                isReady = it.isReady || notice == BillingNotice.PURCHASE_COMPLETE,
                notice = notice,
            )
        }
    }

    private companion object {
        const val TAG = "StudioBilling"
        const val RETRY_DELAY_MILLIS = 1_500L
    }
}
