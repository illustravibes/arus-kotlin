package com.illustravibes.arus.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

@Dao
public interface BudgetDao {
    @Insert
    long insert(BudgetEntity budget);

    @Query("SELECT * FROM budgets WHERE isArchived = 0 ORDER BY periodStartEpochDay, id")
    List<BudgetEntity> activeBudgets();

    @Query("UPDATE budgets SET isArchived = 1 WHERE id = :id AND isArchived = 0")
    int archive(long id);

    @Query("SELECT COALESCE(SUM(s.amountRupiah), 0) FROM transaction_splits s INNER JOIN transactions t ON t.id = s.transactionId WHERE s.countsInBudget = 1 AND t.type = 'EXPENSE' AND t.deletedAtEpochMillis IS NULL AND t.occurredAtEpochMillis >= :startMillis AND t.occurredAtEpochMillis < :endMillis")
    long spent(long startMillis, long endMillis);

    @Query("SELECT COALESCE(SUM(s.amountRupiah), 0) FROM transaction_splits s INNER JOIN transactions t ON t.id = s.transactionId WHERE s.countsInBudget = 1 AND t.type = 'EXPENSE' AND t.deletedAtEpochMillis IS NULL AND t.occurredAtEpochMillis >= :startMillis AND t.occurredAtEpochMillis < :endMillis AND s.categoryId = :categoryId")
    long categorySpent(long categoryId, long startMillis, long endMillis);

    @Query("SELECT s.transactionId, t.occurredAtEpochMillis, s.amountRupiah, s.categoryId, t.note FROM transaction_splits s INNER JOIN transactions t ON t.id = s.transactionId WHERE s.countsInBudget = 1 AND t.type = 'EXPENSE' AND t.deletedAtEpochMillis IS NULL AND t.occurredAtEpochMillis >= :startMillis AND t.occurredAtEpochMillis < :endMillis ORDER BY t.occurredAtEpochMillis DESC, t.id DESC")
    List<BudgetTransaction> totalTransactionDetails(long startMillis, long endMillis);

    @Query("SELECT * FROM budgets WHERE id = :id")
    BudgetEntity findById(long id);

    @Query("SELECT s.transactionId, t.occurredAtEpochMillis, s.amountRupiah, s.categoryId, t.note FROM transaction_splits s INNER JOIN transactions t ON t.id = s.transactionId WHERE s.countsInBudget = 1 AND t.type = 'EXPENSE' AND t.deletedAtEpochMillis IS NULL AND t.occurredAtEpochMillis >= :startMillis AND t.occurredAtEpochMillis < :endMillis AND (:categoryId IS NULL OR s.categoryId = :categoryId) ORDER BY t.occurredAtEpochMillis DESC, t.id DESC")
    List<BudgetTransaction> transactionDetails(Long categoryId, long startMillis, long endMillis);

    default List<BudgetTransaction> detailsForBudget(Long categoryId, long startMillis, long endMillis) {
        return categoryId == null ? totalTransactionDetails(startMillis, endMillis) : transactionDetails(categoryId, startMillis, endMillis);
    }

    @Query("SELECT COUNT(*) FROM budgets WHERE isArchived = 0 AND categoryId IS :categoryId AND periodStartEpochDay < :end AND periodEndEpochDay > :start AND id != :ignoreId")
    int overlappingBudget(Long categoryId, long start, long end, long ignoreId);

    @Query("SELECT COUNT(*) FROM budgets WHERE isArchived = 0 AND periodStartEpochDay < :end AND periodEndEpochDay > :start AND (categoryId IS NULL OR :categoryId IS NULL)")
    int overlappingTotalBudget(Long categoryId, long start, long end);

    @Query("SELECT COUNT(*) FROM budgets WHERE isArchived = 0 AND periodStartEpochDay < :end AND periodEndEpochDay > :start AND (categoryId IS NULL OR :categoryId IS NULL) AND id != :ignoreId")
    int overlappingTotalBudgetExcept(Long categoryId, long start, long end, long ignoreId);

    @Query("SELECT * FROM budgets WHERE isArchived = 0 AND categoryId IS :categoryId AND periodStartEpochDay = :periodStart AND id != :ignoreId LIMIT 1")
    BudgetEntity otherBudgetForPeriod(Long categoryId, long periodStart, long ignoreId);

    @Query("UPDATE budgets SET rolloverRupiah = :rollover, periodStartEpochDay = :start, periodEndEpochDay = :end WHERE id = :id AND isArchived = 0 AND periodEndEpochDay = :start")
    int advancePeriodIfDue(long id, long start, long end, long rollover);

