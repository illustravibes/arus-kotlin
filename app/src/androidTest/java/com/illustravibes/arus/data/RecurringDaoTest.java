package com.illustravibes.arus.data;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class RecurringDaoTest {
    private ArusDatabase database;

    @Before public void setUp() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), ArusDatabase.class).build();
    }

    @After public void tearDown() { database.close(); }

    @Test public void automaticCatchUpPostsOnceAndConfirmationWaitsForDecision() {
        long wallet = database.walletDao().insert(new WalletEntity("Tunai", "CASH", 0, true, false, null));
        long category = database.categoryDao().insert(new CategoryEntity("Makan", "EXPENSE", "restaurant", 0, false, false, 0));
        database.recurringDao().createRule(new RecurringRuleEntity("Otomatis", "EXPENSE", wallet, category,
                5000, null, 1, 20000, null, false, true));
        database.recurringDao().createRule(new RecurringRuleEntity("Konfirmasi", "EXPENSE", wallet, category,
                7000, null, 1, 20000, null, true, true));

        assertEquals(2, database.recurringDao().catchUp(20000));
        assertEquals(0, database.recurringDao().catchUp(20000));
        assertEquals(1, database.recurringDao().pendingOccurrences().size());
        assertEquals(0, database.recurringDao().catchUp(20000));
        assertEquals(1, database.transactionDao().activeTransactions().size());
        assertEquals(1, database.recurringDao().pendingOccurrences().size());
        assertEquals(1, database.transactionSplitDao().splitsForTransaction(
                database.transactionDao().activeTransactions().get(0).id).size());
    }

    @Test public void confirmedOccurrenceCanChangeAmountOrSkipWithoutSilentPost() {
        long wallet = database.walletDao().insert(new WalletEntity("Tunai", "CASH", 0, true, false, null));
        long category = database.categoryDao().insert(new CategoryEntity("Makan", "EXPENSE", "restaurant", 0, false, false, 0));
        database.recurringDao().createRule(new RecurringRuleEntity("Tagihan", "EXPENSE", wallet, category,
                5000, null, 1, 20000, null, true, true));
        database.recurringDao().createRule(new RecurringRuleEntity("Langganan", "EXPENSE", wallet, category,
                3000, null, 1, 20000, null, true, true));
        database.recurringDao().catchUp(20000);
        database.recurringDao().catchUp(20000);
        assertEquals(0, database.transactionDao().activeTransactions().size());

        for (RecurringOccurrenceEntity occurrence : database.recurringDao().pendingOccurrences()) {
            if (database.recurringDao().resolvePending(occurrence.id, "POST", 6200L)) break;
        }
        assertEquals(1, database.transactionDao().activeTransactions().size());
        assertEquals(6200, database.transactionDao().activeTransactions().get(0).amountRupiah);
        RecurringOccurrenceEntity remaining = database.recurringDao().pendingOccurrences().get(0);
        assertEquals(true, database.recurringDao().resolvePending(remaining.id, "SKIP", null));
        assertEquals(0, database.recurringDao().pendingOccurrences().size());
        assertEquals(1, database.transactionDao().activeTransactions().size());
    }

    @Test public void automaticOccurrenceUsesTransactionDateAndBudgetAndReportSplits() {
        long wallet = database.walletDao().insert(new WalletEntity("Tunai", "CASH", 0, true, false, null));
        long category = database.categoryDao().insert(new CategoryEntity("Makan", "EXPENSE", "restaurant", 0, false, false, 0));
        database.recurringDao().createRule(new RecurringRuleEntity("Belanja", "EXPENSE", wallet, category,
                900, "Rutin", 0, 20000, null, false, true));
        database.recurringDao().catchUp(20000);
        TransactionEntity transaction = database.transactionDao().activeTransactions().get(0);
        assertEquals(LocalDate.ofEpochDay(20000).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(), transaction.occurredAtEpochMillis);
        TransactionSplitEntity split = database.transactionSplitDao().splitsForTransaction(transaction.id).get(0);
        assertEquals(true, split.countsInReport);
        assertEquals(true, split.countsInBudget);
        assertEquals(category, split.categoryId.longValue());
    }

    @Test public void notificationDenialDoesNotResolvePendingOccurrence() {
        long wallet = database.walletDao().insert(new WalletEntity("Tunai", "CASH", 0, true, false, null));
        long category = database.categoryDao().insert(new CategoryEntity("Makan", "EXPENSE", "restaurant", 0, false, false, 0));
        database.recurringDao().createRule(new RecurringRuleEntity("Tagihan", "EXPENSE", wallet, category,
                1000, null, 1, 20000, null, true, true));
        database.recurringDao().runDue(20000);
        assertEquals(1, database.recurringDao().pendingOccurrences().size());
        assertEquals(0, database.transactionDao().activeTransactions().size());
    }
}
