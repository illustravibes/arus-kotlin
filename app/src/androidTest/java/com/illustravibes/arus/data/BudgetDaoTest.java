package com.illustravibes.arus.data;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.illustravibes.arus.domain.TransactionType;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

@RunWith(AndroidJUnit4.class)
public class BudgetDaoTest {
    private ArusDatabase database;

    @Before public void setUp() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), ArusDatabase.class).build();
    }

    @After public void tearDown() { database.close(); }

    @Test public void spendingUsesActiveBudgetSplitsOnlyAndHonorsPeriod() {
        long wallet = database.walletDao().insert(new WalletEntity("Tunai", "CASH", 0, true, false, null));
        long food = database.categoryDao().insert(new CategoryEntity("Makan", "EXPENSE", "restaurant", 0, false, false, 0));
        long incomeCategory = database.categoryDao().insert(new CategoryEntity("Gaji", "INCOME", "payments", 0, false, false, 0));
        LocalDate date = LocalDate.of(2026, 5, 12);
        long start = date.withDayOfMonth(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long end = date.withDayOfMonth(1).plusMonths(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long expenseId = database.transactionDao().insert(new TransactionEntity(TransactionType.EXPENSE, wallet, 800, null, food, start + 1000, null, null, null));
        database.transactionSplitDao().deleteForTransaction(expenseId);
        database.transactionSplitDao().insert(new TransactionSplitEntity(expenseId, 600, food, true, true));
        database.transactionSplitDao().insert(new TransactionSplitEntity(expenseId, 200, food, true, false));
        database.transactionDao().insert(new TransactionEntity(TransactionType.INCOME, wallet, 900, null, incomeCategory, start + 2000, null, null, null));
        database.transactionDao().insert(new TransactionEntity(TransactionType.EXPENSE, wallet, 500, null, food, end, null, null, null));
        assertEquals(600, database.budgetDao().categorySpent(food, start, end));
        assertEquals(1, database.budgetDao().transactionDetails(food, start, end).size());
        assertEquals(600, database.budgetDao().spent(start, end));
    }

    @Test public void activeBudgetPeriodsCannotOverlapForSameCategory() {
        long category = database.categoryDao().insert(new CategoryEntity("Makan", "EXPENSE", "restaurant", 0, false, false, 0));
        BudgetEntity first = new BudgetEntity("Mei", category, 1_000, 1, 100, 131, false, 0, 80, false);
        database.budgetDao().insertValidated(first);
        BudgetEntity overlapping = new BudgetEntity("Mei sebagian", category, 2_000, 1, 120, 150, false, 0, 80, false);
        assertThrows(IllegalArgumentException.class, () -> database.budgetDao().insertValidated(overlapping));
    }

    @Test public void totalAndCategoryBudgetsCannotOverlap() {
        long category = database.categoryDao().insert(new CategoryEntity("Makan", "EXPENSE", "restaurant", 0, false, false, 0));
        database.budgetDao().insertValidated(new BudgetEntity("Total", null, 10_000, 1, 100, 131, false, 0, 80, false));
        BudgetEntity categoryBudget = new BudgetEntity("Makan", category, 1_000, 1, 120, 150, false, 0, 80, false);
        assertThrows(IllegalArgumentException.class, () -> database.budgetDao().insertValidated(categoryBudget));
    }

    @Test public void rolloverAdvancesPeriodAndCapsRemainingAtBaseAmount() {
        long wallet = database.walletDao().insert(new WalletEntity("Tunai", "CASH", 0, true, false, null));
        long food = database.categoryDao().insert(new CategoryEntity("Makan", "EXPENSE", "restaurant", 0, false, false, 0));
        long startMillis = LocalDate.of(2026, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long expenseId = database.transactionDao().insert(new TransactionEntity(TransactionType.EXPENSE, wallet, 100, null, food, startMillis + 1000, null, null, null));
        database.transactionSplitDao().deleteForTransaction(expenseId);
        database.transactionSplitDao().insert(new TransactionSplitEntity(expenseId, 100, food, true, true));
        BudgetEntity budget = new BudgetEntity("Januari", food, 1_000, 1,
                LocalDate.of(2026, 1, 1).toEpochDay(), LocalDate.of(2026, 2, 1).toEpochDay(), true, 0, 80, false);
        long budgetId = database.budgetDao().insertValidated(budget);

        assertEquals(true, database.budgetDao().advanceIfDue(budgetId,
                LocalDate.of(2026, 2, 1).toEpochDay(), LocalDate.of(2026, 3, 1).toEpochDay()));
        BudgetEntity advanced = database.budgetDao().findById(budgetId);
        assertEquals(LocalDate.of(2026, 2, 1).toEpochDay(), advanced.periodStartEpochDay);
        assertEquals(900, advanced.rolloverRupiah);
    }

    @Test public void delayedRolloverCatchUpProcessesEachMissedPeriodInOrder() {
        long wallet = database.walletDao().insert(new WalletEntity("Tunai", "CASH", 0, true, false, null));
        long food = database.categoryDao().insert(new CategoryEntity("Makan", "EXPENSE", "restaurant", 0, false, false, 0));
        long firstStart = LocalDate.of(2026, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long firstExpense = database.transactionDao().insert(new TransactionEntity(TransactionType.EXPENSE, wallet, 200, null, food, firstStart + 1000, null, null, null));
        database.transactionSplitDao().deleteForTransaction(firstExpense);
        database.transactionSplitDao().insert(new TransactionSplitEntity(firstExpense, 200, food, true, true));
        BudgetEntity budget = new BudgetEntity("Januari", food, 1_000, 1,
                LocalDate.of(2026, 1, 1).toEpochDay(), LocalDate.of(2026, 2, 1).toEpochDay(), true, 0, 80, false);
        long id = database.budgetDao().insertValidated(budget);

        long februaryStart = LocalDate.of(2026, 2, 1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long februaryExpense = database.transactionDao().insert(new TransactionEntity(TransactionType.EXPENSE, wallet, 300, null, food, februaryStart + 1000, null, null, null));
        database.transactionSplitDao().deleteForTransaction(februaryExpense);
        database.transactionSplitDao().insert(new TransactionSplitEntity(februaryExpense, 300, food, true, true));
        long februaryEnd = LocalDate.of(2026, 3, 1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        assertEquals(300, database.budgetDao().categorySpent(food, februaryStart, februaryEnd));

        assertEquals(2, database.budgetDao().advanceDueBudgets(LocalDate.of(2026, 3, 1).toEpochDay()));
        BudgetEntity caughtUp = database.budgetDao().findById(id);
        assertEquals(LocalDate.of(2026, 3, 1).toEpochDay(), caughtUp.periodStartEpochDay);
        assertEquals(1_000, caughtUp.rolloverRupiah);
        assertEquals(300, database.budgetDao().categorySpent(food, februaryStart, februaryEnd));

        long marchStart = LocalDate.of(2026, 3, 1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long marchExpense = database.transactionDao().insert(new TransactionEntity(TransactionType.EXPENSE, wallet, 1_500, null, food, marchStart + 1000, null, null, null));
        database.transactionSplitDao().deleteForTransaction(marchExpense);
        database.transactionSplitDao().insert(new TransactionSplitEntity(marchExpense, 1_500, food, true, true));
        assertEquals(true, database.budgetDao().advanceIfDue(id,
                LocalDate.of(2026, 4, 1).toEpochDay(), LocalDate.of(2026, 5, 1).toEpochDay()));
        assertEquals(500, database.budgetDao().findById(id).rolloverRupiah);
    }

    @Test public void reportTotalsIncludeOnlyReportableIncomeAndExpenseSplits() {
        long wallet = database.walletDao().insert(new WalletEntity("Tunai", "CASH", 0, true, false, null));
        long food = database.categoryDao().insert(new CategoryEntity("Makan", "EXPENSE", "restaurant", 0, false, false, 0));
        long income = database.categoryDao().insert(new CategoryEntity("Gaji", "INCOME", "payments", 0, false, false, 0));
        long start = LocalDate.of(2026, 5, 1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long end = LocalDate.of(2026, 6, 1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long incomeId = database.transactionDao().insert(new TransactionEntity(TransactionType.INCOME, wallet, 1_000, null, income, start + 1_000, null, null, null));
        database.transactionSplitDao().deleteForTransaction(incomeId);
        database.transactionSplitDao().insert(new TransactionSplitEntity(incomeId, 600, income, true, true));
        database.transactionSplitDao().insert(new TransactionSplitEntity(incomeId, 400, income, false, false));
        long expenseId = database.transactionDao().insert(new TransactionEntity(TransactionType.EXPENSE, wallet, 500, null, food, start + 2_000, null, null, null));
        database.transactionSplitDao().deleteForTransaction(expenseId);
        database.transactionSplitDao().insert(new TransactionSplitEntity(expenseId, 300, food, true, true));
        database.transactionSplitDao().insert(new TransactionSplitEntity(expenseId, 200, food, false, false));
        long transferId = database.transactionDao().insert(new TransactionEntity(TransactionType.TRANSFER_OUT, wallet, 250, null, food, start + 3_000, null, null, null));
        database.transactionSplitDao().insert(new TransactionSplitEntity(transferId, 250, food, true, true));
        long deletedExpenseId = database.transactionDao().insert(new TransactionEntity(TransactionType.EXPENSE, wallet, 100, null, food, start + 4_000, null, null, null));
        database.transactionSplitDao().insert(new TransactionSplitEntity(deletedExpenseId, 100, food, true, true));
        database.transactionDao().softDeleteOrdinary(deletedExpenseId, start + 5_000);
        long endExpenseId = database.transactionDao().insert(new TransactionEntity(TransactionType.EXPENSE, wallet, 700, null, food, end, null, null, null));
        database.transactionSplitDao().insert(new TransactionSplitEntity(endExpenseId, 700, food, true, true));

        ReportTotals totals = database.transactionDao().reportTotals(start, end);
        assertEquals(600, totals.incomeRupiah);
        assertEquals(300, totals.expenseRupiah);
    }
}
