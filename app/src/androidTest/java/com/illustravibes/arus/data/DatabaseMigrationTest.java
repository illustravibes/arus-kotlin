package com.illustravibes.arus.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.room.Room;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(AndroidJUnit4.class)
public class DatabaseMigrationTest {
    private static final String MIGRATION_DB = "migration-test.db";
    private static final String UNSUPPORTED_DB = "unsupported-migration-test.db";
    private static final String MIGRATION_V6_DB = "migration-v6-test.db";
    private static final String MIGRATION_V11_DB = "migration-v11-test.db";
    private static final String MIGRATION_V13_DB = "migration-v13-test.db";
    private static final String MIGRATION_V16_DB = "migration-v16-test.db";
    private static final String MIGRATION_V17_DB = "migration-v17-test.db";
    private static final String MIGRATION_V18_DB = "migration-v18-test.db";
    private static final String MIGRATION_V19_DB = "migration-v19-test.db";
    private static final String MIGRATION_V20_DB = "migration-v20-test.db";
    private final Context context = ApplicationProvider.getApplicationContext();

    @Before
    public void setUp() {
        context.deleteDatabase(MIGRATION_DB);
        context.deleteDatabase(UNSUPPORTED_DB);
        context.deleteDatabase(MIGRATION_V6_DB);
        context.deleteDatabase(MIGRATION_V11_DB);
        context.deleteDatabase(MIGRATION_V13_DB);
        context.deleteDatabase(MIGRATION_V16_DB);
        context.deleteDatabase(MIGRATION_V17_DB);
        context.deleteDatabase(MIGRATION_V18_DB);
        context.deleteDatabase(MIGRATION_V19_DB);
        context.deleteDatabase(MIGRATION_V20_DB);
    }

    @After
    public void tearDown() {
        context.deleteDatabase(MIGRATION_DB);
        context.deleteDatabase(UNSUPPORTED_DB);
        context.deleteDatabase(MIGRATION_V6_DB);
        context.deleteDatabase(MIGRATION_V11_DB);
        context.deleteDatabase(MIGRATION_V13_DB);
        context.deleteDatabase(MIGRATION_V16_DB);
        context.deleteDatabase(MIGRATION_V17_DB);
        context.deleteDatabase(MIGRATION_V18_DB);
        context.deleteDatabase(MIGRATION_V19_DB);
        context.deleteDatabase(MIGRATION_V20_DB);
    }

    @Test
    public void migration16To17AddsGroupOrderTablesAndPreservesDebtRows() {
        FrameworkSQLiteOpenHelperFactory factory = new FrameworkSQLiteOpenHelperFactory();
        androidx.sqlite.db.SupportSQLiteOpenHelper fixture = factory.create(
                new androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.Builder(context)
                        .name(MIGRATION_V16_DB)
                        .callback(new androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(16) {
                            @Override public void onCreate(SupportSQLiteDatabase db) {
                                db.execSQL("CREATE TABLE debts (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, contactId INTEGER NOT NULL, direction TEXT, source TEXT, originTransactionId INTEGER, principalRupiah INTEGER NOT NULL, status TEXT, dueDateEpochMillis INTEGER, note TEXT, createdAtEpochMillis INTEGER NOT NULL, updatedAtEpochMillis INTEGER NOT NULL, deletedAtEpochMillis INTEGER)");
                                db.execSQL("INSERT INTO debts (id,contactId,direction,source,principalRupiah,status,createdAtEpochMillis,updatedAtEpochMillis) VALUES (1,9,'LENT','MANUAL',2500,'OPEN',100,100)");
                            }
                            @Override public void onUpgrade(SupportSQLiteDatabase db, int oldVersion, int newVersion) { }
                        }).build());
        try {
            SupportSQLiteDatabase db = fixture.getWritableDatabase();
            ArusDatabase.MIGRATION_16_17.migrate(db);
            try (android.database.Cursor cursor = db.query("SELECT principalRupiah, groupOrderId FROM debts WHERE id=1")) {
                if (!cursor.moveToFirst()) throw new AssertionError("Debt fixture was lost during migration.");
                assertEquals(2500, cursor.getLong(0));
                if (!cursor.isNull(1)) throw new AssertionError("Existing manual debt must not gain a group order link.");
            }
            try (android.database.Cursor cursor = db.query("SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name LIKE 'group_order_%'")) {
                if (!cursor.moveToFirst() || cursor.getInt(0) < 5) throw new AssertionError("Group order tables were not created.");
            }
        } finally {
            fixture.close();
        }
    }

