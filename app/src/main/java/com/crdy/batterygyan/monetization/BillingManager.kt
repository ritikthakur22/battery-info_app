package com.crdy.batterygyan.monetization

import android.app.Activity
import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class BillingManager(context: Context) : DefaultLifecycleObserver {
    companion object { const val REMOVE_ADS_PRODUCT_ID = "remove_ads_lifetime" }

    private val _removeAds = MutableStateFlow(false)
    val removeAds: StateFlow<Boolean> = _removeAds
    private val billingClient = BillingClient.newBuilder(context)
        .setListener { result, purchases -> handlePurchases(result, purchases) }
        .enablePendingPurchases()
        .build()
    private var productDetails: ProductDetails? = null

    override fun onStart(owner: LifecycleOwner) { connect() }
    override fun onStop(owner: LifecycleOwner) { billingClient.endConnection() }

    fun launchRemoveAds(activity: Activity): BillingResult {
        val product = productDetails ?: return BillingResult.newBuilder()
            .setResponseCode(BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE)
            .setDebugMessage("Remove Ads product is not loaded yet.")
            .build()
        val params = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(product)
            .build()
        return billingClient.launchBillingFlow(activity, BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(params)).build())
    }

    fun restorePurchases() {
        if (!billingClient.isReady) return
        billingClient.queryPurchasesAsync(QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP).build()) { result, purchases ->
            handlePurchases(result, purchases)
        }
    }

    private fun connect() {
        if (billingClient.isReady) { restorePurchases(); return }
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    val params = QueryProductDetailsParams.newBuilder().setProductList(listOf(
                        QueryProductDetailsParams.Product.newBuilder()
                            .setProductId(REMOVE_ADS_PRODUCT_ID)
                            .setProductType(BillingClient.ProductType.INAPP).build()
                    )).build()
                    billingClient.queryProductDetailsAsync(params) { queryResult, details ->
                        if (queryResult.responseCode == BillingClient.BillingResponseCode.OK) productDetails = details.firstOrNull()
                        restorePurchases()
                    }
                }
            }
            override fun onBillingServiceDisconnected() { productDetails = null }
        })
    }

    private fun handlePurchases(result: BillingResult, purchases: List<Purchase>?) {
        if (result.responseCode != BillingClient.BillingResponseCode.OK || purchases == null) return
        purchases.filter { it.products.contains(REMOVE_ADS_PRODUCT_ID) && it.purchaseState == Purchase.PurchaseState.PURCHASED }
            .forEach { purchase ->
                _removeAds.value = true
                if (!purchase.isAcknowledged) billingClient.acknowledgePurchase(
                    AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
                ) { /* entitlement is granted only from the verified Play purchase callback */ }
            }
    }
}
