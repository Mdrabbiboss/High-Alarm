package com.programmer.highalarm

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.edit
import com.google.gson.Gson
import java.util.Calendar

object AlarmScheduler {
    fun schedule(context: Context, alarm: Alarm) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.programmer.highalarm.ALARM_ACTION"
            putExtra("ALARM_ID", alarm.id)
            putExtra("RINGTONE_URI", alarm.ringtoneUri)
            putExtra("BOOST_VOLUME", alarm.isBoostVolumeEnabled)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

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
                // One-time alarm
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            } else {
                // Repeating alarm
                val currentDayOfWeek = get(Calendar.DAY_OF_WEEK) - 1 // Calendar is 1-7, our list is 0-6
                
                // If today is a repeat day and the time hasn't passed yet, we stay on today.
                // Otherwise, find the next available day in the list.
                if (!alarm.repeatDays.contains(currentDayOfWeek) || timeInMillis <= System.currentTimeMillis()) {
                    var daysToAdd = 1
                    while (!alarm.repeatDays.contains((currentDayOfWeek + daysToAdd) % 7)) {
                        daysToAdd++
                    }
                    add(Calendar.DAY_OF_YEAR, daysToAdd)
                }
            }
        }

        setAlarm(context, alarmManager, calendar.timeInMillis, pendingIntent)

        // --- Upcoming Alarm Alert Implementation ---
        try {
            val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            val json = prefs.getString("settings_data", null)
            var isUpcomingAlertEnabled = true
            if (json != null) {
                val settings = Gson().fromJson(json, AppSettings::class.java)
                isUpcomingAlertEnabled = settings.isUpcomingAlarmAlertEnabled
            }
            if (isUpcomingAlertEnabled) {
                // Schedule an alert exactly 30 minutes before the alarm rings
                val alertTime = calendar.timeInMillis - (30 * 60 * 1000L)
                if (alertTime > System.currentTimeMillis()) {
                    val alertIntent = Intent(context, AlarmReceiver::class.java).apply {
                        action = "com.programmer.highalarm.UPCOMING_ALERT_ACTION"
                        putExtra("ALARM_ID", alarm.id)
                        putExtra("ALARM_TIME", alarm.time)
                    }
                    val alertPendingIntent = PendingIntent.getBroadcast(
                        context,
                        alarm.id + 7000,
                        alertIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alertTime, alertPendingIntent)
                    } else {
                        alarmManager.setExact(AlarmManager.RTC_WAKEUP, alertTime, alertPendingIntent)
                    }
                }
            }
        } catch (_: Exception) {}
    }

    fun snooze(context: Context, alarm: Alarm, minutes: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.programmer.highalarm.ALARM_ACTION"
            putExtra("ALARM_ID", alarm.id)
            putExtra("RINGTONE_URI", alarm.ringtoneUri)
            putExtra("BOOST_VOLUME", alarm.isBoostVolumeEnabled)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime = System.currentTimeMillis() + (minutes * 60 * 1000)
        setAlarm(context, alarmManager, triggerTime, pendingIntent)
    }

    fun scheduleWakeUpCheck(context: Context, alarm: Alarm, customTriggerTime: Long? = null) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        // --- 1. Follow-up Alarm Timing (Action) ---
        // The selected interval starts AFTER a fixed 30-second delay from dismissal.
        // Total delay = 30s + selectedInterval.
        val reTriggerTime = customTriggerTime ?: (System.currentTimeMillis() + 30000L + alarm.wakeUpCheckMinutes.toLong() * 60000L)

        // --- 2. Follow-up Notification Timing (Warning) ---
        // Appears exactly 30 seconds after dismissal (independent of interval).
        val warningTime = reTriggerTime - (alarm.wakeUpCheckMinutes.toLong() * 60000L)

        // --- 3. Confirmation Notification Timing (ALWAYS exactly 1 minute after dismissal) ---
        // Which is also exactly 30 seconds after the warning notification.
        val notifyTime = warningTime + 30000L

        // --- 4. PREPARE INTENTS ---
        val notifyIntent = Intent(context, WakeUpCheckReceiver::class.java).apply {
            action = WakeUpCheckReceiver.ACTION_START_WAKE_UP_CHECK
            putExtra("ALARM_ID", alarm.id)
            putExtra("RINGTONE_URI", alarm.ringtoneUri)
            putExtra("BOOST_VOLUME", alarm.isBoostVolumeEnabled)
            putExtra("WAKE_UP_CHECK_MINS", alarm.wakeUpCheckMinutes)
        }
        val notifyPi = PendingIntent.getBroadcast(
            context,
            alarm.id + 500,
            notifyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val reTriggerIntent = Intent(context, WakeUpCheckReceiver::class.java).apply {
            action = WakeUpCheckReceiver.ACTION_RE_TRIGGER_ALARM
            putExtra("ALARM_ID", alarm.id)
            putExtra("RINGTONE_URI", alarm.ringtoneUri)
            putExtra("BOOST_VOLUME", alarm.isBoostVolumeEnabled)
            putExtra("WAKE_UP_CHECK_MINS", alarm.wakeUpCheckMinutes)
        }
        val reTriggerPi = PendingIntent.getBroadcast(
            context,
            alarm.id + 2000,
            reTriggerIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val warningIntent = Intent(context, WakeUpCheckReceiver::class.java).apply {
            action = WakeUpCheckReceiver.ACTION_FOLLOW_UP_NOTIFICATION
            putExtra("ALARM_ID", alarm.id)
            putExtra("RINGTONE_URI", alarm.ringtoneUri)
            putExtra("BOOST_VOLUME", alarm.isBoostVolumeEnabled)
            putExtra("WAKE_UP_CHECK_MINS", alarm.wakeUpCheckMinutes)
        }
        val warningPi = PendingIntent.getBroadcast(
            context,
            alarm.id + 1500,
            warningIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // --- 5. SCHEDULE ALL EVENTS INDEPENDENTLY ---
        
        // A. The Warning notification (Dismissal + 30s)
        if (warningTime > System.currentTimeMillis()) {
            schedulePreciseTrigger(alarmManager, warningTime, warningPi)
        }

        // B. The Confirmation notification (Dismissal + 60s)
        if (notifyTime > System.currentTimeMillis()) {
            schedulePreciseTrigger(alarmManager, notifyTime, notifyPi)
        }

        // C. The Follow-up Alarm (Warning + Selected Interval)
        setAlarm(context, alarmManager, reTriggerTime, reTriggerPi)

        // Persist check state for reboot safety
        context.getSharedPreferences("active_wake_up_checks", Context.MODE_PRIVATE)
            .edit { 
                putLong("check_${alarm.id}", reTriggerTime)
                putString("state_${alarm.id}", "START")
            }
    }

    /**
     * Schedules a precise trigger that doesn't show up as a "Next Alarm" in the system UI.
     * Used for the 1-minute verification notification.
     */
    private fun schedulePreciseTrigger(alarmManager: AlarmManager, triggerTime: Long, pendingIntent: PendingIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        }
    }

    internal fun setAlarm(context: Context, alarmManager: AlarmManager, triggerTime: Long, pendingIntent: PendingIntent) {
        // Use a proper Activity PendingIntent for the showIntent requirement of AlarmClockInfo
        val showIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val showPendingIntent = PendingIntent.getActivity(
            context, 
            (triggerTime % Int.MAX_VALUE).toInt(), // Unique request code based on time
            showIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerTime, showPendingIntent)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
        } else {
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
        }
    }

    fun cancel(context: Context, alarmId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.programmer.highalarm.ALARM_ACTION"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)

        // Also cancel any pending Wake Up Checks
        cancelWakeUpCheck(context, alarmId)
    }

    private fun cancelWakeUpCheck(context: Context, alarmId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        // Clear check state
        context.getSharedPreferences("active_wake_up_checks", Context.MODE_PRIVATE)
            .edit { 
                remove("check_$alarmId")
                remove("state_$alarmId")
            }

        // Cancel the initial interval timer (+1m confirmation)
        val checkIntent = Intent(context, WakeUpCheckReceiver::class.java).apply { action = WakeUpCheckReceiver.ACTION_START_WAKE_UP_CHECK }
        val checkPi = PendingIntent.getBroadcast(context, alarmId + 500, checkIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        alarmManager.cancel(checkPi)
        
        // Cancel the warning timer (Interval - 30s)
        val warningIntent = Intent(context, WakeUpCheckReceiver::class.java).apply { action = WakeUpCheckReceiver.ACTION_FOLLOW_UP_NOTIFICATION }
        val warningPi = PendingIntent.getBroadcast(context, alarmId + 1500, warningIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        alarmManager.cancel(warningPi)

        // Cancel the re-trigger timer (Selected Interval)
        val reTriggerIntent = Intent(context, WakeUpCheckReceiver::class.java).apply { action = WakeUpCheckReceiver.ACTION_RE_TRIGGER_ALARM }
        val reTriggerPi = PendingIntent.getBroadcast(context, alarmId + 2000, reTriggerIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        alarmManager.cancel(reTriggerPi)

        // Dismiss the notification (Consolidated ID)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(alarmId + 1000)
    }
}
