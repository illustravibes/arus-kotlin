package com.illustravibes.arus.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import java.time.LocalDate;
import java.util.List;

@Dao
public abstract class RecurringDao {
    @Insert
    abstract long insertRule(RecurringRuleEntity rule);

    @Insert
    abstract long insertOccurrence(RecurringOccurrenceEntity occurrence);

    @Update
    abstract int updateRule(RecurringRuleEntity rule);

    @Query("SELECT * FROM recurring_rules WHERE id = :id")
    abstract RecurringRuleEntity rule(long id);

    @Query("SELECT * FROM recurring_rules WHERE isActive = 1 ORDER BY nextOccurrenceEpochDay, id")
    public abstract List<RecurringRuleEntity> activeRules();

    @Query("SELECT * FROM recurring_rules ORDER BY id")
    public abstract List<RecurringRuleEntity> allRules();

    @Query("SELECT * FROM recurring_rules WHERE isActive = 0 ORDER BY id")
    public abstract List<RecurringRuleEntity> pausedRules();

    @Query("SELECT o.* FROM recurring_occurrences o INNER JOIN recurring_rules r ON r.id = o.ruleId WHERE o.status IN ('PENDING', 'POSTING') AND r.confirmationRequired = 1 ORDER BY o.occurrenceEpochDay, o.id")
    public abstract List<RecurringOccurrenceEntity> pendingOccurrences();

    @Query("SELECT o.* FROM recurring_occurrences o INNER JOIN recurring_rules r ON r.id = o.ruleId WHERE o.status = 'PENDING' AND r.confirmationRequired = 0 ORDER BY o.occurrenceEpochDay, o.id")
    abstract List<RecurringOccurrenceEntity> automaticOccurrences();

    @Query("SELECT * FROM recurring_occurrences WHERE ruleId = :ruleId AND occurrenceEpochDay = :day LIMIT 1")
    abstract RecurringOccurrenceEntity occurrence(long ruleId, long day);

    @Query("UPDATE recurring_occurrences SET status = :status, transactionId = :transactionId WHERE id = :id AND status = 'PENDING'")
    abstract int resolveOccurrence(long id, String status, Long transactionId);

    @Query("UPDATE recurring_occurrences SET status = 'POSTING' WHERE id = :id AND status = 'PENDING'")
    abstract int claimOccurrence(long id);

    @Query("UPDATE recurring_rules SET isActive = 0 WHERE id = :id")
    public abstract int deactivate(long id);

    @Query("UPDATE recurring_rules SET isActive = 1 WHERE id = :id")
    public abstract int activate(long id);

    @Query("SELECT * FROM wallets WHERE id = :id AND isArchived = 0 AND deletedAtEpochMillis IS NULL")
    abstract WalletEntity activeWallet(long id);

    @Query("SELECT * FROM categories WHERE id = :id AND isArchived = 0 AND kind = :kind")
    abstract CategoryEntity activeCategory(long id, String kind);

    @Insert
    abstract long insertSplit(TransactionSplitEntity split);

    @Transaction
    public long createRule(RecurringRuleEntity rule) {
        validateRule(rule);
        if (activeWallet(rule.walletId) == null || activeCategory(rule.categoryId, rule.transactionType) == null)
            throw new IllegalArgumentException("Dompet dan kategori harus aktif serta sesuai jenis transaksi.");
        return insertRule(rule);
    }

    @Transaction
    public int catchUp(long todayEpochDay) {
        int processed = 0;
        for (RecurringRuleEntity rule : activeRules()) {
            while (rule.nextOccurrenceEpochDay <= todayEpochDay &&
                    (rule.endEpochDay == null || rule.nextOccurrenceEpochDay <= rule.endEpochDay)) {
                long day = rule.nextOccurrenceEpochDay;
                if (occurrence(rule.id, day) == null) {
                    if (rule.confirmationRequired) {
                        insertOccurrence(new RecurringOccurrenceEntity(rule.id, day, "PENDING", null));
                    } else {
                        long transactionId = insertTransaction(rule, day, rule.amountRupiah);
                        insertSplit(new TransactionSplitEntity(transactionId, rule.amountRupiah, rule.categoryId, true, true));
                        insertOccurrence(new RecurringOccurrenceEntity(rule.id, day, "POSTED", transactionId));
                    }
                    processed++;
                }
                rule.nextOccurrenceEpochDay = nextDay(rule.intervalType, day);
                if (rule.endEpochDay != null && rule.nextOccurrenceEpochDay > rule.endEpochDay) rule.isActive = false;
                updateRule(rule);
                if (!rule.isActive) break;
            }
        }
        return processed;
    }

