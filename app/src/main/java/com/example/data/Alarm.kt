package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "alarms")
data class Alarm(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val hour: Int,          // 0-23
    val minute: Int,        // 0-59
    val label: String = "Alarm",
    val isEnabled: Boolean = true,
    val repeatDays: String = "", // Comma-separated or empty (e.g., "1,2,3,4,5" for Mon-Fri)
    val snoozeDurationMinutes: Int = 5, // Custom snooze duration
    val timezoneId: String = "System Default", // Multiple time zones support
    val ringtoneUri: String = "default", // Custom sound selection
    val ringtoneName: String = "Default Ringtone",
    val hasVibrate: Boolean = true
) : Serializable {

    // Helper to determine if alarm repeats at all
    val isRepeating: Boolean
        get() = repeatDays.isNotEmpty()

    // Helper to check if a specific day is selected
    // Day ranges from 1 (Monday) to 7 (Sunday)
    fun repeatsOnDay(day: Int): Boolean {
        if (repeatDays.isEmpty()) return false
        val daysList = repeatDays.split(",").mapNotNull { it.trim().toIntOrNull() }
        return day in daysList
    }

    // Returns a friendly string like "Every day", "Weekdays", "Weekend" or specific days
    fun getRepeatDaysFormatted(): String {
        if (repeatDays.isEmpty()) return "Once"
        val daysList = repeatDays.split(",").mapNotNull { it.trim().toIntOrNull() }.sorted()
        if (daysList.size == 7) return "Every day"
        if (daysList.size == 5 && daysList.containsAll(listOf(1, 2, 3, 4, 5)) && !daysList.contains(6) && !daysList.contains(7)) {
            return "Weekdays"
        }
        if (daysList.size == 2 && daysList.contains(6) && daysList.contains(7)) {
            return "Weekends"
        }

        val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        return daysList.joinToString(", ") { dayNum ->
            if (dayNum in 1..7) dayNames[dayNum - 1] else ""
        }
    }
}
