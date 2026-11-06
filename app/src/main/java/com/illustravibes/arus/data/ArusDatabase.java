package com.illustravibes.arus.data;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {WalletEntity.class, TransactionEntity.class, CategoryEntity.class, ReconciliationCheckpointEntity.class, TransactionSplitEntity.class, BudgetEntity.class, RecurringRuleEntity.class, RecurringOccurrenceEntity.class, ContactEntity.class, DebtEntity.class, RepaymentAllocationEntity.class, GroupOrderEntity.class, GroupOrderItemEntity.class, GroupOrderOwnerEntity.class, GroupOrderFeeEntity.class, GroupOrderFeeShareEntity.class, GroupOrderShareEntity.class, AttachmentEntity.class, MerchantCategoryHintEntity.class, CashDenominationCountEntity.class, ReconciliationItemEntity.class}, version = 21, exportSchema = true)
@TypeConverters(TransactionTypeConverter.class)
public abstract class ArusDatabase extends RoomDatabase {
    public static final int CURRENT_VERSION = 21;
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
    public static final Migration MIGRATION_14_15 = new Migration(14, 15) {
        @Override public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS contacts (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT, preferredWalletNote TEXT, note TEXT, isArchived INTEGER NOT NULL, createdAtEpochMillis INTEGER NOT NULL, updatedAtEpochMillis INTEGER NOT NULL)");
            database.execSQL("CREATE TABLE IF NOT EXISTS debts (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, contactId INTEGER NOT NULL, direction TEXT, source TEXT, originTransactionId INTEGER, principalRupiah INTEGER NOT NULL, status TEXT, dueDateEpochMillis INTEGER, note TEXT, createdAtEpochMillis INTEGER NOT NULL, updatedAtEpochMillis INTEGER NOT NULL, deletedAtEpochMillis INTEGER, FOREIGN KEY(contactId) REFERENCES contacts(id) ON UPDATE NO ACTION ON DELETE RESTRICT, FOREIGN KEY(originTransactionId) REFERENCES transactions(id) ON UPDATE NO ACTION ON DELETE RESTRICT)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_debts_contactId ON debts (contactId)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_debts_originTransactionId ON debts (originTransactionId)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_debts_status ON debts (status)");
            database.execSQL("CREATE TABLE IF NOT EXISTS repayment_allocations (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, transactionId INTEGER NOT NULL, debtId INTEGER NOT NULL, amountRupiah INTEGER NOT NULL, FOREIGN KEY(transactionId) REFERENCES transactions(id) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(debtId) REFERENCES debts(id) ON UPDATE NO ACTION ON DELETE RESTRICT)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_repayment_allocations_transactionId ON repayment_allocations (transactionId)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_repayment_allocations_debtId ON repayment_allocations (debtId)");
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_repayment_allocations_transactionId_debtId ON repayment_allocations (transactionId, debtId)");
            database.execSQL("INSERT OR IGNORE INTO categories (name, kind, icon, colorArgb, isSystem, isArchived, sortOrder) VALUES ('Utang Dihapuskan', 'INCOME', 'handshake', -1198355, 1, 0, 999)");
            database.execSQL("CREATE TRIGGER IF NOT EXISTS repayment_allocation_validate_insert BEFORE INSERT ON repayment_allocations BEGIN SELECT CASE WHEN NEW.amountRupiah <= 0 THEN RAISE(ABORT, 'allocation amount must be positive') END; SELECT CASE WHEN (SELECT type FROM transactions WHERE id = NEW.transactionId) NOT IN ('REPAYMENT_IN','DEBT_PAY_OUT','WRITE_OFF','NETTING') THEN RAISE(ABORT, 'transaction type cannot be allocated') END; SELECT CASE WHEN (SELECT contactId FROM debts WHERE id=NEW.debtId) IS NULL OR (SELECT id FROM contacts WHERE id=(SELECT contactId FROM debts WHERE id=NEW.debtId)) IS NULL THEN RAISE(ABORT, 'debt contact does not exist') END; SELECT CASE WHEN (SELECT COALESCE(SUM(amountRupiah), 0) FROM repayment_allocations WHERE transactionId = NEW.transactionId) + NEW.amountRupiah > (SELECT amountRupiah FROM transactions WHERE id = NEW.transactionId) THEN RAISE(ABORT, 'transaction allocations exceed transaction amount') END; SELECT CASE WHEN (SELECT COALESCE(SUM(amountRupiah), 0) FROM repayment_allocations WHERE debtId = NEW.debtId) + NEW.amountRupiah > (SELECT principalRupiah FROM debts WHERE id = NEW.debtId) THEN RAISE(ABORT, 'debt allocations exceed principal') END; END");
            database.execSQL("CREATE TRIGGER IF NOT EXISTS repayment_allocation_validate_update BEFORE UPDATE ON repayment_allocations BEGIN SELECT CASE WHEN NEW.amountRupiah <= 0 THEN RAISE(ABORT, 'allocation amount must be positive') END; SELECT CASE WHEN (SELECT type FROM transactions WHERE id = NEW.transactionId) NOT IN ('REPAYMENT_IN','DEBT_PAY_OUT','WRITE_OFF','NETTING') THEN RAISE(ABORT, 'transaction type cannot be allocated') END; SELECT CASE WHEN (SELECT COALESCE(SUM(amountRupiah), 0) FROM repayment_allocations WHERE transactionId = NEW.transactionId AND id != OLD.id) + NEW.amountRupiah > (SELECT amountRupiah FROM transactions WHERE id = NEW.transactionId) THEN RAISE(ABORT, 'transaction allocations exceed transaction amount') END; SELECT CASE WHEN (SELECT COALESCE(SUM(amountRupiah), 0) FROM repayment_allocations WHERE debtId = NEW.debtId AND id != OLD.id) + NEW.amountRupiah > (SELECT principalRupiah FROM debts WHERE id = NEW.debtId) THEN RAISE(ABORT, 'debt allocations exceed principal') END; SELECT CASE WHEN NOT ((SELECT type FROM transactions WHERE id=NEW.transactionId)='REPAYMENT_IN' AND (SELECT direction FROM debts WHERE id=NEW.debtId)='LENT') AND NOT ((SELECT type FROM transactions WHERE id=NEW.transactionId)='DEBT_PAY_OUT' AND (SELECT direction FROM debts WHERE id=NEW.debtId)='BORROWED') AND NOT ((SELECT type FROM transactions WHERE id=NEW.transactionId) IN ('WRITE_OFF','NETTING')) THEN RAISE(ABORT, 'transaction direction does not match debt') END; END");
            database.execSQL("CREATE TRIGGER IF NOT EXISTS repayment_allocation_validate_direction_insert BEFORE INSERT ON repayment_allocations BEGIN SELECT CASE WHEN NOT ((SELECT type FROM transactions WHERE id=NEW.transactionId)='REPAYMENT_IN' AND (SELECT direction FROM debts WHERE id=NEW.debtId)='LENT') AND NOT ((SELECT type FROM transactions WHERE id=NEW.transactionId)='DEBT_PAY_OUT' AND (SELECT direction FROM debts WHERE id=NEW.debtId)='BORROWED') AND NOT ((SELECT type FROM transactions WHERE id=NEW.transactionId) IN ('WRITE_OFF','NETTING')) THEN RAISE(ABORT, 'transaction direction does not match debt') END; END");
            database.execSQL("CREATE TRIGGER IF NOT EXISTS repayment_allocation_validate_direction_update BEFORE UPDATE ON repayment_allocations BEGIN SELECT CASE WHEN NOT ((SELECT type FROM transactions WHERE id=NEW.transactionId)='REPAYMENT_IN' AND (SELECT direction FROM debts WHERE id=NEW.debtId)='LENT') AND NOT ((SELECT type FROM transactions WHERE id=NEW.transactionId)='DEBT_PAY_OUT' AND (SELECT direction FROM debts WHERE id=NEW.debtId)='BORROWED') AND NOT ((SELECT type FROM transactions WHERE id=NEW.transactionId) IN ('WRITE_OFF','NETTING')) THEN RAISE(ABORT, 'transaction direction does not match debt') END; END");
        }
    };
    public static final Migration MIGRATION_15_16 = new Migration(15, 16) {
        @Override public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("DROP TRIGGER IF EXISTS repayment_allocation_validate_insert");
            database.execSQL("DROP TRIGGER IF EXISTS repayment_allocation_validate_update");
            database.execSQL("CREATE TRIGGER repayment_allocation_validate_insert BEFORE INSERT ON repayment_allocations BEGIN " +
                    "SELECT CASE WHEN NEW.amountRupiah <= 0 THEN RAISE(ABORT, 'allocation amount must be positive') END; " +
                    "SELECT CASE WHEN (SELECT type FROM transactions WHERE id = NEW.transactionId) NOT IN ('REPAYMENT_IN','DEBT_PAY_OUT','WRITE_OFF','NETTING') THEN RAISE(ABORT, 'transaction type cannot be allocated') END; " +
                    "SELECT CASE WHEN (SELECT contactId FROM debts WHERE id=NEW.debtId) IS NULL OR (SELECT id FROM contacts WHERE id=(SELECT contactId FROM debts WHERE id=NEW.debtId)) IS NULL THEN RAISE(ABORT, 'debt contact does not exist') END; " +
                    "SELECT CASE WHEN (SELECT COALESCE(SUM(amountRupiah), 0) FROM repayment_allocations WHERE transactionId = NEW.transactionId) + NEW.amountRupiah > (SELECT amountRupiah * CASE WHEN type = 'NETTING' THEN 2 ELSE 1 END FROM transactions WHERE id = NEW.transactionId) THEN RAISE(ABORT, 'transaction allocations exceed transaction amount') END; " +
                    "SELECT CASE WHEN (SELECT type FROM transactions WHERE id = NEW.transactionId) = 'NETTING' AND (SELECT COUNT(*) FROM repayment_allocations WHERE transactionId = NEW.transactionId) >= 2 THEN RAISE(ABORT, 'netting supports exactly two debt allocations') END; " +
                    "SELECT CASE WHEN (SELECT COALESCE(SUM(amountRupiah), 0) FROM repayment_allocations WHERE debtId = NEW.debtId) + NEW.amountRupiah > (SELECT principalRupiah FROM debts WHERE id = NEW.debtId) THEN RAISE(ABORT, 'debt allocations exceed principal') END; END");
            database.execSQL("CREATE TRIGGER repayment_allocation_validate_update BEFORE UPDATE ON repayment_allocations BEGIN " +
                    "SELECT CASE WHEN NEW.amountRupiah <= 0 THEN RAISE(ABORT, 'allocation amount must be positive') END; " +
                    "SELECT CASE WHEN (SELECT type FROM transactions WHERE id = NEW.transactionId) NOT IN ('REPAYMENT_IN','DEBT_PAY_OUT','WRITE_OFF','NETTING') THEN RAISE(ABORT, 'transaction type cannot be allocated') END; " +
                    "SELECT CASE WHEN (SELECT COALESCE(SUM(amountRupiah), 0) FROM repayment_allocations WHERE transactionId = NEW.transactionId AND id != OLD.id) + NEW.amountRupiah > (SELECT amountRupiah * CASE WHEN type = 'NETTING' THEN 2 ELSE 1 END FROM transactions WHERE id = NEW.transactionId) THEN RAISE(ABORT, 'transaction allocations exceed transaction amount') END; " +
                    "SELECT CASE WHEN (SELECT type FROM transactions WHERE id = NEW.transactionId) = 'NETTING' AND (SELECT COUNT(*) FROM repayment_allocations WHERE transactionId = NEW.transactionId AND id != OLD.id) >= 2 THEN RAISE(ABORT, 'netting supports exactly two debt allocations') END; " +
                    "SELECT CASE WHEN (SELECT COALESCE(SUM(amountRupiah), 0) FROM repayment_allocations WHERE debtId = NEW.debtId AND id != OLD.id) + NEW.amountRupiah > (SELECT principalRupiah FROM debts WHERE id = NEW.debtId) THEN RAISE(ABORT, 'debt allocations exceed principal') END; " +
                    "SELECT CASE WHEN NOT ((SELECT type FROM transactions WHERE id=NEW.transactionId)='REPAYMENT_IN' AND (SELECT direction FROM debts WHERE id=NEW.debtId)='LENT') AND NOT ((SELECT type FROM transactions WHERE id=NEW.transactionId)='DEBT_PAY_OUT' AND (SELECT direction FROM debts WHERE id=NEW.debtId)='BORROWED') AND NOT ((SELECT type FROM transactions WHERE id=NEW.transactionId) IN ('WRITE_OFF','NETTING')) THEN RAISE(ABORT, 'transaction direction does not match debt') END; END");
        }
    };
    public static final Migration MIGRATION_16_17 = new Migration(16, 17) {
        @Override public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS group_orders (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, title TEXT, merchant TEXT, occurredAtEpochMillis INTEGER NOT NULL, payerWalletId INTEGER NOT NULL, categoryId INTEGER NOT NULL, subtotalRupiah INTEGER NOT NULL, sharedFeeTotalRupiah INTEGER NOT NULL, discountTotalRupiah INTEGER NOT NULL, grandTotalRupiah INTEGER NOT NULL, paymentTransactionId INTEGER, roundingBearer TEXT, status TEXT, createdAtEpochMillis INTEGER NOT NULL, updatedAtEpochMillis INTEGER NOT NULL, deletedAtEpochMillis INTEGER, FOREIGN KEY(payerWalletId) REFERENCES wallets(id) ON UPDATE NO ACTION ON DELETE RESTRICT, FOREIGN KEY(categoryId) REFERENCES categories(id) ON UPDATE NO ACTION ON DELETE RESTRICT, FOREIGN KEY(paymentTransactionId) REFERENCES transactions(id) ON UPDATE NO ACTION ON DELETE RESTRICT)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_group_orders_payerWalletId ON group_orders (payerWalletId)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_group_orders_categoryId ON group_orders (categoryId)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_group_orders_paymentTransactionId ON group_orders (paymentTransactionId)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_group_orders_status ON group_orders (status)");
            database.execSQL("CREATE TABLE IF NOT EXISTS group_order_items (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, groupOrderId INTEGER NOT NULL, name TEXT, unitPriceRupiah INTEGER NOT NULL, quantity INTEGER NOT NULL, source TEXT, status TEXT, FOREIGN KEY(groupOrderId) REFERENCES group_orders(id) ON UPDATE NO ACTION ON DELETE CASCADE)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_group_order_items_groupOrderId ON group_order_items (groupOrderId)");
            database.execSQL("CREATE TABLE IF NOT EXISTS group_order_owners (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, itemId INTEGER NOT NULL, contactId INTEGER, shareWeight INTEGER NOT NULL, shareAmountRupiah INTEGER, FOREIGN KEY(itemId) REFERENCES group_order_items(id) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(contactId) REFERENCES contacts(id) ON UPDATE NO ACTION ON DELETE RESTRICT)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_group_order_owners_itemId ON group_order_owners (itemId)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_group_order_owners_contactId ON group_order_owners (contactId)");
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_group_order_owners_itemId_contactId ON group_order_owners (itemId, contactId)");
            database.execSQL("CREATE TABLE IF NOT EXISTS group_order_fees (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, groupOrderId INTEGER NOT NULL, label TEXT, amountRupiah INTEGER NOT NULL, splitMode TEXT, FOREIGN KEY(groupOrderId) REFERENCES group_orders(id) ON UPDATE NO ACTION ON DELETE CASCADE)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_group_order_fees_groupOrderId ON group_order_fees (groupOrderId)");
            database.execSQL("CREATE TABLE IF NOT EXISTS group_order_fee_shares (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, feeId INTEGER NOT NULL, contactId INTEGER, amountRupiah INTEGER NOT NULL, FOREIGN KEY(feeId) REFERENCES group_order_fees(id) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(contactId) REFERENCES contacts(id) ON UPDATE NO ACTION ON DELETE RESTRICT)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_group_order_fee_shares_feeId ON group_order_fee_shares (feeId)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_group_order_fee_shares_contactId ON group_order_fee_shares (contactId)");
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_group_order_fee_shares_feeId_contactId ON group_order_fee_shares (feeId, contactId)");
            database.execSQL("CREATE TABLE IF NOT EXISTS group_order_shares (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, groupOrderId INTEGER NOT NULL, contactId INTEGER, itemsAmountRupiah INTEGER NOT NULL, feeAmountRupiah INTEGER NOT NULL, totalAmountRupiah INTEGER NOT NULL, FOREIGN KEY(groupOrderId) REFERENCES group_orders(id) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(contactId) REFERENCES contacts(id) ON UPDATE NO ACTION ON DELETE RESTRICT)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_group_order_shares_groupOrderId ON group_order_shares (groupOrderId)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_group_order_shares_contactId ON group_order_shares (contactId)");
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_group_order_shares_groupOrderId_contactId ON group_order_shares (groupOrderId, contactId)");
            database.execSQL("ALTER TABLE debts ADD COLUMN groupOrderId INTEGER REFERENCES group_orders(id) ON UPDATE NO ACTION ON DELETE RESTRICT");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_debts_groupOrderId ON debts (groupOrderId)");
        }
    };
    public static final Migration MIGRATION_17_18 = new Migration(17, 18) {
        @Override public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS attachments (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, ownerType TEXT NOT NULL, ownerId INTEGER, filePath TEXT NOT NULL, ocrRawText TEXT, createdAtEpochMillis INTEGER NOT NULL)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_attachments_ownerType_ownerId ON attachments (ownerType, ownerId)");
        }
    };
    public static final Migration MIGRATION_18_19 = new Migration(18, 19) {
        @Override public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS merchant_category_hints (merchantName TEXT NOT NULL, categoryId INTEGER NOT NULL, useCount INTEGER NOT NULL, PRIMARY KEY(merchantName), FOREIGN KEY(categoryId) REFERENCES categories(id) ON UPDATE NO ACTION ON DELETE RESTRICT)");
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_merchant_category_hints_merchantName ON merchant_category_hints (merchantName)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_merchant_category_hints_categoryId ON merchant_category_hints (categoryId)");
        }
    };
    public static final Migration MIGRATION_19_20 = new Migration(19, 20) {
        @Override public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS cash_denomination_counts (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, checkpointId INTEGER NOT NULL, denominationRupiah INTEGER NOT NULL, pieceCount INTEGER NOT NULL, FOREIGN KEY(checkpointId) REFERENCES reconciliation_checkpoints(id) ON UPDATE NO ACTION ON DELETE CASCADE)");
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_cash_denomination_counts_checkpointId_denominationRupiah ON cash_denomination_counts (checkpointId, denominationRupiah)");
        }
    };
    public static final Migration MIGRATION_20_21 = new Migration(20, 21) {
        @Override public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS reconciliation_items (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, checkpointId INTEGER NOT NULL, transactionId INTEGER NOT NULL, matched INTEGER NOT NULL, FOREIGN KEY(checkpointId) REFERENCES reconciliation_checkpoints(id) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(transactionId) REFERENCES transactions(id) ON UPDATE NO ACTION ON DELETE CASCADE)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_reconciliation_items_checkpointId ON reconciliation_items (checkpointId)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_reconciliation_items_transactionId ON reconciliation_items (transactionId)");
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_reconciliation_items_checkpointId_transactionId ON reconciliation_items (checkpointId, transactionId)");
        }
    };
    public static final Migration MIGRATION_13_15 = new Migration(13, 15) { @Override public void migrate(SupportSQLiteDatabase db) { MIGRATION_13_14.migrate(db); MIGRATION_14_15.migrate(db); } };
    public static final Migration MIGRATION_12_15 = new Migration(12, 15) { @Override public void migrate(SupportSQLiteDatabase db) { MIGRATION_12_13.migrate(db); MIGRATION_13_14.migrate(db); MIGRATION_14_15.migrate(db); } };
    public static final Migration MIGRATION_11_15 = new Migration(11, 15) { @Override public void migrate(SupportSQLiteDatabase db) { MIGRATION_11_12.migrate(db); MIGRATION_12_13.migrate(db); MIGRATION_13_14.migrate(db); MIGRATION_14_15.migrate(db); } };
    public static final Migration MIGRATION_1_15 = new Migration(1, 15) { @Override public void migrate(SupportSQLiteDatabase db) { MIGRATION_1_13.migrate(db); MIGRATION_13_14.migrate(db); MIGRATION_14_15.migrate(db); } };
    public static final Migration MIGRATION_2_15 = new Migration(2, 15) { @Override public void migrate(SupportSQLiteDatabase db) { MIGRATION_2_13.migrate(db); MIGRATION_13_14.migrate(db); MIGRATION_14_15.migrate(db); } };
    public static final Migration MIGRATION_3_15 = new Migration(3, 15) { @Override public void migrate(SupportSQLiteDatabase db) { MIGRATION_3_13.migrate(db); MIGRATION_13_14.migrate(db); MIGRATION_14_15.migrate(db); } };
    public static final Migration MIGRATION_4_15 = new Migration(4, 15) { @Override public void migrate(SupportSQLiteDatabase db) { MIGRATION_4_13.migrate(db); MIGRATION_13_14.migrate(db); MIGRATION_14_15.migrate(db); } };
    public static final Migration MIGRATION_5_15 = new Migration(5, 15) { @Override public void migrate(SupportSQLiteDatabase db) { MIGRATION_5_13.migrate(db); MIGRATION_13_14.migrate(db); MIGRATION_14_15.migrate(db); } };
    public static final Migration MIGRATION_6_15 = new Migration(6, 15) { @Override public void migrate(SupportSQLiteDatabase db) { MIGRATION_6_13.migrate(db); MIGRATION_13_14.migrate(db); MIGRATION_14_15.migrate(db); } };
    public static final Migration MIGRATION_7_15 = new Migration(7, 15) { @Override public void migrate(SupportSQLiteDatabase db) { MIGRATION_7_13.migrate(db); MIGRATION_13_14.migrate(db); MIGRATION_14_15.migrate(db); } };
    public static final Migration MIGRATION_8_15 = new Migration(8, 15) { @Override public void migrate(SupportSQLiteDatabase db) { MIGRATION_8_13.migrate(db); MIGRATION_13_14.migrate(db); MIGRATION_14_15.migrate(db); } };
    public static final Migration MIGRATION_9_15 = new Migration(9, 15) { @Override public void migrate(SupportSQLiteDatabase db) { MIGRATION_9_13.migrate(db); MIGRATION_13_14.migrate(db); MIGRATION_14_15.migrate(db); } };
    public static final Migration MIGRATION_10_15 = new Migration(10, 15) { @Override public void migrate(SupportSQLiteDatabase db) { MIGRATION_10_13.migrate(db); MIGRATION_13_14.migrate(db); MIGRATION_14_15.migrate(db); } };
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

    public abstract DebtDao debtDao();

    public abstract GroupOrderDao groupOrderDao();

    public abstract AttachmentDao attachmentDao();

    public abstract MerchantCategoryHintDao merchantCategoryHintDao();

    public abstract CashDenominationCountDao cashDenominationCountDao();

    public abstract ReconciliationItemDao reconciliationItemDao();
}
