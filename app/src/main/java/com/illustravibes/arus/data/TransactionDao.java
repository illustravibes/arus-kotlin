package com.illustravibes.arus.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Insert;
import androidx.paging.PagingSource;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Collections;

@Dao
public interface TransactionDao {
    @Insert
    long insert(TransactionEntity transaction);

    @Insert
    long insertCheckpoint(ReconciliationCheckpointEntity checkpoint);

    @Query("UPDATE transactions SET verifiedCheckpointId = :checkpointId WHERE walletId = :walletId AND occurredAtEpochMillis <= :occurredAt AND deletedAtEpochMillis IS NULL")
    int markTransactionsVerified(long walletId, long occurredAt, long checkpointId);

    @Query("UPDATE reconciliation_checkpoints SET needsRecheck = 1 WHERE id = (SELECT verifiedCheckpointId FROM transactions WHERE id = :transactionId AND verifiedCheckpointId IS NOT NULL)")
    int markCheckpointForTransactionRecheck(long transactionId);

    @Query("UPDATE reconciliation_checkpoints SET needsRecheck = 1 WHERE id IN (SELECT verifiedCheckpointId FROM transactions WHERE id = :transactionId AND verifiedCheckpointId IS NOT NULL)")
    int markCheckpointsForTransactionRecheck(long transactionId);

    @Query("SELECT * FROM transactions WHERE deletedAtEpochMillis IS NULL ORDER BY occurredAtEpochMillis, id")
    List<TransactionEntity> activeTransactions();

    @Query("SELECT * FROM transactions WHERE deletedAtEpochMillis IS NULL ORDER BY occurredAtEpochMillis DESC, id DESC")
    PagingSource<Integer, TransactionEntity> transactionHistory();

    @Query("SELECT * FROM transactions WHERE deletedAtEpochMillis IS NULL " +
            "AND (:query = '' OR COALESCE(note, '') LIKE '%' || :query || '%' OR COALESCE(merchant, '') LIKE '%' || :query || '%') " +
            "AND (:walletId IS NULL OR walletId = :walletId) " +
            "AND (:type IS NULL OR type = :type) " +
            "AND (:categoryId IS NULL OR categoryId = :categoryId) " +
            "AND (:minimumAmount IS NULL OR amountRupiah >= :minimumAmount) " +
            "AND (:maximumAmount IS NULL OR amountRupiah <= :maximumAmount) " +
            "AND (:startMillis IS NULL OR occurredAtEpochMillis >= :startMillis) " +
            "AND (:endExclusiveMillis IS NULL OR occurredAtEpochMillis < :endExclusiveMillis) " +
            "ORDER BY occurredAtEpochMillis DESC, id DESC")
    PagingSource<Integer, TransactionEntity> searchTransactionHistory(String query, Long walletId, String type,
            Long categoryId, Long minimumAmount, Long maximumAmount, Long startMillis, Long endExclusiveMillis);

    @Query("SELECT * FROM transactions WHERE deletedAtEpochMillis IS NULL " +
            "AND (:query = '' OR COALESCE(note, '') LIKE '%' || :query || '%' OR COALESCE(merchant, '') LIKE '%' || :query || '%') " +
            "AND (:walletId IS NULL OR walletId = :walletId) AND (:type IS NULL OR type = :type) " +
            "AND (:categoryId IS NULL OR categoryId = :categoryId) " +
            "AND (:minimumAmount IS NULL OR amountRupiah >= :minimumAmount) " +
            "AND (:maximumAmount IS NULL OR amountRupiah <= :maximumAmount) " +
            "AND (:startMillis IS NULL OR occurredAtEpochMillis >= :startMillis) " +
            "AND (:endExclusiveMillis IS NULL OR occurredAtEpochMillis < :endExclusiveMillis) " +
            "ORDER BY occurredAtEpochMillis DESC, id DESC")
    List<TransactionEntity> filteredTransactions(String query, Long walletId, String type, Long categoryId,
            Long minimumAmount, Long maximumAmount, Long startMillis, Long endExclusiveMillis);

