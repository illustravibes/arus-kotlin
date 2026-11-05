package com.illustravibes.arus

import com.illustravibes.arus.data.TransactionTypeConverter
import com.illustravibes.arus.data.TransactionEntity
import com.illustravibes.arus.domain.Money
import com.illustravibes.arus.domain.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class TransactionTypeConverterTest {
    private val converter = TransactionTypeConverter()

    @Test
    fun transactionTypeRoundTripsByName() {
        TransactionType.entries.forEach { type ->
            assertEquals(type.name, converter.fromTransactionType(type))
            assertEquals(type, converter.toTransactionType(type.name))
        }
    }

    @Test
    fun moneyUsesNonNegativeIntegerRupiah() {
        assertEquals(Long.MAX_VALUE, Money(Long.MAX_VALUE).rupiah)
        assertThrows(IllegalArgumentException::class.java) { Money(-1) }
    }

    @Test
    fun transactionTypeConverterRejectsUnknownValue() {
        assertThrows(IllegalArgumentException::class.java) { converter.toTransactionType("UNKNOWN") }
    }

    @Test
    fun transactionRejectsZeroAndNegativeAmounts() {
        assertThrows(IllegalArgumentException::class.java) {
            TransactionEntity(TransactionType.EXPENSE, 1L, 0, null, 1, null)
        }
        assertThrows(IllegalArgumentException::class.java) {
            TransactionEntity(TransactionType.EXPENSE, 1L, -100, null, 1, null)
        }
    }
}
