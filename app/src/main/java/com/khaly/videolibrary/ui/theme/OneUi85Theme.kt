package com.khaly.videolibrary.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val OneUiLight = lightColorScheme(
    primary = Color(0xFF2563EB),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDEAFF),
    onPrimaryContainer = Color(0xFF082A63),
    secondary = Color(0xFF5F6C80),
    background = Color(0xFFF7F7FA),
    onBackground = Color(0xFF101114),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF101114),
    surfaceVariant = Color(0xFFEDEEF3),
    onSurfaceVariant = Color(0xFF5B606A),
    outline = Color(0xFFD6D8DF)
)

private val OneUiDark = darkColorScheme(
    primary = Color(0xFF8AB4FF),
    onPrimary = Color(0xFF092A5A),
    primaryContainer = Color(0xFF173E78),
    onPrimaryContainer = Color(0xFFD8E7FF),
    secondary = Color(0xFFB8C2D4),
    background = Color(0xFF0F1014),
    onBackground = Color(0xFFF2F3F7),
    surface = Color(0xFF1A1C22),
    onSurface = Color(0xFFF2F3F7),
    surfaceVariant = Color(0xFF272A33),
    onSurfaceVariant = Color(0xFFC5C8D0),
    outline = Color(0xFF3A3D46)
)

private val OneUiShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(34.dp)
)

@Composable
fun OneUi85Theme(
    dynamicColor: Boolean = true,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    val colors: ColorScheme =
        if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else {
            if (darkTheme) OneUiDark else OneUiLight
        }

    MaterialTheme(
        colorScheme = colors,
        shapes = OneUiShapes,
        content = content
    )
}