    @Query("SELECT t.id AS id, t.occurredAtEpochMillis AS occurredAtEpochMillis, t.type AS type, " +
            "t.amountRupiah AS amountRupiah, w.name AS walletName, c.name AS categoryName, " +
            "t.merchant AS merchant, t.note AS note, " +
            "CASE WHEN t.verifiedCheckpointId IS NULL THEN 0 ELSE 1 END AS verified, " +
            "CASE WHEN EXISTS (SELECT 1 FROM attachments a WHERE a.ownerType = 'TRANSACTION' AND a.ownerId = t.id) THEN 1 ELSE 0 END AS hasAttachment, " +
            "(SELECT GROUP_CONCAT(COALESCE(sc.name, 'Lainnya') || ': ' || s.amountRupiah, ' | ') " +
            "FROM transaction_splits s LEFT JOIN categories sc ON sc.id = s.categoryId WHERE s.transactionId = t.id) AS splitSummary " +
            "FROM transactions t LEFT JOIN wallets w ON w.id = t.walletId LEFT JOIN categories c ON c.id = t.categoryId " +
            "WHERE t.deletedAtEpochMillis IS NULL AND t.occurredAtEpochMillis >= :startMillis " +
            "AND t.occurredAtEpochMillis < :endExclusiveMillis ORDER BY t.occurredAtEpochMillis, t.id")
    List<ExportTransactionRow> exportTransactionsBetween(long startMillis, long endExclusiveMillis);

    @Query("SELECT * FROM transactions WHERE deletedAtEpochMillis IS NULL ORDER BY occurredAtEpochMillis DESC, id DESC LIMIT 5")
    List<TransactionEntity> latestTransactions();

    @Query("SELECT COALESCE(SUM(CASE WHEN t.type = 'INCOME' OR (t.type = 'WRITE_OFF' AND c.kind = 'INCOME') THEN s.amountRupiah ELSE 0 END), 0) AS incomeRupiah, COALESCE(SUM(CASE WHEN t.type = 'EXPENSE' OR (t.type = 'WRITE_OFF' AND c.kind = 'EXPENSE') THEN s.amountRupiah ELSE 0 END), 0) AS expenseRupiah FROM transaction_splits s INNER JOIN transactions t ON t.id = s.transactionId LEFT JOIN categories c ON c.id = s.categoryId WHERE s.countsInReport = 1 AND t.type IN ('INCOME', 'EXPENSE', 'WRITE_OFF') AND t.deletedAtEpochMillis IS NULL AND t.occurredAtEpochMillis >= :startMillis AND t.occurredAtEpochMillis < :endMillis")
    ReportTotals reportTotals(long startMillis, long endMillis);

    @Query("SELECT s.categoryId AS categoryId, COALESCE(c.name, 'Lainnya') AS categoryName, SUM(s.amountRupiah) AS amountRupiah " +
            "FROM transaction_splits s INNER JOIN transactions t ON t.id = s.transactionId " +
            "LEFT JOIN categories c ON c.id = s.categoryId " +
            "WHERE s.countsInReport = 1 AND (t.type = 'EXPENSE' OR (t.type = 'WRITE_OFF' AND c.kind = 'EXPENSE')) AND t.deletedAtEpochMillis IS NULL " +
            "AND t.occurredAtEpochMillis >= :startMillis AND t.occurredAtEpochMillis < :endMillis " +
            "GROUP BY s.categoryId ORDER BY amountRupiah DESC")
    List<ReportCategoryTotal> reportExpenseCategories(long startMillis, long endMillis);

    @Query("SELECT t.id AS id, t.occurredAtEpochMillis AS occurredAtEpochMillis, t.type AS type, " +
            "s.amountRupiah AS amountRupiah, w.name AS walletName, c.name AS categoryName, " +
            "t.merchant AS merchant, t.note AS note " +
            "FROM transaction_splits s INNER JOIN transactions t ON t.id = s.transactionId " +
            "LEFT JOIN wallets w ON w.id = t.walletId LEFT JOIN categories c ON c.id = s.categoryId " +
            "WHERE s.categoryId = :categoryId AND s.countsInReport = 1 AND t.deletedAtEpochMillis IS NULL " +
            "AND t.occurredAtEpochMillis >= :startMillis AND t.occurredAtEpochMillis < :endExclusiveMillis " +
            "ORDER BY t.occurredAtEpochMillis DESC, t.id DESC")
    List<ReportTransactionRow> reportTransactionsForCategory(long categoryId, long startMillis, long endExclusiveMillis);

