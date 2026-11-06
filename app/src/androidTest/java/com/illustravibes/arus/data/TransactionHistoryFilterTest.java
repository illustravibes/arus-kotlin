package com.illustravibes.arus.data;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.illustravibes.arus.domain.TransactionType;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class TransactionHistoryFilterTest {
    private ArusDatabase database;

    @Before public void setUp() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), ArusDatabase.class).build();
    }

    @After public void tearDown() { database.close(); }

    @Test public void filtersByTextWalletTypeAndHalfOpenDateRange() {
        long walletA = database.walletDao().insert(new WalletEntity("Tunai", "CASH", 0, true, false, null));
        long walletB = database.walletDao().insert(new WalletEntity("Bank", "BANK", 0, true, false, null));
        long category = database.categoryDao().insert(new CategoryEntity("Makan", "EXPENSE", "restaurant", 0, false, false, 0));
        long otherCategory = database.categoryDao().insert(new CategoryEntity("Transport", "EXPENSE", "bus", 1, false, false, 1));
        insert(walletA, category, 100, "Belanja pasar", "Pasar Sari", TransactionType.EXPENSE);
        insert(walletB, category, 200, "Belanja", "Toko Lain", TransactionType.EXPENSE);
        insert(walletA, category, 199, "Belanja dekat batas", "Pasar Sari", TransactionType.EXPENSE);
        insert(walletA, category, 200, "Belanja akhir eksklusif", "Pasar Sari", TransactionType.EXPENSE);
        insert(walletA, category, 300, "Gaji", "Kantor", TransactionType.INCOME);
        java.util.List<TransactionEntity> result = database.transactionDao()
                .filteredTransactions("pasar", walletA, "EXPENSE", category, 1000L, 1000L, 100L, 200L);
        assertEquals(2, result.size());
        assertEquals("Pasar Sari", result.get(0).merchant);

        insert(walletA, otherCategory, 150, "Naik bus", "", TransactionType.EXPENSE);
        result = database.transactionDao().filteredTransactions("", walletA, "EXPENSE", category, null, null, 100L, 200L);
        assertEquals(2, result.size());
    }

    private void insert(long walletId, long categoryId, long time, String note, String merchant, TransactionType type) {
        TransactionEntity transaction = new TransactionEntity(type, walletId, 1000, null, categoryId,
                time, note, merchant, null);
        database.transactionDao().insert(transaction);
    }
}
