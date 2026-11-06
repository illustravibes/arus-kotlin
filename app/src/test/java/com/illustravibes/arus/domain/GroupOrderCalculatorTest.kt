package com.illustravibes.arus.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class GroupOrderCalculatorTest {
    @Test
    fun splitsItemAndEqualFeeAndAssignsRemainderToConfiguredParticipant() {
        val result = GroupOrderCalculator.calculate(
            participantIds = listOf(null, 1L, 2L),
            items = listOf(GroupOrderItemInput(101, mapOf(null to 1, 1L to 1))),
            fees = listOf(GroupOrderFeeInput(10, GroupFeeSplitMode.EQUAL)),
            roundingBearer = null,
        )

        assertEquals(101, result.subtotalRupiah)
        assertEquals(10, result.sharedFeeTotalRupiah)
        assertEquals(111, result.grandTotalRupiah)
        assertEquals(51, result.participants.getValue(null).itemsAmountRupiah)
        assertEquals(50, result.participants.getValue(1L).itemsAmountRupiah)
        assertEquals(4, result.participants.getValue(null).feeAmountRupiah)
        assertEquals(3, result.participants.getValue(1L).feeAmountRupiah)
        assertEquals(3, result.participants.getValue(2L).feeAmountRupiah)
        assertEquals(111, result.participants.values.sumOf { it.totalAmountRupiah })
    }

    @Test
    fun proportionalFeeFollowsItemTotalsAndManualFeeMustBalance() {
        val result = GroupOrderCalculator.calculate(
            participantIds = listOf(null, 1L, 2L),
            items = listOf(
                GroupOrderItemInput(60, mapOf(null to 1)),
                GroupOrderItemInput(40, mapOf(1L to 1)),
            ),
            fees = listOf(GroupOrderFeeInput(11, GroupFeeSplitMode.PROPORTIONAL)),
            roundingBearer = null,
        )
        assertEquals(7, result.participants.getValue(null).feeAmountRupiah)
        assertEquals(4, result.participants.getValue(1L).feeAmountRupiah)
        assertEquals(0, result.participants.getValue(2L).feeAmountRupiah)

        assertThrows(IllegalArgumentException::class.java) {
            GroupOrderCalculator.calculate(
                participantIds = listOf(null, 1L),
                items = listOf(GroupOrderItemInput(100, mapOf(null to 1))),
                fees = listOf(GroupOrderFeeInput(10, GroupFeeSplitMode.MANUAL, mapOf(null to 2, 1L to 3))),
            )
        }
    }

    @Test
    fun negativeDiscountIsBalancedAndGrandTotalCannotGoNegative() {
        val result = GroupOrderCalculator.calculate(
            participantIds = listOf(null, 1L),
            items = listOf(GroupOrderItemInput(100, mapOf(null to 1, 1L to 1))),
            fees = listOf(GroupOrderFeeInput(-10, GroupFeeSplitMode.EQUAL)),
            roundingBearer = 1L,
        )
        assertEquals(-10, result.discountTotalRupiah)
        assertEquals(90, result.grandTotalRupiah)
        assertEquals(90, result.participants.values.sumOf { it.totalAmountRupiah })

        assertThrows(IllegalArgumentException::class.java) {
            GroupOrderCalculator.calculate(
                participantIds = listOf(null),
                items = listOf(GroupOrderItemInput(10, mapOf(null to 1))),
                fees = listOf(GroupOrderFeeInput(-20, GroupFeeSplitMode.EQUAL)),
            )
        }
    }
}
