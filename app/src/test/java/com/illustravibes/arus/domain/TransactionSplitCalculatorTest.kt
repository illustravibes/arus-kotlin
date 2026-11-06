package com.illustravibes.arus.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TransactionSplitCalculatorTest {
    @Test
    fun nominalModeRequiresPositiveBalancedRows() {
        assertEquals(listOf(30_000L, 15_000L),
            TransactionSplitCalculator.amountsFromNominal(45_000, listOf(30_000, 15_000)))
        assertNull(TransactionSplitCalculator.amountsFromNominal(45_000, listOf(30_000, 10_000)))
        assertNull(TransactionSplitCalculator.amountsFromNominal(45_000, listOf(45_000)))
    }

    @Test
    fun percentModeAssignsRoundingRemainderWithoutChangingTotal() {
        val amounts = TransactionSplitCalculator.amountsFromPercent(101, listOf(33, 33, 34))

        assertEquals(listOf(33L, 33L, 35L), amounts)
        assertEquals(101L, amounts?.sum())
        assertEquals(listOf(33L, 33L, 34L),
            TransactionSplitCalculator.percentagesFromAmounts(101, listOf(33, 33, 35)))
        assertNull(TransactionSplitCalculator.amountsFromPercent(101, listOf(50, 40)))
    }
}
