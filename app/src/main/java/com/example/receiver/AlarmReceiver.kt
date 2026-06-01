package com.example.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.Alarm
import com.example.data.AlarmDatabase
import com.example.data.AlarmRepository
import com.example.service.AlarmService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "AlarmReceiver"
        const val CHANNEL_ID = "ALARM_CLOCK_CHANNEL"
        const val ACTION_RING = "com.example.ACTION_RING_ALARM"
        const val ACTION_SNOOZE = "com.example.ACTION_SNOOZE_ALARM"
        const val ACTION_DISMISS = "com.example.ACTION_DISMISS_ALARM"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val alarmId = intent.getIntExtra("ALARM_ID", -1)
        val label = intent.getStringExtra("ALARM_LABEL") ?: "Alarm"
        val snoozeMin = intent.getIntExtra("ALARM_SNOOZE", 5)
        val ringtoneUri = intent.getStringExtra("ALARM_RINGTONE_URI") ?: "default"
        val ringtoneName = intent.getStringExtra("ALARM_RINGTONE_NAME") ?: "Default Ringtone"
        val vibrate = intent.getBooleanExtra("ALARM_VIBRATE", true)

        Log.d(TAG, "onReceive action=$action, alarmId=$alarmId")

        if (alarmId == -1) return

        when (action) {
            ACTION_RING -> {
                // 1. Play Alarm Sound & Vibrate via Service
                val serviceIntent = Intent(context, AlarmService::class.java).apply {
                    putExtra("ALARM_ID", alarmId)
                    putExtra("ALARM_LABEL", label)
                    putExtra("ALARM_SNOOZE", snoozeMin)
                    putExtra("ALARM_RINGTONE_URI", ringtoneUri)
                    putExtra("ALARM_RINGTONE_NAME", ringtoneName)
                    putExtra("ALARM_VIBRATE", vibrate)
                    putExtra("ALARM_HOUR", intent.getIntExtra("ALARM_HOUR", -1))
                    putExtra("ALARM_MINUTE", intent.getIntExtra("ALARM_MINUTE", -1))
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }

                // 2. Create Notification Channel
                createNotificationChannel(context)

                // 3. Set up PendingIntents for Snooze and Dismiss
                val snoozeIntent = Intent(context, AlarmReceiver::class.java).apply {
                    this.action = ACTION_SNOOZE
                    putExtra("ALARM_ID", alarmId)
                    putExtra("ALARM_LABEL", label)
                    putExtra("ALARM_SNOOZE", snoozeMin)
                    putExtra("ALARM_RINGTONE_URI", ringtoneUri)
                    putExtra("ALARM_RINGTONE_NAME", ringtoneName)
                    putExtra("ALARM_VIBRATE", vibrate)
                }
                val snoozePendingIntent = PendingIntent.getBroadcast(
                    context,
                    alarmId + 10000, // Safe offset
                    snoozeIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val dismissIntent = Intent(context, AlarmReceiver::class.java).apply {
                    this.action = ACTION_DISMISS
                    putExtra("ALARM_ID", alarmId)
                }
                val dismissPendingIntent = PendingIntent.getBroadcast(
                    context,
                    alarmId + 20000, // Safe offset
                    dismissIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                // Intent to open Main Screen on click
                val mainIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                val mainPendingIntent = PendingIntent.getActivity(
                    context,
                    alarmId,
                    mainIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                // 4. Build and Display Notification
                val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                    .setContentTitle(label)
                    .setContentText("Alarm is ringing!")
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setOngoing(true)
                    .setAutoCancel(false)
                    .setContentIntent(mainPendingIntent)
                    .addAction(android.R.drawable.ic_lock_idle_alarm, "Snooze ($snoozeMin Min)", snoozePendingIntent)
                    .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Dismiss", dismissPendingIntent)
                    .build()

                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                notificationManager?.notify(alarmId, notification)

                // If non-repeating and not a temporary snooze-triggered alarm, schedule next day if we wish,
                // but actually, let's keep it enabled until the user clicks Dismiss, which either disables it (single-shot)
                // or keeps it scheduled (repeating).
            }

            ACTION_SNOOZE -> {
                Log.d(TAG, "Snoozing alarm ID: $alarmId for $snoozeMin mins")
                // Stop Ringing Service
                context.stopService(Intent(context, AlarmService::class.java))

                // Cancel current notification
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                notificationManager?.cancel(alarmId)

                // Schedule the Snooze timer
                AlarmScheduler.scheduleSnooze(
                    context = context,
                    alarmId = alarmId,
                    label = label.replace(" (Snoozed)", ""),
                    snoozeDurationMinutes = snoozeMin,
                    ringtoneUri = ringtoneUri,
                    ringtoneName = ringtoneName,
                    vibrate = vibrate
                )

                Toast.makeText(context, "Alarm snoozed for $snoozeMin minutes", Toast.LENGTH_SHORT).show()
            }

            ACTION_DISMISS -> {
                Log.d(TAG, "Dismissing alarm ID: $alarmId")
                // Stop Ringing Service
                context.stopService(Intent(context, AlarmService::class.java))

                // Cancel current notification
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                notificationManager?.cancel(alarmId)

                val db = AlarmDatabase.getDatabase(context)
                val repository = AlarmRepository(db.alarmDao())

                val result = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val alarm = repository.getAlarmById(alarmId)
                        if (alarm != null) {
                            if (alarm.isRepeating) {
                                // For repeating alarms, schedule the next occurrence immediately
                                AlarmScheduler.scheduleAlarm(context, alarm)
                            } else {
                                // For one-time alarms, disable it after dismissing
                                val updatedAlarm = alarm.copy(isEnabled = false)
                                repository.update(updatedAlarm)
                                AlarmScheduler.cancelAlarm(context, updatedAlarm)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error updating alarm on dismiss", e)
                    } finally {
                        result.finish()
                    }
                }

                Toast.makeText(context, "Alarm dismissed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Alarms & Reminders"
            val descriptionText = "Triggers local notifications when scheduled alarms fire"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
                setSound(null, null) // Silent because AlarmService handles sound separately
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
