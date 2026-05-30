package com.example.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.Alarm
import java.util.Calendar
import java.util.TimeZone

object AlarmScheduler {
    private const val TAG = "AlarmScheduler"

    fun scheduleAlarm(context: Context, alarm: Alarm) {
        if (!alarm.isEnabled) {
            cancelAlarm(context, alarm)
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.example.ACTION_RING_ALARM"
            putExtra("ALARM_ID", alarm.id)
            putExtra("ALARM_LABEL", alarm.label)
            putExtra("ALARM_HOUR", alarm.hour)
            putExtra("ALARM_MINUTE", alarm.minute)
            putExtra("ALARM_TIMEZONE", alarm.timezoneId)
            putExtra("ALARM_SNOOZE", alarm.snoozeDurationMinutes)
            putExtra("ALARM_RINGTONE_URI", alarm.ringtoneUri)
            putExtra("ALARM_RINGTONE_NAME", alarm.ringtoneName)
            putExtra("ALARM_VIBRATE", alarm.hasVibrate)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTimeMs = calculateNextTriggerTime(alarm)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Precise timers even in Idle states
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMs,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMs,
                    pendingIntent
                )
            }
            Log.d(TAG, "Successfully scheduled alarm ID: ${alarm.id} at $triggerTimeMs (Timezone: ${alarm.timezoneId})")
        } catch (e: SecurityException) {
            // Fallback to non-exact scheduling if permission is denied/missing on Android 13+
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerTimeMs,
                pendingIntent
            )
            Log.w(TAG, "Exact alarm permission not granted. Falling back to set().", e)
        }
    }

    fun cancelAlarm(context: Context, alarm: Alarm) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.example.ACTION_RING_ALARM"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d(TAG, "Cancelled scheduled alarm ID: ${alarm.id}")
        }
    }

    fun scheduleSnooze(context: Context, alarmId: Int, label: String, snoozeDurationMinutes: Int, ringtoneUri: String, ringtoneName: String, vibrate: Boolean) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.example.ACTION_RING_ALARM"
            putExtra("ALARM_ID", alarmId)
            putExtra("ALARM_LABEL", "$label (Snoozed)")
            putExtra("ALARM_RINGTONE_URI", ringtoneUri)
            putExtra("ALARM_RINGTONE_NAME", ringtoneName)
            putExtra("ALARM_VIBRATE", vibrate)
            putExtra("ALARM_SNOOZE", snoozeDurationMinutes)
            putExtra("IS_SNOOZED_TRIGGER", true)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime = System.currentTimeMillis() + (snoozeDurationMinutes * 60 * 1000)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
            Log.d(TAG, "Created snooze for alarm ID: $alarmId in $snoozeDurationMinutes min")
        } catch (e: SecurityException) {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }
    }

    fun calculateNextTriggerTime(alarm: Alarm): Long {
        val targetTz = if (alarm.timezoneId == "System Default") {
            TimeZone.getDefault()
        } else {
            TimeZone.getTimeZone(alarm.timezoneId)
        }

        val targetCal = Calendar.getInstance(targetTz).apply {
            set(Calendar.HOUR_OF_DAY, alarm.hour)
            set(Calendar.MINUTE, alarm.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val nowMsInTarget = System.currentTimeMillis()

        // Handle day recurrence (Mon=1, Tue=2, ... Sun=7)
        if (alarm.isRepeating) {
            // Calendar.DAY_OF_WEEK: Sunday=1, Monday=2, ... Saturday=7
            // We map Calendar's DAY_OF_WEEK to 1=Monday...7=Sunday
            val currentCalDay = targetCal.get(Calendar.DAY_OF_WEEK)
            val todayDayMapped = when (currentCalDay) {
                Calendar.MONDAY -> 1
                Calendar.TUESDAY -> 2
                Calendar.WEDNESDAY -> 3
                Calendar.THURSDAY -> 4
                Calendar.FRIDAY -> 5
                Calendar.SATURDAY -> 6
                Calendar.SUNDAY -> 7
                else -> 1
            }

            var daysToAdd = -1
            for (i in 0..7) {
                val nextDayToCheck = (todayDayMapped - 1 + i) % 7 + 1
                if (alarm.repeatsOnDay(nextDayToCheck)) {
                    // Check if it's today and ahead/behind of current time
                    if (i == 0) {
                        if (targetCal.timeInMillis > nowMsInTarget) {
                            daysToAdd = 0
                            break
                        }
                    } else {
                        daysToAdd = i
                        break
                    }
                }
            }

            if (daysToAdd == -1) {
                // Should not happen if custom recurrence is populated
                daysToAdd = if (targetCal.timeInMillis > nowMsInTarget) 0 else 1
            }
            targetCal.add(Calendar.DAY_OF_YEAR, daysToAdd)
        } else {
            // Simple single shot alarm
            if (targetCal.timeInMillis <= nowMsInTarget) {
                targetCal.add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        return targetCal.timeInMillis
    }
}