    @Query("SELECT * FROM transactions WHERE id = :id")
    TransactionEntity transactionById(long id);

    @Query("SELECT * FROM transactions WHERE transferGroupId = (SELECT transferGroupId FROM transactions WHERE id = :id)")
    List<TransactionEntity> rowsForTransfer(long id);

    @Query("UPDATE transactions SET amountRupiah = :amount, walletId = :walletId, categoryId = :categoryId, occurredAtEpochMillis = :occurredAt, note = :note, merchant = :merchant, needsRecheck = CASE WHEN verifiedCheckpointId IS NULL THEN needsRecheck ELSE 1 END WHERE id = :id AND deletedAtEpochMillis IS NULL AND transferGroupId IS NULL AND type IN ('INCOME','EXPENSE') AND EXISTS (SELECT 1 FROM categories WHERE categories.id = :categoryId AND categories.kind = transactions.type AND categories.isArchived = 0)")
    int updateOrdinary(long id, long amount, long walletId, Long categoryId, long occurredAt, String note, String merchant);

    @Transaction
    default int updateOrdinaryTrackingCheckpoint(long id, long amount, long walletId, Long categoryId,
                                                 long occurredAt, String note, String merchant) {
        int updated = updateOrdinary(id, amount, walletId, categoryId, occurredAt, note, merchant);
        if (updated == 1) {
            deleteSplitsForTransaction(id);
            insertSplitRow(id, amount, categoryId, true, true);
            if (splitTotal(id) != amount) throw new IllegalStateException("Total split harus sama dengan nominal transaksi.");
            if (isVerified(id) > 0) markCheckpointsForTransactionRecheck(id);
        }
        return updated;
    }

    @Query("SELECT COUNT(*) FROM transactions WHERE id = :id AND verifiedCheckpointId IS NOT NULL")
    int isVerified(long id);

    @Query("SELECT COUNT(*) FROM transactions WHERE id = :id AND transferGroupId IS NOT NULL")
    int isSpecial(long id);

    @Query("SELECT * FROM transactions WHERE id = :id AND verifiedCheckpointId IS NOT NULL")
    TransactionEntity verifiedTransaction(long id);

    @Query("UPDATE wallets SET initialBalanceRupiah = :balance WHERE id = :walletId AND deletedAtEpochMillis IS NULL")
    int updateInitialBalance(long walletId, long balance);

    @Transaction
    default ReconciliationResult resolveReconciliation(long walletId, long actualBalanceRupiah,
                                                        long occurredAtEpochMillis, String resolution,
                                                        List<ReconciliationPart> parts) {
        return resolveReconciliationWithCashCounts(walletId, actualBalanceRupiah, occurredAtEpochMillis,
                resolution, parts, Collections.emptyList(), Collections.emptyList());
    }

