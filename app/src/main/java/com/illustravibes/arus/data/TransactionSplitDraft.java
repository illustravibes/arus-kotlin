package com.illustravibes.arus.data;

public final class TransactionSplitDraft {
    public final long amountRupiah;
    public final long categoryId;
    public final boolean countsInReport;
    public final boolean countsInBudget;

    public TransactionSplitDraft(long amountRupiah, long categoryId,
                                 boolean countsInReport, boolean countsInBudget) {
        this.amountRupiah = amountRupiah;
        this.categoryId = categoryId;
        this.countsInReport = countsInReport;
        this.countsInBudget = countsInBudget;
    }
}
