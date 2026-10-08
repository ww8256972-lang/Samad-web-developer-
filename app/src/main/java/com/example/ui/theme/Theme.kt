package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = ElectricBlue,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF004953),
    onPrimaryContainer = Color(0xFF80F5FF),
    secondary = NeonPink,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF5A0028),
    onSecondaryContainer = Color(0xFFFFB2D1),
    tertiary = Color(0xFF7C4DFF),
    background = CyberBlack,
    onBackground = TextPrimaryDark,
    surface = CyberDarkCard,
    onSurface = TextPrimaryDark,
    surfaceVariant = CyberSurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,
    outline = CyberDarkCardBorder,
    error = StatusError,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF007299),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFBFE9FF),
    onPrimaryContainer = Color(0xFF001F2B),
    secondary = Color(0xFFC2185B),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFD8E4),
    onSecondaryContainer = Color(0xFF3E0018),
    tertiary = Color(0xFF5E35B1),
    background = LightBackground,
    onBackground = TextPrimaryLight,
    surface = LightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    outline = LightBorder,
    error = Color(0xFFD32F2F),
    onError = Color.White
)

@Composable
fun WebRecordTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
