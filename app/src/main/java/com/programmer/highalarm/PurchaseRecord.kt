package com.programmer.highalarm

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The Gmail that owns PRO, shown in the Settings card for as long as PRO is active.
 *
 * Google Play never gives apps the buyer's email, so right after a purchase we ask
 * Android's account picker. On a phone with one Google account it answers instantly
 * with no screen at all; with several, the user taps the one they bought with once.
 */
object PurchaseRecord {
    private const val PREFS = "purchase_record"

    private val _savedEmail = MutableStateFlow<String?>(null)
    val savedEmail: StateFlow<String?> = _savedEmail.asStateFlow()

    /** True while the app should open the account picker. */
    private val _askNow = MutableStateFlow(false)
    val askNow: StateFlow<Boolean> = _askNow.asStateFlow()

    @Volatile private var loaded = false
    @Volatile private var asked = false

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(context: Context) {
        if (loaded) return
        loaded = true
        val p = prefs(context)
        _savedEmail.value = p.getString("email", null)
        asked = p.getBoolean("asked", false)
    }

    /** Detect the buying Gmail. Without [force] it only runs once per purchase. */
    fun requestGmail(context: Context, force: Boolean = false) {
        load(context)
        if (force || (_savedEmail.value == null && !asked)) _askNow.value = true
    }

    fun onPickerResult(context: Context, email: String?) {
        _askNow.value = false
        asked = true
        val e = prefs(context).edit().putBoolean("asked", true)
        if (!email.isNullOrBlank()) {
            e.putString("email", email)
            _savedEmail.value = email
        }
        e.apply()
    }

    /** Play confirmed there is no PRO purchase any more: forget it so a new purchase detects again. */
    fun clear(context: Context) {
        load(context)
        _askNow.value = false
        _savedEmail.value = null
        asked = false
        prefs(context).edit().clear().apply()
    }
}
