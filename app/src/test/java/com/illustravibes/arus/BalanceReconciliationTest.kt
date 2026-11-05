package com.illustravibes.arus

import com.illustravibes.arus.domain.BalanceReconciliation
import org.junit.Assert.assertEquals
import org.junit.Test

class BalanceReconciliationTest {
    @Test
    fun calculatesPositiveNegativeAndZeroDifferences() {
        assertEquals(250L, BalanceReconciliation(1_000, 1_250).differenceRupiah)
        assertEquals(-250L, BalanceReconciliation(1_000, 750).differenceRupiah)
        assertEquals(0L, BalanceReconciliation(1_000, 1_000).differenceRupiah)
    }
}
