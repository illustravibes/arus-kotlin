package com.illustravibes.arus.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.illustravibes.arus.ui.designsystem.ArusTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ReceiptScannerFallbackTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun deniedCameraKeepsGalleryAndManualEntryAvailable() {
        var selected = ""
        composeRule.setContent {
            ArusTheme {
                ReceiptSourceChooser(
                    cameraPermissionDenied = true,
                    onCamera = { selected = "camera" },
                    onGallery = { selected = "gallery" },
                    onManual = { selected = "manual" },
                )
            }
        }

        composeRule.onNodeWithText("Izin kamera ditolak. Galeri dan input manual tetap tersedia.").assertIsDisplayed()
        composeRule.onNodeWithText("Pilih dari galeri").performClick()
        assertEquals("gallery", selected)
        composeRule.onNodeWithText("Input manual").performClick()
        assertEquals("manual", selected)
    }
}
