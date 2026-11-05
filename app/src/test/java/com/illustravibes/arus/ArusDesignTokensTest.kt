package com.illustravibes.arus

import com.illustravibes.arus.ui.designsystem.ArusDarkColors
import com.illustravibes.arus.ui.designsystem.ArusLightColors
import com.illustravibes.arus.ui.designsystem.ArusRadius
import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.assertEquals
import org.junit.Test

class ArusDesignTokensTest {
    @Test
    fun shadcnZincThemesKeepReadableTextAndSubtleBorders() {
        assertEquals(0xFF18181B.toInt(), ArusLightColors.foreground.toArgb())
        assertEquals(0xFFFFFFFF.toInt(), ArusLightColors.surface.toArgb())
        assertEquals(0xFFFAFAFA.toInt(), ArusDarkColors.foreground.toArgb())
        assertEquals(8f, ArusRadius.Medium.value)
        assertEquals(12f, ArusRadius.Large.value)
        assertEquals(14f, ArusRadius.ExtraLarge.value)
    }
}