    @Transaction
    default ReconciliationResult resolveReconciliationWithCashCounts(
            long walletId,
            long actualBalanceRupiah,
            long occurredAtEpochMillis,
            String resolution,
            List<ReconciliationPart> parts,
            List<CashDenominationCountInput> cashCounts,
            List<ReconciliationItemInput> reconciliationItems) {
        if (actualBalanceRupiah < 0) throw new IllegalArgumentException("Saldo aktual tidak boleh negatif.");
        if (!"UNTRACKED".equals(resolution) && !"MISSED".equals(resolution) &&
                !"OPENING_BALANCE".equals(resolution) && !"MATCHED".equals(resolution))
            throw new IllegalArgumentException("Jenis resolusi tidak valid.");
        if (parts == null) throw new IllegalArgumentException("Rincian resolusi tidak valid.");
        if (cashCounts == null) throw new IllegalArgumentException("Rincian pecahan uang tunai tidak valid.");
        if (reconciliationItems == null) throw new IllegalArgumentException("Daftar cek mutasi tidak valid.");
        HashSet<Long> denominations = new HashSet<>();
        for (CashDenominationCountInput count : cashCounts) {
            if (count.denominationRupiah <= 0 || count.pieceCount < 0)
                throw new IllegalArgumentException("Pecahan harus positif dan jumlah lembar tidak boleh negatif.");
            if (!denominations.add(count.denominationRupiah))
                throw new IllegalArgumentException("Setiap pecahan hanya boleh dicatat sekali.");
        }
        HashSet<Long> checkedTransactionIds = new HashSet<>();
        for (ReconciliationItemInput item : reconciliationItems) {
            if (item.transactionId <= 0 || !checkedTransactionIds.add(item.transactionId) ||
                    transactionMatchesWallet(item.transactionId, walletId, occurredAtEpochMillis) != 1)
                throw new IllegalArgumentException("Transaksi cek mutasi tidak valid untuk dompet ini.");
        }
        WalletEntity wallet = activeWallet(walletId);
        if (wallet == null) throw new IllegalArgumentException("Dompet aktif tidak ditemukan.");
        long originalInitialBalance = wallet.initialBalanceRupiah;
        long currentBalance = Math.addExact(originalInitialBalance, transactionEffect(walletId, occurredAtEpochMillis));
        long delta = Math.subtractExact(actualBalanceRupiah, currentBalance);
        if (parts.isEmpty() && (!"MATCHED".equals(resolution) || delta != 0L))
            throw new IllegalArgumentException("Rincian resolusi wajib diisi kecuali saldo sudah cocok.");
        if ("MATCHED".equals(resolution) && (delta != 0L || !parts.isEmpty()))
            throw new IllegalArgumentException("Cek mutasi tanpa resolusi hanya berlaku saat saldo cocok.");
        long resolved = 0;
        for (ReconciliationPart part : parts) {
            if (part.amountRupiah <= 0) throw new IllegalArgumentException("Nominal resolusi harus positif.");
            if (!"INCOME".equals(part.type) && !"EXPENSE".equals(part.type) && !"ADJUSTMENT".equals(part.type)) {
                throw new IllegalArgumentException("Jenis transaksi resolusi tidak valid.");
            }
            long signed = "INCOME".equals(part.type) || ("ADJUSTMENT".equals(part.type) && "PLUS".equals(part.direction))
                    ? part.amountRupiah : Math.negateExact(part.amountRupiah);
            resolved = Math.addExact(resolved, signed);
            if ("ADJUSTMENT".equals(part.type) && !"PLUS".equals(part.direction) && !"MINUS".equals(part.direction))
                throw new IllegalArgumentException("Arah penyesuaian tidak valid.");
        }
        if (resolved != delta) throw new IllegalArgumentException("Rincian resolusi tidak sama dengan selisih saldo.");
        if ("UNTRACKED".equals(resolution) && parts.stream().anyMatch(part -> !"ADJUSTMENT".equals(part.type)))
            throw new IllegalArgumentException("Selisih tak terlacak harus dicatat sebagai penyesuaian.");
        if ("MISSED".equals(resolution) && parts.stream().anyMatch(part -> !"INCOME".equals(part.type) && !"EXPENSE".equals(part.type)))
            throw new IllegalArgumentException("Transaksi terlupa harus berupa pemasukan atau pengeluaran.");
        if ("OPENING_BALANCE".equals(resolution)) {
            if (parts.size() != 1 || !"ADJUSTMENT".equals(parts.get(0).type))
                throw new IllegalArgumentException("Koreksi saldo awal harus satu penyesuaian.");
            if (updateInitialBalance(walletId, Math.addExact(originalInitialBalance, delta)) != 1)
                throw new IllegalStateException("Saldo awal dompet gagal diperbarui.");
        }
        for (ReconciliationPart part : parts) {
            if ("ADJUSTMENT".equals(part.type) && part.categoryId != null)
                throw new IllegalArgumentException("Penyesuaian saldo tidak boleh memiliki kategori transaksi.");
            if ("INCOME".equals(part.type) || "EXPENSE".equals(part.type)) {
                CategoryEntity category = part.categoryId == null ? null : activeCategory(part.categoryId);
                if (category == null || !part.type.equals(category.kind)) throw new IllegalArgumentException("Kategori transaksi resolusi tidak cocok.");
            }
        }
        if (!"OPENING_BALANCE".equals(resolution)) {
            for (ReconciliationPart part : parts) {
                TransactionEntity transaction = new TransactionEntity(com.illustravibes.arus.domain.TransactionType.valueOf(part.type), walletId,
                        part.amountRupiah, part.direction, part.categoryId, occurredAtEpochMillis,
                        part.note == null ? resolution : part.note, null, null);
                if (transaction.type == com.illustravibes.arus.domain.TransactionType.INCOME ||
                        transaction.type == com.illustravibes.arus.domain.TransactionType.EXPENSE) {
                    insertWithFullCategorySplit(transaction);
                } else {
                    insert(transaction);
                }
            }
        }
        long resultingBalance = Math.addExact(currentBalance, resolved);
        if (resultingBalance != actualBalanceRupiah) throw new IllegalStateException("Saldo hasil rekonsiliasi tidak cocok.");
        long checkpointId = insertCheckpoint(new ReconciliationCheckpointEntity(walletId, occurredAtEpochMillis,
                currentBalance, actualBalanceRupiah, delta, resolution));
        for (CashDenominationCountInput count : cashCounts) {
            if (count.pieceCount > 0)
                insertCashDenominationCount(checkpointId, count.denominationRupiah, count.pieceCount);
        }
        for (ReconciliationItemInput item : reconciliationItems)
            insertReconciliationItem(checkpointId, item.transactionId, item.matched);
        markTransactionsVerified(walletId, occurredAtEpochMillis, checkpointId);
        return new ReconciliationResult(currentBalance, actualBalanceRupiah, delta, resolution, checkpointId);
    }

