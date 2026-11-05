package com.illustravibes.arus

import com.illustravibes.arus.domain.AdjustmentDirection
import com.illustravibes.arus.domain.BalanceCalculator
import com.illustravibes.arus.domain.BalanceTransaction
import com.illustravibes.arus.domain.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test

class BalanceCalculatorTest {
    @Test
    fun ignoresFutureAndSoftDeletedTransactions() {
        val transactions = listOf(
            BalanceTransaction(TransactionType.INCOME, 2_000, 10),
            BalanceTransaction(TransactionType.EXPENSE, 300, 11),
            BalanceTransaction(TransactionType.INCOME, 900, 20),
            BalanceTransaction(TransactionType.EXPENSE, 500, 12, deletedAtEpochMillis = 13),
        )
        assertEquals(1_700, BalanceCalculator.calculate(0, transactions, now = 15))
    }

    @Test
    fun appliesAllWalletEffectsAndAdjustmentDirection() {
        val transactions = listOf(
            BalanceTransaction(TransactionType.TRANSFER_IN, 100, 1),
            BalanceTransaction(TransactionType.REPAYMENT_IN, 200, 2),
            BalanceTransaction(TransactionType.LOAN_IN, 300, 3),
            BalanceTransaction(TransactionType.TRANSFER_OUT, 50, 4),
            BalanceTransaction(TransactionType.REFUND_OUT, 25, 5),
            BalanceTransaction(TransactionType.LOAN_OUT, 35, 6),
            BalanceTransaction(TransactionType.DEBT_PAY_OUT, 40, 7),
            BalanceTransaction(TransactionType.ADJUSTMENT, 10, 8, direction = AdjustmentDirection.PLUS),
            BalanceTransaction(TransactionType.ADJUSTMENT, 5, 9, direction = AdjustmentDirection.MINUS),
            BalanceTransaction(TransactionType.WRITE_OFF, 900, 10),
            BalanceTransaction(TransactionType.NETTING, 900, 11),
        )
        assertEquals(455, BalanceCalculator.calculate(0, transactions, now = 20))
    }

    @Test
    fun handlesLargeAmountsAndNegativeWalletBalance() {
        val transactions = listOf(BalanceTransaction(TransactionType.EXPENSE, Long.MAX_VALUE, 1))
        assertEquals(-Long.MAX_VALUE, BalanceCalculator.calculate(0, transactions, now = 1))
    }
}
