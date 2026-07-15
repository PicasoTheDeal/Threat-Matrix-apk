package com.picasothedeal.threatmatrix.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowInsetsControllerCompat
import com.picasothedeal.threatmatrix.R

private val MatrixBackground = Color(0xFF0F0F0F)
private val MatrixSurface    = Color(0xFF0A0A0A)
private val MatrixText       = Color(0xFFE6EDF3)
private val MatrixTextSecondary = Color(0xFFB1BAC4)
private val MatrixAccent     = Color(0xFF5B7A9E)
private val MatrixAccentHover = Color(0xFF3D5068)

private val DarkColorScheme = darkColorScheme(
    primary = MatrixAccent,
    secondary = MatrixAccentHover,
    tertiary = MatrixTextSecondary,
    background = MatrixBackground,
    surface = MatrixSurface,
    onPrimary = MatrixText,
    onSecondary = MatrixText,
    onTertiary = MatrixText,
    onBackground = MatrixText,
    onSurface = MatrixText
)

val JetBrainsMono = FontFamily(
    Font(R.font.jetbrains_mono, FontWeight.Normal),
    Font(R.font.jetbrains_mono, FontWeight.Bold)
)

private val AppTypography = Typography(
    displayLarge  = TextStyle(fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, fontSize = 24.sp),
    headlineMedium = TextStyle(fontFamily = JetBrainsMono, fontWeight = FontWeight.SemiBold, fontSize = 18.sp),
    bodyLarge     = TextStyle(fontFamily = JetBrainsMono, fontSize = 14.sp),
    bodyMedium    = TextStyle(fontFamily = JetBrainsMono, fontSize = 13.sp),
    labelSmall    = TextStyle(fontFamily = JetBrainsMono, fontSize = 10.sp)
)

@Composable
fun ThreatMatrixTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val controller = WindowInsetsControllerCompat(window, view)
            controller.isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = AppTypography,
        content = content
    )
}