    @Query("INSERT INTO cash_denomination_counts (checkpointId, denominationRupiah, pieceCount) VALUES (:checkpointId, :denominationRupiah, :pieceCount)")
    void insertCashDenominationCount(long checkpointId, long denominationRupiah, long pieceCount);

    @Query("SELECT COUNT(*) FROM transactions WHERE id = :transactionId AND walletId = :walletId AND deletedAtEpochMillis IS NULL AND occurredAtEpochMillis <= :checkedAt")
    int transactionMatchesWallet(long transactionId, long walletId, long checkedAt);

    @Query("INSERT INTO reconciliation_items (checkpointId, transactionId, matched) VALUES (:checkpointId, :transactionId, :matched)")
    void insertReconciliationItem(long checkpointId, long transactionId, boolean matched);

    class ReconciliationPart {
        public final String type;
        public final long amountRupiah;
        public final String direction;
        public final Long categoryId;
        public final String note;
        public ReconciliationPart(String type, long amountRupiah, String direction, Long categoryId, String note) {
            this.type = type;
            this.amountRupiah = amountRupiah;
            this.direction = direction;
            this.categoryId = categoryId;
            this.note = note;
        }
    }

    class ReconciliationResult {
        public final long recordedBalanceRupiah;
        public final long actualBalanceRupiah;
        public final long differenceRupiah;
        public final String resolution;
        public final long checkpointId;
        public ReconciliationResult(long recorded, long actual, long difference, String resolution, long checkpointId) {
            this.recordedBalanceRupiah = recorded;
            this.actualBalanceRupiah = actual;
            this.differenceRupiah = difference;
            this.resolution = resolution;
            this.checkpointId = checkpointId;
        }
    }

    @Query("UPDATE transactions SET needsRecheck = 1 WHERE verifiedCheckpointId = :checkpointId")
    int markVerifiedRowsForRecheck(long checkpointId);

    @Query("SELECT * FROM transactions WHERE walletId = :walletId AND deletedAtEpochMillis IS NULL " +
            "AND occurredAtEpochMillis <= :now ORDER BY occurredAtEpochMillis, id")
    List<TransactionEntity> activeTransactionsThrough(long walletId, long now);

    @Query("SELECT COALESCE(SUM(CASE " +
            "WHEN type IN ('INCOME','TRANSFER_IN','REPAYMENT_IN','LOAN_IN') THEN amountRupiah " +
            "WHEN type = 'ADJUSTMENT' AND direction = 'PLUS' THEN amountRupiah " +
            "WHEN type IN ('EXPENSE','TRANSFER_OUT','REFUND_OUT','LOAN_OUT','DEBT_PAY_OUT') THEN -amountRupiah " +
            "WHEN type = 'ADJUSTMENT' AND direction = 'MINUS' THEN -amountRupiah " +
            "ELSE 0 END), 0) FROM transactions " +
            "WHERE walletId = :walletId AND deletedAtEpochMillis IS NULL AND occurredAtEpochMillis <= :now")
    long transactionEffect(long walletId, long now);

