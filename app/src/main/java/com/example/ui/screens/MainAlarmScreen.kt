package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.Alarm
import com.example.ui.AlarmThemeAccent
import com.example.ui.AlarmViewModel
import com.example.ui.SoundOption
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAlarmScreen(
    viewModel: AlarmViewModel,
    isDarkTheme: Boolean,
    onToggleDarkTheme: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val alarms by viewModel.alarms.collectAsState()
    val editingAlarm by viewModel.editingAlarm.collectAsState()
    val themeAccent by viewModel.themeAccent.collectAsState()
    val syncState by viewModel.syncState.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    // Live local time flow for visual clocks
    var liveTime by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            liveTime = System.currentTimeMillis()
            delay(1000)
        }
    }

    // Checking Notification Permission on API 33+
    var hasNotifyPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotifyPermission = granted
        if (granted) {
            Toast.makeText(context, "Notifications enabled for Alarm Clock", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Alarms might not show notifications without permission!", Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .statusBarsPadding()
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Alarms",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = (-0.5).sp
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Custom Sync badge
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .clickable {
                                    viewModel.performOfflineCloudSync()
                                    Toast.makeText(context, "Cloud sync status: ${syncState.statusMessage}", Toast.LENGTH_SHORT).show()
                                }
                        ) {
                            Icon(
                                imageVector = if (syncState.isSyncing) Icons.Default.CloudSync else Icons.Default.CloudDone,
                                contentDescription = "Sync status badge",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Theme indicator & trigger
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                                .clickable { showSettingsDialog = true }
                                .testTag("menu_settings_button")
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(Color(themeAccent.colorHex))
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    viewModel.setEditingAlarm(null) // clear editing state first
                    showAddDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier
                    .padding(bottom = 24.dp, end = 16.dp)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .testTag("add_alarm_fab"),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add New Alarm",
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Permission Banner
            if (!hasNotifyPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Warning icon",
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Notification permission required",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = "Required to display alarms on exact schedule.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                            )
                        }
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Enable", fontSize = 12.sp)
                        }
                    }
                }
            }

            // World Clocks Dashboard
            WorldClocksDashboard(liveTime = liveTime, timezones = viewModel.worldTimezones)

            Spacer(modifier = Modifier.height(16.dp))

            // Alarm Section Divider/Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Your Schedules",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                )
                Text(
                    text = "${alarms.filter { it.isEnabled }.size} active",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Alarm List with Empty State
            if (alarms.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AlarmOff,
                                    contentDescription = "No Alarms Icon",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                        Text(
                            text = "No Alarms",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Tap the '+' button below to create your first alarm and customize snooze/timezones",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(alarms, key = { it.id }) { alarm ->
                        AlarmItemCard(
                            alarm = alarm,
                            onToggle = { viewModel.toggleAlarm(alarm) },
                            onDelete = { viewModel.deleteAlarm(alarm) },
                            onClick = {
                                viewModel.setEditingAlarm(alarm)
                                showAddDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    // Settings Dialog (Theme & Cloud Sync dashboard)
    if (showSettingsDialog) {
        SettingsDialog(
            syncState = syncState,
            isDarkTheme = isDarkTheme,
            themeAccent = themeAccent,
            onToggleDarkTheme = onToggleDarkTheme,
            onSelectAccent = { viewModel.selectThemeAccent(it) },
            onTriggerSync = { viewModel.performOfflineCloudSync() },
            onDismiss = { showSettingsDialog = false }
        )
    }

    // Add / Edit Dialog Sheet
    if (showAddDialog) {
        AlarmEditDialog(
            alarm = editingAlarm,
            timezones = viewModel.worldTimezones,
            builtInSounds = viewModel.builtInSounds,
            onSave = { h, m, l, days, snooze, tz, ringUri, ringName, vib ->
                viewModel.saveAlarm(h, m, l, days, snooze, tz, ringUri, ringName, vib)
                showAddDialog = false
            },
            onDismiss = {
                viewModel.setEditingAlarm(null)
                showAddDialog = false
            }
        )
    }
}

// Visual horizontal World Clocks representing Multiple timezone support
@Composable
fun WorldClocksDashboard(liveTime: Long, timezones: List<String>) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = "World Map Clocks",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "World Clocks",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(end = 12.dp)
            ) {
                // Curated key cities to represent multiple zones
                val cities = listOf(
                    CityClock("Local", TimeZone.getDefault()),
                    CityClock("London", TimeZone.getTimeZone("Europe/London")),
                    CityClock("New York", TimeZone.getTimeZone("America/New_York")),
                    CityClock("Tokyo", TimeZone.getTimeZone("Asia/Tokyo")),
                    CityClock("Kolkata", TimeZone.getTimeZone("Asia/Kolkata"))
                )

                items(cities) { city ->
                    val sdf = SimpleDateFormat("hh:mm a", Locale.US).apply {
                        timeZone = city.tz
                    }
                    val formattedTime = sdf.format(Date(liveTime))
                    val dayFormat = SimpleDateFormat("EEE", Locale.US).apply {
                        timeZone = city.tz
                    }
                    val formattedDay = dayFormat.format(Date(liveTime))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp,
                        modifier = Modifier.width(115.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = city.name,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1
                            )
                            Text(
                                text = formattedTime,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Black),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = formattedDay,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

data class CityClock(val name: String, val tz: TimeZone)

// Sleek interactive Card item representing each saved alarm
@Composable
fun AlarmItemCard(
    alarm: Alarm,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (alarm.isEnabled) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
            }
        ),
        border = if (!alarm.isEnabled) {
            androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
        } else null,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("alarm_card_${alarm.id}"),
        elevation = CardDefaults.cardElevation(defaultElevation = if (alarm.isEnabled) 2.dp else 0.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Time & Status Header Row
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val formattedHour = if (alarm.hour == 0 || alarm.hour == 12) 12 else alarm.hour % 12
                    val amPm = if (alarm.hour >= 12) "PM" else "AM"
                    val timeStr = String.format(Locale.getDefault(), "%02d:%02d", formattedHour, alarm.minute)
                    
                    Text(
                        text = timeStr,
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Light,
                            fontSize = 44.sp,
                            letterSpacing = (-1).sp
                        ),
                        color = if (alarm.isEnabled) {
                            MaterialTheme.colorScheme.onBackground
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )

                    Text(
                        text = amPm,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                        color = if (alarm.isEnabled) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                // Label & Repeat Days
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    val labelText = if (alarm.label.isNotEmpty()) alarm.label else "Alarm"
                    val daysText = alarm.getRepeatDaysFormatted()
                    Text(
                        text = "$labelText • $daysText",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = if (alarm.isEnabled) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        }
                    )
                }

                // Custom snooze duration badge & sound info (if enabled)
                if (alarm.isEnabled) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .wrapContentWidth()
                            .clip(RoundedCornerShape(100.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Snooze: ${alarm.snoozeDurationMinutes} mins",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Text(
                            text = if (alarm.ringtoneName.length > 20) alarm.ringtoneName.take(17) + "..." else alarm.ringtoneName,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Timezone label if different from system timezone
                if (alarm.timezoneId != "System Default") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = "timezone",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = alarm.timezoneId,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // Power switch & Quick Delete
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Switch(
                    checked = alarm.isEnabled,
                    onCheckedChange = { onToggle() },
                    modifier = Modifier.testTag("alarm_switch_${alarm.id}")
                )

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("delete_alarm_${alarm.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete alarm",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// Add & Edit custom popup view
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AlarmEditDialog(
    alarm: Alarm?,
    timezones: List<String>,
    builtInSounds: List<SoundOption>,
    onSave: (
        hour: Int,
        minute: Int,
        label: String,
        repeatDays: String,
        snoozeDuration: Int,
        timezoneId: String,
        ringtoneUri: String,
        ringtoneName: String,
        hasVibrate: Boolean
    ) -> Unit,
    onDismiss: () -> Unit
) {
    var hour by remember { mutableStateOf(alarm?.hour ?: 8) }
    var minute by remember { mutableStateOf(alarm?.minute ?: 0) }
    var label by remember { mutableStateOf(alarm?.label ?: "") }
    var selectedDays by remember {
        mutableStateOf(
            alarm?.repeatDays?.split(",")?.filter { it.isNotEmpty() }?.mapNotNull { it.toIntOrNull() }?.toSet() ?: emptySet()
        )
    }
    var snoozeDuration by remember { mutableStateOf(alarm?.snoozeDurationMinutes ?: 5) }
    var timezoneId by remember { mutableStateOf(alarm?.timezoneId ?: "System Default") }
    var ringtoneUri by remember { mutableStateOf(alarm?.ringtoneUri ?: "default") }
    var ringtoneName by remember { mutableStateOf(alarm?.ringtoneName ?: "System Default Tone") }
    var hasVibrate by remember { mutableStateOf(alarm?.hasVibrate ?: true) }

    var isAm by remember { mutableStateOf(hour < 12) }

    // Synchronize AM/PM and 24h internal components
    val displayHour = remember(hour) {
        val h = hour % 12
        if (h == 0) 12 else h
    }

    var showTimezoneDropdown by remember { mutableStateOf(false) }
    var showSoundDropdown by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // Ringtone picker launcher from system Storage
    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val ringtoneUriResult = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            if (ringtoneUriResult != null) {
                ringtoneUri = ringtoneUriResult.toString()
                val ringtone = RingtoneManager.getRingtone(context, ringtoneUriResult)
                ringtoneName = ringtone?.getTitle(context) ?: "External Device Tone"
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(MaterialTheme.colorScheme.surface),
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (alarm == null) "Schedule Alarm" else "Configure Alarm",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Custom digital time editor with increment / decrement buttons
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    // Hours Column
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        IconButton(onClick = {
                                            var nextH = (displayHour % 12) + 1
                                            if (nextH == 12 && !isAm) hour = 12
                                            else if (nextH == 12 && isAm) hour = 0
                                            else hour = nextH + (if (isAm) 0 else 12)
                                        }) {
                                            Icon(imageVector = Icons.Default.KeyboardArrowUp, contentDescription = "Up Hour")
                                        }

                                        Text(
                                            text = String.format(Locale.US, "%02d", displayHour),
                                            style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Black),
                                            color = MaterialTheme.colorScheme.primary
                                        )

                                        IconButton(onClick = {
                                            var prevH = displayHour - 1
                                            if (prevH == 0) prevH = 12
                                            hour = (prevH % 12) + (if (isAm) 0 else 12)
                                        }) {
                                            Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = "Down Hour")
                                        }
                                    }

                                    Text(
                                        text = ":",
                                        style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )

                                    // Minutes Column
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        IconButton(onClick = {
                                            minute = (minute + 1) % 60
                                        }) {
                                            Icon(imageVector = Icons.Default.KeyboardArrowUp, contentDescription = "Up Minute")
                                        }

                                        Text(
                                            text = String.format(Locale.US, "%02d", minute),
                                            style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Black),
                                            color = MaterialTheme.colorScheme.primary
                                        )

                                        IconButton(onClick = {
                                            minute = if (minute == 0) 59 else minute - 1
                                        }) {
                                            Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = "Down Minute")
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(16.dp))

                                    // AM/PM Column Selector
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isAm) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                            modifier = Modifier.clickable {
                                                if (!isAm) {
                                                    isAm = true
                                                    hour = hour % 12
                                                }
                                            }
                                        ) {
                                            Text(
                                                "AM",
                                                color = if (isAm) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                            )
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (!isAm) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                            modifier = Modifier.clickable {
                                                if (isAm) {
                                                    isAm = false
                                                    hour = (hour % 12) + 12
                                                }
                                            }
                                        ) {
                                            Text(
                                                "PM",
                                                color = if (!isAm) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Label entry
                    item {
                        OutlinedTextField(
                            value = label,
                            onValueChange = { label = it },
                            label = { Text("Alarm Label") },
                            placeholder = { Text("e.g. Work Out") },
                            leadingIcon = { Icon(imageVector = Icons.Default.Label, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // Recurring schedules days selector
                    item {
                        Text(
                            text = "Repeat Days",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        val daysArr = listOf("M", "T", "W", "T", "F", "S", "S")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            for (index in 1..7) {
                                val isSelected = selectedDays.contains(index)
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .clickable {
                                            selectedDays = if (isSelected) {
                                                selectedDays - index
                                            } else {
                                                selectedDays + index
                                            }
                                        }
                                ) {
                                    Text(
                                        text = daysArr[index - 1],
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }

                    // Snooze selection configuration
                    item {
                        Text(
                            text = "Custom Snooze Duration",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        val snoozeChoices = listOf(1, 2,  5, 10, 15, 30)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            snoozeChoices.forEach { minutes ->
                                val isSelected = snoozeDuration == minutes
                                InputChip(
                                    selected = isSelected,
                                    onClick = { snoozeDuration = minutes },
                                    label = { Text("$minutes Min") },
                                    leadingIcon = if (isSelected) {
                                        { Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                    } else null
                                )
                            }
                        }
                    }

                    // Multiple zones support Dropdown
                    item {
                        Text(
                            text = "Target Time zone",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedCard(
                                onClick = { showTimezoneDropdown = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(imageVector = Icons.Default.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        Text(text = timezoneId, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                                    }
                                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            }

                            DropdownMenu(
                                expanded = showTimezoneDropdown,
                                onDismissRequest = { showTimezoneDropdown = false },
                                modifier = Modifier.fillMaxWidth(0.9f)
                            ) {
                                timezones.forEach { tz ->
                                    DropdownMenuItem(
                                        text = { Text(tz) },
                                        onClick = {
                                            timezoneId = tz
                                            showTimezoneDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Custom sound & ringtone selection dropdown
                    item {
                        Text(
                            text = "Sound & Ringtone",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedCard(
                                onClick = { showSoundDropdown = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(imageVector = Icons.Default.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        val truncName = if (ringtoneName.length > 25) ringtoneName.take(22) + "..." else ringtoneName
                                        Text(text = truncName, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                                    }
                                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            }

                            DropdownMenu(
                                expanded = showSoundDropdown,
                                onDismissRequest = { showSoundDropdown = false },
                                modifier = Modifier.fillMaxWidth(0.9f)
                            ) {
                                builtInSounds.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option.name) },
                                        onClick = {
                                            ringtoneUri = option.uriString
                                            ringtoneName = option.name
                                            showSoundDropdown = false
                                        }
                                    )
                                }

                                HorizontalDivider()

                                DropdownMenuItem(
                                    text = { Text("Choose from Device Storage...") },
                                    leadingIcon = { Icon(imageVector = Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                    onClick = {
                                        showSoundDropdown = false
                                        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                                            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Alarm Ringtone")
                                            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, null as Uri?)
                                        }
                                        ringtonePickerLauncher.launch(intent)
                                    }
                                )
                            }
                        }
                    }

                    // Vibration Switch
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(imageVector = Icons.Default.Vibration, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("Vibrate on Trigger", style = MaterialTheme.typography.bodyMedium)
                                }
                                Switch(checked = hasVibrate, onCheckedChange = { hasVibrate = it })
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom CTA Save/Cancel Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            val daysStr = selectedDays.sorted().joinToString(",")
                            onSave(
                                hour,
                                minute,
                                label,
                                daysStr,
                                snoozeDuration,
                                timezoneId,
                                ringtoneUri,
                                ringtoneName,
                                hasVibrate
                            )
                        },
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("save_alarm_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save Schedule")
                    }
                }
            }
        }
    }
}

// Settings dialog containing Theme Switchers & visual sync panel
@Composable
fun SettingsDialog(
    syncState: com.example.ui.SyncState,
    isDarkTheme: Boolean,
    themeAccent: AlarmThemeAccent,
    onToggleDarkTheme: (Boolean) -> Unit,
    onSelectAccent: (AlarmThemeAccent) -> Unit,
    onTriggerSync: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth(),
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Customize & Sync",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider()

                // Theme Customization Mode
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(text = "Dark Theme UI", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                    }
                    Switch(
                        checked = isDarkTheme,
                        onCheckedChange = onToggleDarkTheme,
                        modifier = Modifier.testTag("settings_dark_switch")
                    )
                }

                // Accent color customization selector representing light theme customization
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Theme Accent Colors",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        AlarmThemeAccent.values().forEach { accent ->
                            val isSelected = themeAccent == accent
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color(accent.colorHex))
                                    .clickable { onSelectAccent(accent) }
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected Theme",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                    Text(
                        text = "Selected Accent: ${themeAccent.displayName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                HorizontalDivider()

                // Offline Cloud backup sync module
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = "Sync",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                "Database cloud Backup",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Text(
                            text = syncState.statusMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = "Last Synced: ${syncState.lastSyncTime}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (syncState.isSyncSuccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        if (syncState.isSyncing) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        } else {
                            Button(
                                onClick = onTriggerSync,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("trigger_sync_button"),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Sync Alarms Now")
                            }
                        }
                    }
                }
            }
        }
    }
}