    @Test
    public void migration17To18CreatesAttachmentTableAndOwnerIndex() {
        FrameworkSQLiteOpenHelperFactory factory = new FrameworkSQLiteOpenHelperFactory();
        androidx.sqlite.db.SupportSQLiteOpenHelper fixture = factory.create(
                new androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.Builder(context)
                        .name(MIGRATION_V17_DB)
                        .callback(new androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(17) {
                            @Override public void onCreate(SupportSQLiteDatabase db) {
                                db.execSQL("CREATE TABLE preserved (id INTEGER PRIMARY KEY NOT NULL)");
                                db.execSQL("INSERT INTO preserved VALUES (42)");
                            }
                            @Override public void onUpgrade(SupportSQLiteDatabase db, int oldVersion, int newVersion) { }
                        }).build());
        try {
            SupportSQLiteDatabase db = fixture.getWritableDatabase();
            ArusDatabase.MIGRATION_17_18.migrate(db);
            db.execSQL("INSERT INTO attachments (ownerType, ownerId, filePath, ocrRawText, createdAtEpochMillis) VALUES ('PENDING', NULL, 'receipts/test.jpg', 'TOTAL 1200', 10)");
            try (android.database.Cursor cursor = db.query("SELECT filePath, ocrRawText FROM attachments WHERE ownerType='PENDING'")) {
                if (!cursor.moveToFirst()) throw new AssertionError("Pending receipt attachment was not saved.");
                assertEquals("receipts/test.jpg", cursor.getString(0));
                assertEquals("TOTAL 1200", cursor.getString(1));
            }
            try (android.database.Cursor cursor = db.query("SELECT id FROM preserved WHERE id=42")) {
                if (!cursor.moveToFirst()) throw new AssertionError("Existing data was lost during migration.");
            }
            try (android.database.Cursor cursor = db.query("SELECT COUNT(*) FROM sqlite_master WHERE type='index' AND name='index_attachments_ownerType_ownerId'")) {
                if (!cursor.moveToFirst() || cursor.getInt(0) != 1) throw new AssertionError("Attachment owner index was not created.");
            }
        } finally {
            fixture.close();
        }
    }

    @Test
    public void migration18To19CreatesMerchantCategoryHints() {
        FrameworkSQLiteOpenHelperFactory factory = new FrameworkSQLiteOpenHelperFactory();
        androidx.sqlite.db.SupportSQLiteOpenHelper fixture = factory.create(
                new androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.Builder(context)
                        .name(MIGRATION_V18_DB)
                        .callback(new androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(18) {
                            @Override public void onCreate(SupportSQLiteDatabase db) {
                                db.execSQL("CREATE TABLE categories (id INTEGER PRIMARY KEY NOT NULL)");
                                db.execSQL("INSERT INTO categories VALUES (5)");
                                db.execSQL("CREATE TABLE preserved (id INTEGER PRIMARY KEY NOT NULL)");
                                db.execSQL("INSERT INTO preserved VALUES (42)");
                            }
                            @Override public void onUpgrade(SupportSQLiteDatabase db, int oldVersion, int newVersion) { }
                        }).build());
        try {
            SupportSQLiteDatabase db = fixture.getWritableDatabase();
            ArusDatabase.MIGRATION_18_19.migrate(db);
            db.execSQL("INSERT INTO merchant_category_hints (merchantName, categoryId, useCount) VALUES ('WARUNG BUDI', 5, 1)");
            try (android.database.Cursor cursor = db.query("SELECT categoryId, useCount FROM merchant_category_hints WHERE merchantName='WARUNG BUDI'")) {
                if (!cursor.moveToFirst()) throw new AssertionError("Merchant hint was not stored.");
                assertEquals(5, cursor.getLong(0));
                assertEquals(1, cursor.getInt(1));
            }
            try (android.database.Cursor cursor = db.query("SELECT id FROM preserved WHERE id=42")) {
                if (!cursor.moveToFirst()) throw new AssertionError("Existing data was lost during migration.");
            }
        } finally {
            fixture.close();
        }
    }

