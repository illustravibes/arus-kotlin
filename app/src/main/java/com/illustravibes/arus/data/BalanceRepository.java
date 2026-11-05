package com.illustravibes.arus.data;

public final class BalanceRepository {
    private final ArusDatabase database;

    public BalanceRepository(ArusDatabase database) {
        this.database = database;
    }

    public Long balance(long walletId, long now) {
        Long initial = database.walletDao().initialBalance(walletId);
        if (initial == null) return null;
        return Math.addExact(initial, database.transactionDao().transactionEffect(walletId, now));
    }

    public Long creditUsed(long walletId, long now) {
        WalletEntity wallet = database.walletDao().findById(walletId);
        if (wallet == null || !"CREDIT".equals(wallet.type)) return null;
        return Math.subtractExact(wallet.creditLimitRupiah == null ? 0L : wallet.creditLimitRupiah,
                database.transactionDao().transferInTotal(walletId, now));
    }

    public FinancialSummary financialSummary(long now) {
        long assets = 0L;
        long liabilities = 0L;
        for (WalletEntity wallet : database.walletDao().allWallets()) {
            if (wallet.deletedAtEpochMillis != null) continue;
            if ("CREDIT".equals(wallet.type)) {
                Long used = creditUsed(wallet.id, now);
                if (used != null) liabilities = Math.addExact(liabilities, used);
            } else if (wallet.includeInTotal) {
                Long walletBalance = balance(wallet.id, now);
                if (walletBalance != null) assets = Math.addExact(assets, walletBalance);
            }
        }
        return new FinancialSummary(assets, liabilities, Math.subtractExact(assets, liabilities));
    }

    public static final class FinancialSummary {
        public final long assetsRupiah;
        public final long creditLiabilitiesRupiah;
        public final long netWorthRupiah;

        public FinancialSummary(long assetsRupiah, long creditLiabilitiesRupiah, long netWorthRupiah) {
            this.assetsRupiah = assetsRupiah;
            this.creditLiabilitiesRupiah = creditLiabilitiesRupiah;
            this.netWorthRupiah = netWorthRupiah;
        }
    }
}
