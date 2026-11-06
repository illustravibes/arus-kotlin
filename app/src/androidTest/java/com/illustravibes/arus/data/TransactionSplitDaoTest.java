package com.illustravibes.arus.data;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.illustravibes.arus.domain.TransactionType;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class TransactionSplitDaoTest {
    private ArusDatabase database;
    private long walletId;
    private long foodCategoryId;
    private long transportCategoryId;

    @Before public void setUp() {
        database = Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(), ArusDatabase.class).build();
        walletId = database.walletDao().insert(new WalletEntity("Tunai", "CASH", 100_000, true, false, null));
        foodCategoryId = database.categoryDao().insert(new CategoryEntity("Makan", "EXPENSE", "food", 0, false, false, 0));
        transportCategoryId = database.categoryDao().insert(new CategoryEntity("Transport", "EXPENSE", "transport", 0, false, false, 1));
    }

    @After public void tearDown() { database.close(); }

    @Test public void balancedSplitRowsPersistAtomicallyWithReportAndBudgetFlags() {
        long id = database.transactionDao().insertOrdinaryWithSplits(
                transaction(10_000, foodCategoryId), true,
                Arrays.asList(
                        new TransactionSplitDraft(6_000, foodCategoryId, true, true),
                        new TransactionSplitDraft(4_000, transportCategoryId, true, true)));

        assertEquals(10_000, database.transactionSplitDao().splitTotal(id));
        assertEquals(2, database.transactionSplitDao().splitsForTransaction(id).size());
        assertTrue(database.transactionSplitDao().splitsForTransaction(id).stream()
                .allMatch(split -> split.countsInReport && split.countsInBudget));
    }

    @Test public void unbalancedSplitRowsDoNotCreateTransactionOrSplit() {
        assertThrows(IllegalArgumentException.class, () -> database.transactionDao().insertOrdinaryWithSplits(
                transaction(10_000, foodCategoryId), true,
                Arrays.asList(
                        new TransactionSplitDraft(6_000, foodCategoryId, true, true),
                        new TransactionSplitDraft(3_000, transportCategoryId, true, true))));

        assertTrue(database.transactionDao().activeTransactions().isEmpty());
        assertTrue(database.transactionSplitDao().splitsForTransaction(1).isEmpty());
    }

    private TransactionEntity transaction(long amount, long categoryId) {
        return new TransactionEntity(TransactionType.EXPENSE, walletId, amount, null, categoryId,
                1_000, "Split test", null, null);
    }
}
