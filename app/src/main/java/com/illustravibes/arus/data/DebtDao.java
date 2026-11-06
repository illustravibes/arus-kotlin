package com.illustravibes.arus.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

@Dao
public abstract class DebtDao {
    @Insert
    public abstract long insertContact(ContactEntity contact);

    @Insert
    public abstract long insertDebt(DebtEntity debt);

    @Insert
    protected abstract long insertOriginTransaction(TransactionEntity transaction);

    @Insert
    protected abstract long insertDebtTransaction(TransactionEntity transaction);

    @Insert
    protected abstract long insertAllocation(RepaymentAllocationEntity allocation);

    @Query("SELECT COALESCE(SUM(amountRupiah), 0) FROM repayment_allocations WHERE debtId = :debtId")
    public abstract long allocatedAmount(long debtId);

    @Query("SELECT COALESCE(SUM(amountRupiah), 0) FROM repayment_allocations WHERE transactionId = :transactionId")
    public abstract long allocatedFromTransaction(long transactionId);

    @Query("SELECT amountRupiah FROM transactions WHERE id = :transactionId AND type IN ('REPAYMENT_IN','DEBT_PAY_OUT','WRITE_OFF','NETTING') AND deletedAtEpochMillis IS NULL")
    protected abstract Long eligibleTransactionAmount(long transactionId);

    @Query("SELECT principalRupiah FROM debts WHERE id = :debtId AND deletedAtEpochMillis IS NULL")
    protected abstract Long activeDebtPrincipal(long debtId);

    @Query("SELECT CASE WHEN t.type = 'REPAYMENT_IN' AND d.direction = 'LENT' THEN 1 WHEN t.type = 'DEBT_PAY_OUT' AND d.direction = 'BORROWED' THEN 1 WHEN t.type IN ('WRITE_OFF','NETTING') THEN 1 ELSE 0 END FROM transactions t, debts d WHERE t.id = :transactionId AND d.id = :debtId")
    protected abstract int allocationDirectionMatches(long transactionId, long debtId);

    @Query("SELECT COALESCE(SUM(amountRupiah), 0) FROM repayment_allocations WHERE debtId = :debtId")
    protected abstract long allocatedToDebt(long debtId);

    @Query("SELECT principalRupiah - COALESCE((SELECT SUM(amountRupiah) FROM repayment_allocations WHERE debtId = :debtId), 0) FROM debts WHERE id = :debtId AND deletedAtEpochMillis IS NULL")
    public abstract long remainingPrincipal(long debtId);

    @Query("SELECT COALESCE(SUM(principalRupiah - COALESCE((SELECT SUM(amountRupiah) FROM repayment_allocations WHERE debtId = debts.id), 0)), 0) FROM debts WHERE direction = 'BORROWED' AND status IN ('OPEN','PARTIAL') AND deletedAtEpochMillis IS NULL")
    public abstract long borrowedOutstandingTotal();

    @Query("SELECT id FROM categories WHERE name = :name AND kind = :kind AND isArchived = 0 LIMIT 1")
    protected abstract Long activeCategory(String name, String kind);

    @Query("SELECT id FROM categories WHERE id = :categoryId AND kind = :kind AND isArchived = 0 LIMIT 1")
    protected abstract Long activeCategoryById(long categoryId, String kind);

    @Query("INSERT INTO transaction_splits (transactionId, amountRupiah, categoryId, countsInReport, countsInBudget) VALUES (:transactionId, :amount, :categoryId, 1, 1)")
    protected abstract void insertDebtSplit(long transactionId, long amount, Long categoryId);

    @Query("UPDATE debts SET status = CASE WHEN (SELECT COALESCE(SUM(amountRupiah), 0) FROM repayment_allocations WHERE debtId = :debtId) >= principalRupiah THEN 'PAID' WHEN (SELECT COALESCE(SUM(amountRupiah), 0) FROM repayment_allocations WHERE debtId = :debtId) > 0 THEN 'PARTIAL' ELSE 'OPEN' END, updatedAtEpochMillis = :updatedAt WHERE id = :debtId")
    protected abstract int refreshDebtStatus(long debtId, long updatedAt);

