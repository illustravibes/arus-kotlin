package com.illustravibes.arus.data;

public final class ReconciliationItemInput {
    public final long transactionId;
    public final boolean matched;

    public ReconciliationItemInput(long transactionId, boolean matched) {
        this.transactionId = transactionId;
        this.matched = matched;
    }
}
