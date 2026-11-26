package com.v2ray.ang.ui.compose

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.v2ray.ang.AppConfig
import com.v2ray.ang.handler.AppNotificationManager
import com.v2ray.ang.handler.MmkvManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private val LightColor = lightColorScheme(
    primary = Color(0xFF1B6BFF), // Matix blue
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD9E6FF),
    onPrimaryContainer = Color(0xFF001A44),
    secondary = Color(0xFF3F6FD8),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDCE6FF),
    onSecondaryContainer = Color(0xFF00174A),
    tertiary = Color(0xFF009966),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFA0F2D0),
    onTertiaryContainer = Color(0xFF00201A),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    onError = Color(0xFFFFFFFF),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF2F6FF),
    onBackground = Color(0xFF0B1220),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0B1220),
    surfaceVariant = Color(0xFFE3E9F7),
    onSurfaceVariant = Color(0xFF454C5E),
    outline = Color(0xFF747C92),
    outlineVariant = Color(0xFFC6CEE0),
    inverseSurface = Color(0xFF1B2132),
    inverseOnSurface = Color(0xFFF1F4FF),
    inversePrimary = Color(0xFF9DBCFF),
    scrim = Color(0xFF000000),
    surfaceTint = Color(0xFF1B6BFF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF6F8FF),
    surfaceContainer = Color(0xFFEEF2FC),
    surfaceContainerHigh = Color(0xFFE8EDF9),
    surfaceContainerHighest = Color(0xFFE1E7F5),
)

private val DarkColor = darkColorScheme(
    primary = Color(0xFF4C93FF), // Matix blue
    onPrimary = Color(0xFF00102B),
    primaryContainer = Color(0xFF0D2A5C),
    onPrimaryContainer = Color(0xFFD6E4FF),
    secondary = Color(0xFF7DB2FF),
    onSecondary = Color(0xFF00183F),
    secondaryContainer = Color(0xFF12315F),
    onSecondaryContainer = Color(0xFFD6E4FF),
    tertiary = Color(0xFF4FD6A5),
    onTertiary = Color(0xFF00382A),
    tertiaryContainer = Color(0xFF005140),
    onTertiaryContainer = Color(0xFFA0F2D0),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    onError = Color(0xFF690005),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF000000), // Pure black
    onBackground = Color(0xFFEAF0FF),
    surface = Color(0xFF05070D),
    onSurface = Color(0xFFEAF0FF),
    surfaceVariant = Color(0xFF141A2A),
    onSurfaceVariant = Color(0xFFB4BDD3),
    outline = Color(0xFF6F7A94),
    outlineVariant = Color(0xFF232B40),
    inverseSurface = Color(0xFFEAF0FF),
    inverseOnSurface = Color(0xFF05070D),
    inversePrimary = Color(0xFF1B5BD6),
    scrim = Color(0xFF000000),
    surfaceTint = Color(0xFF4C93FF),
    surfaceContainerLowest = Color(0xFF000000),
    surfaceContainerLow = Color(0xFF080B13),
    surfaceContainer = Color(0xFF0C101B),
    surfaceContainerHigh = Color(0xFF121827),
    surfaceContainerHighest = Color(0xFF192033),
)

// Semantic Colors
val colorPing = Color(0xFF009966) // Green
val colorPingRed = Color(0xFFFF0099) // Pink Red
val colorConfigType = Color(0xFF7DB2FF) // Soft blue
val colorFabActive = Color(0xFF2F7BFF) // Matix blue
val colorFabInactiveLight = Color(0xFF9C9C9C) // Gray
val colorFabInactiveDark = Color(0xFF646464) // Dark Gray
val dividerColorLight = Color(0xFFE0E0E0) // Light Gray
val dividerColorDark = Color(0xFF424242) // Dark Gray

object ThemeManager {
    private val _themeMode = MutableStateFlow(
        MmkvManager.decodeSettingsString(AppConfig.PREF_UI_MODE_NIGHT, "2") ?: "2"
    )
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _dynamicColorEnabled = MutableStateFlow(
        MmkvManager.decodeSettingsBool(AppConfig.PREF_DYNAMIC_COLOR, false)
    )
    val dynamicColorEnabled: StateFlow<Boolean> = _dynamicColorEnabled.asStateFlow()

    fun setThemeMode(mode: String) {
        MmkvManager.encodeSettings(AppConfig.PREF_UI_MODE_NIGHT, mode)
        _themeMode.value = mode
    }

    fun setDynamicColorEnabled(enabled: Boolean) {
        MmkvManager.encodeSettings(AppConfig.PREF_DYNAMIC_COLOR, enabled)
        _dynamicColorEnabled.value = enabled
    }

    fun refresh() {
        _themeMode.value =
            MmkvManager.decodeSettingsString(AppConfig.PREF_UI_MODE_NIGHT, "2") ?: "2"
        _dynamicColorEnabled.value =
            MmkvManager.decodeSettingsBool(AppConfig.PREF_DYNAMIC_COLOR, false)
    }
}

@Composable
fun resolveDarkTheme(): Boolean {
    val mode by ThemeManager.themeMode.collectAsState()
    return when (mode) {
        "1" -> false
        "2" -> true
        else -> isSystemInDarkTheme()
    }
}

val LocalDarkTheme = compositionLocalOf { false }

@Composable
fun AppTheme(
    darkTheme: Boolean = resolveDarkTheme(),
    content: @Composable () -> Unit
) {
    val dynamicColor by ThemeManager.dynamicColorEnabled.collectAsState()
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColor
        else -> LightColor
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity ?: return@SideEffect
            val window = activity.window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalDarkTheme provides darkTheme,
        LocalAppNotification provides AppNotificationManager
    ) {
        MaterialTheme(
            colorScheme = colorScheme
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AppNotificationBridge()
                content()
            }
        }
    }
}