    @Test
    public void migration19To20CreatesCashDenominationCounts() {
        FrameworkSQLiteOpenHelperFactory factory = new FrameworkSQLiteOpenHelperFactory();
        androidx.sqlite.db.SupportSQLiteOpenHelper fixture = factory.create(
                new androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.Builder(context)
                        .name(MIGRATION_V19_DB)
                        .callback(new androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(19) {
                            @Override public void onCreate(SupportSQLiteDatabase db) {
                                db.execSQL("CREATE TABLE reconciliation_checkpoints (id INTEGER PRIMARY KEY NOT NULL)");
                                db.execSQL("INSERT INTO reconciliation_checkpoints VALUES (9)");
                                db.execSQL("CREATE TABLE preserved (id INTEGER PRIMARY KEY NOT NULL)");
                                db.execSQL("INSERT INTO preserved VALUES (42)");
                            }
                            @Override public void onUpgrade(SupportSQLiteDatabase db, int oldVersion, int newVersion) { }
                        }).build());
        try {
            SupportSQLiteDatabase db = fixture.getWritableDatabase();
            ArusDatabase.MIGRATION_19_20.migrate(db);
            db.execSQL("INSERT INTO cash_denomination_counts (checkpointId, denominationRupiah, pieceCount) VALUES (9, 50000, 2)");
            try (android.database.Cursor cursor = db.query("SELECT denominationRupiah, pieceCount FROM cash_denomination_counts WHERE checkpointId=9")) {
                if (!cursor.moveToFirst()) throw new AssertionError("Cash denomination count was not stored.");
                assertEquals(50_000, cursor.getLong(0));
                assertEquals(2, cursor.getLong(1));
            }
            try (android.database.Cursor cursor = db.query("SELECT id FROM preserved WHERE id=42")) {
                if (!cursor.moveToFirst()) throw new AssertionError("Existing data was lost during migration.");
            }
        } finally {
            fixture.close();
        }
    }

    @Test
    public void migration20To21CreatesReconciliationItems() {
        FrameworkSQLiteOpenHelperFactory factory = new FrameworkSQLiteOpenHelperFactory();
        androidx.sqlite.db.SupportSQLiteOpenHelper fixture = factory.create(
                new androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.Builder(context)
                        .name(MIGRATION_V20_DB)
                        .callback(new androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(20) {
                            @Override public void onCreate(SupportSQLiteDatabase db) {
                                db.execSQL("CREATE TABLE reconciliation_checkpoints (id INTEGER PRIMARY KEY NOT NULL)");
                                db.execSQL("CREATE TABLE transactions (id INTEGER PRIMARY KEY NOT NULL)");
                                db.execSQL("INSERT INTO reconciliation_checkpoints VALUES (9)");
                                db.execSQL("INSERT INTO transactions VALUES (42)");
                            }
                            @Override public void onUpgrade(SupportSQLiteDatabase db, int oldVersion, int newVersion) { }
                        }).build());
        try {
            SupportSQLiteDatabase db = fixture.getWritableDatabase();
            ArusDatabase.MIGRATION_20_21.migrate(db);
            db.execSQL("INSERT INTO reconciliation_items (checkpointId, transactionId, matched) VALUES (9, 42, 1)");
            try (android.database.Cursor cursor = db.query("SELECT matched FROM reconciliation_items WHERE checkpointId=9 AND transactionId=42")) {
                if (!cursor.moveToFirst()) throw new AssertionError("Reconciliation item was not stored.");
                assertEquals(1, cursor.getInt(0));
            }
        } finally {
            fixture.close();
        }
    }

