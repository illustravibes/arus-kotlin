package com.illustravibes.arus.ui.designsystem

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object ArusColorsPalette {
    // Brand Electric Blue
    val Primary = Color(0xFF0009FF)
    val OnPrimary = Color(0xFFFFFFFF)
    val PrimaryPressed = Color(0xFF0007CC)
    val PrimarySubtle = Color(0xFFEEF0FF)

    // Tailwind Zinc Scale
    val Zinc50 = Color(0xFFFAFAFA)
    val Zinc100 = Color(0xFFF4F4F5)
    val Zinc200 = Color(0xFFE4E4E7)
    val Zinc300 = Color(0xFFD4D4D8)
    val Zinc400 = Color(0xFFA1A1AA)
    val Zinc500 = Color(0xFF71717A)
    val Zinc600 = Color(0xFF52525B)
    val Zinc700 = Color(0xFF3F3F46)
    val Zinc800 = Color(0xFF27272A)
    val Zinc900 = Color(0xFF18181B)
    val Zinc950 = Color(0xFF09090B)

    // Semantic Colors
    val Income = Color(0xFF16A34A)
    val IncomeText = Color(0xFF15803D)
    val Expense = Color(0xFFDC2626)
    val ExpenseSurface = Color(0xFFFEF2F2)
    val Warning = Color(0xFFD97706)
    val WarningText = Color(0xFFB45309)
    val Info = Color(0xFF0009FF)
}

object ArusRadius {
    val Small = 6.dp
    val Medium = 8.dp
    val Large = 12.dp
    val ExtraLarge = 14.dp
    val Input = 18.dp
    val Card = 24.dp
    val Dialog = 28.dp
}

object ArusShapes {
    val Pill = CircleShape
    val Input = RoundedCornerShape(18.dp)
    val Card = RoundedCornerShape(24.dp)
    val Sheet = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    val Dialog = RoundedCornerShape(28.dp)
}

object ArusSpacing {
    val XxSmall = 4.dp
    val XSmall = 4.dp
    val ExtraSmall = 4.dp
    val Small = 8.dp
    val Medium = 12.dp
    val Large = 16.dp
    val CardPadding = 16.dp
    val ScreenHorizontal = 16.dp
    val Normal = 20.dp
    val XLarge = 24.dp
    val XXLarge = 32.dp
    val Huge = 40.dp
}
