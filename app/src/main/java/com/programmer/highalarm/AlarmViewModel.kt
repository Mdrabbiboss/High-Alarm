package com.programmer.highalarm

import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Calendar
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

data class ChallengeConfig(
    val id: String,
    val setting: Int = 1,
    val difficulty: Int = 0,
    val extra: String = ""
)

class AlarmViewModel : ViewModel() {
    init {
        _instance = this
    }

    companion object {
        @Volatile
        private var _instance: AlarmViewModel? = null
        fun getInstance(): AlarmViewModel? = _instance
    }

    private val _alarms = MutableStateFlow<List<Alarm>>(emptyList())
    val alarms: StateFlow<List<Alarm>> = _alarms.asStateFlow()

    private val _hasLoaded = MutableStateFlow(false)
    val hasLoaded: StateFlow<Boolean> = _hasLoaded.asStateFlow()

    val nextAlarmInfo: StateFlow<NextAlarmInfo?> = alarms.map { alarmList ->
        calculateNextAlarm(alarmList)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _selectedChallenges = MutableStateFlow<List<ChallengeConfig>>(listOf(ChallengeConfig("none")))
    val selectedChallenges: StateFlow<List<ChallengeConfig>> = _selectedChallenges.asStateFlow()

    private val challengeSessionConfigs = mutableMapOf<String, ChallengeConfig>()

    private val _availableRingtones = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val availableRingtones: StateFlow<List<Pair<String, String>>> = _availableRingtones.asStateFlow()

    private val _customRingtones = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val customRingtones: StateFlow<List<Pair<String, String>>> = _customRingtones.asStateFlow()

    private val _selectedRingtone = MutableStateFlow("Default" to "")
    val selectedRingtone: StateFlow<Pair<String, String>> = _selectedRingtone.asStateFlow()

    private val _selectedTime = MutableStateFlow("08:00 AM")
    val selectedTime: StateFlow<String> = _selectedTime.asStateFlow()

    private val _selectedDays = MutableStateFlow<List<Int>>(emptyList())
    val selectedDays: StateFlow<List<Int>> = _selectedDays.asStateFlow()

    private val _label = MutableStateFlow("")
    val label: StateFlow<String> = _label.asStateFlow()

    private val _isBoostVolumeEnabled = MutableStateFlow(value = false)
    val isBoostVolumeEnabled: StateFlow<Boolean> = _isBoostVolumeEnabled.asStateFlow()

    private val _wakeUpCheckMinutes = MutableStateFlow(0)
    val wakeUpCheckMinutes: StateFlow<Int> = _wakeUpCheckMinutes.asStateFlow()

    private val _challengeSetting = MutableStateFlow(1) // Rounds or count
    val challengeSetting: StateFlow<Int> = _challengeSetting.asStateFlow()

    private val _challengeDifficulty = MutableStateFlow(0) // 0: Easy, 1: Medium, 2: Hard, 3: Extreme
    val challengeDifficulty: StateFlow<Int> = _challengeDifficulty.asStateFlow()

    private val _challengeExtra = MutableStateFlow("") // Extra data like Image URIs
    val challengeExtra: StateFlow<String> = _challengeExtra.asStateFlow()

    private val _isPreviewActive = MutableStateFlow(false)
    val isPreviewActive: StateFlow<Boolean> = _isPreviewActive.asStateFlow()

    fun setPreviewActive(active: Boolean) {
        _isPreviewActive.value = active
    }

    var currentEditingAlarmId: Int? = null
    private val gson = Gson()
    private val prefsName = "alarms_prefs_v4" // Schema change for difficulty
    private val alarmsKey = "alarms_list"

    fun loadAlarms(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            val appSettingsPrefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            val isOnboardingCompleted = appSettingsPrefs.getBoolean("is_onboarding_completed", false)

            if (!isOnboardingCompleted) {
                prefs.edit { remove(alarmsKey) }
            }

            val json = prefs.getString(alarmsKey, null)
            
            val loadedAlarms = if (json != null) {
                try {
                    val type = object : TypeToken<List<AlarmPersistenceModel>>() {}.type
                    val models: List<AlarmPersistenceModel> = gson.fromJson(json, type)
                    models.map { it.toAlarm() }
                } catch (_: Exception) {
                    emptyList()
                }
            } else {
                emptyList()
            }

            val settingsJson = appSettingsPrefs.getString("settings_data", null)
            val isPremium = if (settingsJson != null) {
                try {
                    gson.fromJson(settingsJson, AppSettings::class.java).isPremium
                } catch (_: Exception) { false }
            } else false

            val (sanitizedAlarms, wasModified) = sanitizeAlarmsForProStatus(context, loadedAlarms, isPremium)
            
            withContext(Dispatchers.Main) {
                _alarms.value = sanitizedAlarms
                _hasLoaded.value = true
            }

            if (wasModified) {
                saveAlarms(context)
                sanitizedAlarms.forEach { alarm ->
                    if (alarm.isEnabled) {
                        AlarmScheduler.schedule(context, alarm)
                    }
                }
            }
        }
    }

