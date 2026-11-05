package com.illustravibes.arus.ui

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppLockTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @After
    fun tearDown() {
        AppLock.clear(context)
    }

    @Test
    fun pinIsSaltedAndOnlyMatchingPinUnlocks() {
        AppLock.setPin(context, "123456")

        assertTrue(AppLock.isEnabled(context))
        assertTrue(AppLock.verify(context, "123456"))
        assertFalse(AppLock.verify(context, "654321"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun pinMustContainSixDigits() {
        AppLock.setPin(context, "1234")
    }
}