    @Query("UPDATE debts SET status = 'WRITTEN_OFF', updatedAtEpochMillis = :updatedAt WHERE id = :debtId")
    protected abstract int markDebtWrittenOff(long debtId, long updatedAt);

    @Query("UPDATE group_orders SET status = 'SETTLED', updatedAtEpochMillis = :updatedAt WHERE id = :groupOrderId AND status = 'FINALIZED' AND NOT EXISTS (SELECT 1 FROM debts WHERE groupOrderId = :groupOrderId AND deletedAtEpochMillis IS NULL AND status NOT IN ('PAID','WRITTEN_OFF'))")
    protected abstract int markGroupOrderSettledIfComplete(long groupOrderId, long updatedAt);

    @Query("SELECT * FROM contacts WHERE isArchived = 0 ORDER BY name COLLATE NOCASE")
    public abstract List<ContactEntity> activeContacts();

    @Query("SELECT * FROM contacts WHERE id = :contactId")
    protected abstract ContactEntity contactById(long contactId);

    @Query("SELECT * FROM debts WHERE contactId = :contactId AND deletedAtEpochMillis IS NULL ORDER BY createdAtEpochMillis DESC")
    public abstract List<DebtEntity> debtsForContact(long contactId);

    @Query("SELECT * FROM debts WHERE deletedAtEpochMillis IS NULL ORDER BY createdAtEpochMillis DESC, id DESC")
    public abstract List<DebtEntity> activeDebts();

    @Query("SELECT * FROM wallets WHERE id = :walletId AND deletedAtEpochMillis IS NULL AND isArchived = 0")
    protected abstract WalletEntity activeWallet(long walletId);

    @Transaction
    public long createDebt(long contactId, String direction, long principalRupiah, Long walletId,
                           long occurredAtEpochMillis, Long dueDateEpochMillis, String note,
                           long createdAtEpochMillis) {
        ContactEntity contact = contactById(contactId);
        if (contact == null || contact.isArchived) throw new IllegalArgumentException("Kontak aktif tidak ditemukan.");
        if (principalRupiah <= 0) throw new IllegalArgumentException("Pokok utang harus positif.");
        if (!"LENT".equals(direction) && !"BORROWED".equals(direction))
            throw new IllegalArgumentException("Arah utang tidak valid.");
        Long originTransactionId = null;
        if (walletId != null) {
            WalletEntity wallet = activeWallet(walletId);
            if (wallet == null) throw new IllegalArgumentException("Dompet aktif tidak ditemukan.");
            TransactionEntity transaction = new TransactionEntity(
                    "LENT".equals(direction) ? com.illustravibes.arus.domain.TransactionType.LOAN_OUT
                            : com.illustravibes.arus.domain.TransactionType.LOAN_IN,
                    walletId, principalRupiah, null, occurredAtEpochMillis, null);
            originTransactionId = insertOriginTransaction(transaction);
        }
        DebtEntity debt = new DebtEntity(contactId, direction, "MANUAL", originTransactionId,
                principalRupiah, "OPEN", dueDateEpochMillis, note, createdAtEpochMillis,
                createdAtEpochMillis, null);
        return insertDebt(debt);
    }

    @Transaction
    public long insertAllocationValidated(RepaymentAllocationEntity allocation) {
        Long transactionAmount = eligibleTransactionAmount(allocation.transactionId);
        Long principal = activeDebtPrincipal(allocation.debtId);
        if (transactionAmount == null) throw new IllegalArgumentException("Transaksi tidak valid untuk alokasi pelunasan.");
        if (principal == null) throw new IllegalArgumentException("Catatan utang tidak ditemukan.");
        DebtEntity debt = findActiveDebt(allocation.debtId);
        if (debt == null || contactById(debt.contactId) == null)
            throw new IllegalArgumentException("Kontak catatan utang tidak ditemukan.");
        if (allocationDirectionMatches(allocation.transactionId, allocation.debtId) != 1)
            throw new IllegalArgumentException("Arah transaksi tidak cocok dengan arah utang.");
        if (allocation.amountRupiah <= 0) throw new IllegalArgumentException("Nominal alokasi harus positif.");
        if (allocatedFromTransaction(allocation.transactionId) > transactionAmount - allocation.amountRupiah)
            throw new IllegalArgumentException("Total alokasi melebihi nominal transaksi.");
        if (allocatedToDebt(allocation.debtId) > principal - allocation.amountRupiah)
            throw new IllegalArgumentException("Total alokasi melebihi sisa pokok utang.");
        long id = insertAllocation(allocation);
        long now = System.currentTimeMillis();
        refreshDebtStatus(allocation.debtId, now);
        refreshGroupOrderStatus(allocation.debtId, now);
        return id;
    }

