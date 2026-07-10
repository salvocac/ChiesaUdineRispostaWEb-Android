package com.caccavo.chiesaudinerispostaweb.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.isSystemInDarkTheme

private val ChiesaBlue = Color(0xFF033290)
private val ChiesaBlueLight = Color(0xFF2B6CB0)

private val LightColors = lightColorScheme(
    primary = ChiesaBlue,
    secondary = ChiesaBlueLight,
    tertiary = Color(0xFF00796B)
)

private val DarkColors = darkColorScheme(
    primary = ChiesaBlueLight,
    secondary = ChiesaBlue,
    tertiary = Color(0xFF00796B)
)

@Composable
fun ChiesaTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, content = content)
}
