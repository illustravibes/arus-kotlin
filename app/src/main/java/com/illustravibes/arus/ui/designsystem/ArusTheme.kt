package com.illustravibes.arus.ui.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.illustravibes.arus.R

// Font Families
val PlusJakartaSans = FontFamily(
    Font(R.font.plus_jakarta_sans, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans, FontWeight.Bold),
)

val ArusFontFamily = PlusJakartaSans

val Lora = FontFamily(
    Font(R.font.lora, FontWeight.Normal),
    Font(R.font.lora, FontWeight.SemiBold),
    Font(R.font.lora_italic, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.lora_italic, FontWeight.SemiBold, FontStyle.Italic),
)

val JetBrainsMono = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
    Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
    Font(R.font.jetbrains_mono_bold, FontWeight.Bold),
)

@Immutable
data class ArusColors(
    val background: Color,
    val foreground: Color,
    val surface: Color,
    val surfaceSecondary: Color,
    val surfaceTertiary: Color,
    val surfaceSubtle: Color,
    val surfaceDark: Color,
    val inverseSurface: Color,
    val inverseForeground: Color,
    val muted: Color,
    val mutedForeground: Color,
    val primary: Color,
    val onPrimary: Color,
    val primaryPressed: Color,
    val primarySubtle: Color,
    val border: Color,
    val borderStrong: Color,
    val error: Color,
    val errorSurface: Color,
    val income: Color,
    val incomeText: Color,
    val expense: Color,
    val expenseSurface: Color,
    val warning: Color,
    val warningText: Color,
    val info: Color,
) {
    // Backwards compatibility alias for existing code
    val surfaceVariant: Color get() = surfaceSecondary
    val textPrimary: Color get() = foreground
    val textSecondary: Color get() = mutedForeground
}

@Immutable
data class ArusTypography(
    val display: TextStyle,
    val headlineLarge: TextStyle,
    val headlineMedium: TextStyle,
    val headlineSmall: TextStyle,
    val bodyLarge: TextStyle,
    val bodyMedium: TextStyle,
    val bodySmall: TextStyle,
    val label: TextStyle,
    val editorial: TextStyle,
    val editorialQuote: TextStyle,
    val amountMono: TextStyle,
    val code: TextStyle,
) {
    // Backwards compatibility aliases
    val headline: TextStyle get() = headlineMedium
    val title: TextStyle get() = headlineSmall
    val titleSmall: TextStyle get() = headlineSmall
    val body: TextStyle get() = bodyMedium
    val caption: TextStyle get() = bodySmall
}

@Immutable
data class ArusShapesTheme(
    val pill: Shape = ArusShapes.Pill,
    val input: Shape = ArusShapes.Input,
    val card: Shape = ArusShapes.Card,
    val sheet: Shape = ArusShapes.Sheet,
    val dialog: Shape = ArusShapes.Dialog,
)

// Light Colors (Zinc foundation + Brand Blue #0009FF + Semantics)
private val LightColors = ArusColors(
    background = Color(0xFFFFFFFF),
    foreground = ArusColorsPalette.Zinc950,
    surface = Color(0xFFFFFFFF),
    surfaceSecondary = ArusColorsPalette.Zinc100,
    surfaceTertiary = ArusColorsPalette.Zinc200,
    surfaceSubtle = ArusColorsPalette.Zinc50,
    surfaceDark = ArusColorsPalette.Zinc800,
    inverseSurface = ArusColorsPalette.Zinc900,
    inverseForeground = Color(0xFFFFFFFF),
    muted = ArusColorsPalette.Zinc100,
    mutedForeground = ArusColorsPalette.Zinc500,
    primary = ArusColorsPalette.Primary,
    onPrimary = ArusColorsPalette.OnPrimary,
    primaryPressed = ArusColorsPalette.PrimaryPressed,
    primarySubtle = ArusColorsPalette.PrimarySubtle,
    border = ArusColorsPalette.Zinc200,
    borderStrong = ArusColorsPalette.Zinc300,
    error = ArusColorsPalette.Expense,
    errorSurface = ArusColorsPalette.ExpenseSurface,
    income = ArusColorsPalette.Income,
    incomeText = ArusColorsPalette.IncomeText,
    expense = ArusColorsPalette.Expense,
    expenseSurface = ArusColorsPalette.ExpenseSurface,
    warning = ArusColorsPalette.Warning,
    warningText = ArusColorsPalette.WarningText,
    info = ArusColorsPalette.Info,
)