    @Query("SELECT * FROM debts WHERE id = :debtId AND deletedAtEpochMillis IS NULL")
    public abstract DebtEntity findActiveDebt(long debtId);

    public static final class AllocationRequest {
        public final long debtId;
        public final long amountRupiah;

        public AllocationRequest(long debtId, long amountRupiah) {
            this.debtId = debtId;
            this.amountRupiah = amountRupiah;
        }
    }

    @Transaction
    public long recordSettlement(long contactId, String direction, long walletId, long tenderedAmountRupiah,
                                List<AllocationRequest> allocations, String excessResolution,
                                Long excessCategoryId, long occurredAtEpochMillis, String note,
                                long createdAtEpochMillis) {
        ContactEntity contact = contactById(contactId);
        if (contact == null || contact.isArchived) throw new IllegalArgumentException("Kontak aktif tidak ditemukan.");
        if (!"LENT".equals(direction) && !"BORROWED".equals(direction))
            throw new IllegalArgumentException("Arah utang tidak valid.");
        if (activeWallet(walletId) == null) throw new IllegalArgumentException("Dompet aktif tidak ditemukan.");
        if (tenderedAmountRupiah <= 0 || allocations == null || allocations.isEmpty())
            throw new IllegalArgumentException("Nominal dan alokasi pelunasan wajib positif.");

        long allocatedAmount = 0;
        for (AllocationRequest allocation : allocations) {
            if (allocation == null || allocation.amountRupiah <= 0)
                throw new IllegalArgumentException("Nominal alokasi harus positif.");
            DebtEntity debt = findActiveDebt(allocation.debtId);
            if (debt == null || debt.contactId != contactId || !direction.equals(debt.direction))
                throw new IllegalArgumentException("Semua alokasi harus mengarah ke utang kontak dan arah yang sama.");
            if (allocation.amountRupiah > remainingPrincipal(allocation.debtId))
                throw new IllegalArgumentException("Nominal alokasi melebihi sisa pokok utang.");
            allocatedAmount = Math.addExact(allocatedAmount, allocation.amountRupiah);
        }
        if (allocatedAmount > tenderedAmountRupiah)
            throw new IllegalArgumentException("Total alokasi melebihi nominal pembayaran.");

        long excess = tenderedAmountRupiah - allocatedAmount;
        if (excess > 0) validateExcessResolution(direction, excessResolution, excessCategoryId);
        com.illustravibes.arus.domain.TransactionType repaymentType = "LENT".equals(direction)
                ? com.illustravibes.arus.domain.TransactionType.REPAYMENT_IN
                : com.illustravibes.arus.domain.TransactionType.DEBT_PAY_OUT;
        long repaymentId = insertDebtTransaction(new TransactionEntity(repaymentType, walletId,
                allocatedAmount, null, occurredAtEpochMillis, null));
        for (AllocationRequest allocation : allocations) {
            insertAllocationValidated(new RepaymentAllocationEntity(repaymentId, allocation.debtId, allocation.amountRupiah));
        }
        if (excess > 0) recordExcess(contactId, direction, walletId, excess, excessResolution,
                excessCategoryId, occurredAtEpochMillis, note, createdAtEpochMillis);
        return repaymentId;
    }

    private void validateExcessResolution(String direction, String resolution, Long categoryId) {
        if ("LENT".equals(direction)) {
            if ("REFUND".equals(resolution)) return;
            if ("FORGIVE".equals(resolution) && categoryId != null && activeCategoryById(categoryId, "INCOME") != null) return;
        } else {
            if ("CREATE_RECEIVABLE".equals(resolution)) return;
            if ("FORGIVE".equals(resolution) && categoryId != null && activeCategoryById(categoryId, "EXPENSE") != null) return;
        }
        throw new IllegalArgumentException("Resolusi kelebihan pelunasan tidak valid.");
    }

