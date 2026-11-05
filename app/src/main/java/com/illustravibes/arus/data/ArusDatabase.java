package com.illustravibes.arus.data;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {WalletEntity.class, TransactionEntity.class, CategoryEntity.class, ReconciliationCheckpointEntity.class, TransactionSplitEntity.class, BudgetEntity.class, RecurringRuleEntity.class, RecurringOccurrenceEntity.class}, version = 14, exportSchema = true)
@TypeConverters(TransactionTypeConverter.class)
public abstract class ArusDatabase extends RoomDatabase {
    public static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE wallets ADD COLUMN icon TEXT");
            database.execSQL("ALTER TABLE wallets ADD COLUMN colorArgb INTEGER NOT NULL DEFAULT -10453621");
            database.execSQL("CREATE TABLE IF NOT EXISTS categories (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT, kind TEXT, icon TEXT, colorArgb INTEGER NOT NULL, isSystem INTEGER NOT NULL, isArchived INTEGER NOT NULL)");
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_categories_kind_name ON categories (kind, name)");
        }
    };
    public static final Migration MIGRATION_1_2_3 = new Migration(1, 3) {
        @Override public void migrate(SupportSQLiteDatabase database) {
            MIGRATION_1_2.migrate(database);
            MIGRATION_2_3.migrate(database);
        }
    };
    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_occurredAtEpochMillis_id " +
                    "ON transactions (occurredAtEpochMillis, id)");
        }
    };
    public static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE transactions ADD COLUMN categoryId INTEGER REFERENCES categories(id) ON DELETE RESTRICT");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_categoryId ON transactions (categoryId)");
            database.execSQL("ALTER TABLE categories ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE wallets ADD COLUMN defaultExpenseCategoryId INTEGER");
        }
    };
    public static final Migration MIGRATION_3_5 = new Migration(3, 5) {
        @Override public void migrate(SupportSQLiteDatabase database) {
            MIGRATION_3_4.migrate(database);
            MIGRATION_4_5.migrate(database);
        }
    };
    public static final Migration MIGRATION_2_5 = new Migration(2, 5) {
        @Override public void migrate(SupportSQLiteDatabase database) {
            MIGRATION_2_4.migrate(database);
            MIGRATION_4_5.migrate(database);
        }
    };
    public static final Migration MIGRATION_1_5 = new Migration(1, 5) {
        @Override public void migrate(SupportSQLiteDatabase database) {
            MIGRATION_1_4.migrate(database);
            MIGRATION_4_5.migrate(database);
        }
    };
    public static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("DROP INDEX IF EXISTS index_categories_kind_name");
            database.execSQL("CREATE TABLE categories_new (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT, kind TEXT, icon TEXT, colorArgb INTEGER NOT NULL, isSystem INTEGER NOT NULL, isArchived INTEGER NOT NULL, sortOrder INTEGER NOT NULL)");
            database.execSQL("INSERT INTO categories_new (id,name,kind,icon,colorArgb,isSystem,isArchived,sortOrder) SELECT id,name,kind,icon,colorArgb,isSystem,isArchived,sortOrder FROM categories");
            database.execSQL("DROP TABLE categories");
            database.execSQL("ALTER TABLE categories_new RENAME TO categories");
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_categories_kind_name ON categories (kind, name)");
        }
    };
    public static final Migration MIGRATION_6_7 = new Migration(6, 7) {
        @Override public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE transactions ADD COLUMN note TEXT");
            database.execSQL("ALTER TABLE transactions ADD COLUMN merchant TEXT");
        }
    };
    public static final Migration MIGRATION_7_8 = new Migration(7, 8) {
        @Override public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE transactions ADD COLUMN transferGroupId INTEGER");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_transferGroupId ON transactions (transferGroupId)");
        }
    };
    public static final Migration MIGRATION_8_9 = new Migration(8, 9) {
        @Override public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE transactions ADD COLUMN verifiedCheckpointId INTEGER");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_verifiedCheckpointId ON transactions (verifiedCheckpointId)");
        }
    };
    public static final Migration MIGRATION_9_10 = new Migration(9, 10) {
        @Override public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE transactions ADD COLUMN needsRecheck INTEGER NOT NULL DEFAULT 0");
        }
    };
    public static final Migration MIGRATION_10_11 = new Migration(10, 11) {
        @Override public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS reconciliation_checkpoints (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, walletId INTEGER NOT NULL, occurredAtEpochMillis INTEGER NOT NULL, recordedBalanceRupiah INTEGER NOT NULL, actualBalanceRupiah INTEGER NOT NULL, differenceRupiah INTEGER NOT NULL, resolution TEXT, needsRecheck INTEGER NOT NULL, FOREIGN KEY(walletId) REFERENCES wallets(id) ON UPDATE NO ACTION ON DELETE RESTRICT)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_reconciliation_checkpoints_walletId ON reconciliation_checkpoints (walletId)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_reconciliation_checkpoints_occurredAtEpochMillis ON reconciliation_checkpoints (occurredAtEpochMillis)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_verifiedCheckpointId ON transactions (verifiedCheckpointId)");
        }
    };
    public static final Migration MIGRATION_11_12 = new Migration(11, 12) {
        @Override public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS transaction_splits (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, transactionId INTEGER NOT NULL, amountRupiah INTEGER NOT NULL, categoryId INTEGER, countsInReport INTEGER NOT NULL, countsInBudget INTEGER NOT NULL, FOREIGN KEY(transactionId) REFERENCES transactions(id) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(categoryId) REFERENCES categories(id) ON UPDATE NO ACTION ON DELETE RESTRICT)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_transaction_splits_transactionId ON transaction_splits (transactionId)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_transaction_splits_categoryId ON transaction_splits (categoryId)");
            database.execSQL("INSERT INTO transaction_splits (transactionId, amountRupiah, categoryId, countsInReport, countsInBudget) SELECT id, amountRupiah, categoryId, 1, 1 FROM transactions WHERE type IN ('INCOME','EXPENSE') AND categoryId IS NOT NULL");
        }
    };
    public static final Migration MIGRATION_1_9 = new Migration(1, 9) {
        @Override public void migrate(SupportSQLiteDatabase database) {
            MIGRATION_1_8.migrate(database);
            MIGRATION_8_9.migrate(database);
        }
    };
    public static final Migration MIGRATION_2_9 = new Migration(2, 9) {
        @Override public void migrate(SupportSQLiteDatabase database) {
            MIGRATION_2_8.migrate(database);
            MIGRATION_8_9.migrate(database);
        }
    };
    public static final Migration MIGRATION_3_9 = new Migration(3, 9) {
        @Override public void migrate(SupportSQLiteDatabase database) {
            MIGRATION_3_8.migrate(database);
            MIGRATION_8_9.migrate(database);
        }
    };
    public static final Migration MIGRATION_4_9 = new Migration(4, 9) {
        @Override public void migrate(SupportSQLiteDatabase database) {
            MIGRATION_4_8.migrate(database);
            MIGRATION_8_9.migrate(database);
        }
    };
    public static final Migration MIGRATION_5_9 = new Migration(5, 9) {
        @Override public void migrate(SupportSQLiteDatabase database) {
            MIGRATION_5_8.migrate(database);
            MIGRATION_8_9.migrate(database);
        }
    };
    public static final Migration MIGRATION_6_9 = new Migration(6, 9) {
        @Override public void migrate(SupportSQLiteDatabase database) {
            MIGRATION_6_8.migrate(database);
            MIGRATION_8_9.migrate(database);
        }
    };
    public static final Migration MIGRATION_7_9 = new Migration(7, 9) {
        @Override public void migrate(SupportSQLiteDatabase database) {
            MIGRATION_7_8.migrate(database);
            MIGRATION_8_9.migrate(database);
        }
    };
    public static final Migration MIGRATION_1_11 = new Migration(1, 11) { @Override public void migrate(SupportSQLiteDatabase db) { MIGRATION_1_10.migrate(db); MIGRATION_10_11.migrate(db); } };
    public static final Migration MIGRATION_2_11 = new Migration(2, 11) { @Override public void migrate(SupportSQLiteDatabase db) { MIGRATION_2_10.migrate(db); MIGRATION_10_11.migrate(db); } };
    public static final Migration MIGRATION_3_11 = new Migration(3, 11) { @Override public void migrate(SupportSQLiteDatabase db) { MIGRATION_3_10.migrate(db); MIGRATION_10_11.migrate(db); } };
    public static final Migration MIGRATION_4_11 = new Migration(4, 11) { @Override public void migrate(SupportSQLiteDatabase db) { MIGRATION_4_10.migrate(db); MIGRATION_10_11.migrate(db); } };
    public static final Migration MIGRATION_5_11 = new Migration(5, 11) { @Override public void migrate(SupportSQLiteDatabase db) { MIGRATION_5_10.migrate(db); MIGRATION_10_11.migrate(db); } };
    public static final Migration MIGRATION_6_11 = new Migration(6, 11) { @Override public void migrate(SupportSQLiteDatabase db) { MIGRATION_6_10.migrate(db); MIGRATION_10_11.migrate(db); } };
    public static final Migration MIGRATION_7_11 = new Migration(7, 11) { @Override public void migrate(SupportSQLiteDatabase db) { MIGRATION_7_10.migrate(db); MIGRATION_10_11.migrate(db); } };
    public static final Migration MIGRATION_8_11 = new Migration(8, 11) { @Override public void migrate(SupportSQLiteDatabase db) { MIGRATION_8_10.migrate(db); MIGRATION_10_11.migrate(db); } };
    public static final Migration MIGRATION_9_11 = new Migration(9, 11) { @Override public void migrate(SupportSQLiteDatabase db) { MIGRATION_9_10.migrate(db); MIGRATION_10_11.migrate(db); } };
    public static final Migration MIGRATION_1_12 = new Migration(1, 12) { @Override public void migrate(SupportSQLiteDatabase db) { MIGRATION_1_11.migrate(db); MIGRATION_11_12.migrate(db); } };
    public static final Migration MIGRATION_2_12 = new Migration(2, 12) { @Override public void migrate(SupportSQLiteDatabase db) { MIGRATION_2_11.migrate(db); MIGRATION_11_12.migrate(db); } };
    public static final Migration MIGRATION_3_12 = new Migration(3, 12) { @Override public void migrate(SupportSQLiteDatabase db) { MIGRATION_3_11.migrate(db); MIGRATION_11_12.migrate(db); } };
    public static final Migration MIGRATION_4_12 = new Migration(4, 12) { @Override public void migrate(SupportSQLiteDatabase db) { MIGRATION_4_11.migrate(db); MIGRATION_11_12.migrate(db); } };
    public static final Migration MIGRATION_5_12 = new Migration(5, 12) { @Override public void migrate(SupportSQLiteDatabase db) { MIGRATION_5_11.migrate(db); MIGRATION_11_12.migrate(db); } };
    public static final Migration MIGRATION_6_12 = new Migration(6, 12) { @Override public void migrate(SupportSQLiteDatabase db) { MIGRATION_6_11.migrate(db); MIGRATION_11_12.migrate(db); } };
    public static final Migration MIGRATION_7_12 = new Migration(7, 12) { @Override public void migrate(SupportSQLiteDatabase db) { MIGRATION_7_11.migrate(db); MIGRATION_11_12.migrate(db); } };
    public static final Migration MIGRATION_8_12 = new Migration(8, 12) { @Override public void migrate(SupportSQLiteDatabase db) { MIGRATION_8_11.migrate(db); MIGRATION_11_12.migrate(db); } };
    public static final Migration MIGRATION_9_12 = new Migration(9, 12) { @Override public void migrate(SupportSQLiteDatabase db) { MIGRATION_9_11.migrate(db); MIGRATION_11_12.migrate(db); } };
    public static final Migration MIGRATION_10_12 = new Migration(10, 12) { @Override public void migrate(SupportSQLiteDatabase db) { MIGRATION_10_11.migrate(db); MIGRATION_11_12.migrate(db); } };
    public static final Migration MIGRATION_12_13 = new Migration(12, 13) {
        @Override public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS budgets (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT DEFAULT '', categoryId INTEGER, amountRupiah INTEGER NOT NULL, periodType INTEGER NOT NULL, periodStartEpochDay INTEGER NOT NULL, periodEndEpochDay INTEGER NOT NULL, rolloverEnabled INTEGER NOT NULL, rolloverRupiah INTEGER NOT NULL, warningPercent INTEGER NOT NULL, isArchived INTEGER NOT NULL, FOREIGN KEY(categoryId) REFERENCES categories(id) ON UPDATE NO ACTION ON DELETE RESTRICT)");
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_budgets_categoryId_periodStartEpochDay_periodEndEpochDay ON budgets (categoryId, periodStartEpochDay, periodEndEpochDay)");
        }
    };
    public static final Migration MIGRATION_13_14 = new Migration(13, 14) {
        @Override public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS recurring_rules (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT, transactionType TEXT, walletId INTEGER NOT NULL, categoryId INTEGER NOT NULL, amountRupiah INTEGER NOT NULL, note TEXT, intervalType INTEGER NOT NULL, nextOccurrenceEpochDay INTEGER NOT NULL, endEpochDay INTEGER, confirmationRequired INTEGER NOT NULL, isActive INTEGER NOT NULL, FOREIGN KEY(walletId) REFERENCES wallets(id) ON UPDATE NO ACTION ON DELETE RESTRICT, FOREIGN KEY(categoryId) REFERENCES categories(id) ON UPDATE NO ACTION ON DELETE RESTRICT)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_recurring_rules_walletId ON recurring_rules (walletId)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_recurring_rules_categoryId ON recurring_rules (categoryId)");
            database.execSQL("CREATE TABLE IF NOT EXISTS recurring_occurrences (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, ruleId INTEGER NOT NULL, occurrenceEpochDay INTEGER NOT NULL, status TEXT, transactionId INTEGER, FOREIGN KEY(ruleId) REFERENCES recurring_rules(id) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(transactionId) REFERENCES transactions(id) ON UPDATE NO ACTION ON DELETE SET NULL)");
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_recurring_occurrences_ruleId_occurrenceEpochDay ON recurring_occurrences (ruleId, occurrenceEpochDay)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_recurring_occurrences_transactionId ON recurring_occurrences (transactionId)");
        }
    };
    public static final Migration MIGRATION_1_13 = new Migration(1, 13) { @Override public void migrate(SupportSQLiteDatabase db) { MIGRATION_1_12.migrate(db); MIGRATION_12_13.migrate(db); } };
    public static final Migration MIGRATION_2_13 = new Migration(2, 13) { @Override public void migrate(SupportSQLiteDatabase db) { MIGRATION_2_12.migrate(db); MIGRATION_12_13.migrate(db); } };
    public static final Migration MIGRATION_3_13 = new Migration(3, 13) { @Override public void migrate(SupportSQLiteDatabase db) { MIGRATION_3_12.migrate(db); MIGRATION_12_13.migrate(db); } };
    public static final Migration MIGRATION_4_13 = new Migration(4, 13) { @Override public void migrate(SupportSQLiteDatabase db) { MIGRATION_4_12.migrate(db); MIGRATION_12_13.migrate(db); } };
    public static final Migration MIGRATION_5_13 = new Migration(5, 13) { @Override public void migrate(SupportSQLiteDatabase db) { MIGRATION_5_12.migrate(db); MIGRATION_12_13.migrate(db); } };
    public static final Migration MIGRATION_6_13 = new Migration(6, 13) { @Override public void migrate(SupportSQLiteDatabase db) { MIGRATION_6_12.migrate(db); MIGRATION_12_13.migrate(db); } };
    public static final Migration MIGRATION_7_13 = new Migration(7, 13) { @Override public void migrate(SupportSQLiteDatabase db) { MIGRATION_7_12.migrate(db); MIGRATION_12_13.migrate(db); } };
    public static final Migration MIGRATION_8_13 = new Migration(8, 13) { @Override public void migrate(SupportSQLiteDatabase db) { MIGRATION_8_12.migrate(db); MIGRATION_12_13.migrate(db); } };
    public static final Migration MIGRATION_9_13 = new Migration(9, 13) { @Override public void migrate(SupportSQLiteDatabase db) { MIGRATION_9_12.migrate(db); MIGRATION_12_13.migrate(db); } };
    public static final Migration MIGRATION_10_13 = new Migration(10, 13) { @Override public void migrate(SupportSQLiteDatabase db) { MIGRATION_10_12.migrate(db); MIGRATION_12_13.migrate(db); } };
    public static final Migration MIGRATION_11_13 = new Migration(11, 13) { @Override public void migrate(SupportSQLiteDatabase db) { MIGRATION_11_12.migrate(db); MIGRATION_12_13.migrate(db); } };
    public static final Migration MIGRATION_12_14 = new Migration(12, 14) { @Override public void migrate(SupportSQLiteDatabase db) { MIGRATION_12_13.migrate(db); MIGRATION_13_14.migrate(db); } };
    public static final Migration MIGRATION_1_14 = new Migration(1, 14) { @Override public void migrate(SupportSQLiteDatabase db) { MIGRATION_1_13.migrate(db); MIGRATION_13_14.migrate(db); } };
    public static final Migration MIGRATION_2_14 = new Migration(2, 14) { @Override public void migrate(SupportSQLiteDatabase db) { MIGRATION_2_13.migrate(db); MIGRATION_13_14.migrate(db); } };
    public static final Migration MIGRATION_3_14 = new Migration(3, 14) { @Override public void migrate(SupportSQLiteDatabase db) { MIGRATION_3_13.migrate(db); MIGRATION_13_14.migrate(db); } };
    public static final Migration MIGRATION_4_14 = new Migration(4, 14) { @Override public void migrate(SupportSQLiteDatabase db) { MIGRATION_4_13.migrate(db); MIGRATION_13_14.migrate(db); } };
    public static final Migration MIGRATION_5_14 = new Migration(5, 14) { @Override public void migrate(SupportSQLiteDatabase db) { MIGRATION_5_13.migrate(db); MIGRATION_13_14.migrate(db); } };
    public static final Migration MIGRATION_6_14 = new Migration(6, 14) { @Override public void migrate(SupportSQLiteDatabase db) { MIGRATION_6_13.migrate(db); MIGRATION_13_14.migrate(db); } };
    public static final Migration MIGRATION_7_14 = new Migration(7, 14) { @Override public void migrate(SupportSQLiteDatabase db) { MIGRATION_7_13.migrate(db); MIGRATION_13_14.migrate(db); } };
    public static final Migration MIGRATION_8_14 = new Migration(8, 14) { @Override public void migrate(SupportSQLiteDatabase db) { MIGRATION_8_13.migrate(db); MIGRATION_13_14.migrate(db); } };
    public static final Migration MIGRATION_9_14 = new Migration(9, 14) { @Override public void migrate(SupportSQLiteDatabase db) { MIGRATION_9_13.migrate(db); MIGRATION_13_14.migrate(db); } };
    public static final Migration MIGRATION_10_14 = new Migration(10, 14) { @Override public void migrate(SupportSQLiteDatabase db) { MIGRATION_10_13.migrate(db); MIGRATION_13_14.migrate(db); } };
    public static final Migration MIGRATION_11_14 = new Migration(11, 14) { @Override public void migrate(SupportSQLiteDatabase db) { MIGRATION_11_13.migrate(db); MIGRATION_13_14.migrate(db); } };
    public static final Migration MIGRATION_1_10 = new Migration(1, 10) { @Override public void migrate(SupportSQLiteDatabase db) { MIGRATION_1_9.migrate(db); MIGRATION_9_10.migrate(db); } };
    public static final Migration MIGRATION_2_10 = new Migration(2, 10) { @Override public void migrate(SupportSQLiteDatabase db) { MIGRATION_2_9.migrate(db); MIGRATION_9_10.migrate(db); } };
    public static final Migration MIGRATION_3_10 = new Migration(3, 10) { @Override public void migrate(SupportSQLiteDatabase db) { MIGRATION_3_9.migrate(db); MIGRATION_9_10.migrate(db); } };
    public static final Migration MIGRATION_4_10 = new Migration(4, 10) { @Override public void migrate(SupportSQLiteDatabase db) { MIGRATION_4_9.migrate(db); MIGRATION_9_10.migrate(db); } };
    public static final Migration MIGRATION_5_10 = new Migration(5, 10) { @Override public void migrate(SupportSQLiteDatabase db) { MIGRATION_5_9.migrate(db); MIGRATION_9_10.migrate(db); } };
    public static final Migration MIGRATION_6_10 = new Migration(6, 10) { @Override public void migrate(SupportSQLiteDatabase db) { MIGRATION_6_9.migrate(db); MIGRATION_9_10.migrate(db); } };
    public static final Migration MIGRATION_7_10 = new Migration(7, 10) { @Override public void migrate(SupportSQLiteDatabase db) { MIGRATION_7_9.migrate(db); MIGRATION_9_10.migrate(db); } };
    public static final Migration MIGRATION_8_10 = new Migration(8, 10) { @Override public void migrate(SupportSQLiteDatabase db) { MIGRATION_8_9.migrate(db); MIGRATION_9_10.migrate(db); } };
    public static final Migration MIGRATION_1_8 = new Migration(1, 8) {
        @Override public void migrate(SupportSQLiteDatabase database) {
            MIGRATION_1_7.migrate(database);
            MIGRATION_7_8.migrate(database);
        }
    };
    public static final Migration MIGRATION_2_8 = new Migration(2, 8) {
        @Override public void migrate(SupportSQLiteDatabase database) {
            MIGRATION_2_7.migrate(database);
            MIGRATION_7_8.migrate(database);
        }
    };
    public static final Migration MIGRATION_3_8 = new Migration(3, 8) {
        @Override public void migrate(SupportSQLiteDatabase database) {
            MIGRATION_3_7.migrate(database);
            MIGRATION_7_8.migrate(database);
        }
    };
    public static final Migration MIGRATION_4_8 = new Migration(4, 8) {
        @Override public void migrate(SupportSQLiteDatabase database) {
            MIGRATION_4_7.migrate(database);
            MIGRATION_7_8.migrate(database);
        }
    };
    public static final Migration MIGRATION_5_8 = new Migration(5, 8) {
        @Override public void migrate(SupportSQLiteDatabase database) {
            MIGRATION_5_7.migrate(database);
            MIGRATION_7_8.migrate(database);
        }
    };
    public static final Migration MIGRATION_6_8 = new Migration(6, 8) {
        @Override public void migrate(SupportSQLiteDatabase database) {
            MIGRATION_6_7.migrate(database);
            MIGRATION_7_8.migrate(database);
        }
    };
    public static final Migration MIGRATION_1_7 = new Migration(1, 7) {
        @Override public void migrate(SupportSQLiteDatabase database) {
            MIGRATION_1_6.migrate(database);
            MIGRATION_6_7.migrate(database);
        }
    };
    public static final Migration MIGRATION_2_7 = new Migration(2, 7) {
        @Override public void migrate(SupportSQLiteDatabase database) {
            MIGRATION_2_6.migrate(database);
            MIGRATION_6_7.migrate(database);
        }
    };
    public static final Migration MIGRATION_3_7 = new Migration(3, 7) {
        @Override public void migrate(SupportSQLiteDatabase database) {
            MIGRATION_3_6.migrate(database);
            MIGRATION_6_7.migrate(database);
        }
    };
    public static final Migration MIGRATION_4_7 = new Migration(4, 7) {
        @Override public void migrate(SupportSQLiteDatabase database) {
            MIGRATION_4_6.migrate(database);
            MIGRATION_6_7.migrate(database);
        }
    };
    public static final Migration MIGRATION_5_7 = new Migration(5, 7) {
        @Override public void migrate(SupportSQLiteDatabase database) {
            MIGRATION_5_6.migrate(database);
            MIGRATION_6_7.migrate(database);
        }
    };
    public static final Migration MIGRATION_4_6 = new Migration(4, 6) {
        @Override public void migrate(SupportSQLiteDatabase database) {
            MIGRATION_4_5.migrate(database);
            MIGRATION_5_6.migrate(database);
        }
    };
    public static final Migration MIGRATION_3_6 = new Migration(3, 6) {
        @Override public void migrate(SupportSQLiteDatabase database) {
            MIGRATION_3_5.migrate(database);
            MIGRATION_5_6.migrate(database);
        }
    };
    public static final Migration MIGRATION_2_6 = new Migration(2, 6) {
        @Override public void migrate(SupportSQLiteDatabase database) {
            MIGRATION_2_5.migrate(database);
            MIGRATION_5_6.migrate(database);
        }
    };
    public static final Migration MIGRATION_1_6 = new Migration(1, 6) {
        @Override public void migrate(SupportSQLiteDatabase database) {
            MIGRATION_1_5.migrate(database);
            MIGRATION_5_6.migrate(database);
        }
    };
    public static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE wallets ADD COLUMN creditLimitRupiah INTEGER");
            database.execSQL("ALTER TABLE wallets ADD COLUMN billingDay INTEGER");
            database.execSQL("ALTER TABLE wallets ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0");
        }
    };
    public static final Migration MIGRATION_2_4 = new Migration(2, 4) {
        @Override public void migrate(SupportSQLiteDatabase database) {
            MIGRATION_2_3.migrate(database);
            MIGRATION_3_4.migrate(database);
        }
    };
    public static final Migration MIGRATION_1_4 = new Migration(1, 4) {
        @Override public void migrate(SupportSQLiteDatabase database) {
            MIGRATION_1_2.migrate(database);
            MIGRATION_2_4.migrate(database);
        }
    };

    public abstract TransactionDao transactionDao();

    public abstract ReconciliationCheckpointDao reconciliationCheckpointDao();

    public abstract WalletDao walletDao();

    public abstract CategoryDao categoryDao();

    public abstract TransactionSplitDao transactionSplitDao();

    public abstract BudgetDao budgetDao();

    public abstract RecurringDao recurringDao();
}