    @Query("SELECT COALESCE(SUM(amountRupiah), 0) FROM transactions WHERE walletId = :walletId AND type = 'TRANSFER_IN' AND deletedAtEpochMillis IS NULL AND occurredAtEpochMillis <= :now")
    long transferInTotal(long walletId, long now);

    @Query("UPDATE transactions SET deletedAtEpochMillis = :deletedAt WHERE id = :id AND deletedAtEpochMillis IS NULL")
    int softDelete(long id, long deletedAt);

    @Query("UPDATE transactions SET deletedAtEpochMillis = :deletedAt WHERE id = :id AND deletedAtEpochMillis IS NULL AND verifiedCheckpointId IS NULL AND transferGroupId IS NULL AND type IN ('INCOME','EXPENSE')")
    int softDeleteOrdinary(long id, long deletedAt);

    @Query("UPDATE transactions SET deletedAtEpochMillis = :deletedAt, needsRecheck = CASE WHEN verifiedCheckpointId IS NULL THEN needsRecheck ELSE 1 END WHERE id = :id AND deletedAtEpochMillis IS NULL AND transferGroupId IS NULL AND type IN ('INCOME','EXPENSE')")
    int softDeleteOrdinaryAllowVerified(long id, long deletedAt);

    @Transaction
    default int softDeleteOrdinaryTrackingCheckpoint(long id, long deletedAt) {
        int deleted = softDeleteOrdinaryAllowVerified(id, deletedAt);
        if (deleted == 1) {
            markCheckpointsForTransactionRecheck(id);
            deleteSplitsForTransaction(id);
        }
        return deleted;
    }

    @Query("UPDATE transactions SET deletedAtEpochMillis = NULL WHERE id = :id AND deletedAtEpochMillis = :deletedAt")
    int restore(long id, long deletedAt);

    @Transaction
    default int restoreOrdinary(long id, long deletedAt) {
        TransactionEntity row = transactionById(id);
        if (row == null || row.deletedAtEpochMillis == null || row.deletedAtEpochMillis != deletedAt) return 0;
        int restored = restore(id, deletedAt);
        if (restored == 1 && row.categoryId != null &&
                (row.type == com.illustravibes.arus.domain.TransactionType.INCOME || row.type == com.illustravibes.arus.domain.TransactionType.EXPENSE)) {
            deleteSplitsForTransaction(id);
            insertSplitRow(id, row.amountRupiah, row.categoryId, true, true);
            if (splitTotal(id) != row.amountRupiah) throw new IllegalStateException("Total split harus sama dengan nominal transaksi.");
        }
        return restored;
    }

    @Query("SELECT * FROM wallets WHERE id = :walletId AND deletedAtEpochMillis IS NULL AND isArchived = 0")
    WalletEntity activeWallet(long walletId);

    @Query("SELECT * FROM categories WHERE id = :categoryId AND isArchived = 0")
    CategoryEntity activeCategory(long categoryId);

    @Query("SELECT id FROM categories WHERE name = 'Admin Bank' AND kind = 'EXPENSE' AND isArchived = 0 LIMIT 1")
    Long adminFeeCategoryId();

    default Long activeAdminFeeCategoryId() {
        Long id = adminFeeCategoryId();
        return id == null || activeCategory(id) == null ? null : id;
    }

    @Query("SELECT COALESCE(SUM(CASE WHEN type IN ('INCOME','TRANSFER_IN','REPAYMENT_IN','LOAN_IN') THEN amountRupiah WHEN type IN ('EXPENSE','TRANSFER_OUT','REFUND_OUT','LOAN_OUT','DEBT_PAY_OUT') THEN -amountRupiah WHEN type = 'ADJUSTMENT' AND direction = 'PLUS' THEN amountRupiah WHEN type = 'ADJUSTMENT' AND direction = 'MINUS' THEN -amountRupiah ELSE 0 END), 0) FROM transactions WHERE walletId = :walletId AND deletedAtEpochMillis IS NULL AND occurredAtEpochMillis <= :now")
    long currentWalletEffect(long walletId, long now);

    @Query("SELECT * FROM transactions WHERE transferGroupId = :groupId ORDER BY id")
    List<TransactionEntity> activeTransferGroup(long groupId);

    @Query("UPDATE transactions SET transferGroupId = :groupId WHERE id = :transactionId")
    int updateTransferGroup(long transactionId, long groupId);