    @Transaction
    default long insertValidated(BudgetEntity budget) {
        if (budget.categoryId != null) {
            CategoryEntity category = categoryForBudget(budget.categoryId);
            if (category == null || !"EXPENSE".equals(category.kind))
                throw new IllegalArgumentException("Anggaran hanya dapat memakai kategori pengeluaran aktif.");
        }
        if (otherBudgetForPeriod(budget.categoryId, budget.periodStartEpochDay, 0) != null) {
            throw new IllegalArgumentException("Anggaran aktif sudah ada untuk periode ini.");
        }
        if (overlappingBudget(budget.categoryId, budget.periodStartEpochDay, budget.periodEndEpochDay, 0) > 0 ||
                overlappingTotalBudget(budget.categoryId, budget.periodStartEpochDay, budget.periodEndEpochDay) > 0)
            throw new IllegalArgumentException("Periode anggaran aktif tidak boleh tumpang tindih.");
        return insert(budget);
    }

    @Transaction
    default boolean advanceIfDue(long budgetId, long newStartEpochDay, long newEndEpochDay) {
        BudgetEntity current = findById(budgetId);
        if (current == null || current.isArchived || current.periodEndEpochDay != newStartEpochDay || newEndEpochDay <= newStartEpochDay)
            return false;
        if (otherBudgetForPeriod(current.categoryId, newStartEpochDay, current.id) != null)
            throw new IllegalArgumentException("Anggaran aktif sudah ada untuk periode ini.");
        long oldStart = LocalDay.toMillis(current.periodStartEpochDay);
        long oldEnd = LocalDay.toMillis(current.periodEndEpochDay);
        long oldSpent = current.categoryId == null ? spent(oldStart, oldEnd) : categorySpent(current.categoryId, oldStart, oldEnd);
        long carry = current.rolloverEnabled ? Math.max(0, Math.min(current.amountRupiah, current.amountRupiah + current.rolloverRupiah - oldSpent)) : 0;
        if (overlappingBudget(current.categoryId, newStartEpochDay, newEndEpochDay, current.id) > 0 ||
                overlappingTotalBudgetExcept(current.categoryId, newStartEpochDay, newEndEpochDay, current.id) > 0)
            throw new IllegalArgumentException("Periode anggaran aktif tidak boleh tumpang tindih.");
        return advancePeriodIfDue(current.id, newStartEpochDay, newEndEpochDay, carry) == 1;
    }

    @Transaction
    default int advanceDueBudgets(long todayEpochDay) {
        int advanced = 0;
        for (BudgetEntity budget : activeBudgets()) {
            if (budget.periodEndEpochDay > todayEpochDay) continue;
            while (budget.periodEndEpochDay <= todayEpochDay) {
                java.time.LocalDate nextDate = java.time.LocalDate.ofEpochDay(budget.periodEndEpochDay);
                com.illustravibes.arus.domain.BudgetPeriod next = com.illustravibes.arus.domain.BudgetPeriod.containing(budget.periodType, nextDate);
                if (!advanceIfDue(budget.id, next.start.toEpochDay(), next.endExclusive.toEpochDay())) break;
                advanced++;
                budget = findById(budget.id);
            }
        }
        return advanced;
    }

    @Transaction
    default int updateBudget(BudgetEntity budget) {
        BudgetEntity current = findById(budget.id);
        if (current == null) throw new IllegalArgumentException("Anggaran tidak ditemukan.");
        if (budget.amountRupiah <= 0 || budget.periodEndEpochDay <= budget.periodStartEpochDay ||
                budget.warningPercent < 50 || budget.warningPercent > 99 || budget.rolloverRupiah < 0)
            throw new IllegalArgumentException("Detail anggaran tidak valid.");
        if (budget.categoryId != null) {
            CategoryEntity category = categoryForBudget(budget.categoryId);
            if (category == null || !"EXPENSE".equals(category.kind))
                throw new IllegalArgumentException("Anggaran hanya dapat memakai kategori pengeluaran aktif.");
        }
        if (overlappingBudget(budget.categoryId, budget.periodStartEpochDay, budget.periodEndEpochDay, budget.id) > 0 ||
                overlappingTotalBudget(budget.categoryId, budget.periodStartEpochDay, budget.periodEndEpochDay) > 0)
            throw new IllegalArgumentException("Periode anggaran aktif tidak boleh tumpang tindih.");
        return update(budget);
    }

    @androidx.room.Update
    int update(BudgetEntity budget);

    final class LocalDay {
        private LocalDay() {}
        static long toMillis(long epochDay) {
            return java.time.LocalDate.ofEpochDay(epochDay).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
        }
    }

    @Query("SELECT * FROM categories WHERE id = :id AND kind = 'EXPENSE' AND isArchived = 0")
    CategoryEntity categoryForBudget(long id);
}