internal val ArusLightColors = LightColors

// Dark Colors (Prepared for dark mode)
private val DarkColors = ArusColors(
    background = ArusColorsPalette.Zinc950,
    foreground = ArusColorsPalette.Zinc50,
    surface = ArusColorsPalette.Zinc900,
    surfaceSecondary = ArusColorsPalette.Zinc800,
    surfaceTertiary = ArusColorsPalette.Zinc700,
    surfaceSubtle = Color(0xFF1F1F23),
    surfaceDark = ArusColorsPalette.Zinc900,
    inverseSurface = ArusColorsPalette.Zinc50,
    inverseForeground = ArusColorsPalette.Zinc950,
    muted = ArusColorsPalette.Zinc800,
    mutedForeground = ArusColorsPalette.Zinc400,
    primary = Color(0xFF4D55FF), // Brighter blue for dark background readability
    onPrimary = Color(0xFFFFFFFF),
    primaryPressed = Color(0xFF333DFF),
    primarySubtle = Color(0xFF1E224D),
    border = ArusColorsPalette.Zinc800,
    borderStrong = ArusColorsPalette.Zinc700,
    error = Color(0xFFEF4444),
    errorSurface = Color(0xFF331414),
    income = Color(0xFF22C55E),
    incomeText = Color(0xFF4ADE80),
    expense = Color(0xFFEF4444),
    expenseSurface = Color(0xFF331414),
    warning = Color(0xFFF59E0B),
    warningText = Color(0xFFFBBF24),
    info = Color(0xFF4D55FF),
)

internal val ArusDarkColors = DarkColors

private val ArusTextStyles = ArusTypography(
    display = TextStyle(fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 40.sp, letterSpacing = (-0.8).sp),
    headlineLarge = TextStyle(fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 32.sp, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 28.sp, letterSpacing = (-0.3).sp),
    headlineSmall = TextStyle(fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 24.sp, letterSpacing = (-0.2).sp),
    bodyLarge = TextStyle(fontFamily = PlusJakartaSans, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = PlusJakartaSans, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = PlusJakartaSans, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    label = TextStyle(fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
    editorial = TextStyle(fontFamily = Lora, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 32.sp, letterSpacing = (-0.3).sp),
    editorialQuote = TextStyle(fontFamily = Lora, fontWeight = FontWeight.Normal, fontStyle = FontStyle.Italic, fontSize = 16.sp, lineHeight = 24.sp),
    amountMono = TextStyle(fontFamily = JetBrainsMono, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 22.sp),
    code = TextStyle(fontFamily = JetBrainsMono, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
)

val LocalArusColors = staticCompositionLocalOf { LightColors }
val LocalArusTypography = staticCompositionLocalOf { ArusTextStyles }
val LocalArusShapes = staticCompositionLocalOf { ArusShapesTheme() }
val LocalArusContentColor = staticCompositionLocalOf { LightColors.foreground }
val LocalArusTextStyle = staticCompositionLocalOf { ArusTextStyles.bodyMedium }

object ArusTheme {
    val colors: ArusColors
        @Composable get() = LocalArusColors.current
    val typography: ArusTypography
        @Composable get() = LocalArusTypography.current
    val shapes: ArusShapesTheme
        @Composable get() = LocalArusShapes.current
    val spacing: ArusSpacing
        get() = ArusSpacing
}

@Composable
fun ArusTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (darkTheme) DarkColors else LightColors
    CompositionLocalProvider(
        LocalArusColors provides colors,
        LocalArusTypography provides ArusTextStyles,
        LocalArusShapes provides ArusShapesTheme(),
        LocalArusContentColor provides colors.foreground,
        LocalArusTextStyle provides ArusTextStyles.bodyMedium,
        content = content,
    )
}
