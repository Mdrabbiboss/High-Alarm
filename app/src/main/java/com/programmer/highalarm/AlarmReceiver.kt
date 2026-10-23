package com.programmer.highalarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.programmer.highalarm.UPCOMING_ALERT_ACTION") {
            val alarmTime = intent.getStringExtra("ALARM_TIME") ?: ""
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel("UpcomingAlertChannel", "Upcoming Alarms", NotificationManager.IMPORTANCE_DEFAULT)
                notificationManager.createNotificationChannel(channel)
            }
            val builder = NotificationCompat.Builder(context, "UpcomingAlertChannel")
                .setSmallIcon(R.drawable.ic_stat_ringo)
                .setContentTitle("⏰ Upcoming Alarm Alert")
                .setContentText("Your alarm will ring soon at $alarmTime")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
            notificationManager.notify(intent.getIntExtra("ALARM_ID", 0) + 7000, builder.build())
            return
        }

        if (intent.action == "DISMISS_ALARM") {
            // Check if a challenge is required. If so, DO NOT allow dismissal from here.
            // For now, we force all dismissals through the UI to ensure challenges are met.
            Log.d("AlarmReceiver", "Dismiss attempted from notification. Action ignored to force challenge.")
            return 
        }
        
        Log.d("AlarmReceiver", "Alarm received!")
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "HighAlarm:WakeLock"
        )
        wakeLock.acquire(3000) // 3 seconds

        val alarmId = intent.getIntExtra("ALARM_ID", -1)
        val fromWakeUpCheck = intent.getBooleanExtra("FROM_WAKE_UP_CHECK", false)
        
        // --- Prevent Ghost Alarms ---
        // Verify that the alarm exists and is enabled in the database
        // Bypassed if triggered by a Follow-up (Wake Up) Check
        val prefs = context.getSharedPreferences("alarms_prefs_v4", Context.MODE_PRIVATE)
        val json = prefs.getString("alarms_list", null)
        var shouldTrigger = fromWakeUpCheck
        if (!shouldTrigger && json != null) {
            try {
                val type = object : TypeToken<List<AlarmPersistenceModel>>() {}.type
                val models: List<AlarmPersistenceModel> = Gson().fromJson(json, type)
                val alarm = models.find { it.id == alarmId }
                if (alarm != null && alarm.isEnabled) {
                    shouldTrigger = true
                }
            } catch (_: Exception) {}
        }
        
        if (!shouldTrigger && alarmId != -1) {
            Log.d("AlarmReceiver", "Ghost alarm suppressed. ID $alarmId is either deleted or disabled.")
            return
        }

        val ringtoneUri = intent.getStringExtra("RINGTONE_URI")
        val isBoosted = intent.getBooleanExtra("BOOST_VOLUME", false)
        
        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            putExtra("RINGTONE_URI", ringtoneUri)
            putExtra("ALARM_ID", alarmId)
            putExtra("BOOST_VOLUME", isBoosted)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}
