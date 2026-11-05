package com.illustravibes.arus

import org.junit.Assert.assertEquals
import org.junit.Test

class BuildConfigurationTest {
    @Test
    fun applicationIdIsArus() {
        assertEquals("com.illustravibes.arus", BuildConfig.APPLICATION_ID)
    }
}
