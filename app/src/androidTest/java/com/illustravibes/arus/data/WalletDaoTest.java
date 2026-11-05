package com.illustravibes.arus.data;

import android.content.Context;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@RunWith(AndroidJUnit4.class)
public class WalletDaoTest {
    private final Context context = ApplicationProvider.getApplicationContext();
    private ArusDatabase db;
    @Before public void setUp() {
        context.deleteDatabase("wallet-dao-test");
        db = Room.databaseBuilder(context, ArusDatabase.class, "wallet-dao-test")
                .addMigrations(ArusDatabase.MIGRATION_1_6, ArusDatabase.MIGRATION_2_6,
                        ArusDatabase.MIGRATION_3_6, ArusDatabase.MIGRATION_4_6, ArusDatabase.MIGRATION_5_6).build();
    }
    @After public void tearDown() { db.close(); context.deleteDatabase("wallet-dao-test"); }

    @Test public void archiveHidesWalletFromActiveListButKeepsRowAndTransactions() {
        long id = db.walletDao().insert(new WalletEntity("Tunai", "CASH", 5000, true, false, null));
        db.transactionDao().insert(new com.illustravibes.arus.data.TransactionEntity(
                com.illustravibes.arus.domain.TransactionType.EXPENSE, id, 500, null, 1000, null));
        assertEquals(1, db.walletDao().archive(id));
        assertEquals(0, db.walletDao().activeWallets().size());
        assertEquals(1, db.transactionDao().activeTransactions().size());
        assertEquals(0, db.walletDao().activeWalletCount());
    }

    @Test public void creditOnlyValuesCanBeStoredAndCleared() {
        WalletEntity wallet = new WalletEntity("Kartu", "CREDIT", 0, false, false, null);
        wallet.creditLimitRupiah = 5000000L;
        wallet.billingDay = 12;
        long id = db.walletDao().insert(wallet);
        WalletEntity stored = db.walletDao().findById(id);
        assertEquals(Long.valueOf(5000000L), stored.creditLimitRupiah);
        assertEquals(Integer.valueOf(12), stored.billingDay);
        stored.type = "CASH";
        stored.creditLimitRupiah = null;
        stored.billingDay = null;
        db.walletDao().update(stored);
        assertNull(db.walletDao().findById(id).creditLimitRupiah);
    }
}