    @Test
    public void migration13To15CreatesRecurringAndDebtTables() {
        FrameworkSQLiteOpenHelperFactory factory = new FrameworkSQLiteOpenHelperFactory();
        androidx.sqlite.db.SupportSQLiteOpenHelper fixture = factory.create(new androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.Builder(context)
                .name(MIGRATION_V13_DB).callback(new androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(13) {
                    @Override public void onCreate(SupportSQLiteDatabase db) {
                        db.execSQL("CREATE TABLE wallets (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT, type TEXT, icon TEXT, colorArgb INTEGER NOT NULL DEFAULT -10453621, initialBalanceRupiah INTEGER NOT NULL, creditLimitRupiah INTEGER, billingDay INTEGER, sortOrder INTEGER NOT NULL, defaultExpenseCategoryId INTEGER, includeInTotal INTEGER NOT NULL, isArchived INTEGER NOT NULL, deletedAtEpochMillis INTEGER)");
                        db.execSQL("CREATE TABLE categories (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT, kind TEXT, icon TEXT, colorArgb INTEGER NOT NULL, isSystem INTEGER NOT NULL, isArchived INTEGER NOT NULL, sortOrder INTEGER NOT NULL)");
                        db.execSQL("CREATE UNIQUE INDEX index_categories_kind_name ON categories(kind,name)");
                        db.execSQL("CREATE TABLE transactions (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, type TEXT, walletId INTEGER, amountRupiah INTEGER NOT NULL, direction TEXT, categoryId INTEGER, occurredAtEpochMillis INTEGER NOT NULL, note TEXT, merchant TEXT, transferGroupId INTEGER, verifiedCheckpointId INTEGER, needsRecheck INTEGER NOT NULL, deletedAtEpochMillis INTEGER, FOREIGN KEY(walletId) REFERENCES wallets(id) ON DELETE RESTRICT, FOREIGN KEY(categoryId) REFERENCES categories(id) ON DELETE RESTRICT)");
                        db.execSQL("CREATE INDEX index_transactions_walletId ON transactions(walletId)");
                        db.execSQL("CREATE INDEX index_transactions_categoryId ON transactions(categoryId)");
                        db.execSQL("CREATE INDEX index_transactions_occurredAtEpochMillis_id ON transactions(occurredAtEpochMillis,id)");
                        db.execSQL("CREATE INDEX index_transactions_transferGroupId ON transactions(transferGroupId)");
                        db.execSQL("CREATE INDEX index_transactions_verifiedCheckpointId ON transactions(verifiedCheckpointId)");
                        db.execSQL("CREATE TABLE transaction_splits (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, transactionId INTEGER NOT NULL, amountRupiah INTEGER NOT NULL, categoryId INTEGER, countsInReport INTEGER NOT NULL, countsInBudget INTEGER NOT NULL, FOREIGN KEY(transactionId) REFERENCES transactions(id) ON DELETE CASCADE, FOREIGN KEY(categoryId) REFERENCES categories(id) ON DELETE RESTRICT)");
                        db.execSQL("CREATE INDEX index_transaction_splits_transactionId ON transaction_splits(transactionId)");
                        db.execSQL("CREATE INDEX index_transaction_splits_categoryId ON transaction_splits(categoryId)");
                        db.execSQL("CREATE TABLE reconciliation_checkpoints (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, walletId INTEGER NOT NULL, occurredAtEpochMillis INTEGER NOT NULL, recordedBalanceRupiah INTEGER NOT NULL, actualBalanceRupiah INTEGER NOT NULL, differenceRupiah INTEGER NOT NULL, resolution TEXT, needsRecheck INTEGER NOT NULL, FOREIGN KEY(walletId) REFERENCES wallets(id) ON UPDATE NO ACTION ON DELETE RESTRICT)");
                        db.execSQL("CREATE INDEX index_reconciliation_checkpoints_walletId ON reconciliation_checkpoints(walletId)");
                        db.execSQL("CREATE INDEX index_reconciliation_checkpoints_occurredAtEpochMillis ON reconciliation_checkpoints(occurredAtEpochMillis)");
                        db.execSQL("CREATE TABLE budgets (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT DEFAULT '', categoryId INTEGER, amountRupiah INTEGER NOT NULL, periodType INTEGER NOT NULL, periodStartEpochDay INTEGER NOT NULL, periodEndEpochDay INTEGER NOT NULL, rolloverEnabled INTEGER NOT NULL, rolloverRupiah INTEGER NOT NULL, warningPercent INTEGER NOT NULL, isArchived INTEGER NOT NULL, FOREIGN KEY(categoryId) REFERENCES categories(id) ON UPDATE NO ACTION ON DELETE RESTRICT)");
                        db.execSQL("CREATE UNIQUE INDEX index_budgets_categoryId_periodStartEpochDay_periodEndEpochDay ON budgets(categoryId,periodStartEpochDay,periodEndEpochDay)");
                        db.execSQL("INSERT INTO wallets (id,name,type,icon,colorArgb,initialBalanceRupiah,sortOrder,includeInTotal,isArchived) VALUES (1,'Tunai','CASH','wallet',-10453621,0,0,1,0)");
                        db.execSQL("INSERT INTO categories VALUES (1,'Makan','EXPENSE','restaurant',0,0,0,0)");
                    }
                    @Override public void onUpgrade(SupportSQLiteDatabase db, int oldVersion, int newVersion) { }
                }).build());
        try { fixture.getWritableDatabase(); } finally { fixture.close(); }
        ArusDatabase migrated = Room.databaseBuilder(context, ArusDatabase.class, MIGRATION_V13_DB)
                .addMigrations(ArusDatabase.MIGRATION_20_21, ArusDatabase.MIGRATION_19_20,
                        ArusDatabase.MIGRATION_13_15, ArusDatabase.MIGRATION_15_16,
                        ArusDatabase.MIGRATION_19_20,
                        ArusDatabase.MIGRATION_18_19,
                        ArusDatabase.MIGRATION_17_18,
                        ArusDatabase.MIGRATION_16_17).build();
        try {
            long ruleId = migrated.recurringDao().createRule(new RecurringRuleEntity("Sewa", "EXPENSE", 1, 1,
                    1000, null, 1, 20000, null, true, true));
            assertEquals(1, migrated.recurringDao().catchUp(20000));
            assertEquals(1, migrated.recurringDao().pendingOccurrences().size());
            assertEquals(0, migrated.recurringDao().catchUp(20000));
            assertEquals(1, ruleId);
            assertEquals(1, migrated.recurringDao().resolvePending(
                    migrated.recurringDao().pendingOccurrences().get(0).id, "POST", 1200L) ? 1 : 0);
            assertEquals(0, migrated.recurringDao().pendingOccurrences().size());
            assertEquals(1, migrated.transactionDao().activeTransactions().size());
            assertEquals(1200, migrated.transactionDao().activeTransactions().get(0).amountRupiah);
            assertEquals(1, migrated.transactionSplitDao().splitsForTransaction(
                    migrated.transactionDao().activeTransactions().get(0).id).size());
            assertEquals(0, migrated.debtDao().activeContacts().size());
            try (android.database.Cursor cursor = migrated.getOpenHelper().getWritableDatabase().query("SELECT 1 FROM categories WHERE name='Utang Dihapuskan' AND kind='INCOME' AND isSystem=1")) {
                if (!cursor.moveToFirst()) throw new AssertionError("System category for forgiven debt was not seeded.");
            }
            try (android.database.Cursor cursor = migrated.getOpenHelper().getWritableDatabase().query("SELECT COUNT(*) FROM sqlite_master WHERE type='trigger' AND name LIKE 'repayment_allocation_validate_%'")) {
                if (!cursor.moveToFirst() || cursor.getInt(0) < 4) throw new AssertionError("Allocation integrity triggers were not migrated.");
            }
        } finally {
            migrated.close();
        }
    }


