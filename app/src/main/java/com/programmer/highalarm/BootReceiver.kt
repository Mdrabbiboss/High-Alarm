package com.programmer.highalarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            Log.d("BootReceiver", "Re-scheduling alarms after reboot...")
            
            val prefs = context.getSharedPreferences("alarms_prefs_v4", Context.MODE_PRIVATE)
            val json = prefs.getString("alarms_list", null)
            
            if (json != null) {
                try {
                    val gson = Gson()
                    val type = object : TypeToken<List<AlarmPersistenceModel>>() {}.type
                    val models: List<AlarmPersistenceModel> = gson.fromJson(json, type)
                    val alarms = models.map { it.toAlarm() }
                    
                    for (alarm in alarms) {
                        if (alarm.isEnabled) {
                            AlarmScheduler.schedule(context, alarm)
                            Log.d("BootReceiver", "Scheduled alarm: ${alarm.time}")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("BootReceiver", "Error re-scheduling alarms", e)
                }
            }

            // --- Re-schedule active Wake Up Checks ---
            reScheduleWakeUpChecks(context)
        }
    }

    private fun reScheduleWakeUpChecks(context: Context) {
        val prefs = context.getSharedPreferences("active_wake_up_checks", Context.MODE_PRIVATE)
        val allEntries = prefs.all
        val now = System.currentTimeMillis()
        val gson = Gson()

        // Extract alarm IDs that have active checks
        val alarmIds = allEntries.keys
            .filter { it.startsWith("check_") }
            .mapNotNull { it.substringAfter("check_").toIntOrNull() }
            .distinct()

        for (id in alarmIds) {
            try {
                val triggerTime = prefs.getLong("check_$id", 0L)
                val state = prefs.getString("state_$id", "START")
                
                if (triggerTime > now) {
                    // Load alarm data
                    val alarmPrefs = context.getSharedPreferences("alarms_prefs_v4", Context.MODE_PRIVATE)
                    val json = alarmPrefs.getString("alarms_list", null)
                    if (json != null) {
                        val type = object : TypeToken<List<AlarmPersistenceModel>>() {}.type
                        val models: List<AlarmPersistenceModel> = gson.fromJson(json, type)
                        val alarm = models.find { it.id == id }?.toAlarm()
                        
                        if (alarm != null) {
                            if (state == "RE_TRIGGER") {
                                // Re-show notification and re-schedule the final trigger
                                WakeUpCheckReceiver().showFollowUpNotification(
                                    context, id, alarm.ringtoneUri, alarm.isBoostVolumeEnabled, alarm.wakeUpCheckMinutes,
                                    isWarning = true
                                )
                                // Re-schedule the final trigger
                                scheduleExistingReTrigger(context, alarm, triggerTime)
                            } else {
                                // Re-schedule the initial countdown
                                AlarmScheduler.scheduleWakeUpCheck(context, alarm, triggerTime)
                            }
                            Log.d("BootReceiver", "Re-scheduled $state Wake Up Check for alarm $id")
                        }
                    }
                } else {
                    // Time passed during reboot
                    prefs.edit { 
                        remove("check_$id")
                        remove("state_$id")
                    }
                }
            } catch (e: Exception) {
                Log.e("BootReceiver", "Error re-scheduling Wake Up Check for alarm $id", e)
            }
        }
    }

    private fun scheduleExistingReTrigger(context: Context, alarm: Alarm, triggerTime: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, WakeUpCheckReceiver::class.java).apply {
            action = WakeUpCheckReceiver.ACTION_RE_TRIGGER_ALARM
            putExtra("ALARM_ID", alarm.id)
            putExtra("RINGTONE_URI", alarm.ringtoneUri)
            putExtra("BOOST_VOLUME", alarm.isBoostVolumeEnabled)
            putExtra("WAKE_UP_CHECK_MINS", alarm.wakeUpCheckMinutes)
        }
        val pi = PendingIntent.getBroadcast(
            context, alarm.id + 2000, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        AlarmScheduler.setAlarm(context, alarmManager, triggerTime, pi)
    }
}
