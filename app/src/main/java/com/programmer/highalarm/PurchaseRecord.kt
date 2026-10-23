package com.programmer.highalarm

import android.content.Context
import com.android.billingclient.api.Purchase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** What Google Play tells us about the active PRO purchase (Play never shares the buyer's Gmail). */
data class PurchaseInfo(
    val productId: String,
    val isLifetime: Boolean,
    val orderId: String?,
    val purchaseTime: Long,
    val isAutoRenewing: Boolean,
)

/**
 * Keeps the active purchase's details (order ID, date, renewal) and the Gmail the user
 * chose to save for it, so the Settings card can show them even before Play reconnects.
 */
object PurchaseRecord {
    private const val PREFS = "purchase_record"

    private val _info = MutableStateFlow<PurchaseInfo?>(null)
    val info: StateFlow<PurchaseInfo?> = _info.asStateFlow()

    private val _savedEmail = MutableStateFlow<String?>(null)
    val savedEmail: StateFlow<String?> = _savedEmail.asStateFlow()

    @Volatile private var loaded = false

    fun load(context: Context) {
        if (loaded) return
        loaded = true
        val p = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        _savedEmail.value = p.getString("email", null)
        val product = p.getString("product", null)
        if (product != null) {
            _info.value = PurchaseInfo(
                productId = product,
                isLifetime = p.getBoolean("lifetime", false),
                orderId = p.getString("order", null),
                purchaseTime = p.getLong("time", 0L),
                isAutoRenewing = p.getBoolean("renewing", false),
            )
        }
    }

    /** Pass the active purchase, or null when the user no longer owns PRO. */
    fun update(context: Context, purchase: Purchase?) {
        load(context)
        val p = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (purchase == null) {
            _info.value = null
            p.edit().remove("product").remove("lifetime").remove("order").remove("time").remove("renewing").apply()
            return
        }
        val product = purchase.products.firstOrNull().orEmpty()
        val info = PurchaseInfo(
            productId = product,
            isLifetime = product.contains("lifetime"),
            orderId = purchase.orderId?.takeIf { it.isNotBlank() },
            purchaseTime = purchase.purchaseTime,
            isAutoRenewing = purchase.isAutoRenewing,
        )
        _info.value = info
        p.edit()
            .putString("product", info.productId)
            .putBoolean("lifetime", info.isLifetime)
            .putString("order", info.orderId)
            .putLong("time", info.purchaseTime)
            .putBoolean("renewing", info.isAutoRenewing)
            .apply()
    }

    fun saveEmail(context: Context, email: String?) {
        load(context)
        _savedEmail.value = email
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString("email", email).apply()
    }
}
