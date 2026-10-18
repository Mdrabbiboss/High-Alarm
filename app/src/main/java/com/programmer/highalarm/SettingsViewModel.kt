package com.programmer.highalarm

import android.content.Context
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AppSettings(
    val isVibrationEnabled: Boolean = true,
    val selectedVibrationPattern: String = "Default",
    val isSnoozeEnabled: Boolean = true,
    val autoSilenceMinutes: Int = 15,
    val isUpcomingAlarmAlertEnabled: Boolean = true,
    val isMuteDuringChallengeEnabled: Boolean = false,
    val isPreventUninstallEnabled: Boolean = false,
    val isPreventLastMinuteEditsEnabled: Boolean = false,
    val isFadeInEnabled: Boolean = true,
    val fadeInSeconds: Int = 25,
    val isPremium: Boolean = false,
    val activePurchaseType: String = "",
    val isPendingDisablePreventEdits: Boolean = false,
)

class SettingsViewModel : ViewModel() {
    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val prefsName = "app_settings"
    private val settingsKey = "settings_data"
    private val gson = Gson()

    /**
     * Synchronous initialization from disk.
     * Call this as early as possible (e.g. Activity onCreate) to prevent blank states.
     */
    fun init(context: Context) {
        _instance = this
        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val json = prefs.getString(settingsKey, null)
        if (json != null) {
            try {
                val loaded = gson.fromJson(json, AppSettings::class.java)
                _settings.value = loaded
            } catch (_: Exception) { }
        }
    }

    fun updateSettings(context: Context, newSettings: AppSettings) {
        _settings.value = newSettings
        viewModelScope.launch {
            val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            val json = gson.toJson(newSettings)
            prefs.edit { putString(settingsKey, json) }
        }
    }
    
    fun togglePremium(context: Context) {
        val newSettings = _settings.value.copy(isPremium = !_settings.value.isPremium)
        updateSettings(context, newSettings)
    }

    companion object {
        private var _instance: SettingsViewModel? = null
        fun getInstance(): SettingsViewModel {
            if (_instance == null) {
                _instance = SettingsViewModel()
            }
            return _instance!!
        }

        fun setPremiumStatus(context: Context, isPremium: Boolean, purchaseType: String = "") {
            val vm = getInstance()
            val currentType = vm.settings.value.activePurchaseType
            val finalType = when {
                !isPremium -> ""
                purchaseType.isNotEmpty() -> purchaseType
                currentType.isNotEmpty() -> currentType
                else -> "yearly"
            }
            val newSettings = vm.settings.value.copy(
                isPremium = isPremium,
                activePurchaseType = finalType
            )
            vm.updateSettings(context, newSettings)
        }
    }
}