    @Test
    public void migration1To2PreservesWalletAndTransactionFixture() {
        createVersionOneFixture(MIGRATION_DB);
        ArusDatabase migrated = Room.databaseBuilder(context, ArusDatabase.class, MIGRATION_DB)
                .addMigrations(ArusDatabase.MIGRATION_20_21, ArusDatabase.MIGRATION_19_20,
                        ArusDatabase.MIGRATION_1_15, ArusDatabase.MIGRATION_15_16,
                        ArusDatabase.MIGRATION_19_20,
                        ArusDatabase.MIGRATION_18_19,
                        ArusDatabase.MIGRATION_17_18,
                        ArusDatabase.MIGRATION_16_17)
                .build();
        try {
            assertEquals(1000, migrated.walletDao().initialBalance(10).longValue());
            assertEquals(1, migrated.transactionDao().activeTransactions().size());
            assertEquals(20, migrated.transactionDao().activeTransactions().get(0).id);
            assertEquals(null, migrated.transactionDao().activeTransactions().get(0).transferGroupId);
        } finally {
            migrated.close();
        }
    }

    @Test
    public void unsupportedMigrationFailsWithoutDeletingFixture() {
        createVersionOneFixture(UNSUPPORTED_DB);
        SQLiteDatabase before = SQLiteDatabase.openDatabase(
                context.getDatabasePath(UNSUPPORTED_DB).getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);
        try (android.database.Cursor cursor = before.rawQuery(
                "SELECT initialBalanceRupiah FROM wallets WHERE id = 10", null)) {
            if (!cursor.moveToFirst()) throw new AssertionError("Fixture setup failed.");
            assertEquals(1000, cursor.getLong(0));
        }
        before.close();
        ArusDatabase unsupported = Room.databaseBuilder(context, ArusDatabase.class, UNSUPPORTED_DB)
                .addMigrations(ArusDatabase.MIGRATION_14_15).build();
        try {
            unsupported.getOpenHelper().getWritableDatabase();
            throw new AssertionError("Opening without a migration must fail.");
        } catch (IllegalStateException expected) {
            SQLiteDatabase preserved = SQLiteDatabase.openDatabase(
                    context.getDatabasePath(UNSUPPORTED_DB).getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);
            try (android.database.Cursor cursor = preserved.rawQuery(
                    "SELECT initialBalanceRupiah FROM wallets WHERE id = 10", null)) {
                if (!cursor.moveToFirst()) throw new AssertionError("Fixture wallet was deleted.");
                assertEquals(1000, cursor.getLong(0));
            }
            preserved.close();
        } finally {
            unsupported.close();
        }
    }

