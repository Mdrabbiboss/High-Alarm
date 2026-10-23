package com.programmer.highalarm

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.content.edit

class WakeUpCheckReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "HighAlarm:WakeUpCheckReceiver")
        wakeLock.acquire(10000) // 10 seconds

        try {
            val alarmId = intent.getIntExtra("ALARM_ID", -1)
            if (alarmId == -1) return

            val ringtoneUri = intent.getStringExtra("RINGTONE_URI")
            val isBoosted = intent.getBooleanExtra("BOOST_VOLUME", false)
            val checkMins = intent.getIntExtra("WAKE_UP_CHECK_MINS", 5)

            when (intent.action) {
                ACTION_START_WAKE_UP_CHECK -> {
                    // This is the trigger to SHOW THE CONFIRMATION NOTIFICATION (Always at T+1m)
                    showFollowUpNotification(context, alarmId, ringtoneUri, isBoosted, checkMins, isWarning = false)
                }
                ACTION_FOLLOW_UP_NOTIFICATION -> {
                    // This is the trigger to SHOW THE WARNING NOTIFICATION (Always at Interval - 30s)
                    showFollowUpNotification(context, alarmId, ringtoneUri, isBoosted, checkMins, isWarning = true)
                }
                ACTION_CONFIRM_AWAKE -> {
                    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    
                    // --- 1. SET STATE FIRST (To prevent race with dismissal recovery) ---
                    val prefs = context.getSharedPreferences("active_wake_up_checks", Context.MODE_PRIVATE)
                    
                    // Check if already confirmed to avoid duplicate activity launches/actions
                    if (!prefs.contains("check_$alarmId")) {
                        // If it's already gone from prefs, it might have been cleared by a previous click
                        nm.cancel(alarmId + 1000) // Confirmation ID
                        nm.cancel(alarmId + 5000) // Warning ID
                        return
                    }

                    // Mark as completed by removing the pending check keys
                    prefs.edit { 
                        remove("check_$alarmId")
                        remove("state_$alarmId")
                    }

                    // --- 2. CANCEL ALL NOTIFICATIONS ---
                    nm.cancel(alarmId + 1000)
                    nm.cancel(alarmId + 5000)

                    // --- 3. CANCEL ALL PENDING TIMERS ---
                    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                    
                    // Cancel re-trigger alarm
                    val reTriggerIntent = Intent(context, WakeUpCheckReceiver::class.java).apply { action = ACTION_RE_TRIGGER_ALARM }
                    val reTriggerPi = PendingIntent.getBroadcast(context, alarmId + 2000, reTriggerIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                    alarmManager.cancel(reTriggerPi)

                    // Cancel confirmation trigger
                    val startIntent = Intent(context, WakeUpCheckReceiver::class.java).apply { action = ACTION_START_WAKE_UP_CHECK }
                    val startPi = PendingIntent.getBroadcast(context, alarmId + 500, startIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                    alarmManager.cancel(startPi)
                    
                    // Cancel warning trigger
                    val warningIntent = Intent(context, WakeUpCheckReceiver::class.java).apply { action = ACTION_FOLLOW_UP_NOTIFICATION }
                    val warningPi = PendingIntent.getBroadcast(context, alarmId + 1500, warningIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                    alarmManager.cancel(warningPi)

                    // --- 4. LAUNCH SUCCESS UI ---
                    val activityIntent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra("SHOW_GOOD_MORNING", true)
                        putExtra("CONFIRMED_ALARM_ID", alarmId)
                    }
                    context.startActivity(activityIntent)
                }
                ACTION_NOTIFICATION_DISMISSED -> {
                    // Safety Layer: Check if confirmation was already completed
                    val prefs = context.getSharedPreferences("active_wake_up_checks", Context.MODE_PRIVATE)
                    val isWarning = intent.getBooleanExtra("EXTRA_IS_WARNING", false)
                    if (prefs.contains("check_$alarmId")) {
                        // User swiped it away but didn't confirm. RE-POST.
                        showFollowUpNotification(context, alarmId, ringtoneUri, isBoosted, checkMins, isWarning)
                    }
                }
                ACTION_RE_TRIGGER_ALARM -> {
                    // Dismiss the check notification (Consolidated ID)
                    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    nm.cancel(alarmId + 1000)

                    // Clear check state
                    context.getSharedPreferences("active_wake_up_checks", Context.MODE_PRIVATE)
                        .edit { 
                            remove("check_$alarmId")
                            remove("state_$alarmId")
                        }

                    // User didn't confirm, start alarm again!
                    val alarmIntent = Intent(context, AlarmReceiver::class.java).apply {
                        action = "com.programmer.highalarm.ALARM_ACTION"
                        putExtra("ALARM_ID", alarmId)
                        putExtra("RINGTONE_URI", ringtoneUri)
                        putExtra("BOOST_VOLUME", isBoosted)
                        putExtra("FROM_WAKE_UP_CHECK", true)
                    }
                    context.sendBroadcast(alarmIntent)
                }
                else -> {
                    // Fallback for any unexpected triggers (matches ACTION_START_WAKE_UP_CHECK logic)
                    showFollowUpNotification(context, alarmId, ringtoneUri, isBoosted, checkMins)
                    scheduleReTrigger(context, alarmId, ringtoneUri, isBoosted, checkMins)
                }
            }
        } finally {
            if (wakeLock.isHeld) wakeLock.release()
        }
    }

    private fun scheduleReTrigger(context: Context, alarmId: Int, ringtoneUri: String?, isBoosted: Boolean, checkMins: Int) {
        // Redundant as AlarmScheduler now handles this directly on dismissal.
        // Keeping here for legacy compatibility if called elsewhere.
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, WakeUpCheckReceiver::class.java).apply {
            action = ACTION_RE_TRIGGER_ALARM
            putExtra("ALARM_ID", alarmId)
            putExtra("RINGTONE_URI", ringtoneUri)
            putExtra("BOOST_VOLUME", isBoosted)
            putExtra("WAKE_UP_CHECK_MINS", checkMins)
        }
        val pi = PendingIntent.getBroadcast(
            context, alarmId + 2000, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val triggerTime = System.currentTimeMillis() + (checkMins * 60 * 1000L)
        
        // Update persisted state
        context.getSharedPreferences("active_wake_up_checks", Context.MODE_PRIVATE)
            .edit { 
                putLong("check_$alarmId", triggerTime)
                putString("state_$alarmId", "RE_TRIGGER")
            }

        AlarmScheduler.setAlarm(context, alarmManager, triggerTime, pi)
    }

    internal fun showFollowUpNotification(context: Context, alarmId: Int, ringtoneUri: String?, isBoosted: Boolean, checkMins: Int, isWarning: Boolean = false) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "WakeUpCheckChannel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Wake Up Check", NotificationManager.IMPORTANCE_HIGH)
            nm.createNotificationChannel(channel)
        }

        // --- Intent for the "CONFIRM I'M AWAKE" button ---
        val confirmIntent = Intent(context, WakeUpCheckReceiver::class.java).apply {
            action = ACTION_CONFIRM_AWAKE
            putExtra("ALARM_ID", alarmId)
            putExtra("RINGTONE_URI", ringtoneUri)
            putExtra("BOOST_VOLUME", isBoosted)
            putExtra("WAKE_UP_CHECK_MINS", checkMins)
        }
        val confirmPi = PendingIntent.getBroadcast(
            context, alarmId + 100, confirmIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // --- Intent for the notification body tap (just opens app, does NOT confirm) ---
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openAppPi = PendingIntent.getActivity(
            context, alarmId + 200, openAppIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // --- Intent for when the notification is dismissed/swiped ---
        val deleteIntent = Intent(context, WakeUpCheckReceiver::class.java).apply {
            action = ACTION_NOTIFICATION_DISMISSED
            putExtra("ALARM_ID", alarmId)
            putExtra("RINGTONE_URI", ringtoneUri)
            putExtra("BOOST_VOLUME", isBoosted)
            putExtra("WAKE_UP_CHECK_MINS", checkMins)
            putExtra("EXTRA_IS_WARNING", isWarning)
        }
        val deleteRequestCode = if (isWarning) alarmId + 6000 else alarmId + 3000
        val deletePi = PendingIntent.getBroadcast(
            context, deleteRequestCode, deleteIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // --- 1. Create Custom Layouts (RemoteViews) ---
        val collapsedView = RemoteViews(context.packageName, R.layout.notification_wake_up_check_collapsed)
        val expandedView = RemoteViews(context.packageName, R.layout.notification_wake_up_check_expanded)

        // Set Text programmatically to handle Warning vs Confirmation
        if (isWarning) {
            val timeText = if (checkMins == 1) "1 minute" else "$checkMins minutes"
            collapsedView.setTextViewText(R.id.notification_title, "Final Warning! ⚠️")
            collapsedView.setTextViewText(R.id.notification_message, "Alarm re-ringing in $timeText!")
            collapsedView.setTextViewText(R.id.btn_confirm_awake, "CONFIRM")
            
            expandedView.setTextViewText(R.id.notification_message, "Your alarm will re-ring in exactly $timeText unless you confirm you are awake now.")
            expandedView.setTextViewText(R.id.btn_confirm_awake_big, "CONFIRM I'M AWAKE NOW")
        }

        // Set PendingIntents to the buttons in our custom layouts
        collapsedView.setOnClickPendingIntent(R.id.btn_confirm_awake, confirmPi)
        expandedView.setOnClickPendingIntent(R.id.btn_confirm_awake_big, confirmPi)

        // --- 2. Build the Notification ---
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_stat_ringo)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(false)
            .setOngoing(true) 
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openAppPi)
            .setDeleteIntent(deletePi)
            .setCustomContentView(collapsedView)
            .setCustomBigContentView(expandedView)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .build()

        // --- STABLE NOTIFICATION ID ---
        // We now use ONE stable ID per alarm instance (alarmId + 1000).
        // This ensures the notification updates itself instead of creating duplicates.
        nm.notify(alarmId + 1000, notification)
    }

    companion object {
        const val ACTION_START_WAKE_UP_CHECK = "com.programmer.highalarm.START_WAKE_UP_CHECK"
        const val ACTION_FOLLOW_UP_NOTIFICATION = "com.programmer.highalarm.FOLLOW_UP_NOTIFICATION"
        const val ACTION_CONFIRM_AWAKE = "com.programmer.highalarm.CONFIRM_AWAKE"
        const val ACTION_RE_TRIGGER_ALARM = "com.programmer.highalarm.RE_TRIGGER_ALARM"
        const val ACTION_NOTIFICATION_DISMISSED = "com.programmer.highalarm.NOTIFICATION_DISMISSED"
    }
}
