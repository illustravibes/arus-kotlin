package com.illustravibes.arus.domain;

import org.junit.Test;

import java.time.LocalDate;

import static org.junit.Assert.assertEquals;

public class BudgetPeriodTest {
    @Test public void warningLevelsUseEightyAndOneHundredPercentBoundaries() {
        assertEquals(0, BudgetPeriod.warningLevel(799, 1_000));
        assertEquals(80, BudgetPeriod.warningLevel(800, 1_000));
        assertEquals(100, BudgetPeriod.warningLevel(1_000, 1_000));
        assertEquals(100, BudgetPeriod.warningLevel(1_001, 1_000));
    }

    @Test public void monthlyPeriodChangesAtMonthBoundary() {
        BudgetPeriod before = BudgetPeriod.containing(BudgetPeriod.MONTHLY, LocalDate.of(2026, 1, 31));
        BudgetPeriod after = BudgetPeriod.containing(BudgetPeriod.MONTHLY, LocalDate.of(2026, 2, 1));
        assertEquals(LocalDate.of(2026, 1, 1), before.start);
        assertEquals(LocalDate.of(2026, 2, 1), before.endExclusive);
        assertEquals(LocalDate.of(2026, 2, 1), after.start);
    }

    @Test public void weeklyAndYearlyPeriodsUseLocalDateBoundaries() {
        BudgetPeriod week = BudgetPeriod.containing(BudgetPeriod.WEEKLY, LocalDate.of(2026, 5, 10));
        assertEquals(LocalDate.of(2026, 5, 4), week.start);
        assertEquals(LocalDate.of(2026, 5, 11), week.endExclusive);
        BudgetPeriod year = BudgetPeriod.containing(BudgetPeriod.YEARLY, LocalDate.of(2026, 12, 31));
        assertEquals(LocalDate.of(2026, 1, 1), year.start);
        assertEquals(LocalDate.of(2027, 1, 1), year.endExclusive);
    }

    @Test public void monthlyReportPeriodUsesConfiguredStartDay() {
        BudgetPeriod beforeStart = BudgetPeriod.containing(BudgetPeriod.MONTHLY,
                LocalDate.of(2026, 10, 24), 25);
        BudgetPeriod afterStart = BudgetPeriod.containing(BudgetPeriod.MONTHLY,
                LocalDate.of(2026, 10, 25), 25);

        assertEquals(LocalDate.of(2026, 9, 25), beforeStart.start);
        assertEquals(LocalDate.of(2026, 10, 25), beforeStart.endExclusive);
        assertEquals(LocalDate.of(2026, 10, 25), afterStart.start);
        assertEquals(LocalDate.of(2026, 11, 25), afterStart.endExclusive);
    }

    @Test public void configuredThresholdControlsWarning() {
        assertEquals(0, BudgetPeriod.warningLevel(699, 1_000, 70));
        assertEquals(70, BudgetPeriod.warningLevel(700, 1_000, 70));
    }
}