    @Test
    public void migration11To12AddsBalancedSplitsForExistingOrdinaryTransactions() {
        String name = MIGRATION_V11_DB;
        FrameworkSQLiteOpenHelperFactory factory = new FrameworkSQLiteOpenHelperFactory();
        androidx.sqlite.db.SupportSQLiteOpenHelper fixture = factory.create(new androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.Builder(context)
                .name(name).callback(new androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(11) {
                    @Override public void onCreate(SupportSQLiteDatabase db) {
                        db.execSQL("CREATE TABLE wallets (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT, type TEXT, icon TEXT, colorArgb INTEGER NOT NULL DEFAULT -10453621, initialBalanceRupiah INTEGER NOT NULL, creditLimitRupiah INTEGER, billingDay INTEGER, sortOrder INTEGER NOT NULL, defaultExpenseCategoryId INTEGER, includeInTotal INTEGER NOT NULL, isArchived INTEGER NOT NULL, deletedAtEpochMillis INTEGER)");
                        db.execSQL("CREATE TABLE categories (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT, kind TEXT, icon TEXT, colorArgb INTEGER NOT NULL, isSystem INTEGER NOT NULL, isArchived INTEGER NOT NULL, sortOrder INTEGER NOT NULL)");
                        db.execSQL("CREATE TABLE transactions (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, type TEXT, walletId INTEGER, amountRupiah INTEGER NOT NULL, direction TEXT, categoryId INTEGER, occurredAtEpochMillis INTEGER NOT NULL, note TEXT, merchant TEXT, transferGroupId INTEGER, verifiedCheckpointId INTEGER, needsRecheck INTEGER NOT NULL, deletedAtEpochMillis INTEGER)");
                        db.execSQL("CREATE TABLE reconciliation_checkpoints (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, walletId INTEGER NOT NULL, occurredAtEpochMillis INTEGER NOT NULL, recordedBalanceRupiah INTEGER NOT NULL, actualBalanceRupiah INTEGER NOT NULL, differenceRupiah INTEGER NOT NULL, resolution TEXT, needsRecheck INTEGER NOT NULL DEFAULT 0)");
                        db.execSQL("INSERT INTO categories VALUES (1,'Makan','EXPENSE','restaurant',0,0,0,0)");
                        db.execSQL("INSERT INTO transactions (id,type,amountRupiah,categoryId,occurredAtEpochMillis,needsRecheck) VALUES (1,'EXPENSE',250,1,1234,0)");
                    }
                    @Override public void onUpgrade(SupportSQLiteDatabase db, int oldVersion, int newVersion) { }
                }).build());
        try { fixture.getWritableDatabase(); } finally { fixture.close(); }
        SupportSQLiteDatabase migrated = fixture.getWritableDatabase();
        try {
            ArusDatabase.MIGRATION_11_12.migrate(migrated);
            try (android.database.Cursor cursor = migrated.query("SELECT amountRupiah, categoryId, countsInReport, countsInBudget FROM transaction_splits WHERE transactionId = 1")) {
                if (!cursor.moveToFirst()) throw new AssertionError("Migration did not create the transaction split.");
                assertEquals(250, cursor.getLong(0));
                assertEquals(1, cursor.getLong(1));
                assertEquals(1, cursor.getInt(2));
                assertEquals(1, cursor.getInt(3));
            }
        } finally {
            fixture.close();
            context.deleteDatabase(name);
        }
    }