    @Transaction
    public int runDue(long todayEpochDay) {
        catchUp(todayEpochDay);
        processPendingAutomatically();
        return pendingOccurrences().size();
    }

    @Transaction
    public boolean resolvePending(long occurrenceId, String action, Long adjustedAmount) {
        if (!"POST".equals(action) && !"SKIP".equals(action)) throw new IllegalArgumentException("Tindakan occurrence tidak valid.");
        RecurringOccurrenceEntity pending = pendingOccurrence(occurrenceId);
        if (pending == null) return false;
        Long transactionId = null;
        if ("POST".equals(action)) {
            RecurringRuleEntity rule = rule(pending.ruleId);
            long amount = adjustedAmount == null ? rule.amountRupiah : adjustedAmount;
            if (amount <= 0) throw new IllegalArgumentException("Nominal occurrence harus lebih dari nol.");
            if (claimOccurrence(occurrenceId) != 1) return false;
            transactionId = insertTransaction(rule, pending.occurrenceEpochDay, amount);
            insertSplit(new TransactionSplitEntity(transactionId, amount, rule.categoryId, true, true));
            return resolveClaimed(occurrenceId, transactionId) == 1;
        }
        return resolveOccurrence(occurrenceId, "SKIPPED", null) == 1;
    }

    @Transaction
    public int processPendingAutomatically() {
        int posted = 0;
        for (RecurringOccurrenceEntity pending : automaticOccurrences()) {
            RecurringRuleEntity rule = rule(pending.ruleId);
            if (rule == null || rule.confirmationRequired || !"PENDING".equals(pending.status)) continue;
            if (claimOccurrence(pending.id) != 1) continue;
            long transactionId = insertTransaction(rule, pending.occurrenceEpochDay, rule.amountRupiah);
            insertSplit(new TransactionSplitEntity(transactionId, rule.amountRupiah, rule.categoryId, true, true));
            posted += resolveClaimed(pending.id, transactionId);
        }
        return posted;
    }

    @Query("UPDATE recurring_occurrences SET status = 'POSTED', transactionId = :transactionId WHERE id = :id AND status = 'POSTING'")
    abstract int resolveClaimed(long id, long transactionId);

    @Query("SELECT * FROM recurring_occurrences WHERE id = :id AND status = 'PENDING'")
    abstract RecurringOccurrenceEntity pendingOccurrence(long id);

    private long insertTransaction(RecurringRuleEntity rule, long day, long amount) {
        WalletEntity wallet = activeWallet(rule.walletId);
        CategoryEntity category = activeCategory(rule.categoryId, rule.transactionType);
        if (wallet == null || category == null) throw new IllegalArgumentException("Dompet atau kategori rutin tidak lagi aktif.");
        TransactionEntity row = new TransactionEntity(
                com.illustravibes.arus.domain.TransactionType.valueOf(rule.transactionType),
                rule.walletId, amount, null, rule.categoryId,
                LocalDate.ofEpochDay(day).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(),
                rule.note, null, null);
        return insertTransactionRow(row);
    }

    @Insert
    abstract long insertTransactionRow(TransactionEntity transaction);

    private static void validateRule(RecurringRuleEntity rule) {
        if (rule.name == null || rule.name.trim().isEmpty() || rule.walletId <= 0 || rule.categoryId <= 0 ||
                rule.amountRupiah <= 0 || rule.nextOccurrenceEpochDay < 1 ||
                (rule.endEpochDay != null && rule.endEpochDay < rule.nextOccurrenceEpochDay) ||
                (!"INCOME".equals(rule.transactionType) && !"EXPENSE".equals(rule.transactionType)) ||
                rule.intervalType < 0 || rule.intervalType > 2)
            throw new IllegalArgumentException("Aturan rutin tidak valid.");
    }

    private static long nextDay(int type, long epochDay) {
        LocalDate day = LocalDate.ofEpochDay(epochDay);
        if (type == 0) return day.plusWeeks(1).toEpochDay();
        if (type == 1) return day.plusMonths(1).toEpochDay();
        return day.plusYears(1).toEpochDay();
    }
}
