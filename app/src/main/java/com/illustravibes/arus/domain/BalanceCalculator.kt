package com.illustravibes.arus.domain

data class BalanceTransaction(
    val type: TransactionType,
    val amountRupiah: Long,
    val occurredAtEpochMillis: Long,
    val deletedAtEpochMillis: Long? = null,
    val direction: AdjustmentDirection? = null,
)

enum class AdjustmentDirection {
    PLUS,
    MINUS,
}

object BalanceCalculator {
    fun calculate(initialBalanceRupiah: Long, transactions: Iterable<BalanceTransaction>, now: Long): Long =
        transactions.asSequence()
            .filter { it.deletedAtEpochMillis == null && it.occurredAtEpochMillis <= now }
            .fold(initialBalanceRupiah) { balance, transaction ->
                Math.addExact(balance, effect(transaction))
            }

    private fun effect(transaction: BalanceTransaction): Long {
        val amount = transaction.amountRupiah
        require(amount > 0) { "Nominal transaksi harus positif." }
        val multiplier = when (transaction.type) {
            TransactionType.INCOME,
            TransactionType.TRANSFER_IN,
            TransactionType.REPAYMENT_IN,
            TransactionType.LOAN_IN -> 1
            TransactionType.ADJUSTMENT -> when (transaction.direction) {
                AdjustmentDirection.PLUS -> 1
                AdjustmentDirection.MINUS -> -1
                null -> throw IllegalArgumentException("Penyesuaian saldo harus memiliki arah.")
            }
            TransactionType.EXPENSE,
            TransactionType.TRANSFER_OUT,
            TransactionType.REFUND_OUT,
            TransactionType.LOAN_OUT,
            TransactionType.DEBT_PAY_OUT -> -1
            TransactionType.WRITE_OFF,
            TransactionType.NETTING -> 0
        }
        return if (multiplier < 0) Math.negateExact(amount) else if (multiplier > 0) amount else 0
    }
}

data class TransactionDraft(
    val type: TransactionType,
    val amountRupiah: Long,
    val walletType: String,
    val currentBalanceRupiah: Long,
    val occurredAtEpochMillis: Long,
    val nowEpochMillis: Long,
    val categoryId: Long?,
    val note: String? = null,
    val merchant: String? = null,
    val walletId: Long,
) {
    init {
        require(type == TransactionType.INCOME || type == TransactionType.EXPENSE) {
            "Transaksi biasa hanya mendukung pemasukan atau pengeluaran."
        }
        require(amountRupiah > 0) { "Nominal transaksi harus positif." }
        require(walletType in setOf("BANK", "EWALLET", "CASH", "CREDIT")) {
            "Tipe dompet tidak valid."
        }
        require(walletId > 0) { "Dompet transaksi wajib dipilih." }
        require(categoryId != null) { "Kategori transaksi wajib dipilih." }
    }

    val isScheduled: Boolean get() = occurredAtEpochMillis > nowEpochMillis
    val resultingBalanceRupiah: Long get() = when (type) {
        TransactionType.INCOME -> Math.addExact(currentBalanceRupiah, amountRupiah)
        TransactionType.EXPENSE -> Math.subtractExact(currentBalanceRupiah, amountRupiah)
        else -> error("Jenis transaksi tidak didukung.")
    }
    val requiresNegativeBalanceWarning: Boolean
        get() = type == TransactionType.EXPENSE && walletType != "CREDIT" &&
            !isScheduled && resultingBalanceRupiah < 0

    val balanceAfterEffectiveTransactionRupiah: Long
        get() = if (isScheduled) currentBalanceRupiah else resultingBalanceRupiah
}