    @Test
    public void migration6To7PreservesRowsAndAddsTransactionMetadata() {
        String name = MIGRATION_V6_DB;
        context.deleteDatabase(name);
        FrameworkSQLiteOpenHelperFactory factory = new FrameworkSQLiteOpenHelperFactory();
        androidx.sqlite.db.SupportSQLiteOpenHelper fixture = factory.create(new androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.Builder(context)
                .name(name).callback(new androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(6) {
                    @Override public void onCreate(SupportSQLiteDatabase db) {
                        db.execSQL("CREATE TABLE wallets (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT, type TEXT, icon TEXT, colorArgb INTEGER NOT NULL DEFAULT -10453621, initialBalanceRupiah INTEGER NOT NULL, creditLimitRupiah INTEGER, billingDay INTEGER, sortOrder INTEGER NOT NULL, defaultExpenseCategoryId INTEGER, includeInTotal INTEGER NOT NULL, isArchived INTEGER NOT NULL, deletedAtEpochMillis INTEGER)");
                        db.execSQL("CREATE TABLE categories (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT, kind TEXT, icon TEXT, colorArgb INTEGER NOT NULL, isSystem INTEGER NOT NULL, isArchived INTEGER NOT NULL, sortOrder INTEGER NOT NULL)");
                        db.execSQL("CREATE UNIQUE INDEX index_categories_kind_name ON categories (kind, name)");
                        db.execSQL("CREATE TABLE transactions (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, type TEXT, walletId INTEGER, amountRupiah INTEGER NOT NULL, direction TEXT, categoryId INTEGER, occurredAtEpochMillis INTEGER NOT NULL, deletedAtEpochMillis INTEGER, FOREIGN KEY(walletId) REFERENCES wallets(id) ON UPDATE NO ACTION ON DELETE RESTRICT, FOREIGN KEY(categoryId) REFERENCES categories(id) ON UPDATE NO ACTION ON DELETE RESTRICT)");
                        db.execSQL("CREATE INDEX index_transactions_walletId ON transactions(walletId)");
                        db.execSQL("CREATE INDEX index_transactions_categoryId ON transactions(categoryId)");
                        db.execSQL("CREATE INDEX index_transactions_occurredAtEpochMillis_id ON transactions(occurredAtEpochMillis, id)");
                        db.execSQL("INSERT INTO wallets VALUES (1,'Tunai','CASH','wallet',-10453621,700,NULL,NULL,0,NULL,1,0,NULL)");
                        db.execSQL("INSERT INTO categories VALUES (1,'Makan','EXPENSE','restaurant',0,0,0,0)");
                        db.execSQL("INSERT INTO transactions (id,type,walletId,amountRupiah,direction,categoryId,occurredAtEpochMillis,deletedAtEpochMillis) VALUES (1,'EXPENSE',1,100,NULL,1,10,NULL)");
                    }
                    @Override public void onUpgrade(SupportSQLiteDatabase db, int oldVersion, int newVersion) { }
                }).build());
        try {
            fixture.getWritableDatabase();
        } finally { fixture.close(); }
        ArusDatabase migrated = Room.databaseBuilder(context, ArusDatabase.class, name)
                .addMigrations(ArusDatabase.MIGRATION_20_21, ArusDatabase.MIGRATION_19_20,
                        ArusDatabase.MIGRATION_6_15, ArusDatabase.MIGRATION_15_16,
                        ArusDatabase.MIGRATION_19_20,
                        ArusDatabase.MIGRATION_18_19,
                        ArusDatabase.MIGRATION_17_18,
                        ArusDatabase.MIGRATION_16_17).build();
        try {
            TransactionEntity row = migrated.transactionDao().activeTransactions().get(0);
            assertEquals(100, row.amountRupiah);
            assertEquals(null, row.note);
            assertEquals(null, row.merchant);
            assertEquals(null, row.transferGroupId);
        } finally {
            migrated.close();
            context.deleteDatabase(name);
        }
    }

