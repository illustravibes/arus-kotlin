package com.illustravibes.arus.ui.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import com.illustravibes.arus.R

data class ArusColors(
    val background: Color,
    val foreground: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val muted: Color,
    val mutedForeground: Color,
    val primary: Color,
    val onPrimary: Color,
    val border: Color,
    val error: Color,
)

data class ArusTypography(
    val display: TextStyle,
    val headline: TextStyle,
    val title: TextStyle,
    val body: TextStyle,
    val label: TextStyle,
)

private val LightColors = ArusColors(
    background = Color(0xFFFAFAFA), foreground = Color(0xFF18181B),
    surface = Color(0xFFFFFFFF), surfaceVariant = Color(0xFFF4F4F5),
    muted = Color(0xFFF4F4F5), mutedForeground = Color(0xFF3F3F46),
    primary = Color(0xFF27272A), onPrimary = Color(0xFFFAFAFA),
    border = Color(0xFFE4E4E7), error = Color(0xFFB91C1C),
)

internal val ArusLightColors = LightColors

private val DarkColors = ArusColors(
    background = Color(0xFF18181B), foreground = Color(0xFFFAFAFA),
    surface = Color(0xFF202024), surfaceVariant = Color(0xFF27272A),
    muted = Color(0xFF27272A), mutedForeground = Color(0xFFD4D4D8),
    primary = Color(0xFFFAFAFA), onPrimary = Color(0xFF18181B),
    border = Color(0xFF3F3F46), error = Color(0xFFF87171),
)

internal val ArusDarkColors = DarkColors

val ArusFontFamily = FontFamily(
    Font(R.font.plus_jakarta_sans, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans, FontWeight.Bold),
)

private val ArusTextStyles = ArusTypography(
    display = TextStyle(fontFamily = ArusFontFamily, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 38.sp, letterSpacing = (-0.8).sp),
    headline = TextStyle(fontFamily = ArusFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 30.sp, letterSpacing = (-0.3).sp),
    title = TextStyle(fontFamily = ArusFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp, letterSpacing = (-0.1).sp),
    body = TextStyle(fontFamily = ArusFontFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    label = TextStyle(fontFamily = ArusFontFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
)

val LocalArusColors = staticCompositionLocalOf { LightColors }
val LocalArusTypography = staticCompositionLocalOf { ArusTextStyles }
val LocalArusContentColor = staticCompositionLocalOf { LightColors.foreground }
val LocalArusTextStyle = staticCompositionLocalOf { ArusTextStyles.body }

object ArusTheme {
    val colors: ArusColors
        @Composable get() = LocalArusColors.current
    val typography: ArusTypography
        @Composable get() = LocalArusTypography.current
}

@Composable
fun ArusTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalArusColors provides if (darkTheme) DarkColors else LightColors,
        LocalArusTypography provides ArusTextStyles,
        LocalArusContentColor provides if (darkTheme) DarkColors.foreground else LightColors.foreground,
        LocalArusTextStyle provides ArusTextStyles.body,
        content = content,
    )
}
