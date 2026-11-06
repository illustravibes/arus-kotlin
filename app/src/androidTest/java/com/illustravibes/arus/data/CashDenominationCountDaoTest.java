package com.illustravibes.arus.data;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

@RunWith(AndroidJUnit4.class)
public class CashDenominationCountDaoTest {
    private ArusDatabase database;
    private long walletId;

    @Before public void setUp() {
        database = Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(), ArusDatabase.class).build();
        walletId = database.walletDao().insert(new WalletEntity("Tunai", "CASH", 0, true, false, null));
    }

    @After public void tearDown() { database.close(); }

    @Test public void denominationCountsAttachToCheckpointWithoutCreatingTransactions() {
        TransactionDao.ReconciliationResult result = database.transactionDao().resolveReconciliationWithCashCounts(
                walletId, 120_000, 1_000, "OPENING_BALANCE",
                Collections.singletonList(new TransactionDao.ReconciliationPart(
                        "ADJUSTMENT", 120_000, "PLUS", null, "Saldo awal")),
                Arrays.asList(new CashDenominationCountInput(100_000, 1),
                        new CashDenominationCountInput(10_000, 2)),
                Collections.emptyList());

        assertEquals(2, database.cashDenominationCountDao().forCheckpoint(result.checkpointId).size());
        assertEquals(120_000, database.cashDenominationCountDao().forCheckpoint(result.checkpointId).stream()
                .mapToLong(row -> row.denominationRupiah * row.pieceCount).sum());
        assertEquals(0, database.transactionDao().activeTransactions().size());
        assertEquals(120_000, database.walletDao().initialBalance(walletId).longValue());
    }

    @Test public void invalidOrDuplicateDenominationsDoNotCreateCheckpoint() {
        assertThrows(IllegalArgumentException.class, () -> database.transactionDao().resolveReconciliationWithCashCounts(
                walletId, 10_000, 1_000, "OPENING_BALANCE",
                Collections.singletonList(new TransactionDao.ReconciliationPart(
                        "ADJUSTMENT", 10_000, "PLUS", null, "Saldo awal")),
                Arrays.asList(new CashDenominationCountInput(10_000, 1),
                        new CashDenominationCountInput(10_000, 0)),
                Collections.emptyList()));
        assertEquals(0, database.reconciliationCheckpointDao().checkpointsForWallet(walletId).size());
    }
}
