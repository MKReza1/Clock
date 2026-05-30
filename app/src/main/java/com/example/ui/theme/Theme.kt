package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.ui.AlarmThemeAccent

@Composable
fun AlarmClockTheme(
    accent: AlarmThemeAccent = AlarmThemeAccent.SKY,
    isDark: Boolean = false,
    content: @Composable () -> Unit
) {
    val primaryColor = Color(accent.colorHex)
    
    // Sophisticated, clean Minimalism color scales
    val colorScheme = if (isDark) {
        darkColorScheme(
            primary = Color(0xFFA8C7FA),
            onPrimary = Color(0xFF003054),
            primaryContainer = Color(0xFF004A77),
            onPrimaryContainer = Color(0xFFD3E3FD),
            secondary = Color(0xFF7FC9FF),
            onSecondary = Color(0xFF003054),
            tertiary = Color(0xFFC4C7D0),
            background = Color(0xFF111318), // Deep minimal charcoal slate
            surface = Color(0xFF1A1C22),    // Clean surface card
            surfaceVariant = Color(0xFF2E3036),
            onBackground = Color(0xFFE2E2E6),
            onSurface = Color(0xFFE2E2E6),
            onSurfaceVariant = Color(0xFFC4C7D0),
            outline = Color(0xFF8E9099)
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF0061A4),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFD3E3FD),
            onPrimaryContainer = Color(0xFF001D36),
            secondary = Color(0xFF004A77),
            onSecondary = Color.White,
            tertiary = Color(0xFF0061A4),
            background = Color(0xFFF7F9FF), // Accurate Light Clean Minimalism color
            surface = Color.White,
            surfaceVariant = Color(0xFFEAF1FB), // Light bluish container
            onBackground = Color(0xFF191C20),
            onSurface = Color(0xFF191C20),
            onSurfaceVariant = Color(0xFF44474E),
            outline = Color(0xFFC4C7D0)
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
