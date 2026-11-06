package com.illustravibes.arus.ui

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GroupOrderShareIntentTest {
    @Test
    fun billSharingUsesChooserAndDoesNotSendAutomatically() {
        val chooser = createGroupOrderShareIntent("Makan tim", "Budi", 40_000, "Sisa Rp 20.000")

        assertEquals(Intent.ACTION_CHOOSER, chooser.action)
        val sendIntent = chooser.getParcelableExtra(Intent.EXTRA_INTENT) as? Intent
        assertNotNull(sendIntent)
        assertEquals(Intent.ACTION_SEND, sendIntent!!.action)
        assertEquals("text/plain", sendIntent.type)
        assertTrue(sendIntent.getStringExtra(Intent.EXTRA_TEXT).orEmpty().contains("Budi: Rp 40.000"))
    }
}