    fun checkAndSanitizeProChallenges(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val appSettingsPrefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            val settingsJson = appSettingsPrefs.getString("settings_data", null)
            val isPremium = if (settingsJson != null) {
                try {
                    gson.fromJson(settingsJson, AppSettings::class.java).isPremium
                } catch (_: Exception) { false }
            } else false

            val currentAlarms = _alarms.value
            val (sanitized, wasModified) = sanitizeAlarmsForProStatus(context, currentAlarms, isPremium)
            if (wasModified) {
                withContext(Dispatchers.Main) {
                    _alarms.value = sanitized
                }
                saveAlarms(context)
                sanitized.forEach { alarm ->
                    if (alarm.isEnabled) {
                        AlarmScheduler.schedule(context, alarm)
                    }
                }
            }
        }
    }

    fun sanitizeAlarmsForProStatus(context: Context, alarmsList: List<Alarm>, isPremium: Boolean): Pair<List<Alarm>, Boolean> {
        if (isPremium) return Pair(alarmsList, false)
        
        val allChallengesMap = getAllChallenges().associateBy { it.id }
        var modified = false
        
        val sanitizedAlarms = alarmsList.map { alarm ->
            val sanitizedChallenges = alarm.challenges.filter { config ->
                val chal = allChallengesMap[config.id]
                chal != null && !chal.isPremium
            }
            
            val finalChallenges = if (sanitizedChallenges.isEmpty()) {
                listOf(ChallengeConfig("none"))
            } else {
                sanitizedChallenges
            }

            if (finalChallenges != alarm.challenges) {
                modified = true
                alarm.copy(challenges = finalChallenges)
            } else {
                alarm
            }
        }
        
        return Pair(sanitizedAlarms, modified)
    }

    private fun saveAlarms(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            val models = _alarms.value.map { AlarmPersistenceModel.fromAlarm(it) }
            val json = gson.toJson(models)
            prefs.edit { putString(alarmsKey, json) }
        }
    }

    fun loadRingtones(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val manager = RingtoneManager(context)
            manager.setType(RingtoneManager.TYPE_ALARM)
            val list = mutableListOf<Pair<String, String>>()
            
            // Add Default System Alarm Sound
            val defaultUri = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            list.add("Default" to (defaultUri?.toString() ?: ""))
            
            try {
                val cursor = manager.cursor
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX)
                        val uri = manager.getRingtoneUri(cursor.position).toString()
                        if (title != null && uri.isNotEmpty()) {
                            list.add(title to uri)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Load Custom Ringtones from Prefs
            val prefs = context.getSharedPreferences("custom_ringtones", Context.MODE_PRIVATE)
            val customJson = prefs.getString("list", null)
            val loadedCustom = if (customJson == null) {
                // Fresh app launch / cleared app data: initialize built-in "Military Alarm Sound FX"
                val builtIn = ensureBuiltInCustomRingtone(context)
                val initialList = listOf(builtIn)
                prefs.edit { putString("list", gson.toJson(initialList)) }
                initialList
            } else {
                try {
                    val type = object : TypeToken<List<Pair<String, String>>>() {}.type
                    val rawList: List<Pair<String, String>> = gson.fromJson(customJson, type)

                    var modified = false
                    val migratedList = rawList.map { (name, uriStr) ->
                        val cleanName = sanitizeDisplayName(name)
                        if (cleanName != name) {
                            modified = true
                        }
                        val parsedUri = try { Uri.parse(uriStr) } catch (_: Exception) { null }
                        if (parsedUri != null && parsedUri.scheme == "content") {
                            val copyResult = copyAudioToAppStorage(context, parsedUri)
                            if (copyResult != null) {
                                modified = true
                                val finalCleanName = sanitizeDisplayName(if (name.startsWith("audio:") || name == "Audio" || name == "Custom Sound") copyResult.first else name)
                                finalCleanName to copyResult.second
                            } else {
                                cleanName to uriStr
                            }
                        } else {
                            cleanName to uriStr
                        }
                    }
                    if (modified) {
                        prefs.edit { putString("list", gson.toJson(migratedList)) }
                    }
                    migratedList
                } catch (_: Exception) { emptyList() }
            }

            withContext(Dispatchers.Main) {
                _availableRingtones.value = list
                _customRingtones.value = loadedCustom
                if ((_selectedRingtone.value.second.isEmpty() || _selectedRingtone.value.first == "Default") && list.isNotEmpty()) {
                    val defaultItem = list.firstOrNull { it.first == "Default" } ?: list.first()
                    _selectedRingtone.value = defaultItem
                }
            }
        }
    }

    fun sanitizeDisplayName(rawName: String): String {
        if (rawName.isBlank()) return rawName
        val audioExtensions = listOf(".mp3", ".wav", ".m4a", ".aac", ".ogg", ".flac", ".wma", ".opus", ".amr", ".3gp", ".mid")
        var cleaned = rawName.trim()
        for (ext in audioExtensions) {
            if (cleaned.endsWith(ext, ignoreCase = true)) {
                cleaned = cleaned.substring(0, cleaned.length - ext.length).trim()
                break
            }
        }
        return if (cleaned.isNotEmpty()) cleaned else rawName
    }

    private fun ensureBuiltInCustomRingtone(context: Context): Pair<String, String> {
        val ringtoneDir = File(context.filesDir, "custom_ringtones")
        if (!ringtoneDir.exists()) {
            ringtoneDir.mkdirs()
        }
        val targetFile = File(ringtoneDir, "military_alarm_sound_fx.mp3")

        if (!targetFile.exists() || targetFile.length() == 0L) {
            try {
                val resId = context.resources.getIdentifier("military_alarm_sound_fx", "raw", context.packageName)
                if (resId != 0) {
                    context.resources.openRawResource(resId).use { input ->
                        targetFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                } else {
                    generateMilitaryAlarmAudioFile(targetFile)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                generateMilitaryAlarmAudioFile(targetFile)
            }
        }

        val persistentUri = Uri.fromFile(targetFile).toString()
        return "Military Alarm Sound FX" to persistentUri
    }

    private fun generateMilitaryAlarmAudioFile(targetFile: File) {
        try {
            val sampleRate = 44100
            val durationSec = 4.0
            val numSamples = (sampleRate * durationSec).toInt()
            val bytesPerSample = 2
            val dataSize = numSamples * bytesPerSample
            val fileSize = 36 + dataSize

            val header = ByteArray(44)
            val buffer = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
            buffer.put("RIFF".toByteArray(Charsets.US_ASCII))
            buffer.putInt(fileSize)
            buffer.put("WAVE".toByteArray(Charsets.US_ASCII))
            buffer.put("fmt ".toByteArray(Charsets.US_ASCII))
            buffer.putInt(16) // Subchunk1Size
            buffer.putShort(1) // AudioFormat (PCM)
            buffer.putShort(1) // NumChannels (Mono)
            buffer.putInt(sampleRate)
            buffer.putInt(sampleRate * bytesPerSample)
            buffer.putShort(bytesPerSample.toShort())
            buffer.putShort(16) // BitsPerSample
            buffer.put("data".toByteArray(Charsets.US_ASCII))
            buffer.putInt(dataSize)

            targetFile.outputStream().use { output ->
                output.write(header)
                val sampleBuffer = ByteBuffer.allocate(2048).order(ByteOrder.LITTLE_ENDIAN)
                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate.toDouble()
                    val cycleT = t % 1.0
                    val sample: Short = if (cycleT < 0.15 || (cycleT in 0.25..0.40) || (cycleT in 0.50..0.65)) {
                        val freq1 = 950.0
                        val freq2 = 1450.0
                        val valSample = 0.5 * Math.sin(2.0 * Math.PI * freq1 * t) + 0.5 * Math.sin(2.0 * Math.PI * freq2 * t)
                        val envelope = Math.sin(Math.PI * (cycleT % 0.15) / 0.15)
                        (valSample * envelope * 24000.0).toInt().coerceIn(-32768, 32767).toShort()
                    } else {
                        0
                    }
                    if (!sampleBuffer.hasRemaining()) {
                        output.write(sampleBuffer.array(), 0, sampleBuffer.position())
                        sampleBuffer.clear()
                    }
                    sampleBuffer.putShort(sample)
                }
                if (sampleBuffer.position() > 0) {
                    output.write(sampleBuffer.array(), 0, sampleBuffer.position())
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getOriginalFileName(context: Context, uri: Uri): String {
        var name: String? = null
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(
                    uri,
                    arrayOf(OpenableColumns.DISPLAY_NAME),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx != -1) {
                            name = cursor.getString(idx)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        var finalName = name
        if (finalName.isNullOrEmpty()) {
            finalName = uri.lastPathSegment?.substringAfterLast('/')
        }

        if (finalName.isNullOrEmpty() || finalName.startsWith("audio:")) {
            try {
                val ringtone = RingtoneManager.getRingtone(context, uri)
                val title = ringtone?.getTitle(context)
                if (!title.isNullOrEmpty() && !title.startsWith("audio:") && title != "Audio" && title != "Custom Sound") {
                    finalName = title
                }
            } catch (_: Exception) {}
        }

        if (finalName.isNullOrEmpty() || finalName.startsWith("audio:")) {
            finalName = "Custom Ringtone.mp3"
        }

        return sanitizeDisplayName(finalName)
    }

    private fun copyAudioToAppStorage(context: Context, sourceUri: Uri): Pair<String, String>? {
        return try {
            val displayName = sanitizeDisplayName(getOriginalFileName(context, sourceUri))
            val ringtoneDir = File(context.filesDir, "custom_ringtones")
            if (!ringtoneDir.exists()) {
                ringtoneDir.mkdirs()
            }

            val rawOriginal = getOriginalFileName(context, sourceUri)
            val extension = if (rawOriginal.contains(".")) {
                ".${rawOriginal.substringAfterLast('.')}"
            } else {
                ".mp3"
            }

            val tempFile = File.createTempFile("temp_ringtone_", extension, ringtoneDir)

            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            if (!tempFile.exists() || tempFile.length() == 0L) {
                if (tempFile.exists()) tempFile.delete()
                return null
            }

            val uniqueId = "${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}"
            val permanentFile = File(ringtoneDir, "$uniqueId$extension")

            if (!tempFile.renameTo(permanentFile)) {
                tempFile.copyTo(permanentFile, overwrite = true)
                tempFile.delete()
            }

            val persistentUriString = Uri.fromFile(permanentFile).toString()
            displayName to persistentUriString
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun importAndAddCustomRingtone(context: Context, sourceUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = copyAudioToAppStorage(context, sourceUri)
            if (result != null) {
                val (displayName, persistentUri) = result
                withContext(Dispatchers.Main) {
                    addCustomRingtone(context, displayName, persistentUri)
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed to import custom ringtone", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun addCustomRingtone(context: Context, name: String, uri: String) {
        val cleanName = sanitizeDisplayName(name)
        val newList = _customRingtones.value.filter { it.second != uri && it.first != cleanName } + (cleanName to uri)
        _customRingtones.value = newList
        saveCustomRingtones(context, newList)
        selectRingtone(cleanName, uri)
    }

    fun removeCustomRingtone(context: Context, uri: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val parsedUri = Uri.parse(uri)
                val path = parsedUri.path
                if (path != null) {
                    val file = File(path)
                    if (file.exists() && file.canonicalPath.contains("custom_ringtones")) {
                        file.delete()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            val newList = _customRingtones.value.filter { it.second != uri }
            withContext(Dispatchers.Main) {
                _customRingtones.value = newList
                saveCustomRingtones(context, newList)

                if (_selectedRingtone.value.second == uri) {
                    _selectedRingtone.value = "Default" to (RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)?.toString() ?: "")
                }
            }
        }
    }

    private fun saveCustomRingtones(context: Context, list: List<Pair<String, String>>) {
        viewModelScope.launch(Dispatchers.IO) {
            val prefs = context.getSharedPreferences("custom_ringtones", Context.MODE_PRIVATE)
            prefs.edit { putString("list", gson.toJson(list)) }
        }
    }

    fun toggleAlarm(context: Context, id: Int) {
        val original = _alarms.value.find { it.id == id }
        if (original != null && isAlarmLastMinuteLocked(context, original)) {
            Toast.makeText(context, "This alarm is locked (Last Minute Edits blocked)", Toast.LENGTH_SHORT).show()
            return
        }
        _alarms.value = _alarms.value.map {
            if (it.id == id) {
                val newState = !it.isEnabled
                if (newState) {
                    AlarmScheduler.schedule(context, it.copy(isEnabled = true))
                } else {
                    AlarmScheduler.cancel(context, it.id)
                }
                it.copy(isEnabled = newState)
            } else it
        }
        saveAlarms(context)
    }

    fun selectChallenge(context: Context, challenge: Challenge, isPremium: Boolean) {
        val currentList = _selectedChallenges.value.toMutableList()
        val existing = currentList.find { it.id == challenge.id }
        
        if (challenge.id == "none") {
            _selectedChallenges.value = listOf(ChallengeConfig("none"))
            return
        }

        if (existing != null) {
            // Unselect if there are others, or if it's the only one, switch to none
            currentList.remove(existing)
            if (currentList.isEmpty()) {
                currentList.add(ChallengeConfig("none"))
            }
        } else {
            // Select new
            currentList.removeIf { it.id == "none" }
            
            if (!isPremium && currentList.size >= 2) {
                Toast.makeText(context, "Go premium to select more challenges at once", Toast.LENGTH_SHORT).show()
                return
            }
            
            // Add with smart defaults or existing session config
            val config = challengeSessionConfigs[challenge.id] ?: when (challenge) {
                is Challenge.LightsOn -> ChallengeConfig(challenge.id, setting = 100)
                is Challenge.Shake -> ChallengeConfig(challenge.id, setting = 30)
                is Challenge.Steps -> ChallengeConfig(challenge.id, setting = 30)
                is Challenge.PushUp -> ChallengeConfig(challenge.id, setting = 15)
                is Challenge.Squat -> ChallengeConfig(challenge.id, setting = 15, extra = "camera")
                is Challenge.BalanceTest -> ChallengeConfig(challenge.id, setting = 15, difficulty = 0)
                is Challenge.SafeCode -> ChallengeConfig(challenge.id, setting = 1, difficulty = 0)
                is Challenge.ControlPanel -> ChallengeConfig(challenge.id, setting = 1, difficulty = 0)
                is Challenge.ScanObject -> ChallengeConfig(challenge.id, setting = 1, extra = ObjectCategories.list.joinToString(","))
                is Challenge.Tap -> ChallengeConfig(challenge.id, setting = 30)
                is Challenge.Rewrite -> ChallengeConfig(challenge.id, setting = 0)
                is Challenge.QRCode -> ChallengeConfig(challenge.id, setting = 0)
                is Challenge.ScanSink -> ChallengeConfig(challenge.id, setting = 0)
                else -> ChallengeConfig(challenge.id)
            }.also {
                challengeSessionConfigs[challenge.id] = it
            }
            currentList.add(config)
        }
        _selectedChallenges.value = currentList
    }

    fun setSelectedChallenges(configs: List<ChallengeConfig>) {
        _selectedChallenges.value = configs
    }

    fun selectRingtone(name: String, uri: String) {
        val cleanName = sanitizeDisplayName(name)
        _selectedRingtone.value = cleanName to uri
        if (!_availableRingtones.value.any { it.second == uri }) {
            _availableRingtones.value = listOf(cleanName to uri) + _availableRingtones.value
        }
    }

    fun handleRingtonePicked(context: Context, pickedUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val defaultSystemUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val defaultActualUri = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM)

            var ringtoneName = "Default"
            var ringtoneUriStr = pickedUri.toString()

            val isDefaultUri = pickedUri == defaultSystemUri || 
                    pickedUri == defaultActualUri || 
                    pickedUri.toString() == "content://settings/system/alarm_alert"

            if (isDefaultUri) {
                ringtoneName = "Default"
                ringtoneUriStr = defaultActualUri?.toString() ?: defaultSystemUri?.toString() ?: ""
            } else {
                if (pickedUri.scheme == "content") {
                    try {
                        context.contentResolver.takePersistableUriPermission(
                            pickedUri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    } catch (_: Exception) {}
                }

                val isCustomContent = pickedUri.scheme == "content" && !pickedUri.toString().contains("media/internal")

                if (isCustomContent) {
                    val copyResult = copyAudioToAppStorage(context, pickedUri)
                    if (copyResult != null) {
                        ringtoneName = sanitizeDisplayName(copyResult.first)
                        ringtoneUriStr = copyResult.second
                        addCustomRingtone(context, ringtoneName, ringtoneUriStr)
                    } else {
                        val title = try {
                            RingtoneManager.getRingtone(context, pickedUri)?.getTitle(context)
                        } catch (_: Exception) { null }
                        ringtoneName = sanitizeDisplayName(title ?: getOriginalFileName(context, pickedUri))
                    }
                } else {
                    val title = try {
                        RingtoneManager.getRingtone(context, pickedUri)?.getTitle(context)
                    } catch (_: Exception) { null }
                    ringtoneName = sanitizeDisplayName(title ?: getOriginalFileName(context, pickedUri))
                }
            }

            withContext(Dispatchers.Main) {
                selectRingtone(ringtoneName, ringtoneUriStr)
            }
        }
    }

    fun selectTime(time: String) {
        _selectedTime.value = time
    }

    fun toggleDay(day: Int) {
        _selectedDays.value = if (_selectedDays.value.contains(day)) {
            _selectedDays.value - day
        } else {
            _selectedDays.value + day
        }
    }

    fun setRepeatDays(days: List<Int>) {
        _selectedDays.value = days
    }

    fun updateLabel(newLabel: String) {
        _label.value = newLabel
    }

    fun toggleBoostVolume(enabled: Boolean) {
        _isBoostVolumeEnabled.value = enabled
    }

    fun updateWakeUpCheck(minutes: Int) {
        _wakeUpCheckMinutes.value = minutes
    }

    fun getChallengeConfig(id: String): ChallengeConfig {
        return challengeSessionConfigs[id] ?: _selectedChallenges.value.find { it.id == id } ?: when (id) {
            "lightson" -> ChallengeConfig(id, setting = 100)
            "shake" -> ChallengeConfig(id, setting = 30)
            "steps" -> ChallengeConfig(id, setting = 30)
            "pushup" -> ChallengeConfig(id, setting = 15)
            "squat" -> ChallengeConfig(id, setting = 15, extra = "camera")
            "scan_object" -> ChallengeConfig(id, setting = 1, extra = ObjectCategories.list.joinToString(","))
            "tap" -> ChallengeConfig(id, setting = 30)
            "rewrite" -> ChallengeConfig(id, setting = 0)
            "qr_code" -> ChallengeConfig(id, setting = 0)
            "sink" -> ChallengeConfig(id, setting = 0)
            "balance_test" -> ChallengeConfig(id, setting = 15, difficulty = 0)
            "safe_code" -> ChallengeConfig(id, setting = 1, difficulty = 0)
            "control_panel" -> ChallengeConfig(id, setting = 1, difficulty = 0)
            else -> ChallengeConfig(id)
        }.also {
            challengeSessionConfigs[id] = it
        }
    }

    fun updateChallengeSetting(id: String, value: Int) {
        val newConfig = getChallengeConfig(id).copy(setting = value)
        challengeSessionConfigs[id] = newConfig

        val list = _selectedChallenges.value.toMutableList()
        val index = list.indexOfFirst { it.id == id }
        if (index != -1) {
            list[index] = newConfig
            _selectedChallenges.value = list
        }
    }

    fun updateChallengeDifficulty(id: String, value: Int) {
        val newConfig = getChallengeConfig(id).copy(difficulty = value)
        challengeSessionConfigs[id] = newConfig

        val list = _selectedChallenges.value.toMutableList()
        val index = list.indexOfFirst { it.id == id }
        if (index != -1) {
            list[index] = newConfig
            _selectedChallenges.value = list
        }
    }

    fun updateChallengeExtra(id: String, value: String) {
        val newConfig = getChallengeConfig(id).copy(extra = value)
        challengeSessionConfigs[id] = newConfig

        val list = _selectedChallenges.value.toMutableList()
        val index = list.indexOfFirst { it.id == id }
        if (index != -1) {
            list[index] = newConfig
            _selectedChallenges.value = list
        }
    }

    fun getDefaultRingtoneUri(context: Context): String {
        val actual = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM)
        if (actual != null) return actual.toString()
        return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)?.toString() ?: ""
    }

    fun prepareEditSession(context: Context, alarmId: Int?) {
        challengeSessionConfigs.clear()
        currentEditingAlarmId = alarmId
        val defaultUri = _availableRingtones.value.firstOrNull { it.first == "Default" }?.second
            .takeIf { !it.isNullOrEmpty() } ?: getDefaultRingtoneUri(context)

        if (alarmId != null) {
            val alarm = _alarms.value.find { it.id == alarmId }
            alarm?.let {
                _selectedChallenges.value = it.challenges
                it.challenges.forEach { config ->
                    challengeSessionConfigs[config.id] = config
                }
                val rName = if (it.ringtoneName.isBlank()) "Default" else it.ringtoneName
                val rUri = if (it.ringtoneUri.isBlank() && rName == "Default") defaultUri else it.ringtoneUri
                _selectedRingtone.value = rName to rUri
                _selectedTime.value = it.time
                _selectedDays.value = it.repeatDays
                _label.value = it.label
                _isBoostVolumeEnabled.value = it.isBoostVolumeEnabled
                _wakeUpCheckMinutes.value = it.wakeUpCheckMinutes
            }
        } else {
            val now = Calendar.getInstance()
            val h = now.get(Calendar.HOUR)
            val hourFormatted = if (h == 0) 12 else h
            val minuteFormatted = now.get(Calendar.MINUTE)
            val amPmFormatted = if (now.get(Calendar.AM_PM) == Calendar.AM) "AM" else "PM"
            val currentTime = String.format(Locale.getDefault(), "%02d:%02d %s", hourFormatted, minuteFormatted, amPmFormatted)

            _selectedChallenges.value = listOf(ChallengeConfig("none"))
            _selectedRingtone.value = "Default" to defaultUri
            _selectedTime.value = currentTime
            _selectedDays.value = listOf(0, 1, 2, 3, 4, 5, 6)
            _label.value = ""
            _isBoostVolumeEnabled.value = false
            _wakeUpCheckMinutes.value = 0
        }
    }

    fun prepareEditSession(alarmId: Int?) {
        val defaultUri = _availableRingtones.value.firstOrNull { it.first == "Default" }?.second
            .takeIf { !it.isNullOrEmpty() } ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)?.toString() ?: ""

        if (alarmId != null) {
            val alarm = _alarms.value.find { it.id == alarmId }
            alarm?.let {
                _selectedChallenges.value = it.challenges
                it.challenges.forEach { config ->
                    challengeSessionConfigs[config.id] = config
                }
                val rName = if (it.ringtoneName.isBlank()) "Default" else it.ringtoneName
                val rUri = if (it.ringtoneUri.isBlank() && rName == "Default") defaultUri else it.ringtoneUri
                _selectedRingtone.value = rName to rUri
                _selectedTime.value = it.time
                _selectedDays.value = it.repeatDays
                _label.value = it.label
                _isBoostVolumeEnabled.value = it.isBoostVolumeEnabled
                _wakeUpCheckMinutes.value = it.wakeUpCheckMinutes
            }
        } else {
            val now = Calendar.getInstance()
            val h = now.get(Calendar.HOUR)
            val hourFormatted = if (h == 0) 12 else h
            val minuteFormatted = now.get(Calendar.MINUTE)
            val amPmFormatted = if (now.get(Calendar.AM_PM) == Calendar.AM) "AM" else "PM"
            val currentTime = String.format(Locale.getDefault(), "%02d:%02d %s", hourFormatted, minuteFormatted, amPmFormatted)

            _selectedChallenges.value = listOf(ChallengeConfig("none"))
            _selectedRingtone.value = "Default" to defaultUri
            _selectedTime.value = currentTime
            _selectedDays.value = listOf(0, 1, 2, 3, 4, 5, 6)
            _label.value = ""
            _isBoostVolumeEnabled.value = false
            _wakeUpCheckMinutes.value = 0
        }
    }

    fun saveAlarm(context: Context) {
        val id = currentEditingAlarmId
        val time = _selectedTime.value

        val appSettingsPrefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val settingsJson = appSettingsPrefs.getString("settings_data", null)
        val isPremium = if (settingsJson != null) {
            try {
                gson.fromJson(settingsJson, AppSettings::class.java).isPremium
            } catch (_: Exception) { false }
        } else false

        val rawChallenges = _selectedChallenges.value
        val (sanitizedAlarms, _) = sanitizeAlarmsForProStatus(context, listOf(Alarm(0, time, isEnabled = true, challenges = rawChallenges)), isPremium)
        val challenges = sanitizedAlarms.firstOrNull()?.challenges ?: listOf(ChallengeConfig("none"))

        var ringtoneName = _selectedRingtone.value.first
        var ringtoneUri = _selectedRingtone.value.second

        if (ringtoneUri.isBlank() || (ringtoneName == "Default" && ringtoneUri.isBlank())) {
            ringtoneName = "Default"
            ringtoneUri = getDefaultRingtoneUri(context)
        }
        val repeatDays = _selectedDays.value
        val label = _label.value
        val isBoostVolumeEnabled = _isBoostVolumeEnabled.value
        val wakeUpCheckMinutes = _wakeUpCheckMinutes.value

        if (id != null) {
            val original = _alarms.value.find { it.id == id }
            if (original != null && isAlarmLastMinuteLocked(context, original)) {
                Toast.makeText(context, "Cannot save changes: Alarm is locked (Last Minute Edits blocked)", Toast.LENGTH_SHORT).show()
                return
            }
            _alarms.value = _alarms.value.map {
                if (it.id == id) {
                    val updated = it.copy(
                        time = time,
                        challenges = challenges,
                        ringtoneName = ringtoneName,
                        ringtoneUri = ringtoneUri,
                        repeatDays = repeatDays,
                        label = label,
                        isBoostVolumeEnabled = isBoostVolumeEnabled,
                        wakeUpCheckMinutes = wakeUpCheckMinutes
                    )
                    if (updated.isEnabled) {
                        AlarmScheduler.schedule(context, updated)
                    }
                    updated
                } else it
            }
        } else {
            val existingIdentical = _alarms.value.find { 
                it.time == time && it.repeatDays == repeatDays && it.challenges == challenges 
            }
            if (existingIdentical != null) {
                return
            }

            val newId = (_alarms.value.maxOfOrNull { it.id } ?: 0) + 1
            val newAlarm = Alarm(
                id = newId,
                time = time,
                isEnabled = true,
                challenges = challenges,
                ringtoneName = ringtoneName,
                ringtoneUri = ringtoneUri,
                repeatDays = repeatDays,
                label = label,
                isBoostVolumeEnabled = isBoostVolumeEnabled,
                wakeUpCheckMinutes = wakeUpCheckMinutes
            )
            _alarms.value += newAlarm
            AlarmScheduler.schedule(context, newAlarm)
        }
        saveAlarms(context)
        currentEditingAlarmId = null
    }

    fun deleteAlarm(context: Context, id: Int) {
        val original = _alarms.value.find { it.id == id }
        if (original != null && isAlarmLastMinuteLocked(context, original)) {
            Toast.makeText(context, "Cannot delete: Alarm is locked (Last Minute Edits blocked)", Toast.LENGTH_SHORT).show()
            return
        }
        AlarmScheduler.cancel(context, id)
        _alarms.value = _alarms.value.filter { it.id != id }
        saveAlarms(context)
    }

    fun isAlarmLastMinuteLocked(context: Context, alarm: Alarm): Boolean {
        val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val json = prefs.getString("settings_data", null)
        var isPreventEnabled = false
        if (json != null) {
            try {
                val settings = gson.fromJson(json, AppSettings::class.java)
                isPreventEnabled = settings.isPreventLastMinuteEditsEnabled
            } catch (_: Exception) {}
        }
        
        if (!isPreventEnabled) return false

        // If alarm is currently ringing
        if (AlarmService.isRinging && AlarmService.currentAlarmId == alarm.id) {
            return true
        }

        if (!alarm.isEnabled) return false

        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance().apply {
            val parts = alarm.time.split(":")
            val hourStr = parts[0]
            val minuteAmPm = parts[1].split(" ")
            val minute = minuteAmPm[0].toInt()
            val amPm = minuteAmPm[1]

            var hour = hourStr.toInt()
            if (amPm == "PM" && hour < 12) hour += 12
            if (amPm == "AM" && hour == 12) hour = 0

            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            if (alarm.repeatDays.isEmpty()) {
                if (timeInMillis <= now + 10000) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            } else {
                val currentDayOfWeek = get(Calendar.DAY_OF_WEEK) - 1
                if (!alarm.repeatDays.contains(currentDayOfWeek) || timeInMillis <= now + 10000) {
                    var daysToAdd = 1
                    while (!alarm.repeatDays.contains((currentDayOfWeek + daysToAdd) % 7)) {
                        daysToAdd++
                    }
                    add(Calendar.DAY_OF_YEAR, daysToAdd)
                }
            }
        }

        val diff = calendar.timeInMillis - now
        return diff in 0..(30 * 60 * 1000L)
    }

    fun duplicateAlarm(context: Context, id: Int) {
        val original = _alarms.value.find { it.id == id } ?: return
        val newId = (_alarms.value.maxOfOrNull { it.id } ?: 0) + 1
        val duplicated = original.copy(id = newId, isEnabled = true)
        _alarms.value += duplicated
        AlarmScheduler.schedule(context, duplicated)
        saveAlarms(context)
    }

    private fun calculateNextAlarm(alarmList: List<Alarm>): NextAlarmInfo? {
        val enabledAlarms = alarmList.filter { it.isEnabled }
        if (enabledAlarms.isEmpty()) return null

        val now = Calendar.getInstance()
        var earliestTime: Long = Long.MAX_VALUE
        var selectedAlarmTime: String? = null

        enabledAlarms.forEach { alarm ->
            val alarmCal = Calendar.getInstance().apply {
                val parts = alarm.time.split(":")
                val hourStr = parts[0]
                val minuteAmPm = parts[1].split(" ")
                val minute = minuteAmPm[0].toInt()
                val amPm = minuteAmPm[1]

                var hour = hourStr.toInt()
                if (amPm == "PM" && hour < 12) hour += 12
                if (amPm == "AM" && hour == 12) hour = 0

                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)

                if (timeInMillis <= now.timeInMillis) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }
            
            if (alarmCal.timeInMillis < earliestTime) {
                earliestTime = alarmCal.timeInMillis
                selectedAlarmTime = alarm.time
            }
        }

        selectedAlarmTime?.let {
            val diff = earliestTime - now.timeInMillis
            val hours = TimeUnit.MILLISECONDS.toHours(diff)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(diff) % 60
            
            val remaining = buildString {
                if (hours > 0) append("${hours}h ")
                append("${minutes}m")
            }
            
            return NextAlarmInfo(it, remaining)
        }
        return null
    }
}

data class NextAlarmInfo(
    val time: String,
    val remainingFormatted: String
)

data class Alarm(
    val id: Int,
    val time: String,
    val isEnabled: Boolean,
    val challenges: List<ChallengeConfig>,
    val ringtoneName: String = "Default",
    val ringtoneUri: String = "",
    val repeatDays: List<Int> = emptyList(), 
    val label: String = "",
    val isBoostVolumeEnabled: Boolean = false,
    val wakeUpCheckMinutes: Int = 0
)

data class AlarmPersistenceModel(
    val id: Int,
    val time: String,
    val isEnabled: Boolean,
    val ringtoneName: String,
    val ringtoneUri: String,
    val repeatDays: List<Int> = emptyList(),
    val label: String = "",
    val isBoostVolumeEnabled: Boolean = false,
    val wakeUpCheckMinutes: Int = 0,
    val challengesJson: String = "" // Multi-challenge storage
) {
    fun toAlarm(): Alarm {
        val type = object : TypeToken<List<ChallengeConfig>>() {}.type
        val configs: List<ChallengeConfig> = if (challengesJson.isNotEmpty()) {
            Gson().fromJson(challengesJson, type)
        } else emptyList()
        
        return Alarm(
            id, time, isEnabled, configs, ringtoneName, ringtoneUri,
            repeatDays, label, isBoostVolumeEnabled, wakeUpCheckMinutes
        )
    }

    companion object {
        fun fromAlarm(alarm: Alarm): AlarmPersistenceModel {
            return AlarmPersistenceModel(
                alarm.id, alarm.time, alarm.isEnabled,
                alarm.ringtoneName, alarm.ringtoneUri,
                alarm.repeatDays, alarm.label, alarm.isBoostVolumeEnabled, 
                alarm.wakeUpCheckMinutes, Gson().toJson(alarm.challenges)
            )
        }
    }
}
