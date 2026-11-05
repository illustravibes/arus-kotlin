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
import static org.junit.Assert.assertNotEquals;

@RunWith(AndroidJUnit4.class)
public class CategoryDaoTest {
    private final Context context = ApplicationProvider.getApplicationContext();
    private ArusDatabase database;
    @Before public void setUp() {
        context.deleteDatabase("category-dao-test");
        database = Room.databaseBuilder(context, ArusDatabase.class, "category-dao-test")
                .addMigrations(ArusDatabase.MIGRATION_1_6, ArusDatabase.MIGRATION_2_6,
                        ArusDatabase.MIGRATION_3_6, ArusDatabase.MIGRATION_4_6, ArusDatabase.MIGRATION_5_6).build();
    }
    @After public void tearDown() { database.close(); context.deleteDatabase("category-dao-test"); }

    @Test public void transactionsMoveBeforeCategoryDeleteAndSystemCategoryCannotBeDeleted() {
        long walletId = database.walletDao().insert(new WalletEntity("Cash", "CASH", 0, true, false, null));
        CategoryDao dao = database.categoryDao();
        long sourceId = dao.insert(new CategoryEntity("Makan", "EXPENSE", "restaurant", 1, false, false, 0));
        long targetId = dao.insert(new CategoryEntity("Belanja", "EXPENSE", "shopping_bag", 2, false, false, 1));
        long systemId = dao.insert(new CategoryEntity("expense:admin", "EXPENSE", "more_horiz", 3, true, false, 2));
        TransactionEntity transaction = new TransactionEntity(com.illustravibes.arus.domain.TransactionType.EXPENSE,
                walletId, 250, null, 1, null);
        transaction.categoryId = sourceId;
        database.transactionDao().insert(transaction);
        database.runInTransaction(() -> {
            dao.moveTransactions(sourceId, targetId);
            assertEquals(1, dao.deleteCustom(sourceId));
        });
        assertEquals(Long.valueOf(targetId), database.transactionDao().activeTransactions().get(0).categoryId);
        assertNotEquals(1, dao.deleteCustom(systemId));
        assertEquals(systemId, dao.findById(systemId).id);
        assertEquals(0, dao.editCustom(systemId, "Bebas", "more_horiz", 7));
        assertEquals(0, dao.archive(systemId));
    }

    @Test public void archiveRetainsCategoryAndReferencedTransactions() {
        long walletId = database.walletDao().insert(new WalletEntity("Cash", "CASH", 0, true, false, null));
        CategoryDao dao = database.categoryDao();
        long categoryId = dao.insert(new CategoryEntity("Transport", "EXPENSE", "directions_bus", 1, false, false, 0));
        TransactionEntity transaction = new TransactionEntity(com.illustravibes.arus.domain.TransactionType.EXPENSE,
                walletId, 100, null, 1, null);
        transaction.categoryId = categoryId;
        database.transactionDao().insert(transaction);
        assertEquals(1, dao.archive(categoryId));
        assertEquals(1, dao.activeTransactionCount(categoryId));
        assertEquals(1, dao.findById(categoryId).isArchived ? 1 : 0);
    }
}
