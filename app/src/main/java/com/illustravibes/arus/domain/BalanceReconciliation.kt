package com.illustravibes.arus.domain

data class BalanceReconciliation(
    val recordedBalanceRupiah: Long,
    val actualBalanceRupiah: Long,
) {
    init {
        require(actualBalanceRupiah >= 0) { "Saldo aktual tidak boleh negatif." }
    }

    val differenceRupiah: Long
        get() = Math.subtractExact(actualBalanceRupiah, recordedBalanceRupiah)
}