    private void createVersionOneFixture(String name) {
        SQLiteOpenHelper helper = new SQLiteOpenHelper(context, name, null, 1) {
            @Override
            public void onCreate(SQLiteDatabase db) {
                db.execSQL("CREATE TABLE wallets (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT, type TEXT, " +
                        "initialBalanceRupiah INTEGER NOT NULL, includeInTotal INTEGER NOT NULL, isArchived INTEGER NOT NULL, " +
                        "deletedAtEpochMillis INTEGER)");
                db.execSQL("CREATE TABLE transactions (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, type TEXT, walletId INTEGER, " +
                        "amountRupiah INTEGER NOT NULL, direction TEXT, occurredAtEpochMillis INTEGER NOT NULL, " +
                        "deletedAtEpochMillis INTEGER, FOREIGN KEY(walletId) REFERENCES wallets(id) ON DELETE RESTRICT)");
                db.execSQL("CREATE INDEX index_transactions_walletId ON transactions(walletId)");
                db.execSQL("INSERT INTO wallets VALUES (10, 'Tunai', 'CASH', 1000, 1, 0, NULL)");
                db.execSQL("INSERT INTO transactions VALUES (20, 'EXPENSE', 10, 250, NULL, 1234, NULL)");
            }
            @Override
            public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}
        };
        helper.getWritableDatabase().close();
        helper.close();
    }

    private static final class SupportSQLiteOpenHelperConfiguration {
        static androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration forName(Context context, String name) {
            return androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
                    .name(name)
                    .callback(new androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(1) {
                        @Override public void onCreate(SupportSQLiteDatabase db) {}
                        @Override public void onUpgrade(SupportSQLiteDatabase db, int oldVersion, int newVersion) {}
                    }).build();
        }
    }
}
