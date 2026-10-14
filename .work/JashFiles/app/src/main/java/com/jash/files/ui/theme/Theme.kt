package com.jash.files.ui.theme

import android.app.Activity
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

/** Apple system colors (iOS 17), light and dark. */
@Immutable
data class IOSColors(
    val isDark: Boolean,
    val bg: Color,        // systemGroupedBackground
    val plainBg: Color,   // systemBackground
    val card: Color,      // secondarySystemGroupedBackground
    val sheet: Color,
    val docBg: Color,
    val label: Color,
    val label2: Color,
    val label3: Color,
    val separator: Color,
    val fill: Color,
    val pressed: Color,
    val barBg: Color,
    val segSel: Color,
    val switchOff: Color,
    val blue: Color,
    val red: Color,
    val green: Color,
    val orange: Color,
    val yellow: Color,
    val purple: Color,
    val pink: Color,
    val teal: Color,
    val indigo: Color,
    val gray: Color,
    val folder: Color,
)

val LightIOS = IOSColors(
    isDark = false,
    bg = Color(0xFFF2F2F7), plainBg = Color(0xFFFFFFFF), card = Color(0xFFFFFFFF),
    sheet = Color(0xFFF9F9F9), docBg = Color(0xFFFFFFFF),
    label = Color(0xFF000000), label2 = Color(0x993C3C43), label3 = Color(0x4D3C3C43),
    separator = Color(0x4A3C3C43), fill = Color(0x1F767680), pressed = Color(0xFFE5E5EA),
    barBg = Color(0xF5F9F9F9), segSel = Color(0xFFFFFFFF), switchOff = Color(0x29787880),
    blue = Color(0xFF007AFF), red = Color(0xFFFF3B30), green = Color(0xFF34C759), orange = Color(0xFFFF9500),
    yellow = Color(0xFFFFCC00), purple = Color(0xFFAF52DE), pink = Color(0xFFFF2D55), teal = Color(0xFF30B0C7),
    indigo = Color(0xFF5856D6), gray = Color(0xFF8E8E93), folder = Color(0xFF4BA8F2),
)

val DarkIOS = IOSColors(
    isDark = true,
    bg = Color(0xFF000000), plainBg = Color(0xFF000000), card = Color(0xFF1C1C1E),
    sheet = Color(0xFF2C2C2E), docBg = Color(0xFF2C2C2E),
    label = Color(0xFFFFFFFF), label2 = Color(0x99EBEBF5), label3 = Color(0x4DEBEBF5),
    separator = Color(0xA6545458), fill = Color(0x3D767680), pressed = Color(0xFF3A3A3C),
    barBg = Color(0xF0161618), segSel = Color(0xFF636366), switchOff = Color(0x52787880),
    blue = Color(0xFF0A84FF), red = Color(0xFFFF453A), green = Color(0xFF30D158), orange = Color(0xFFFF9F0A),
    yellow = Color(0xFFFFD60A), purple = Color(0xFFBF5AF2), pink = Color(0xFFFF375F), teal = Color(0xFF40C8E0),
    indigo = Color(0xFF5E5CE6), gray = Color(0xFF8E8E93), folder = Color(0xFF3E9CEB),
)

val LocalIOS = staticCompositionLocalOf { LightIOS }

object IOS {
    val colors: IOSColors
        @Composable get() = LocalIOS.current
}

/** iOS Dynamic Type sizes (Large). */
object T {
    val largeTitle = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.37.sp, lineHeight = 41.sp)
    val title2 = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.35.sp)
    val title3 = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.38.sp)
    val headline = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.41).sp)
    val body = TextStyle(fontSize = 17.sp, letterSpacing = (-0.41).sp)
    val callout = TextStyle(fontSize = 16.sp, letterSpacing = (-0.32).sp)
    val subhead = TextStyle(fontSize = 15.sp, letterSpacing = (-0.24).sp)
    val footnote = TextStyle(fontSize = 13.sp, letterSpacing = (-0.08).sp)
    val caption = TextStyle(fontSize = 12.sp)
    val caption2 = TextStyle(fontSize = 11.sp, letterSpacing = 0.07.sp)
}

@Composable
fun JashTheme(dark: Boolean, content: @Composable () -> Unit) {
    val colors = if (dark) DarkIOS else LightIOS
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }
    val scheme = if (dark) darkColorScheme(primary = colors.blue, background = colors.bg, surface = colors.card)
    else lightColorScheme(primary = colors.blue, background = colors.bg, surface = colors.card)
    MaterialTheme(colorScheme = scheme) {
        CompositionLocalProvider(
            LocalIOS provides colors,
            LocalTextStyle provides T.body.copy(color = colors.label),
            content = content,
        )
    }
}
