package com.rahmatsobrian.sirohaequ.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.rahmatsobrian.sirohaequ.data.ThemeMode

private val FallbackDark = darkColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF9ECAFF),
    secondary = androidx.compose.ui.graphics.Color(0xFFBCC7DC)
)
private val FallbackLight = lightColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF3D5F8A),
    secondary = androidx.compose.ui.graphics.Color(0xFF545F71)
)

@Composable
fun SirohaEquTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColorEnabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val useDark = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }

    val context = LocalContext.current
    val supportsDynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val colorScheme = when {
        dynamicColorEnabled && supportsDynamicColor && useDark -> dynamicDarkColorScheme(context)
        dynamicColorEnabled && supportsDynamicColor && !useDark -> dynamicLightColorScheme(context)
        useDark -> FallbackDark
        else -> FallbackLight
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}
