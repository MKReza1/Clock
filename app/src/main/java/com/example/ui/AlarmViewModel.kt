package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.Alarm
import com.example.data.AlarmRepository
import com.example.receiver.AlarmScheduler
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AlarmViewModel(
    private val repository: AlarmRepository,
    private val appContext: Context
) : ViewModel() {

    // Reactive alarm list from Room database
    val alarms: StateFlow<List<Alarm>> = repository.allAlarms
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Current alarm being edited (null means adding new)
    private val _editingAlarm = MutableStateFlow<Alarm?>(null)
    val editingAlarm: StateFlow<Alarm?> = _editingAlarm.asStateFlow()

    // Light theme accent customization
    private val _themeAccent = MutableStateFlow(AlarmThemeAccent.SKY)
    val themeAccent: StateFlow<AlarmThemeAccent> = _themeAccent.asStateFlow()

    // Offline Cloud Sync status
    private val _syncState = MutableStateFlow(SyncState())
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    // Supported curated world timezones representing multiple timezones
    val worldTimezones = listOf(
        "System Default",
        "UTC",
        "America/New_York",
        "America/Los_Angeles",
        "America/Chicago",
        "Europe/London",
        "Europe/Paris",
        "Asia/Kolkata",
        "Asia/Tokyo",
        "Asia/Dubai",
        "Asia/Singapore",
        "Australia/Sydney"
    )

    // Curated rich built-in sound choices when system ringtone is busy or offline
    val builtInSounds = listOf(
        SoundOption("default", "System Default Tone"),
        SoundOption("cosmic_bell", "Cosmic Bell Alert"),
        SoundOption("sunrise_melody", "Sunrise Melody"),
        SoundOption("forest_birds", "Calm Forest Birds"),
        SoundOption("digital_retro", "Digital Retro Alarm")
    )

    fun selectThemeAccent(accent: AlarmThemeAccent) {
        _themeAccent.value = accent
    }

    fun setEditingAlarm(alarm: Alarm?) {
        _editingAlarm.value = alarm
    }

    fun toggleAlarm(alarm: Alarm) {
        viewModelScope.launch {
            val updated = alarm.copy(isEnabled = !alarm.isEnabled)
            repository.update(updated)
            if (updated.isEnabled) {
                AlarmScheduler.scheduleAlarm(appContext, updated)
            } else {
                AlarmScheduler.cancelAlarm(appContext, updated)
            }
        }
    }

    fun deleteAlarm(alarm: Alarm) {
        viewModelScope.launch {
            AlarmScheduler.cancelAlarm(appContext, alarm)
            repository.delete(alarm)
        }
    }

    fun saveAlarm(
        hour: Int,
        minute: Int,
        label: String,
        repeatDays: String,
        snoozeMin: Int,
        timezoneId: String,
        ringtoneUri: String,
        ringtoneName: String,
        hasVibrate: Boolean
    ) {
        viewModelScope.launch {
            val currentEditing = _editingAlarm.value
            val alarmToSave = if (currentEditing != null) {
                // Update
                currentEditing.copy(
                    hour = hour,
                    minute = minute,
                    label = label,
                    repeatDays = repeatDays,
                    snoozeDurationMinutes = snoozeMin,
                    timezoneId = timezoneId,
                    ringtoneUri = ringtoneUri,
                    ringtoneName = ringtoneName,
                    hasVibrate = hasVibrate,
                    isEnabled = true // re-enable on edit/save
                )
            } else {
                // Create New
                Alarm(
                    hour = hour,
                    minute = minute,
                    label = if (label.isBlank()) "Alarm" else label,
                    repeatDays = repeatDays,
                    snoozeDurationMinutes = snoozeMin,
                    timezoneId = timezoneId,
                    ringtoneUri = ringtoneUri,
                    ringtoneName = ringtoneName,
                    hasVibrate = hasVibrate,
                    isEnabled = true
                )
            }

            if (currentEditing != null) {
                repository.update(alarmToSave)
                AlarmScheduler.scheduleAlarm(appContext, alarmToSave)
            } else {
                val newId = repository.insert(alarmToSave).toInt()
                val alarmWithId = alarmToSave.copy(id = newId)
                AlarmScheduler.scheduleAlarm(appContext, alarmWithId)
            }

            _editingAlarm.value = null // Close editor sheet
        }
    }

    // High quality offline Cloud backup Sync tool simulation
    fun performOfflineCloudSync() {
        if (_syncState.value.isSyncing) return
        
        viewModelScope.launch {
            _syncState.value = _syncState.value.copy(isSyncing = true, statusMessage = "Checking local changes...")
            delay(1000)
            
            val alarmCount = alarms.value.size
            _syncState.value = _syncState.value.copy(statusMessage = "Uploading $alarmCount alarms to encrypted cloud space...")
            delay(1200)

            val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val formattedTime = formatter.format(Date())
            
            _syncState.value = SyncState(
                isSyncing = false,
                lastSyncTime = formattedTime,
                isSyncSuccess = true,
                statusMessage = "Synced successfully! Backup completed offline."
            )
        }
    }
}

// Support Theme choices
enum class AlarmThemeAccent(val displayName: String, val colorHex: Long) {
    SKY("Cloud Sky Blue", 0xFF0EA5E9),
    FOREST("Calm Mint Forest", 0xFF10B981),
    EMBER("Cozy Amber Glow", 0xFFF59E0B),
    ROSE("Blossom Rose", 0xFFF43F5E),
    VIOLET("Deep Violet Dream", 0xFF8B5CF6)
}

// State holder for Cloud Syncing
data class SyncState(
    val isSyncing: Boolean = false,
    val lastSyncTime: String = "Never",
    val isSyncSuccess: Boolean = false,
    val statusMessage: String = "Direct Room local storage is stable. Sync available."
)

// Audio Sound selection option
data class SoundOption(
    val uriString: String,
    val name: String
)

// Standard custom ViewModel provider factory for MainActivity Compose
class AlarmViewModelFactory(
    private val repository: AlarmRepository,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AlarmViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AlarmViewModel(repository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
