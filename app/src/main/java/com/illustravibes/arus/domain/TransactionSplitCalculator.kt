package com.illustravibes.arus.domain

import java.math.BigInteger

internal object TransactionSplitCalculator {
    fun amountsFromNominal(totalRupiah: Long, amounts: List<Long>): List<Long>? {
        if (totalRupiah <= 0L || amounts.size < 2 || amounts.any { it <= 0L }) return null
        val sum = amounts.fold(0L) { total, amount ->
            runCatching { Math.addExact(total, amount) }.getOrNull() ?: return null
        }
        return amounts.takeIf { sum == totalRupiah }
    }

    fun amountsFromPercent(totalRupiah: Long, percentages: List<Long>): List<Long>? {
        if (totalRupiah <= 0L || percentages.size < 2 || percentages.any { it <= 0L }) return null
        val percentTotal = percentages.fold(0L) { total, percent ->
            runCatching { Math.addExact(total, percent) }.getOrNull() ?: return null
        }
        if (percentTotal != 100L) return null
        val whole = BigInteger.valueOf(totalRupiah)
        val amounts = percentages.map { percent ->
            whole.multiply(BigInteger.valueOf(percent)).divide(BigInteger.valueOf(100L)).toLong()
        }.toMutableList()
        val assigned = amounts.dropLast(1).sum()
        amounts[amounts.lastIndex] = Math.subtractExact(totalRupiah, assigned)
        return amounts.takeIf { it.all { amount -> amount > 0L } }
    }

    fun percentagesFromAmounts(totalRupiah: Long, amounts: List<Long>): List<Long>? {
        if (totalRupiah <= 0L || amounts.size < 2 || amounts.any { it <= 0L }) return null
        val balanced = amountsFromNominal(totalRupiah, amounts) ?: return null
        val whole = BigInteger.valueOf(totalRupiah)
        val percentages = balanced.dropLast(1).map { amount ->
            val numerator = BigInteger.valueOf(amount).multiply(BigInteger.valueOf(100L))
            val division = numerator.divideAndRemainder(whole)
            val rounded = if (division[1].multiply(BigInteger.valueOf(2L)) >= whole)
                division[0].add(BigInteger.ONE) else division[0]
            rounded.toLong()
        }.toMutableList()
        val assigned = percentages.sum()
        percentages += 100L - assigned
        return percentages.takeIf { it.all { percent -> percent > 0L } }
    }
}
