package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.Alarm
import com.example.ui.screens.AlarmItemCard
import com.example.ui.theme.AlarmClockTheme
import com.example.ui.AlarmThemeAccent
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    val sampleAlarm = Alarm(
        id = 1,
        hour = 7,
        minute = 30,
        label = "Rise and Shine!",
        isEnabled = true,
        repeatDays = "1,2,3,4,5",
        snoozeDurationMinutes = 5,
        timezoneId = "System Default",
        ringtoneUri = "default",
        ringtoneName = "Default Tone",
        hasVibrate = true
    )

    composeTestRule.setContent {
        AlarmClockTheme(accent = AlarmThemeAccent.SKY, isDark = false) {
            AlarmItemCard(
                alarm = sampleAlarm,
                onToggle = {},
                onDelete = {},
                onClick = {}
            )
        }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
