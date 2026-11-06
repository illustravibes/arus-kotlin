package com.illustravibes.arus.data;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.illustravibes.arus.domain.TransactionType;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class TransactionExportDaoTest {
    private ArusDatabase database;
    private long walletId;
    private long categoryId;

    @Before public void setUp() {
        database = Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(), ArusDatabase.class).build();
        walletId = database.walletDao().insert(new WalletEntity("Tunai", "CASH", 100_000, true, false, null));
        categoryId = database.categoryDao().insert(new CategoryEntity("Makan", "EXPENSE", "restaurant", 0, false, false, 0));
    }

    @After public void tearDown() { database.close(); }

    @Test public void exportUsesInclusiveStartExclusiveEndAndIncludesTransactionDetails() {
        long firstId = insert("EXPENSE", 1_000, 1_500, "Makan, siang", "Kantor \"Pusat\"");
        insert("EXPENSE", 2_000, 1_999, "Makan", null);
        database.attachmentDao().insert(new AttachmentEntity("TRANSACTION", firstId, "receipts/a.jpg", null, 1_600));

        List<ExportTransactionRow> rows = database.transactionDao().exportTransactionsBetween(1_000, 2_000);

        assertEquals(2, rows.size());
        assertEquals(firstId, rows.get(0).id);
        assertEquals("EXPENSE", rows.get(0).type);
        assertEquals("Tunai", rows.get(0).walletName);
        assertEquals("Makan", rows.get(0).categoryName);
        assertEquals("Kantor \"Pusat\"", rows.get(0).merchant);
        assertTrue(rows.get(0).hasAttachment);
        assertFalse(rows.get(0).verified);
        assertTrue(rows.get(0).splitSummary.contains("Makan: 1000"));
        assertEquals(1_999, rows.get(1).occurredAtEpochMillis);

        List<ReportTransactionRow> reportRows = database.transactionDao()
                .reportTransactionsForCategory(categoryId, 1_000, 2_000);
        assertEquals(2, reportRows.size());
        assertEquals(2_000L, reportRows.get(0).amountRupiah);
        assertEquals("Makan", reportRows.get(0).categoryName);
    }

    private long insert(String type, long amount, long occurredAt, String note, String merchant) {
        return database.transactionDao().insertOrdinary(new TransactionEntity(
                TransactionType.valueOf(type), walletId, amount, null, categoryId, occurredAt,
                note, merchant, null), true);
    }
}
