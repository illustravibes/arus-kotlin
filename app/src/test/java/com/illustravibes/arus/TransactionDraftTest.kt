package com.illustravibes.arus

import com.illustravibes.arus.domain.TransactionDraft
import com.illustravibes.arus.domain.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionDraftTest {
    @Test
    fun rejectsZeroAmount() {
        val error = runCatching { draft(amount = 0) }.exceptionOrNull()
        assertEquals("Nominal transaksi harus positif.", error?.message)
    }

    @Test
    fun allowsNegativeBalanceForConfirmationOnNonCreditWallet() {
        val draft = draft(type = TransactionType.EXPENSE, amount = 150, balance = 100)
        assertEquals(-50, draft.resultingBalanceRupiah)
        assertTrue(draft.requiresNegativeBalanceWarning)
    }

    @Test
    fun doesNotWarnForCreditOrFutureExpense() {
        assertFalse(draft(walletType = "CREDIT", amount = 150, balance = 100).requiresNegativeBalanceWarning)
        assertFalse(draft(amount = 150, balance = 100, occurred = 200, now = 100).requiresNegativeBalanceWarning)
    }

    @Test
    fun futureTransactionIsScheduledAndLeavesCurrentBalanceUnchanged() {
        val transaction = draft(amount = 700, balance = 1_000, occurred = 200, now = 100)
        assertTrue(transaction.isScheduled)
        assertEquals(1_000, transaction.currentBalanceRupiah)
        assertEquals(1_000, transaction.balanceAfterEffectiveTransactionRupiah)
    }

    private fun draft(
        type: TransactionType = TransactionType.EXPENSE,
        amount: Long = 50,
        balance: Long = 100,
        walletType: String = "CASH",
        occurred: Long = 100,
        now: Long = 100,
    ) = TransactionDraft(type, amount, walletType, balance, occurred, now, 1L, walletId = 1L)
}
