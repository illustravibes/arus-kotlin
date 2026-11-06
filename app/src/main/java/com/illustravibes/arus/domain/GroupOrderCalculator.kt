package com.illustravibes.arus.domain

import java.math.BigInteger
enum class GroupFeeSplitMode { EQUAL, PROPORTIONAL, MANUAL }

data class GroupOrderItemInput(
    val amountRupiah: Long,
    val ownerWeights: Map<Long?, Long>,
)

data class GroupOrderFeeInput(
    val amountRupiah: Long,
    val mode: GroupFeeSplitMode,
    val manualShares: Map<Long?, Long> = emptyMap(),
)

data class GroupOrderParticipantShare(
    val itemsAmountRupiah: Long,
    val feeAmountRupiah: Long,
    val totalAmountRupiah: Long,
)

data class GroupOrderCalculation(
    val participants: Map<Long?, GroupOrderParticipantShare>,
    val subtotalRupiah: Long,
    val sharedFeeTotalRupiah: Long,
    val discountTotalRupiah: Long,
    val grandTotalRupiah: Long,
)

object GroupOrderCalculator {
    fun calculate(
        participantIds: List<Long?>,
        items: List<GroupOrderItemInput>,
        fees: List<GroupOrderFeeInput>,
        roundingBearer: Long? = null,
    ): GroupOrderCalculation {
        require(participantIds.isNotEmpty() && participantIds.distinct().size == participantIds.size) {
            "Peserta group order tidak valid."
        }
        require(participantIds.contains(null)) { "Peserta Aku wajib ada." }
        require(roundingBearer in participantIds) { "Penanggung pembulatan harus menjadi peserta." }

        val itemShares = participantIds.associateWith { 0L }.toMutableMap()
        items.forEach { item ->
            require(item.amountRupiah > 0L) { "Nominal item harus positif." }
            require(item.ownerWeights.isNotEmpty() && item.ownerWeights.keys.all(participantIds::contains)) {
                "Setiap item harus memiliki pemilik yang terdaftar."
            }
            require(item.ownerWeights.values.all { it > 0L }) { "Bobot pemilik item harus positif." }
            val split = split(item.amountRupiah, item.ownerWeights, roundingBearer, false)
            split.forEach { (id, amount) -> itemShares[id] = Math.addExact(itemShares.getValue(id), amount) }
        }

        val feeShares = participantIds.associateWith { 0L }.toMutableMap()
        fees.forEach { fee ->
            val split = when (fee.mode) {
                GroupFeeSplitMode.EQUAL -> split(fee.amountRupiah, participantIds.associateWith { 1L }, roundingBearer, true)
                GroupFeeSplitMode.PROPORTIONAL -> {
                    val weights = itemShares.filterValues { it > 0L }
                        .takeIf { it.isNotEmpty() }
                        ?: participantIds.associateWith { 1L }
                    val allocated = split(fee.amountRupiah, weights, roundingBearer, true)
                    participantIds.associateWith { allocated[it] ?: 0L }
                }
                GroupFeeSplitMode.MANUAL -> {
                    require(fee.manualShares.keys == participantIds.toSet()) {
                        "Input manual harus memiliki nominal untuk setiap peserta."
                    }
                    require(fee.manualShares.values.fold(0L, Math::addExact) == fee.amountRupiah) {
                        "Total input manual harus sama dengan nominal biaya."
                    }
                    fee.manualShares
                }
            }
            split.forEach { (id, amount) -> feeShares[id] = Math.addExact(feeShares.getValue(id), amount) }
        }

        val shares = participantIds.associateWith { id ->
            val itemAmount = itemShares.getValue(id)
            val feeAmount = feeShares.getValue(id)
            GroupOrderParticipantShare(itemAmount, feeAmount, Math.addExact(itemAmount, feeAmount))
        }
        val subtotal = items.fold(0L) { total, item -> Math.addExact(total, item.amountRupiah) }
        val feesTotal = fees.filter { it.amountRupiah >= 0L }
            .fold(0L) { total, fee -> Math.addExact(total, fee.amountRupiah) }
        val discountTotal = fees.filter { it.amountRupiah < 0L }
            .fold(0L) { total, fee -> Math.addExact(total, fee.amountRupiah) }
        val grandTotal = Math.addExact(Math.addExact(subtotal, feesTotal), discountTotal)
        require(grandTotal >= 0L) { "Total group order tidak boleh negatif." }
        require(shares.values.fold(0L) { total, share -> Math.addExact(total, share.totalAmountRupiah) } == grandTotal) {
            "Total tagihan peserta harus sama dengan total yang dibayar."
        }
        return GroupOrderCalculation(shares, subtotal, feesTotal, discountTotal, grandTotal)
    }

    private fun split(amount: Long, weights: Map<Long?, Long>, roundingBearer: Long?,
                      allowExternalRoundingBearer: Boolean): Map<Long?, Long> {
        require(weights.isNotEmpty() && weights.values.all { it > 0L }) { "Bobot pembagian tidak valid." }
        require(amount != Long.MIN_VALUE) { "Nominal group order terlalu besar." }
        val sign = if (amount < 0L) -1L else 1L
        val magnitude = kotlin.math.abs(amount)
        val totalWeight = weights.values.fold(0L, Math::addExact)
        val totalWeightValue = BigInteger.valueOf(totalWeight)
        val shares = weights.mapValues { (_, weight) ->
            BigInteger.valueOf(magnitude).multiply(BigInteger.valueOf(weight))
                .divide(totalWeightValue).toLong()
        }.toMutableMap()
        val assigned = shares.values.fold(0L, Math::addExact)
        val remainder = magnitude - assigned
        if (remainder > 0L) {
            val bearer = roundingBearer?.takeIf(weights::containsKey)
                ?: if (allowExternalRoundingBearer) roundingBearer else weights.keys.first()
            shares[bearer] = Math.addExact(shares.getValue(bearer), remainder)
        }
        return shares.mapValues { (_, value) -> Math.multiplyExact(value, sign) }
    }
}
