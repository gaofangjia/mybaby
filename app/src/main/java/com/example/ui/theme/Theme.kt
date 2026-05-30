package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = FlowerCoral,
    secondary = Amber600,
    tertiary = Sky600,
    background = CozyDarkBg,
    surface = CozyDarkSurface,
    onPrimary = WarmIvory,
    onSecondary = CozyDarkBg,
    onBackground = DarkEarthyText,
    onSurface = DarkEarthyText
)

private val LightColorScheme = lightColorScheme(
    primary = FlowerCoral,
    secondary = Amber600,
    tertiary = Sky600,
    background = WarmIvory,
    surface = SoftCardWhite,
    onPrimary = WarmIvory,
    onSecondary = EarthyText,
    onBackground = EarthyText,
    onSurface = EarthyText
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Keep child custom feel by default (so disable play-services dynamic coloring)
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