    default long insertOrdinary(TransactionEntity transaction, boolean allowNegativeBalance) {
        if (transaction.categoryId == null) throw new IllegalArgumentException("Kategori transaksi wajib dipilih.");
        return insertOrdinaryWithSplits(transaction, allowNegativeBalance,
                Collections.singletonList(new TransactionSplitDraft(
                        transaction.amountRupiah, transaction.categoryId, true, true)));
    }

    @Transaction
    default long insertOrdinaryWithSplits(TransactionEntity transaction, boolean allowNegativeBalance,
                                          List<TransactionSplitDraft> splits) {
        if (transaction.type != com.illustravibes.arus.domain.TransactionType.INCOME &&
                transaction.type != com.illustravibes.arus.domain.TransactionType.EXPENSE) {
            throw new IllegalArgumentException("Transaksi biasa hanya mendukung pemasukan atau pengeluaran.");
        }
        if (transaction.amountRupiah <= 0) throw new IllegalArgumentException("Nominal transaksi harus positif.");
        if (transaction.walletId == null) throw new IllegalArgumentException("Dompet transaksi wajib dipilih.");
        if (splits == null || splits.isEmpty()) throw new IllegalArgumentException("Tambahkan setidaknya satu kategori split.");
        WalletEntity wallet = activeWallet(transaction.walletId);
        if (wallet == null) throw new IllegalArgumentException("Dompet aktif tidak ditemukan.");
        String expectedKind = transaction.type == com.illustravibes.arus.domain.TransactionType.INCOME ? "INCOME" : "EXPENSE";
        long splitTotal = 0L;
        HashSet<Long> categories = new HashSet<>();
        for (TransactionSplitDraft split : splits) {
            if (split.amountRupiah <= 0) throw new IllegalArgumentException("Nominal setiap split harus positif.");
            CategoryEntity category = activeCategory(split.categoryId);
            if (category == null || !expectedKind.equals(category.kind))
                throw new IllegalArgumentException("Kategori split tidak cocok dengan jenis transaksi.");
            if (!categories.add(split.categoryId)) throw new IllegalArgumentException("Setiap kategori split hanya boleh digunakan sekali.");
            splitTotal = Math.addExact(splitTotal, split.amountRupiah);
        }
        if (splitTotal != transaction.amountRupiah)
            throw new IllegalArgumentException("Total split harus sama dengan nominal transaksi.");
        if (transaction.categoryId == null || !categories.contains(transaction.categoryId))
            throw new IllegalArgumentException("Kategori utama harus ada di dalam split.");
        long now = System.currentTimeMillis();
        Long currentBalance = null;
        if (transaction.occurredAtEpochMillis <= now) {
            currentBalance = Math.addExact(wallet.initialBalanceRupiah, transactionEffect(wallet.id, now));
        }
        if (transaction.occurredAtEpochMillis <= System.currentTimeMillis() &&
                transaction.type == com.illustravibes.arus.domain.TransactionType.EXPENSE &&
                !"CREDIT".equals(wallet.type) &&
                currentBalance != null && currentBalance < transaction.amountRupiah && !allowNegativeBalance) {
            throw new InsufficientBalanceException();
        }
        long transactionId = insert(transaction);
        for (TransactionSplitDraft split : splits) {
            insertSplitRow(transactionId, split.amountRupiah, split.categoryId,
                    split.countsInReport, split.countsInBudget);
        }
        if (splitTotal(transactionId) != transaction.amountRupiah)
            throw new IllegalStateException("Total split harus sama dengan nominal transaksi.");
        return transactionId;
    }

    default long insertWithFullCategorySplit(TransactionEntity transaction) {
        if (transaction.type != com.illustravibes.arus.domain.TransactionType.INCOME &&
                transaction.type != com.illustravibes.arus.domain.TransactionType.EXPENSE) {
            throw new IllegalArgumentException("Split kategori hanya berlaku untuk pemasukan atau pengeluaran.");
        }
        if (transaction.amountRupiah <= 0 || transaction.categoryId == null || transaction.walletId == null)
            throw new IllegalArgumentException("Nominal dan kategori split wajib diisi.");
        WalletEntity wallet = activeWallet(transaction.walletId);
        CategoryEntity category = activeCategory(transaction.categoryId);
        String expectedKind = transaction.type == com.illustravibes.arus.domain.TransactionType.INCOME ? "INCOME" : "EXPENSE";
        if (wallet == null || category == null || !expectedKind.equals(category.kind))
            throw new IllegalArgumentException("Dompet atau kategori split tidak valid.");
        long transactionId = insert(transaction);
        insertSplitRow(transactionId, transaction.amountRupiah, transaction.categoryId, true, true);
        if (splitTotal(transactionId) != transaction.amountRupiah)
            throw new IllegalStateException("Total split harus sama dengan nominal transaksi.");
        return transactionId;
    }

