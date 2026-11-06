package com.illustravibes.arus.data;

import androidx.room.Room;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.illustravibes.arus.domain.TransactionType;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(AndroidJUnit4.class)
public class DebtDaoTest {
    private ArusDatabase database;
    private long debtId;
    private long secondDebtId;
    private long repaymentTransactionId;
    private long walletId;

    @Before public void setUp() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), ArusDatabase.class).build();
        SupportSQLiteDatabase raw = database.getOpenHelper().getWritableDatabase();
        raw.execSQL("CREATE TABLE IF NOT EXISTS contacts (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT, preferredWalletNote TEXT, note TEXT, isArchived INTEGER NOT NULL, createdAtEpochMillis INTEGER NOT NULL, updatedAtEpochMillis INTEGER NOT NULL)");
        raw.execSQL("CREATE TABLE IF NOT EXISTS debts (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, contactId INTEGER NOT NULL, direction TEXT, source TEXT, originTransactionId INTEGER, principalRupiah INTEGER NOT NULL, status TEXT, dueDateEpochMillis INTEGER, note TEXT, createdAtEpochMillis INTEGER NOT NULL, updatedAtEpochMillis INTEGER NOT NULL, deletedAtEpochMillis INTEGER)");
        raw.execSQL("CREATE TABLE IF NOT EXISTS repayment_allocations (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, transactionId INTEGER NOT NULL, debtId INTEGER NOT NULL, amountRupiah INTEGER NOT NULL)");
        ArusDatabase.MIGRATION_14_15.migrate(raw);
        ArusDatabase.MIGRATION_15_16.migrate(raw);
        raw.execSQL("CREATE TABLE IF NOT EXISTS wallets (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT, type TEXT, icon TEXT, colorArgb INTEGER NOT NULL DEFAULT -10453621, initialBalanceRupiah INTEGER NOT NULL, creditLimitRupiah INTEGER, billingDay INTEGER, sortOrder INTEGER NOT NULL, defaultExpenseCategoryId INTEGER, includeInTotal INTEGER NOT NULL, isArchived INTEGER NOT NULL, deletedAtEpochMillis INTEGER)");
        long contactId = database.debtDao().insertContact(new ContactEntity("Rani", null, null, false, 1, 1));
        debtId = database.debtDao().insertDebt(new DebtEntity(contactId, "LENT", "MANUAL", null,
                1000, "OPEN", null, null, 1, 1, null));
        secondDebtId = database.debtDao().insertDebt(new DebtEntity(contactId, "BORROWED", "MANUAL", null,
                1000, "OPEN", null, null, 1, 1, null));
        walletId = database.walletDao().insert(new WalletEntity("Tunai", "CASH", 0, true, false, null));
        repaymentTransactionId = database.transactionDao().insert(new TransactionEntity(
                TransactionType.REPAYMENT_IN, walletId, 700, null, 2, null));
    }

    @After public void tearDown() { database.close(); }

    @Test public void allocationMustFitTransactionAndDebtPrincipal() {
        database.debtDao().insertAllocationValidated(new RepaymentAllocationEntity(repaymentTransactionId, debtId, 600));
        assertEquals(600, database.debtDao().allocatedAmount(debtId));
        expectRejected(() -> database.debtDao().insertAllocationValidated(
                new RepaymentAllocationEntity(repaymentTransactionId, secondDebtId, 101)));
        long secondPayment = database.transactionDao().insert(new TransactionEntity(
                TransactionType.REPAYMENT_IN, 1L, 500, null, 3, null));
        expectRejected(() -> database.debtDao().insertAllocationValidated(
                new RepaymentAllocationEntity(secondPayment, debtId, 401)));
    }

    @Test public void oldDebtDoesNotCreateWalletTransaction() {
        int before = database.transactionDao().activeTransactions().size();
        Long dueDate = 800L;
        long oldDebtId = database.debtDao().createDebt(1, "LENT", 250, null, 4,
                dueDate, "Pinjaman lama", 4);
        DebtEntity debt = database.debtDao().findActiveDebt(oldDebtId);
        assertEquals(before, database.transactionDao().activeTransactions().size());
        assertEquals(null, debt.originTransactionId);
        assertEquals(1L, debt.contactId);
        assertEquals("LENT", debt.direction);
        assertEquals("MANUAL", debt.source);
        assertEquals(250L, debt.principalRupiah);
        assertEquals("OPEN", debt.status);
        assertEquals(dueDate, debt.dueDateEpochMillis);
        assertEquals("Pinjaman lama", debt.note);
        assertEquals(4L, debt.createdAtEpochMillis);
        assertEquals(4L, debt.updatedAtEpochMillis);
    }

    @Test public void walletBackedDebtCreatesLinkedLoanTransactionWithCorrectDirection() {
        long walletId = database.walletDao().insert(new WalletEntity("Bank", "BANK", 0, true, false, null));
        long lentDebtId = database.debtDao().createDebt(1, "LENT", 300, walletId, 5,
                null, null, 5);
        long borrowedDebtId = database.debtDao().createDebt(1, "BORROWED", 450, walletId, 6,
                null, null, 6);
        DebtEntity lent = database.debtDao().findActiveDebt(lentDebtId);
        DebtEntity borrowed = database.debtDao().findActiveDebt(borrowedDebtId);
        assertNotNull(lent.originTransactionId);
        assertNotNull(borrowed.originTransactionId);
        TransactionEntity lentTransaction = database.transactionDao().transactionById(lent.originTransactionId);
        TransactionEntity borrowedTransaction = database.transactionDao().transactionById(borrowed.originTransactionId);
        assertEquals(TransactionType.LOAN_OUT, lentTransaction.type);
        assertEquals(TransactionType.LOAN_IN, borrowedTransaction.type);
        assertEquals(walletId, lentTransaction.walletId.longValue());
        assertEquals(walletId, borrowedTransaction.walletId.longValue());
        assertEquals(300L, lentTransaction.amountRupiah);
        assertEquals(450L, borrowedTransaction.amountRupiah);
        assertEquals(5L, lentTransaction.occurredAtEpochMillis);
        assertEquals(6L, borrowedTransaction.occurredAtEpochMillis);
        assertEquals(null, lentTransaction.categoryId);
        assertEquals(null, borrowedTransaction.categoryId);
        assertEquals(null, lentTransaction.deletedAtEpochMillis);
        assertEquals(null, borrowedTransaction.deletedAtEpochMillis);
        assertEquals(lent.originTransactionId, Long.valueOf(lentTransaction.id));
        assertEquals(borrowed.originTransactionId, Long.valueOf(borrowedTransaction.id));
    }

    @Test public void createDebtRejectsInvalidContactDirectionPrincipalAndWallet() {
        expectRejected(() -> database.debtDao().createDebt(999, "LENT", 1, null, 1, null, null, 1));
        expectRejected(() -> database.debtDao().createDebt(1, "SIDEWAYS", 1, null, 1, null, null, 1));
        expectRejected(() -> database.debtDao().createDebt(1, "LENT", 0, null, 1, null, null, 1));
        expectRejected(() -> database.debtDao().createDebt(1, "LENT", 1, 999L, 1, null, null, 1));
        database.walletDao().archive(walletId);
        expectRejected(() -> database.debtDao().createDebt(1, "LENT", 1, walletId, 1, null, null, 1));
        database.debtDao().insertContact(new ContactEntity("Arsip", null, null, true, 1, 1));
        expectRejected(() -> database.debtDao().createDebt(2, "LENT", 1, null, 1, null, null, 1));
    }

    @Test public void walletBackedLoanMayMakeNonCreditBalanceNegative() {
        long walletId = database.walletDao().insert(new WalletEntity("Tipis", "CASH", 0, true, false, null));
        long debtId = database.debtDao().createDebt(1, "LENT", 900, walletId, 7,
                null, null, 7);
        DebtEntity debt = database.debtDao().findActiveDebt(debtId);
        assertNotNull(debt.originTransactionId);
        assertEquals(-900L, database.transactionDao().currentWalletEffect(walletId, 7));
    }

    @Test public void foreignKeysRejectMissingContactAndTransaction() {
        try {
            database.debtDao().insertDebt(new DebtEntity(999, "BORROWED", "MANUAL", null,
                    100, "OPEN", null, null, 1, 1, null));
            throw new AssertionError("Unknown contact must be rejected.");
        } catch (android.database.sqlite.SQLiteConstraintException expected) { }
        try {
            database.debtDao().insertAllocationValidated(new RepaymentAllocationEntity(999, debtId, 1));
            throw new AssertionError("Unknown transaction must be rejected.");
        } catch (IllegalArgumentException expected) { }
    }

    @Test public void allocationRejectsDebtWithMissingContact() {
        database.getOpenHelper().getWritableDatabase().execSQL("PRAGMA foreign_keys=OFF");
        database.getOpenHelper().getWritableDatabase().execSQL("DELETE FROM contacts WHERE id=(SELECT contactId FROM debts WHERE id=" + debtId + ")");
        expectRejected(() -> database.debtDao().insertAllocationValidated(
                new RepaymentAllocationEntity(repaymentTransactionId, debtId, 10)));
    }

    @Test public void allocationDirectionMustMatchDebtDirection() {
        expectRejected(() -> database.debtDao().insertAllocationValidated(
                new RepaymentAllocationEntity(repaymentTransactionId, secondDebtId, 10)));
        assertEquals(0, database.debtDao().allocatedAmount(secondDebtId));
    }

    @Test public void installmentCanAllocateOnePaymentAcrossMultipleDebtsAndResolveOverpayment() {
        long secondLentDebt = database.debtDao().insertDebt(new DebtEntity(1, "LENT", "MANUAL", null,
                1000, "OPEN", null, null, 1, 1, null));
        long incomeCategory = database.categoryDao().insert(
                new CategoryEntity("Pendapatan lain", "INCOME", "payments", 0, false, false, 1));

        long repaymentId = database.debtDao().recordSettlement(1, "LENT", walletId, 700,
                Arrays.asList(new DebtDao.AllocationRequest(debtId, 300),
                        new DebtDao.AllocationRequest(secondLentDebt, 300)),
                "FORGIVE", incomeCategory, 10, "Bayar dua cicilan", 10);

        assertEquals(TransactionType.REPAYMENT_IN, database.transactionDao().transactionById(repaymentId).type);
        assertEquals(300, database.debtDao().allocatedAmount(debtId));
        assertEquals(300, database.debtDao().allocatedAmount(secondLentDebt));
        assertEquals("PARTIAL", database.debtDao().findActiveDebt(debtId).status);
        assertEquals("PARTIAL", database.debtDao().findActiveDebt(secondLentDebt).status);
        assertEquals(1400, database.transactionDao().currentWalletEffect(walletId, 10));
        ReportTotals totals = database.transactionDao().reportTotals(0, 20);
        assertEquals(100, totals.incomeRupiah);
        assertEquals(0, totals.expenseRupiah);
    }

    @Test public void repaymentOverageRefundBalancesToAllocatedAmount() {
        long walletEffectBefore = database.transactionDao().currentWalletEffect(walletId, 10);

        database.debtDao().recordSettlement(1, "LENT", walletId, 1200,
                Arrays.asList(new DebtDao.AllocationRequest(debtId, 1000)),
                "REFUND", null, 10, "Kembalikan kelebihan", 10);

        assertEquals(walletEffectBefore + 800, database.transactionDao().currentWalletEffect(walletId, 10));
        assertEquals(1000, database.debtDao().allocatedAmount(debtId));
        assertEquals("PAID", database.debtDao().findActiveDebt(debtId).status);
    }

    @Test public void borrowedOverpaymentCanBeExpenseOrNewReceivable() {
        long expenseCategory = database.categoryDao().insert(
                new CategoryEntity("Biaya lain", "EXPENSE", "receipt", 0, false, false, 1));
        long expenseDebt = database.debtDao().insertDebt(new DebtEntity(1, "BORROWED", "MANUAL", null,
                1000, "OPEN", null, null, 1, 1, null));
        long beforeExpensePayment = database.transactionDao().currentWalletEffect(walletId, 20);

        database.debtDao().recordSettlement(1, "BORROWED", walletId, 300,
                Arrays.asList(new DebtDao.AllocationRequest(expenseDebt, 200)),
                "FORGIVE", expenseCategory, 20, "Kelebihan diikhlaskan", 20);

        assertEquals(beforeExpensePayment - 300, database.transactionDao().currentWalletEffect(walletId, 20));
        assertEquals(100, database.transactionDao().reportTotals(0, 30).expenseRupiah);

        long receivableDebt = database.debtDao().insertDebt(new DebtEntity(1, "BORROWED", "MANUAL", null,
                1000, "OPEN", null, null, 1, 1, null));
        long beforeReceivablePayment = database.transactionDao().currentWalletEffect(walletId, 30);
        database.debtDao().recordSettlement(1, "BORROWED", walletId, 300,
                Arrays.asList(new DebtDao.AllocationRequest(receivableDebt, 200)),
                "CREATE_RECEIVABLE", null, 30, "Sisa menjadi piutang", 30);

        assertEquals(beforeReceivablePayment - 300, database.transactionDao().currentWalletEffect(walletId, 30));
        assertEquals(100, database.debtDao().activeDebts().stream()
                .filter(debt -> "LENT".equals(debt.direction) && "Sisa menjadi piutang".equals(debt.note))
                .mapToLong(debt -> debt.principalRupiah).sum());
    }

    @Test public void nettingAllocatesSameAmountToOppositeDebtsWithoutChangingWallet() {
        database.debtDao().insertAllocationValidated(new RepaymentAllocationEntity(repaymentTransactionId, debtId, 300));
        long walletEffectBefore = database.transactionDao().currentWalletEffect(walletId, 10);

        long nettingId = database.debtDao().netDebts(debtId, secondDebtId, 11);

        assertEquals(TransactionType.NETTING, database.transactionDao().transactionById(nettingId).type);
        assertEquals(700, database.transactionDao().transactionById(nettingId).amountRupiah);
        assertEquals(700, database.debtDao().allocatedAmount(debtId) - 300);
        assertEquals(700, database.debtDao().allocatedAmount(secondDebtId));
        assertEquals("PAID", database.debtDao().findActiveDebt(debtId).status);
        assertEquals("PARTIAL", database.debtDao().findActiveDebt(secondDebtId).status);
        assertEquals(walletEffectBefore, database.transactionDao().currentWalletEffect(walletId, 11));
    }

    @Test public void nettingRejectsDifferentContactsOrSameDirection() {
        long otherContact = database.debtDao().insertContact(new ContactEntity("Bima", null, null, false, 1, 1));
        long otherContactDebt = database.debtDao().insertDebt(new DebtEntity(otherContact, "BORROWED", "MANUAL", null,
                100, "OPEN", null, null, 1, 1, null));
        expectRejected(() -> database.debtDao().netDebts(debtId, otherContactDebt, 11));
        long sameDirectionDebt = database.debtDao().insertDebt(new DebtEntity(1, "LENT", "MANUAL", null,
                100, "OPEN", null, null, 1, 1, null));
        expectRejected(() -> database.debtDao().netDebts(debtId, sameDirectionDebt, 11));
    }

    @Test public void writeOffCreatesReportedTransactionAndMarksDebtWrittenOff() {
        database.categoryDao().insert(new CategoryEntity("Ikhlas", "EXPENSE", "volunteer_activism", 0, true, false, 0));
        database.debtDao().insertAllocationValidated(new RepaymentAllocationEntity(repaymentTransactionId, debtId, 300));

        long transactionId = database.debtDao().writeOff(debtId, 12, "Tidak ditagih lagi");

        TransactionEntity transaction = database.transactionDao().transactionById(transactionId);
        assertEquals(TransactionType.WRITE_OFF, transaction.type);
        assertEquals(700, transaction.amountRupiah);
        assertEquals("WRITTEN_OFF", database.debtDao().findActiveDebt(debtId).status);
        assertEquals(1, database.transactionSplitDao().splitsForTransaction(transactionId).size());
        assertEquals(1000, database.debtDao().allocatedAmount(debtId));
        assertEquals(700, database.transactionDao().reportTotals(0, 20).expenseRupiah);
    }

    @Test public void forgivingBorrowedDebtIsReportedAsIncome() {
        long transactionId = database.debtDao().writeOff(secondDebtId, 12, "Dihapus pemberi pinjaman");
        assertEquals("WRITTEN_OFF", database.debtDao().findActiveDebt(secondDebtId).status);
        assertEquals(TransactionType.WRITE_OFF, database.transactionDao().transactionById(transactionId).type);
        assertEquals(1000, database.transactionDao().reportTotals(0, 20).incomeRupiah);
    }

    @Test public void borrowedDebtAndInstallmentReduceNetWorthWithWalletBalance() {
        long borrowedDebt = database.debtDao().createDebt(1, "BORROWED", 500, walletId, 3,
                null, "Pinjaman", 3);
        BalanceRepository.FinancialSummary afterBorrow = new BalanceRepository(database).financialSummary(10);
        assertEquals(1200, afterBorrow.assetsRupiah);
        assertEquals(1500, afterBorrow.debtLiabilitiesRupiah);
        assertEquals(-300, afterBorrow.netWorthRupiah);

        database.debtDao().recordSettlement(1, "BORROWED", walletId, 200,
                Arrays.asList(new DebtDao.AllocationRequest(borrowedDebt, 200)),
                null, null, 4, "Cicilan", 4);

        BalanceRepository.FinancialSummary afterInstallment = new BalanceRepository(database).financialSummary(10);
        assertEquals(1000, afterInstallment.assetsRupiah);
        assertEquals(1300, afterInstallment.debtLiabilitiesRupiah);
        assertEquals(-300, afterInstallment.netWorthRupiah);
    }

    @Test public void debtConstructorRejectsInvalidDirectionSourceAndPrincipal() {
        expectInvalidDebt(() -> new DebtEntity(1, "SIDEWAYS", "MANUAL", null, 1, "OPEN", null, null, 1, 1, null));
        expectInvalidDebt(() -> new DebtEntity(1, "LENT", "UNKNOWN", null, 1, "OPEN", null, null, 1, 1, null));
        expectInvalidDebt(() -> new DebtEntity(1, "LENT", "MANUAL", null, 0, "OPEN", null, null, 1, 1, null));
    }

    @Test public void sqliteTriggerRejectsDirectAllocationAbovePrincipal() {
        try {
            database.getOpenHelper().getWritableDatabase().execSQL(
                    "INSERT INTO repayment_allocations (transactionId, debtId, amountRupiah) VALUES (?, ?, ?)",
                    new Object[]{repaymentTransactionId, debtId, 701});
            throw new AssertionError("Database trigger must reject over-allocation.");
        } catch (android.database.sqlite.SQLiteConstraintException expected) { }
        assertEquals(0, database.debtDao().allocatedAmount(debtId));
        try {
            database.getOpenHelper().getWritableDatabase().execSQL(
                    "INSERT INTO repayment_allocations (transactionId, debtId, amountRupiah) VALUES (?, ?, ?)",
                    new Object[]{repaymentTransactionId, secondDebtId, 10});
            throw new AssertionError("Database trigger must reject direction mismatch.");
        } catch (android.database.sqlite.SQLiteConstraintException expected) { }
    }

    @Test public void sqliteTriggerRejectsDirectDirectionMismatch() {
        try {
            database.getOpenHelper().getWritableDatabase().execSQL(
                    "INSERT INTO repayment_allocations (transactionId, debtId, amountRupiah) VALUES (?, ?, ?)",
                    new Object[]{repaymentTransactionId, secondDebtId, 10});
            throw new AssertionError("Database trigger must reject direction mismatch.");
        } catch (android.database.sqlite.SQLiteConstraintException expected) { }
        assertEquals(0, database.debtDao().allocatedAmount(secondDebtId));
    }

    @Test public void sqliteTriggerRejectsDirectionMismatchOnUpdate() {
        long allocationId = database.debtDao().insertAllocationValidated(
                new RepaymentAllocationEntity(repaymentTransactionId, debtId, 10));
        try {
            database.getOpenHelper().getWritableDatabase().execSQL(
                    "UPDATE repayment_allocations SET debtId=? WHERE id=?",
                    new Object[]{secondDebtId, allocationId});
            throw new AssertionError("Database trigger must reject direction mismatch on update.");
        } catch (android.database.sqlite.SQLiteConstraintException expected) { }
        assertEquals(10, database.debtDao().allocatedAmount(debtId));
    }

    private static void expectRejected(Runnable action) {
        try { action.run(); throw new AssertionError("Invalid allocation must be rejected."); }
        catch (IllegalArgumentException expected) { }
    }

    private static void expectInvalidDebt(Runnable action) {
        try { action.run(); throw new AssertionError("Invalid debt must be rejected."); }
        catch (IllegalArgumentException expected) { }
    }
}
