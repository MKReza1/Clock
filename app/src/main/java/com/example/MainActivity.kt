package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.example.data.AlarmDatabase
import com.example.data.AlarmRepository
import com.example.service.AlarmService
import com.example.ui.AlarmViewModel
import com.example.ui.AlarmViewModelFactory
import com.example.ui.screens.AlarmPlayScreen
import com.example.ui.screens.MainAlarmScreen
import com.example.ui.theme.AlarmClockTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Edge-to-edge support configuration
        enableEdgeToEdge()

        // Room local storage instances
        val database = AlarmDatabase.getDatabase(applicationContext)
        val repository = AlarmRepository(database.alarmDao())
        
        // Factory based safe ViewModel instantiation
        val viewModelFactory = AlarmViewModelFactory(repository, applicationContext)
        val viewModel = ViewModelProvider(this, viewModelFactory)[AlarmViewModel::class.java]

        setContent {
            // Collect theme selections reactively from state flows
            val themeAccent by viewModel.themeAccent.collectAsState()
            val activeAlarm by AlarmService.activeAlarmState.collectAsState()
            var isDarkTheme by remember { mutableStateOf(false) } // Default to beautiful clean light theme

            AlarmClockTheme(
                accent = themeAccent,
                isDark = isDarkTheme
            ) {
                if (activeAlarm != null) {
                    AlarmPlayScreen(
                        activeAlarm = activeAlarm!!,
                        isDark = isDarkTheme,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Scaffold(
                        modifier = Modifier.fillMaxSize()
                    ) { innerPadding ->
                        MainAlarmScreen(
                            viewModel = viewModel,
                            isDarkTheme = isDarkTheme,
                            onToggleDarkTheme = { isDarkTheme = it },
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }
}