    private void recordExcess(long contactId, String direction, long walletId, long amount,
                              String resolution, Long categoryId, long occurredAtEpochMillis,
                              String note, long createdAtEpochMillis) {
        if ("REFUND".equals(resolution)) {
            insertDebtTransaction(new TransactionEntity(com.illustravibes.arus.domain.TransactionType.REFUND_OUT,
                    walletId, amount, null, occurredAtEpochMillis, null));
            return;
        }
        if ("CREATE_RECEIVABLE".equals(resolution)) {
            createDebt(contactId, "LENT", amount, walletId, occurredAtEpochMillis, null, note, createdAtEpochMillis);
            return;
        }
        boolean income = "LENT".equals(direction);
        com.illustravibes.arus.domain.TransactionType type = income
                ? com.illustravibes.arus.domain.TransactionType.INCOME
                : com.illustravibes.arus.domain.TransactionType.EXPENSE;
        long transactionId = insertDebtTransaction(new TransactionEntity(type, walletId, amount, null,
                categoryId, occurredAtEpochMillis, note, null, null));
        insertDebtSplit(transactionId, amount, categoryId);
    }

    @Transaction
    public long netDebts(long firstDebtId, long secondDebtId, long occurredAtEpochMillis) {
        DebtEntity first = findActiveDebt(firstDebtId);
        DebtEntity second = findActiveDebt(secondDebtId);
        if (first == null || second == null) throw new IllegalArgumentException("Catatan utang tidak ditemukan.");
        if (first.id == second.id || first.contactId != second.contactId || first.direction.equals(second.direction))
            throw new IllegalArgumentException("Netting hanya boleh untuk dua utang berlawanan arah pada kontak yang sama.");
        long amount = Math.min(remainingPrincipal(firstDebtId), remainingPrincipal(secondDebtId));
        if (amount <= 0) throw new IllegalArgumentException("Tidak ada sisa utang untuk dinetting.");
        long transactionId = insertDebtTransaction(new TransactionEntity(
                com.illustravibes.arus.domain.TransactionType.NETTING, null, amount, null,
                occurredAtEpochMillis, null));
        insertAllocation(new RepaymentAllocationEntity(transactionId, firstDebtId, amount));
        insertAllocation(new RepaymentAllocationEntity(transactionId, secondDebtId, amount));
        long now = System.currentTimeMillis();
        refreshDebtStatus(firstDebtId, now);
        refreshDebtStatus(secondDebtId, now);
        refreshGroupOrderStatus(firstDebtId, now);
        refreshGroupOrderStatus(secondDebtId, now);
        return transactionId;
    }

    @Transaction
    public long writeOff(long debtId, long occurredAtEpochMillis, String note) {
        DebtEntity debt = findActiveDebt(debtId);
        if (debt == null) throw new IllegalArgumentException("Catatan utang tidak ditemukan.");
        long remaining = remainingPrincipal(debtId);
        if (remaining <= 0) throw new IllegalArgumentException("Tidak ada sisa utang untuk diikhlaskan.");
        String categoryName = "LENT".equals(debt.direction) ? "Ikhlas" : "Utang Dihapuskan";
        String categoryKind = "LENT".equals(debt.direction) ? "EXPENSE" : "INCOME";
        Long categoryId = activeCategory(categoryName, categoryKind);
        if (categoryId == null) throw new IllegalArgumentException("Kategori sistem untuk penghapusan utang tidak tersedia.");
        long transactionId = insertDebtTransaction(new TransactionEntity(
                com.illustravibes.arus.domain.TransactionType.WRITE_OFF, null, remaining, null,
                categoryId, occurredAtEpochMillis, note, null, null));
        insertDebtSplit(transactionId, remaining, categoryId);
        insertAllocation(new RepaymentAllocationEntity(transactionId, debtId, remaining));
        markDebtWrittenOff(debtId, System.currentTimeMillis());
        refreshGroupOrderStatus(debtId, System.currentTimeMillis());
        return transactionId;
    }

    private void refreshGroupOrderStatus(long debtId, long now) {
        DebtEntity debt = findActiveDebt(debtId);
        if (debt != null && debt.groupOrderId != null) markGroupOrderSettledIfComplete(debt.groupOrderId, now);
    }
}
