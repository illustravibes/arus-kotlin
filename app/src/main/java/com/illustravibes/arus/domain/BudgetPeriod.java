package com.illustravibes.arus.domain;

import java.time.LocalDate;

public final class BudgetPeriod {
    public static final int WEEKLY = 0;
    public static final int MONTHLY = 1;
    public static final int YEARLY = 2;

    public final LocalDate start;
    public final LocalDate endExclusive;

    private BudgetPeriod(LocalDate start, LocalDate endExclusive) {
        this.start = start;
        this.endExclusive = endExclusive;
    }

    public static BudgetPeriod containing(int type, LocalDate date) {
        switch (type) {
            case WEEKLY: {
                LocalDate start = date.minusDays(date.getDayOfWeek().getValue() - 1L);
                return new BudgetPeriod(start, start.plusWeeks(1));
            }
            case MONTHLY: {
                LocalDate start = date.withDayOfMonth(1);
                return new BudgetPeriod(start, start.plusMonths(1));
            }
            case YEARLY: {
                LocalDate start = date.withDayOfYear(1);
                return new BudgetPeriod(start, start.plusYears(1));
            }
            default: throw new IllegalArgumentException("Jenis periode anggaran tidak valid.");
        }
    }

    public static BudgetPeriod containing(int type, LocalDate date, int monthlyStartDay) {
        if (monthlyStartDay < 1 || monthlyStartDay > 28)
            throw new IllegalArgumentException("Tanggal awal periode harus antara 1 dan 28.");
        if (type != MONTHLY) return containing(type, date);
        LocalDate start = date.withDayOfMonth(monthlyStartDay);
        if (date.isBefore(start)) start = start.minusMonths(1);
        return new BudgetPeriod(start, start.plusMonths(1));
    }

    public static int warningLevel(long spent, long limit) {
        return warningLevel(spent, limit, 80);
    }

    public static int warningLevel(long spent, long limit, int thresholdPercent) {
        if (limit <= 0 || spent <= 0) return 0;
        if (spent >= limit) return 100;
        if (thresholdPercent < 50 || thresholdPercent > 99) throw new IllegalArgumentException("Ambang peringatan harus antara 50 dan 99 persen.");
        long threshold = (long) Math.ceil(limit * (thresholdPercent / 100.0));
        return spent >= threshold ? thresholdPercent : 0;
    }
}
