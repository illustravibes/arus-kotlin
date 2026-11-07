package com.illustravibes.arus

import androidx.compose.ui.graphics.toArgb
import com.illustravibes.arus.ui.designsystem.AmountFormatter
import com.illustravibes.arus.ui.designsystem.AmountStyle
import com.illustravibes.arus.ui.designsystem.ArusDarkColors
import com.illustravibes.arus.ui.designsystem.ArusLightColors
import com.illustravibes.arus.ui.designsystem.ArusRadius
import org.junit.Assert.assertEquals
import org.junit.Test

class ArusDesignTokensTest {
    @Test
    fun designTokensMatchDesignSpecification() {
        // Brand & Palette tokens
        assertEquals(0xFF0009FF.toInt(), ArusLightColors.primary.toArgb())
        assertEquals(0xFF09090B.toInt(), ArusLightColors.foreground.toArgb())
        assertEquals(0xFFFFFFFF.toInt(), ArusLightColors.surface.toArgb())
        assertEquals(0xFF16A34A.toInt(), ArusLightColors.income.toArgb())
        assertEquals(0xFFDC2626.toInt(), ArusLightColors.expense.toArgb())
        assertEquals(0xFFD97706.toInt(), ArusLightColors.warning.toArgb())

        // Dark theme tokens
        assertEquals(0xFFFAFAFA.toInt(), ArusDarkColors.foreground.toArgb())

        // Shape / Radius tokens
        assertEquals(18f, ArusRadius.Input.value)
        assertEquals(24f, ArusRadius.Card.value)
        assertEquals(28f, ArusRadius.Dialog.value)
    }

    @Test
    fun amountFormatterHandlesRupiahAndCompactCorrectly() {
        // Standard format
        assertEquals("Rp 1.250.000", AmountFormatter.format(1_250_000L))
        assertEquals("-Rp 65.000", AmountFormatter.format(-65_000L))
        assertEquals("+Rp 500.000", AmountFormatter.format(500_000L, includeSign = true))

        // Compact format
        assertEquals("1,2 jt", AmountFormatter.formatCompact(1_200_000L))
        assertEquals("450 rb", AmountFormatter.formatCompact(450_000L))
        assertEquals("2,5 M", AmountFormatter.formatCompact(2_500_000_000L))
    }
}
