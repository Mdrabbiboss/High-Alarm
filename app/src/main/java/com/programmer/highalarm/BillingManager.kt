package com.programmer.highalarm

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.SystemClock
import android.util.Log
import android.widget.Toast
import com.android.billingclient.api.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

class BillingManager private constructor(private val context: Context, @Suppress("UNUSED_PARAMETER") scope: CoroutineScope) : PurchasesUpdatedListener {

    private val tag = "BillingManager"
    private var lastClickTime = 0L

    companion object {
        @Volatile
        private var INSTANCE: BillingManager? = null

        var lastLaunchedPlanId: String? = null

        fun getInstance(context: Context, scope: CoroutineScope = CoroutineScope(Dispatchers.Main)): BillingManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BillingManager(context.applicationContext, scope).also { INSTANCE = it }
            }
        }
    }

    private val billingClient: BillingClient by lazy {
        try {
            BillingClient.newBuilder(context)
                .setListener(this)
                .enablePendingPurchases(
                    PendingPurchasesParams.newBuilder()
                        .enableOneTimeProducts()
                        .build()
                )
                .build()
        } catch (e: Exception) {
            Log.e(tag, "Error building BillingClient", e)
            throw e
        }
    }

    private val _isPremium = MutableStateFlow(false)
    val isPremium = _isPremium.asStateFlow()

    private val _productDetails = MutableStateFlow<List<ProductDetails>>(emptyList())
    val productDetails = _productDetails.asStateFlow()

    init {
        try {
            startConnection()
        } catch (e: Exception) {
            Log.e(tag, "Error in BillingManager init", e)
        }
    }

    fun startConnection() {
        try {
            Log.d(tag, "Starting BillingClient connection...")
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    Log.d(tag, "onBillingSetupFinished: responseCode = ${billingResult.responseCode}, debugMessage = ${billingResult.debugMessage}")
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        queryProductDetails()
                        queryPurchases()
                    } else {
                        Log.w(tag, "Billing setup failed with responseCode: ${billingResult.responseCode}")
                    }
                }

                override fun onBillingServiceDisconnected() {
                    Log.w(tag, "Billing service disconnected. Attempting reconnect...")
                    try {
                        startConnection()
                    } catch (e: Exception) {
                        Log.e(tag, "Error reconnecting billing service", e)
                    }
                }
            })
        } catch (e: Exception) {
            Log.e(tag, "Error in startConnection", e)
        }
    }

    private fun queryProductDetails() {
        try {
            val cachedMap = ConcurrentHashMap<String, ProductDetails>()

            // 1. Query SUBS product "premium"
            val subsParams = QueryProductDetailsParams.newBuilder()
                .setProductList(
                    listOf(
                        QueryProductDetailsParams.Product.newBuilder()
                            .setProductId("premium")
                            .setProductType(BillingClient.ProductType.SUBS)
                            .build()
                    )
                )
                .build()

            // 2. Query INAPP product "lifetime"
            val inappParams = QueryProductDetailsParams.newBuilder()
                .setProductList(
                    listOf(
                        QueryProductDetailsParams.Product.newBuilder()
                            .setProductId("lifetime")
                            .setProductType(BillingClient.ProductType.INAPP)
                            .build()
                    )
                )
                .build()

            Log.d(tag, "Pre-fetching SUBS & INAPP product details in parallel...")

            billingClient.queryProductDetailsAsync(subsParams) { billingResultSubs, queryResultSubs ->
                Log.d(tag, "SUBS query responseCode = ${billingResultSubs.responseCode}")
                val subsList = queryResultSubs.productDetailsList ?: emptyList()
                subsList.forEach { cachedMap[it.productId] = it }
                _productDetails.value = cachedMap.values.toList()

                val premiumDetails = cachedMap["premium"]
                if (premiumDetails == null) {
                    Log.e(tag, "SUBSCRIPTION PRODUCT ID MISSING: 'premium'")
                } else {
                    val monthlyFound = premiumDetails.subscriptionOfferDetails?.any { it.basePlanId == "monthly" } == true
                    val yearlyFound = premiumDetails.subscriptionOfferDetails?.any { it.basePlanId == "yearly" } == true
                    Log.d(tag, "Monthly base plan ('monthly') found: $monthlyFound, Yearly base plan ('yearly') found: $yearlyFound")
                }
            }

            billingClient.queryProductDetailsAsync(inappParams) { billingResultInApp, queryResultInApp ->
                Log.d(tag, "INAPP query responseCode = ${billingResultInApp.responseCode}")
                val inappList = queryResultInApp.productDetailsList ?: emptyList()
                inappList.forEach { cachedMap[it.productId] = it }
                _productDetails.value = cachedMap.values.toList()

                val lifetimeDetails = cachedMap["lifetime"]
                if (lifetimeDetails == null) {
                    Log.e(tag, "LIFETIME INAPP PRODUCT MISSING: 'lifetime'")
                } else {
                    Log.d(tag, "Lifetime product ('lifetime') pre-fetched successfully.")
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error in queryProductDetails", e)
        }
    }

    fun launchPurchaseFlow(activity: Activity, selectedPlanId: String) {
        try {
            val now = SystemClock.elapsedRealtime()
            if (now - lastClickTime < 1200L) {
                Log.w(tag, "Debounced rapid purchase click.")
                return
            }
            lastClickTime = now

            val normalizedPlan = when (selectedPlanId) {
                "yearly", "annual_subscription" -> "yearly"
                "monthly", "monthly_subscription" -> "monthly"
                "lifetime", "lifetime_purchase" -> "lifetime"
                else -> selectedPlanId
            }
            lastLaunchedPlanId = normalizedPlan

            Log.d(tag, "launchPurchaseFlow called for selectedPlanId=$selectedPlanId (normalized=$normalizedPlan)")
            if (!billingClient.isReady) {
                Log.w(tag, "BillingClient is not ready. Reconnecting...")
                Toast.makeText(activity, "Connecting to Google Play... Please try again.", Toast.LENGTH_SHORT).show()
                startConnection()
                return
            }

            val detailsList = _productDetails.value
            if (detailsList.isEmpty()) {
                Log.w(tag, "ProductDetails list is empty. Triggering emergency query...")
                queryProductDetails()
                Toast.makeText(activity, "Loading product details... Please tap again in a moment.", Toast.LENGTH_SHORT).show()
                return
            }

            Log.d(tag, "Available cached productDetails count: ${detailsList.size}, IDs: ${detailsList.map { it.productId }}")

            val (targetProduct, basePlanToMatch) = when (selectedPlanId) {
                "yearly", "annual_subscription" -> detailsList.find { it.productId == "premium" } to "yearly"
                "monthly", "monthly_subscription" -> detailsList.find { it.productId == "premium" } to "monthly"
                "lifetime", "lifetime_purchase" -> detailsList.find { it.productId == "lifetime" } to null
                else -> detailsList.find { it.productId == selectedPlanId } to null
            }

            if (targetProduct == null) {
                Log.e(tag, "ERROR: ProductDetails not found for selectedPlanId=$selectedPlanId. Available products: ${detailsList.map { it.productId }}")
                Toast.makeText(activity, "Product details loading, please try again in a moment.", Toast.LENGTH_SHORT).show()
                queryProductDetails()
                return
            }

            val offerToken = if (targetProduct.productType == BillingClient.ProductType.SUBS && basePlanToMatch != null) {
                val offer = targetProduct.subscriptionOfferDetails?.find { it.basePlanId == basePlanToMatch }
                    ?: targetProduct.subscriptionOfferDetails?.firstOrNull()
                
                if (offer == null) {
                    Log.e(tag, "ERROR: Subscription offer token not found for basePlanId='$basePlanToMatch' under product '${targetProduct.productId}'. Available offers: ${targetProduct.subscriptionOfferDetails?.map { it.basePlanId }}")
                    Toast.makeText(activity, "Subscription offer not available.", Toast.LENGTH_SHORT).show()
                    return
                }
                Log.d(tag, "Found offerToken for basePlanId='$basePlanToMatch'")
                offer.offerToken
            } else {
                null
            }

            val productDetailsParamsList = listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(targetProduct)
                    .apply {
                        if (offerToken != null) {
                            setOfferToken(offerToken)
                        }
                    }
                    .build()
            )

            val billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParamsList)
                .build()

            val billingResult = billingClient.launchBillingFlow(activity, billingFlowParams)
            Log.d(tag, "launchBillingFlow result: responseCode = ${billingResult.responseCode}, debugMessage = ${billingResult.debugMessage}")

            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                val errorMsg = when (billingResult.responseCode) {
                    BillingClient.BillingResponseCode.ITEM_UNAVAILABLE -> "Item unavailable for purchase."
                    BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> "You already own this item."
                    BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE,
                    BillingClient.BillingResponseCode.SERVICE_DISCONNECTED,
                    BillingClient.BillingResponseCode.BILLING_UNAVAILABLE -> {
                        startConnection()
                        "Google Play billing service unavailable. Reconnecting..."
                    }
                    BillingClient.BillingResponseCode.USER_CANCELED -> "Purchase cancelled."
                    BillingClient.BillingResponseCode.DEVELOPER_ERROR -> "Developer configuration error (${billingResult.responseCode})."
                    else -> "Unable to launch purchase flow (${billingResult.responseCode})."
                }
                if (billingResult.responseCode != BillingClient.BillingResponseCode.USER_CANCELED) {
                    Toast.makeText(activity, errorMsg, Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Exception in launchPurchaseFlow", e)
            Toast.makeText(activity, "An error occurred while launching purchase.", Toast.LENGTH_SHORT).show()
        }
    }

    fun queryPurchases() {
        try {
            if (!billingClient.isReady) {
                Log.d(tag, "queryPurchases: BillingClient not ready, starting connection...")
                startConnection()
                return
            }

            billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder()
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            ) { resultSubs, subsPurchases ->
                val activeSubsList = if (resultSubs.responseCode == BillingClient.BillingResponseCode.OK) {
                    subsPurchases.filter { purchase ->
                        purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                    }
                } else emptyList()

                // Auto-acknowledge unacknowledged active subscriptions
                activeSubsList.filter { !it.isAcknowledged }.forEach { handlePurchase(it) }

                val hasActiveSubs = activeSubsList.isNotEmpty()

                val subType = if (activeSubsList.isNotEmpty()) {
                    val launched = lastLaunchedPlanId
                    val savedType = SettingsViewModel.getInstance().settings.value.activePurchaseType
                    when {
                        launched == "yearly" || launched == "monthly" -> launched
                        savedType == "yearly" || savedType == "monthly" -> savedType
                        else -> "yearly"
                    }
                } else ""

                billingClient.queryPurchasesAsync(
                    QueryPurchasesParams.newBuilder()
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                ) { resultInApp, inAppPurchases ->
                    val activeInAppList = if (resultInApp.responseCode == BillingClient.BillingResponseCode.OK) {
                        inAppPurchases.filter { purchase ->
                            purchase.purchaseState == Purchase.PurchaseState.PURCHASED && 
                            purchase.products.any { it.contains("lifetime") }
                        }
                    } else emptyList()

                    // Auto-acknowledge unacknowledged in-app purchases
                    activeInAppList.filter { !it.isAcknowledged }.forEach { handlePurchase(it) }

                    val hasLifetime = activeInAppList.isNotEmpty()

                    // PRO is gone (confirmed by Play): forget the saved Gmail so a new purchase detects it again
                    val bothOk = resultSubs.responseCode == BillingClient.BillingResponseCode.OK &&
                        resultInApp.responseCode == BillingClient.BillingResponseCode.OK
                    if (bothOk && !hasActiveSubs && !hasLifetime) PurchaseRecord.clear(context)

                    val isProActive = hasActiveSubs || hasLifetime
                    val finalPurchaseType = when {
                        hasLifetime -> "lifetime"
                        hasActiveSubs -> subType
                        else -> ""
                    }
                    Log.d(tag, "queryPurchases: hasActiveSubs=$hasActiveSubs, hasLifetime=$hasLifetime, isProActive=$isProActive, type=$finalPurchaseType")
                    _isPremium.value = isProActive
                    SettingsViewModel.setPremiumStatus(context, isProActive, finalPurchaseType)
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error in queryPurchases", e)
        }
    }

    fun restorePurchases(onResult: (Boolean, String) -> Unit) {
        try {
            if (!billingClient.isReady) {
                onResult(false, "Billing service not ready")
                startConnection()
                return
            }

            billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder()
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            ) { resultSubs, subsPurchases ->
                val validSubs = if (resultSubs.responseCode == BillingClient.BillingResponseCode.OK) {
                    subsPurchases.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
                } else emptyList()

                billingClient.queryPurchasesAsync(
                    QueryPurchasesParams.newBuilder()
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                ) { resultInApp, inAppPurchases ->
                    val validInApp = if (resultInApp.responseCode == BillingClient.BillingResponseCode.OK) {
                        inAppPurchases.filter { purchase ->
                            purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                            purchase.products.any { it.contains("lifetime") }
                        }
                    } else emptyList()

                    val allPurchases = validSubs + validInApp
                    if (allPurchases.isNotEmpty()) {
                        PurchaseRecord.requestGmail(context)
                        for (p in allPurchases) {
                            if (!p.isAcknowledged) {
                                handlePurchase(p)
                            }
                        }
                        val launched = lastLaunchedPlanId
                        val savedType = SettingsViewModel.getInstance().settings.value.activePurchaseType
                        val pType = when {
                            validInApp.isNotEmpty() -> "lifetime"
                            launched == "yearly" || launched == "monthly" -> launched
                            savedType == "yearly" || savedType == "monthly" -> savedType
                            else -> "yearly"
                        }
                        _isPremium.value = true
                        SettingsViewModel.setPremiumStatus(context, true, pType)
                        onResult(true, "Purchases successfully restored!")
                    } else {
                        _isPremium.value = false
                        SettingsViewModel.setPremiumStatus(context, false)
                        onResult(false, "No active subscriptions or purchases found to restore")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error in restorePurchases", e)
            onResult(false, "Error restoring purchases.")
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        try {
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                for (purchase in purchases) {
                    handlePurchase(purchase)
                }
                // Just bought: find out which Gmail it was bought with
                if (purchases.any { it.purchaseState == Purchase.PurchaseState.PURCHASED }) {
                    PurchaseRecord.requestGmail(context)
                }
            } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
                Log.d(tag, "User cancelled purchase flow.")
            } else {
                Log.w(tag, "onPurchasesUpdated responseCode: ${billingResult.responseCode}, debugMessage: ${billingResult.debugMessage}")
            }
        } catch (e: Exception) {
            Log.e(tag, "Error in onPurchasesUpdated", e)
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        try {
            if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                val launched = lastLaunchedPlanId
                val savedType = SettingsViewModel.getInstance().settings.value.activePurchaseType
                val pType = when {
                    purchase.products.any { it.contains("lifetime") } -> "lifetime"
                    launched == "yearly" || launched == "monthly" -> launched
                    savedType == "yearly" || savedType == "monthly" -> savedType
                    else -> "yearly"
                }

                if (!purchase.isAcknowledged) {
                    val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()
                    billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                            _isPremium.value = true
                            SettingsViewModel.setPremiumStatus(context, true, pType)
                        }
                    }
                } else {
                    _isPremium.value = true
                    SettingsViewModel.setPremiumStatus(context, true, pType)
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error in handlePurchase", e)
        }
    }
}
