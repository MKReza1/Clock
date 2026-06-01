package com.example.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.receiver.AlarmReceiver
import com.example.service.AlarmService
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AlarmPlayScreen(
    activeAlarm: AlarmService.Companion.ActiveAlarmVal,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Live ringing clock
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = System.currentTimeMillis()
            kotlinx.coroutines.delay(1000)
        }
    }

    val timeString = remember(currentTime) {
        SimpleDateFormat("hh:mm", Locale.getDefault()).format(Date(currentTime))
    }
    val amPmString = remember(currentTime) {
        SimpleDateFormat("a", Locale.getDefault()).format(Date(currentTime))
    }

    // Gentle visual organic pulsing scales for the ambient background rings
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale1 by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = EaseOutBack),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseRing1"
    )
    val pulseScale2 by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseRing2"
    )

    // Cohesive visual atmosphere gradients:
    // Light mode: Cozy, energetic morning Peach-Orange-Yellow sunrise
    // Dark mode: Tranquil ultra-deep cosmic space Navy-indigo slate
    val bgGradient = if (!isDark) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFFFECE2), // Soft morning peach
                Color(0xFFFFD4C1), // Warm glow orange
                Color(0xFFFFEFA8)  // Gentle morning sun yellow
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF0F111A), // Deep stellar night
                Color(0xFF1B1E30), // Velvet midnight indigo
                Color(0xFF101116)  // Pitch space dark
            )
        )
    }

    val contentColor = if (!isDark) Color(0xFF3B1D11) else Color(0xFFE3E1E9)
    val accentTone = if (!isDark) Color(0xFFF96132) else Color(0xFFA8C7FA)
    val ringColor = if (!isDark) Color(0xFFF96132).copy(alpha = 0.08f) else Color(0xFFA8C7FA).copy(alpha = 0.06f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgGradient)
            .testTag("alarm_play_screen_root")
    ) {
        // 1. Decorative Visual Ambient Pulsing Rings
        Box(
            modifier = Modifier
                .fillMaxSize()
                .wrapContentSize(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .scale(pulseScale2)
                    .clip(CircleShape)
                    .background(ringColor)
            )
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .scale(pulseScale1)
                    .clip(CircleShape)
                    .background(ringColor)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Upper Column: Alarm Indicators
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 32.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = accentTone.copy(alpha = 0.15f),
                    modifier = Modifier.size(76.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "Ringing bell Icon",
                            tint = accentTone,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = activeAlarm.label.uppercase(Locale.getDefault()),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    ),
                    color = contentColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("alarm_play_label")
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Ringing active",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = contentColor.copy(alpha = 0.6f)
                )
            }

            // Middle Column: Massive Gorgeous typography time display
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = timeString,
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontWeight = FontWeight.Light,
                            fontSize = 80.sp,
                            letterSpacing = (-2).sp
                        ),
                        color = contentColor,
                        modifier = Modifier.testTag("alarm_play_time")
                    )

                    Text(
                        text = amPmString,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp
                        ),
                        color = accentTone,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                if (activeAlarm.ringtoneName.isNotEmpty() && activeAlarm.ringtoneName != "Unknown") {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(contentColor.copy(alpha = 0.06f))
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Alarm,
                            contentDescription = "Ringtone icon",
                            tint = contentColor.copy(alpha = 0.7f),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = activeAlarm.ringtoneName,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = contentColor.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // Lower Column: Super-sized tactile action bars
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                // SNOOZE ACTION BUTTON (Extra large pill plate)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(68.dp)
                        .clip(RoundedCornerShape(34.dp))
                        .background(contentColor.copy(alpha = 0.08f))
                        .border(
                            width = 1.dp,
                            color = contentColor.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(34.dp)
                        )
                        .clickable { triggerSnooze(context, activeAlarm) }
                        .testTag("alarm_play_snooze_button")
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Snooze,
                            contentDescription = "Snooze symbol",
                            tint = contentColor,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Snooze (${activeAlarm.snoozeMinutes} Min)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = contentColor
                        )
                    }
                }

                // DISMISS ACTION BUTTON (Prominent primary button)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(68.dp)
                        .clip(RoundedCornerShape(34.dp))
                        .background(accentTone)
                        .clickable { triggerDismiss(context, activeAlarm) }
                        .testTag("alarm_play_dismiss_button")
                ) {
                    Text(
                        text = "DISMISS",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFF101116) else Color.White,
                            letterSpacing = 1.sp
                        )
                    )
                }
            }
        }
    }
}

private fun triggerSnooze(context: Context, alarm: AlarmService.Companion.ActiveAlarmVal) {
    val snoozeIntent = Intent(context, AlarmReceiver::class.java).apply {
        action = AlarmReceiver.ACTION_SNOOZE
        putExtra("ALARM_ID", alarm.id)
        putExtra("ALARM_LABEL", alarm.label)
        putExtra("ALARM_SNOOZE", alarm.snoozeMinutes)
        putExtra("ALARM_RINGTONE_URI", alarm.ringtoneUri)
        putExtra("ALARM_RINGTONE_NAME", alarm.ringtoneName)
        putExtra("ALARM_VIBRATE", alarm.vibrate)
    }
    context.sendBroadcast(snoozeIntent)
}

private fun triggerDismiss(context: Context, alarm: AlarmService.Companion.ActiveAlarmVal) {
    val dismissIntent = Intent(context, AlarmReceiver::class.java).apply {
        action = AlarmReceiver.ACTION_DISMISS
        putExtra("ALARM_ID", alarm.id)
    }
    context.sendBroadcast(dismissIntent)
}
