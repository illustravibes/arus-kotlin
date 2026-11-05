package com.illustravibes.arus.domain;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import com.illustravibes.arus.data.BudgetDao;
import com.illustravibes.arus.data.BudgetEntity;
import com.illustravibes.arus.data.BudgetTransaction;

public final class BudgetProgress {
    public final long spentRupiah;
    public final long rolloverRupiah;
    public final long effectiveLimitRupiah;
    public final int warningLevel;
    public final List<BudgetTransaction> transactions;

    private BudgetProgress(long spentRupiah, long rolloverRupiah, long effectiveLimitRupiah,
                           int warningLevel, List<BudgetTransaction> transactions) {
        this.spentRupiah = spentRupiah;
        this.rolloverRupiah = rolloverRupiah;
        this.effectiveLimitRupiah = effectiveLimitRupiah;
        this.warningLevel = warningLevel;
        this.transactions = transactions;
    }

    public static BudgetProgress load(BudgetDao dao, BudgetEntity budget, LocalDate today, ZoneId zone) {
        BudgetPeriod period = BudgetPeriod.containing(budget.periodType, today);
        long start = period.start.atStartOfDay(zone).toInstant().toEpochMilli();
        long end = period.endExclusive.atStartOfDay(zone).toInstant().toEpochMilli();
        long spent = budget.categoryId == null ? dao.spent(start, end) : dao.categorySpent(budget.categoryId, start, end);
        long rollover = budget.rolloverEnabled ? Math.min(budget.rolloverRupiah, budget.amountRupiah) : 0;
        long limit = Math.addExact(budget.amountRupiah, rollover);
        return new BudgetProgress(spent, rollover, limit, BudgetPeriod.warningLevel(spent, limit, budget.warningPercent),
                dao.detailsForBudget(budget.categoryId, start, end));
    }
}
