package com.hihusky.mnemora.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ============================================================
// Material Design 3 Color Schemes
// ============================================================
// We define complete light/dark schemes with a quiet system-neutral base.
// The palette intentionally avoids a dominant blue wash; blue appears mainly
// on primary actions and selected states.
//
// Surface container roles (M3 tone-based surfaces):
//   - surfaceContainerLowest: 4dp  tonal — lowest container
//   - surfaceContainerLow:    4-8dp tonal — cards at rest
//   - surfaceContainer:       8-12dp tonal — default container (RECOMMENDED)
//   - surfaceContainerHigh:   12-16dp tonal — elevated container
//   - surfaceContainerHighest:16-24dp tonal — dialogs, menus
//
// For static themes we hand-pick tones from the generated palette.
// For dynamic themes (Android 12+) the system generates these automatically.
// ============================================================

private val LightColorScheme = lightColorScheme(
    primary = SeedColor,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE5F1FF),
    onPrimaryContainer = Color(0xFF003E7E),
    secondary = Color(0xFF8E8E93),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE5E5EA),
    onSecondaryContainer = Color(0xFF1D1D1F),
    tertiary = SuccessColor,
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFE8F8EE),
    onTertiaryContainer = Color(0xFF0B5F28),
    error = Color(0xFFFF3B30),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFE9E8),
    onErrorContainer = Color(0xFF8A120D),
    background = Color(0xFFF5F5F7),
    onBackground = Color(0xFF1D1D1F),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1D1D1F),
    surfaceVariant = Color(0xFFE5E5EA),
    onSurfaceVariant = Color(0xFF6E6E73),
    outline = Color(0xFFC7C7CC),
    outlineVariant = Color(0xFFE5E5EA),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFF1D1D1F),
    inverseOnSurface = Color(0xFFFFFFFF),
    inversePrimary = Color(0xFF66B2FF),
    surfaceDim = Color(0xFFE5E5EA),
    surfaceBright = Color(0xFFFFFFFF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF9F9FB),
    surfaceContainer = Color(0xFFF2F2F7),
    surfaceContainerHigh = Color(0xFFEDEDF2),
    surfaceContainerHighest = Color(0xFFE5E5EA),
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF0A84FF),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF003E7E),
    onPrimaryContainer = Color(0xFFE5F1FF),
    secondary = Color(0xFF98989D),
    onSecondary = Color(0xFF1C1C1E),
    secondaryContainer = Color(0xFF3A3A3C),
    onSecondaryContainer = Color(0xFFF2F2F7),
    tertiary = Color(0xFF32D74B),
    onTertiary = Color(0xFF062D13),
    tertiaryContainer = Color(0xFF0B5F28),
    onTertiaryContainer = Color(0xFFE8F8EE),
    error = Color(0xFFFF453A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFF8A120D),
    onErrorContainer = Color(0xFFFFE9E8),
    background = Color(0xFF000000),
    onBackground = Color(0xFFF2F2F7),
    surface = Color(0xFF1C1C1E),
    onSurface = Color(0xFFF2F2F7),
    surfaceVariant = Color(0xFF3A3A3C),
    onSurfaceVariant = Color(0xFFAEAEB2),
    outline = Color(0xFF636366),
    outlineVariant = Color(0xFF3A3A3C),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFFF2F2F7),
    inverseOnSurface = Color(0xFF1D1D1F),
    inversePrimary = Color(0xFF007AFF),
    surfaceDim = Color(0xFF000000),
    surfaceBright = Color(0xFF2C2C2E),
    surfaceContainerLowest = Color(0xFF000000),
    surfaceContainerLow = Color(0xFF1C1C1E),
    surfaceContainer = Color(0xFF2C2C2E),
    surfaceContainerHigh = Color(0xFF3A3A3C),
    surfaceContainerHighest = Color(0xFF48484A),
)

@Composable
fun MnemoraTheme(
    themeMode: Int = 0, // 0=system, 1=light, 2=dark
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        1 -> false
        2 -> true
        else -> systemDark
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
