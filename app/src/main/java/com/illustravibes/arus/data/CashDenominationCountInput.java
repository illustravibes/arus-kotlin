package com.illustravibes.arus.data;

public final class CashDenominationCountInput {
    public final long denominationRupiah;
    public final long pieceCount;

    public CashDenominationCountInput(long denominationRupiah, long pieceCount) {
        this.denominationRupiah = denominationRupiah;
        this.pieceCount = pieceCount;
    }
}
