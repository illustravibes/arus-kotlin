package com.illustravibes.arus.domain

enum class TransactionType {
    INCOME,
    EXPENSE,
    TRANSFER_OUT,
    TRANSFER_IN,
    REPAYMENT_IN,
    REFUND_OUT,
    LOAN_OUT,
    LOAN_IN,
    DEBT_PAY_OUT,
    WRITE_OFF,
    NETTING,
    ADJUSTMENT,
}

data class Money(val rupiah: Long) {
    init {
        require(rupiah >= 0) { "Nominal harus nol atau positif." }
    }
}
