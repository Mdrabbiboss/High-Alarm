package com.programmer.highalarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.media.AudioAttributes as AndroidAudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.RingtoneManager
import android.media.audiofx.LoudnessEnhancer
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.google.gson.Gson

class AlarmService : Service() {

    private var exoPlayer: ExoPlayer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private val channelId = "AlarmServiceChannel"
    private val handler = Handler(Looper.getMainLooper())
    private var lastAlarmId = -1
    private var isBoostEnabled = false
    private var wakeLock: PowerManager.WakeLock? = null
    private var volumeObserver: ContentObserver? = null
    private var isMuteDuringChallenge = false
    private var isFadeInEnabled = true
    private var fadeInSeconds = 25
    private var fadeInRunnable: Runnable? = null

    private val autoSilenceRunnable = Runnable {
        if (isRinging) {
            getSharedPreferences("alarm_state", MODE_PRIVATE).edit().putBoolean("is_active", false).apply()
            stopSelf()
        }
    }

    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF && isRinging) {
                relaunchActivity()
            }
        }
    }

    private fun relaunchActivity() {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            putExtra("ALARM_TRIGGERED", true)
            putExtra("ALARM_ID", lastAlarmId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(launchIntent)
    }

    override fun onCreate() {
        super.onCreate()
        isRinging = true
        acquireWakeLock()
        createNotificationChannel()
        registerReceiver(screenOffReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF))
        
        setupVolumeObserver()
    }

    private var lastForceVolumeTime = 0L

    private fun setupVolumeObserver() {
        volumeObserver = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean) {
                val now = SystemClock.elapsedRealtime()
                if (now - lastForceVolumeTime > 1000L) {
                    lastForceVolumeTime = now
                    if ((isRinging || isPreviewActive) && isBoostEnabled) {
                        forceMaxVolume()
                    }
                }
            }
        }
        contentResolver.registerContentObserver(
            Settings.System.CONTENT_URI,
            true,
            volumeObserver!!
        )
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "HighAlarm:AlarmWakeLock")
        wakeLock?.acquire(10 * 60 * 1000L) // 10 minutes max safety
    }

    private fun forceMaxVolume() {
        try {
            val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0)
        } catch (_: Exception) {}
    }

    private var currentVibrationPattern: String? = null
    private var currentRingtoneUri: String? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Handle explicit volume control actions
        when (intent?.action) {
            "ACTION_NO_OP" -> {
                return START_NOT_STICKY
            }
            ACTION_LOWER_VOLUME -> {
                stopFadeInAnimation()
                exoPlayer?.volume = 0.38f
                return START_STICKY
            }
            ACTION_RESTORE_VOLUME -> {
                stopFadeInAnimation()
                exoPlayer?.volume = 1.0f
                return START_STICKY
            }
        }

        val isPreview = intent?.getBooleanExtra("IS_PREVIEW", false) ?: isPreviewActive
        isPreviewActive = isPreview
        
        val isBypass = intent?.getBooleanExtra("FROM_SETTINGS_BYPASS", false) ?: false
        if (isBypass) {
            isBypassActive = true
        }
        
        // Safety: If intent is null (system restart) and no alarm was ringing, just stop
        if (intent == null && !isRinging && !isPreviewActive) {
            stopSelf()
            return START_NOT_STICKY
        }

        isRinging = true // Treat preview as a ringing state for audio/volume management

        val alarmId = intent?.getIntExtra("ALARM_ID", -1) ?: -1
        
        // If it's a system restart (intent is null), we should use the last known alarmId
        val effectiveAlarmId = if (alarmId == -1) lastAlarmId else alarmId
        
        // Read settings
        val prefs = getSharedPreferences("app_settings", MODE_PRIVATE)
        val json = prefs.getString("settings_data", null)
        var isVibrationEnabled = true
        var vibrationPatternName = "Default"
        var autoSilenceMins = 15

        
        if (json != null) {
            try {
                val settings = Gson().fromJson(json, AppSettings::class.java)
                isVibrationEnabled = settings.isVibrationEnabled
                vibrationPatternName = settings.selectedVibrationPattern
                autoSilenceMins = settings.autoSilenceMinutes
                isMuteDuringChallenge = settings.isMuteDuringChallengeEnabled
                isFadeInEnabled = settings.isFadeInEnabled
                fadeInSeconds = settings.fadeInSeconds
            } catch (_: Exception) { }
        }

        val ringtoneUri = intent?.getStringExtra("RINGTONE_URI") ?: currentRingtoneUri
        isBoostEnabled = intent?.getBooleanExtra("BOOST_VOLUME", isBoostEnabled) ?: isBoostEnabled

        // If we are already ringing for this EXACT alarm/preview, don't restart everything
        if (effectiveAlarmId == lastAlarmId && effectiveAlarmId != -1 && exoPlayer?.isPlaying == true) {
            // Just update volume/vibration if needed
            forceMaxVolume()
            return START_STICKY
        }

        if (!isPreview) {
            // Persist ringing state for real alarms to prevent bypass
            getSharedPreferences("alarm_state", MODE_PRIVATE).edit().putBoolean("is_active", true).apply()
        }
        
        lastAlarmId = effectiveAlarmId
        currentAlarmId = effectiveAlarmId
        
        // Force Max Volume for the Alarm Stream (Bypass silent/DND)
        forceMaxVolume()

        if (isBoostEnabled) {
            forceMaxVolume()
        }

        val notification = createNotification(effectiveAlarmId)
        startForeground(effectiveAlarmId + 4000, notification) // Unique foreground notification ID
        
        handler.removeCallbacks(autoSilenceRunnable)

        if (!isPreview) {
            handler.postDelayed(autoSilenceRunnable, autoSilenceMins * 60 * 1000L)
        }

        playAlarmSound(ringtoneUri, isBoostEnabled)
        currentRingtoneUri = ringtoneUri
        
        if (isVibrationEnabled) {
            val vibPattern = VibrationPattern.getByName(vibrationPatternName)
            startVibration(vibPattern.pattern, vibPattern.amplitudes)
            currentVibrationPattern = vibrationPatternName
        } else {
            stopVibration()
            currentVibrationPattern = null
        }

        return if (isPreview) START_NOT_STICKY else START_STICKY
    }

    private fun startVibration(pattern: LongArray, amplitudes: IntArray? = null) {
        VibrationUtils.vibrate(this, pattern, 0, amplitudes)
    }

    private fun stopVibration() {
        VibrationUtils.stop(this)
    }

    @OptIn(UnstableApi::class)
    private fun playAlarmSound(customUri: String?, isBoosted: Boolean) {
        try {
            val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
            
            // Set Alarm stream to Max
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0)
            
            // Bypass Silent/Vibrate mode if possible (Modern Android requires DND bypass but STREAM_ALARM often works)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(AndroidAudioAttributes.Builder()
                        .setUsage(AndroidAudioAttributes.USAGE_ALARM)
                        .setContentType(AndroidAudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build())
                    .build()
                audioManager.requestAudioFocus(focusRequest)
            } else {
                @Suppress("DEPRECATION")
                audioManager.requestAudioFocus(null, AudioManager.STREAM_ALARM, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            }
        } catch (_: Exception) {}

        try {
            stopFadeInAnimation()
            exoPlayer?.stop()
            exoPlayer?.release()
            loudnessEnhancer?.release()
            exoPlayer = null
            loudnessEnhancer = null

            val alarmUri = if (!customUri.isNullOrEmpty()) {
                val rawUri = customUri.toUri()
                if (rawUri.toString() == "content://settings/system/alarm_alert" || rawUri.toString().startsWith("content://settings/")) {
                    RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM) ?: rawUri
                } else {
                    rawUri
                }
            } else {
                RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            }

            val audioAttributes = AudioAttributes.Builder()
                .setUsage(C.USAGE_ALARM)
                .setContentType(C.AUDIO_CONTENT_TYPE_SONIFICATION)
                .build()

            val finalAlarmUri = alarmUri ?: RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            if (finalAlarmUri == null) {
                // If even default is null, we can't play anything. But at least don't crash.
                return
            }

            exoPlayer = ExoPlayer.Builder(this)
                .setAudioAttributes(audioAttributes, false)
                .build().apply {
                    setMediaItem(MediaItem.fromUri(finalAlarmUri))
                    setWakeMode(C.WAKE_MODE_LOCAL)
                    repeatMode = ExoPlayer.REPEAT_MODE_ONE
                    
                    addListener(object : Player.Listener {
                        override fun onPlayerError(error: PlaybackException) {
                            playDefaultFallback()
                        }
                    })

                    // Set initial volume before prepare & play to prevent full-volume start
                    if (isFadeInEnabled) {
                        volume = 0.05f
                    } else {
                        volume = 1.0f
                    }

                    prepare()
                    play()
                    
                    if (isBoosted) {
                        try {
                            loudnessEnhancer = LoudnessEnhancer(audioSessionId).apply {
                                setTargetGain(1500) 
                                enabled = true
                            }
                        } catch (_: Exception) { }
                    }
                }

            if (isFadeInEnabled) {
                startFadeInAnimation(fadeInSeconds, 1.0f)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            playDefaultFallback()
        }
    }

    private fun startFadeInAnimation(durationSeconds: Int, targetVolume: Float = 1.0f) {
        stopFadeInAnimation()
        val player = exoPlayer ?: return
        val startVolume = 0.05f
        player.volume = startVolume

        val durationMs = (durationSeconds * 1000L).coerceAtLeast(500L)
        var fadeStartTime = 0L

        val runnable = object : Runnable {
            override fun run() {
                val p = exoPlayer
                if (p == null) {
                    return
                }

                if (!p.isPlaying) {
                    p.volume = startVolume
                    handler.postDelayed(this, 50L)
                    return
                }

                if (fadeStartTime == 0L) {
                    fadeStartTime = SystemClock.elapsedRealtime()
                }

                val elapsed = SystemClock.elapsedRealtime() - fadeStartTime
                val fraction = (elapsed.toFloat() / durationMs.toFloat()).coerceIn(0.0f, 1.0f)
                val currentVol = startVolume + fraction * (targetVolume - startVolume)
                p.volume = currentVol

                if (fraction < 1.0f) {
                    handler.postDelayed(this, 50L)
                }
            }
        }
        fadeInRunnable = runnable
        handler.post(runnable)
    }

    private fun stopFadeInAnimation() {
        fadeInRunnable?.let { handler.removeCallbacks(it) }
        fadeInRunnable = null
    }

    private fun playDefaultFallback() {
        try {
            stopFadeInAnimation()
            val defaultUri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            if (defaultUri == null) return
            exoPlayer?.stop()
            exoPlayer?.release()
            exoPlayer = ExoPlayer.Builder(this).build().apply {
                setMediaItem(MediaItem.fromUri(defaultUri))
                repeatMode = ExoPlayer.REPEAT_MODE_ONE
                if (isFadeInEnabled) {
                    volume = 0.05f
                } else {
                    volume = 1.0f
                }
                prepare()
                play()
            }
            if (isFadeInEnabled) {
                startFadeInAnimation(fadeInSeconds, 1.0f)
            }
        } catch (_: Exception) { }
    }

    private fun createNotification(alarmId: Int): Notification {
        val fullScreenIntent = if (isPreviewActive) {
            Intent(this, AlarmService::class.java).apply { action = "ACTION_NO_OP" }
        } else {
            Intent(this, MainActivity::class.java).apply {
                putExtra("ALARM_TRIGGERED", true)
                putExtra("ALARM_ID", alarmId)
                if (isBypassActive) {
                    putExtra("FROM_SETTINGS_BYPASS", true)
                }
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        }
        val fullScreenPendingIntent = if (isPreviewActive) {
            PendingIntent.getService(
                this, 0, fullScreenIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            PendingIntent.getActivity(
                this, 0, fullScreenIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        // Automatically launch activity from service for faster response - ONLY for real alarms
        if (!isPreviewActive) {
            val launchIntent = Intent(this, MainActivity::class.java).apply {
                putExtra("ALARM_TRIGGERED", true)
                putExtra("ALARM_ID", alarmId)
                if (isBypassActive) {
                    putExtra("FROM_SETTINGS_BYPASS", true)
                }
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            
            try {
                // Some devices need a small delay or specific flags to launch from background
                val pendingIntent = PendingIntent.getActivity(
                    this, alarmId, launchIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
                pendingIntent.send()
            } catch (_: Exception) {
                try {
                    startActivity(launchIntent)
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("High Alarm Ringing!")
            .setContentText("Wake up and complete your challenge!")
            .setSmallIcon(R.drawable.ic_stat_ringo)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                channelId,
                "Alarm Service Channel",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Channel for Alarm Service"
                setSound(null, null)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    override fun onDestroy() {
        isRinging = false
        isPreviewActive = false
        isBypassActive = false
        stopFadeInAnimation()
        
        // Note: We only clear the persisted state when the service is explicitly stopped by the app logic
        // This usually happens in the dismiss screen when the challenge is solved.
        
        val effectiveId = if (lastAlarmId != -1) lastAlarmId else currentAlarmId
        stopForeground(STOP_FOREGROUND_REMOVE)
        
        // Additional cleanup for unique notification ID
        if (effectiveId != -1) {
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(effectiveId + 4000)
        }
        
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
        try {
            unregisterReceiver(screenOffReceiver)
        } catch (_: Exception) {}
        
        volumeObserver?.let {
            contentResolver.unregisterContentObserver(it)
        }
        
        handler.removeCallbacks(autoSilenceRunnable)
        exoPlayer?.stop()
        exoPlayer?.release()
        loudnessEnhancer?.release()
        stopVibration()
        exoPlayer = null
        loudnessEnhancer = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        var isRinging = false
        var currentAlarmId = -1
        var isPreviewActive = false
        var isBypassActive = false
        
        const val ACTION_LOWER_VOLUME = "com.programmer.highalarm.LOWER_VOLUME"
        const val ACTION_RESTORE_VOLUME = "com.programmer.highalarm.RESTORE_VOLUME"
    }
}
