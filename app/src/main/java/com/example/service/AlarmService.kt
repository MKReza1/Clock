package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.core.app.NotificationCompat

class AlarmService : Service() {
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    companion object {
        data class ActiveAlarmVal(
            val id: Int,
            val label: String,
            val snoozeMinutes: Int,
            val ringtoneUri: String,
            val ringtoneName: String,
            val vibrate: Boolean,
            val hour: Int,
            val minute: Int
        )

        val activeAlarmState = kotlinx.coroutines.flow.MutableStateFlow<ActiveAlarmVal?>(null)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val alarmId = intent?.getIntExtra("ALARM_ID", -1) ?: -1
        val label = intent?.getStringExtra("ALARM_LABEL") ?: "Alarm"
        val snoozeMin = intent?.getIntExtra("ALARM_SNOOZE", 5) ?: 5
        val ringtoneUriStr = intent?.getStringExtra("ALARM_RINGTONE_URI") ?: intent?.getStringExtra("RINGTONE_URI")
        val ringtoneName = intent?.getStringExtra("ALARM_RINGTONE_NAME") ?: "Default Ringtone"
        val vibrate = intent?.getBooleanExtra("ALARM_VIBRATE", true) ?: intent?.getBooleanExtra("VIBRATE", true) ?: true
        val hour = intent?.getIntExtra("ALARM_HOUR", -1) ?: -1
        val minute = intent?.getIntExtra("ALARM_MINUTE", -1) ?: -1

        Log.d("AlarmService", "Alarm service onStartCommand. Id: $alarmId, Label: $label, Ringtone: $ringtoneUriStr, Vibrate: $vibrate")

        // 0. Promote to Foreground immediately to avoid ForegroundServiceDidNotStartInTimeException
        val channelId = "ALARM_SERVICE_FOREGROUND_CHANNEL"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                channelId,
                "Alarm Active Ringing Indicator",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Alarm Ringing")
            .setContentText("Alarm sound is currently playing...")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1001, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(1001, notification)
        }

        if (alarmId != -1) {
            activeAlarmState.value = ActiveAlarmVal(
                id = alarmId,
                label = label,
                snoozeMinutes = snoozeMin,
                ringtoneUri = ringtoneUriStr ?: "default",
                ringtoneName = ringtoneName,
                vibrate = vibrate,
                hour = hour,
                minute = minute
            )
        } else {
            activeAlarmState.value = ActiveAlarmVal(
                id = 9999,
                label = label,
                snoozeMinutes = snoozeMin,
                ringtoneUri = ringtoneUriStr ?: "default",
                ringtoneName = ringtoneName,
                vibrate = vibrate,
                hour = 8,
                minute = 0
            )
        }

        // Clean up first
        stopAlarm()

        // 1. Play Alarm Sound
        try {
            val uri = if (ringtoneUriStr == null || ringtoneUriStr == "default") {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM) ?:
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            } else {
                Uri.parse(ringtoneUriStr)
            }

            mediaPlayer = MediaPlayer().apply {
                setDataSource(applicationContext, uri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e("AlarmService", "Could not play Ringtone, using default alarm fallback", e)
            try {
                val sysAlarm = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(applicationContext, sysAlarm)
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    isLooping = true
                    prepare()
                    start()
                }
            } catch (ex: Exception) {
                Log.e("AlarmService", "Double fallback failed", ex)
            }
        }

        // 2. Continuous Vibration
        if (vibrate) {
            vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            vibrator?.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val pattern = longArrayOf(0, 800, 800) // Vibrate 800ms, rest 800ms
                    it.vibrate(VibrationEffect.createWaveform(pattern, 0))
                } else {
                    @Suppress("DEPRECATION")
                    it.vibrate(longArrayOf(0, 800, 800), 0)
                }
            }
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        stopAlarm()
        super.onDestroy()
    }

    private fun stopAlarm() {
        activeAlarmState.value = null
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
        } catch (e: Exception) {
            Log.e("AlarmService", "Error stopping MediaPlayer", e)
        } finally {
            mediaPlayer = null
        }

        try {
            vibrator?.cancel()
        } catch (e: Exception) {
            Log.e("AlarmService", "Error stopping Vibrator", e)
        } finally {
            vibrator = null
        }
    }
}