    @Query("SELECT COALESCE(SUM(amountRupiah), 0) FROM transaction_splits WHERE transactionId = :transactionId")
    long splitTotal(long transactionId);

    @Query("DELETE FROM transaction_splits WHERE transactionId = :transactionId")
    int deleteSplitsForTransaction(long transactionId);

    @Query("INSERT INTO transaction_splits (transactionId, amountRupiah, categoryId, countsInReport, countsInBudget) VALUES (:transactionId, :amountRupiah, :categoryId, :countsInReport, :countsInBudget)")
    void insertSplitRow(long transactionId, long amountRupiah, Long categoryId,
                        boolean countsInReport, boolean countsInBudget);

    @Transaction
    default long insertTransfer(long sourceWalletId, long destinationWalletId, long amountRupiah,
                                long feeRupiah, long occurredAtEpochMillis, String note,
                                boolean allowNegativeBalance) {
        if (sourceWalletId == destinationWalletId) throw new IllegalArgumentException("Dompet asal dan tujuan harus berbeda.");
        if (amountRupiah <= 0 || feeRupiah < 0) throw new IllegalArgumentException("Nominal transfer harus positif dan biaya tidak boleh negatif.");
        WalletEntity source = activeWallet(sourceWalletId);
        WalletEntity destination = activeWallet(destinationWalletId);
        if (source == null || destination == null) throw new IllegalArgumentException("Dompet aktif tidak ditemukan.");
        Long feeCategory = feeRupiah > 0 ? activeAdminFeeCategoryId() : null;
        if (feeRupiah > 0 && feeCategory == null) throw new IllegalStateException("Kategori biaya admin tidak ditemukan.");
        long now = System.currentTimeMillis();
        if (occurredAtEpochMillis <= now && !"CREDIT".equals(source.type) && !allowNegativeBalance) {
            long available = Math.addExact(source.initialBalanceRupiah, currentWalletEffect(sourceWalletId, now));
            long required = Math.addExact(amountRupiah, feeRupiah);
            if (available < required) throw new InsufficientBalanceException();
        }
        long sourceTransactionId = insert(new TransactionEntity(com.illustravibes.arus.domain.TransactionType.TRANSFER_OUT,
                sourceWalletId, amountRupiah, null, null, occurredAtEpochMillis, note, null, null));
        long groupId = sourceTransactionId;
        long destinationTransactionId = insert(new TransactionEntity(com.illustravibes.arus.domain.TransactionType.TRANSFER_IN,
                destinationWalletId, amountRupiah, null, null, occurredAtEpochMillis, note, null, null));
        updateTransferGroup(sourceTransactionId, groupId);
        updateTransferGroup(destinationTransactionId, groupId);
        if (feeRupiah > 0) {
            long feeTransactionId = insertWithFullCategorySplit(new TransactionEntity(com.illustravibes.arus.domain.TransactionType.EXPENSE,
                    sourceWalletId, feeRupiah, null, feeCategory, occurredAtEpochMillis, note, null, null));
            updateTransferGroup(feeTransactionId, groupId);
        }
        return groupId;
    }

    @Transaction
    default int softDeleteTransferGroup(long groupId, long deletedAt) {
        int affected = 0;
        for (TransactionEntity transaction : activeTransferGroup(groupId)) {
            int deleted = softDelete(transaction.id, deletedAt);
            if (deleted == 1 && transaction.type == com.illustravibes.arus.domain.TransactionType.EXPENSE) {
                deleteSplitsForTransaction(transaction.id);
            }
            affected += deleted;
        }
        return affected;
    }

    final class InsufficientBalanceException extends IllegalStateException {
        public InsufficientBalanceException() { super("Saldo tidak cukup, saldo akan minus."); }
    }

}
