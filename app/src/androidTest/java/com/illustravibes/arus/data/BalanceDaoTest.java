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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotEquals;

@RunWith(AndroidJUnit4.class)
public class BalanceDaoTest {
    private ArusDatabase database;
    private WalletDao wallets;
    private TransactionDao transactions;
    private BalanceRepository balances;

    @Before
    public void setUp() {
        database = Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(), ArusDatabase.class)
                .addMigrations(ArusDatabase.MIGRATION_11_12).build();
        wallets = database.walletDao();
        transactions = database.transactionDao();
        balances = new BalanceRepository(database);
    }

    @After
    public void tearDown() {
        database.close();
    }

    @Test
    public void balanceUsesInitialBalanceAndOnlyActiveTransactionsThroughNow() {
        long walletId = wallets.insert(new WalletEntity("Tunai", "CASH", 1_000, true, false, null));
        transactions.insert(new TransactionEntity(TransactionType.INCOME, walletId, 500, null, 10, null));
        transactions.insert(new TransactionEntity(TransactionType.EXPENSE, walletId, 200, null, 11, null));
        transactions.insert(new TransactionEntity(TransactionType.INCOME, walletId, 900, null, 30, null));
        transactions.insert(new TransactionEntity(TransactionType.EXPENSE, walletId, 100, null, 12, 13L));

        assertEquals(Long.valueOf(1_300), balances.balance(walletId, 20));
        assertEquals(Long.valueOf(2_200), balances.balance(walletId, 40));
        assertNull(balances.balance(walletId + 99, 40));
    }

    @Test
    public void balanceAppliesDirectionalTransactionsAndSupportsLargeValues() {
        long walletId = wallets.insert(new WalletEntity("Rekening", "BANK", 0, true, false, null));
        transactions.insert(new TransactionEntity(TransactionType.INCOME, walletId, Long.MAX_VALUE, null, 1, null));
        assertEquals(Long.valueOf(Long.MAX_VALUE), balances.balance(walletId, 2));
    }

    @Test
    public void ordinaryTransactionInsertValidatesWalletCategoryAndBalanceAndKeepsFutureOutOfBalance() {
        long walletId = wallets.insert(new WalletEntity("Tunai", "CASH", 100, true, false, null));
        CategoryEntity expense = new CategoryEntity("Makan", "EXPENSE", "restaurant", 0, false, false, 0);
        CategoryEntity income = new CategoryEntity("Gaji", "INCOME", "payments", 0, false, false, 0);
        long expenseId = database.categoryDao().insert(expense);
        long incomeId = database.categoryDao().insert(income);

        TransactionEntity future = new TransactionEntity(TransactionType.EXPENSE, walletId, 500, null,
                expenseId, System.currentTimeMillis() + 86_400_000L, "Besok", "Toko", null);
        long futureId = transactions.insertOrdinary(future, false);
        assertEquals(Long.valueOf(100), balances.balance(walletId, System.currentTimeMillis()));

        TransactionEntity incomeTx = new TransactionEntity(TransactionType.INCOME, walletId, 200, null,
                incomeId, System.currentTimeMillis(), null, null, null);
        long incomeTransactionId = transactions.insertOrdinary(incomeTx, false);
        assertEquals(Long.valueOf(300), balances.balance(walletId, System.currentTimeMillis()));

        try {
            transactions.insertOrdinary(new TransactionEntity(TransactionType.EXPENSE, walletId, 500, null,
                    expenseId, System.currentTimeMillis(), null, null, null), false);
            throw new AssertionError("Insufficient balance must be surfaced for confirmation.");
        } catch (TransactionDao.InsufficientBalanceException expected) {
            assertEquals("Saldo tidak cukup, saldo akan minus.", expected.getMessage());
        }
        assertEquals(2, transactions.activeTransactions().size());
        long negativeBalanceTransactionId = transactions.insertOrdinary(new TransactionEntity(TransactionType.EXPENSE, walletId, 500, null,
                expenseId, System.currentTimeMillis(), null, null, null), true);
        assertEquals(Long.valueOf(-200), balances.balance(walletId, System.currentTimeMillis()));
        assertEquals(500, transactions.splitTotal(futureId));
        assertEquals(200, transactions.splitTotal(incomeTransactionId));
        assertEquals(500, transactions.splitTotal(negativeBalanceTransactionId));
    }

    @Test
    public void reportCategoryTotalsUseActiveReportSplitsAndExcludeFutureTransactions() {
        long walletId = wallets.insert(new WalletEntity("Tunai", "CASH", 0, true, false, null));
        long foodId = database.categoryDao().insert(new CategoryEntity("Makan", "EXPENSE", "restaurant", 0, false, false, 0));
        long travelId = database.categoryDao().insert(new CategoryEntity("Transport", "EXPENSE", "bus", 1, false, false, 1));
        TransactionEntity food = new TransactionEntity(TransactionType.EXPENSE, walletId, 300, null,
                foodId, 10, null, null, null);
        long foodTransactionId = transactions.insertOrdinary(food, true);
        TransactionEntity travel = new TransactionEntity(TransactionType.EXPENSE, walletId, 200, null,
                travelId, 20, null, null, null);
        long travelTransactionId = transactions.insertOrdinary(travel, true);
        TransactionEntity future = new TransactionEntity(TransactionType.EXPENSE, walletId, 500, null,
                foodId, 200, null, null, null);
        transactions.insertOrdinary(future, true);
        database.transactionSplitDao().deleteForTransaction(foodTransactionId);
        database.transactionSplitDao().insert(new TransactionSplitEntity(foodTransactionId, 300, foodId, false, true));

        List<ReportCategoryTotal> result = transactions.reportExpenseCategories(0, 100);
        assertEquals(1, result.size());
        assertEquals("Transport", result.get(0).categoryName);
        assertEquals(200, result.get(0).amountRupiah);
    }

    @Test
    public void undoDeleteRestoresTransactionAndBalance() {
        long walletId = wallets.insert(new WalletEntity("Tunai", "CASH", 500, true, false, null));
        long categoryId = database.categoryDao().insert(new CategoryEntity("Makan", "EXPENSE", "restaurant", 0, false, false, 0));
        long transactionId = transactions.insert(new TransactionEntity(TransactionType.EXPENSE, walletId, 100,
                null, categoryId, System.currentTimeMillis(), null, null, null));
        long deletedAt = System.currentTimeMillis();
        assertEquals(Long.valueOf(400), balances.balance(walletId, deletedAt));
        assertEquals(1, transactions.softDelete(transactionId, deletedAt));
        assertEquals(Long.valueOf(500), balances.balance(walletId, deletedAt));
        assertEquals(1, transactions.restore(transactionId, deletedAt));
        assertEquals(Long.valueOf(400), balances.balance(walletId, deletedAt));
    }

    @Test
    public void transferWithoutFeeMovesValueAndLinksBothRows() {
        long source = wallets.insert(new WalletEntity("Sumber", "BANK", 10_000, true, false, null));
        long destination = wallets.insert(new WalletEntity("Tujuan", "EWALLET", 1_000, true, false, null));
        long group = transactions.insertTransfer(source, destination, 2_500, 0, System.currentTimeMillis(), "Pindah", false);
        assertEquals(Long.valueOf(7_500), balances.balance(source, System.currentTimeMillis()));
        assertEquals(Long.valueOf(3_500), balances.balance(destination, System.currentTimeMillis()));
        assertEquals(2, transactions.activeTransferGroup(group).size());
        assertEquals(2, transactions.softDeleteTransferGroup(group, System.currentTimeMillis()));
        assertEquals(Long.valueOf(10_000), balances.balance(source, System.currentTimeMillis()));
        assertEquals(Long.valueOf(1_000), balances.balance(destination, System.currentTimeMillis()));
    }

    @Test
    public void transferWithFeeDebitsSourceAndRecordsExpenseCategory() {
        long source = wallets.insert(new WalletEntity("Sumber", "BANK", 10_000, true, false, null));
        long destination = wallets.insert(new WalletEntity("Tujuan", "CASH", 0, true, false, null));
        long feeCategory = database.categoryDao().insert(new CategoryEntity("Admin Bank", "EXPENSE", "receipt", 0, false, false, 0));
        long group = transactions.insertTransfer(source, destination, 2_500, 500, System.currentTimeMillis(), null, false);
        assertEquals(Long.valueOf(7_000), balances.balance(source, System.currentTimeMillis()));
        assertEquals(Long.valueOf(2_500), balances.balance(destination, System.currentTimeMillis()));
        assertEquals(3, transactions.activeTransferGroup(group).size());
        assertEquals(feeCategory, transactions.activeTransferGroup(group).get(2).categoryId.longValue());
    }

    @Test
    public void failedTransferRollsBackEveryRowAndSameWalletIsRejected() {
        long source = wallets.insert(new WalletEntity("Sumber", "BANK", 100, true, false, null));
        long destination = wallets.insert(new WalletEntity("Tujuan", "CASH", 0, true, false, null));
        long feeCategory = database.categoryDao().insert(new CategoryEntity("Admin Bank", "EXPENSE", "receipt", 0, false, false, 0));
        try {
            transactions.insertTransfer(source, destination, 200, 10, System.currentTimeMillis(), null, false);
            throw new AssertionError("Insufficient source balance must fail transfer.");
        } catch (TransactionDao.InsufficientBalanceException expected) {
            assertEquals(0, transactions.activeTransactions().size());
        }
        database.categoryDao().archive(feeCategory);
        try {
            transactions.insertTransfer(source, destination, 50, 10, System.currentTimeMillis(), null, true);
            throw new AssertionError("Missing admin fee category must roll back all transfer rows.");
        } catch (IllegalStateException expected) {
            assertEquals(0, transactions.activeTransactions().size());
        }
        try {
            transactions.insertTransfer(source, source, 10, 0, System.currentTimeMillis(), null, true);
            throw new AssertionError("Transfer to same wallet must fail.");
        } catch (IllegalArgumentException expected) {
            assertEquals(0, transactions.activeTransactions().size());
        }
        assertEquals(0, database.categoryDao().activeTransactionCount(feeCategory));
    }

    @Test
    public void creditUsedTracksTransferPaymentsWithoutTreatingTransferOutAsNewDebt() {
        long source = wallets.insert(new WalletEntity("Bank", "BANK", 5_000, true, false, null));
        WalletEntity creditWallet = new WalletEntity("Kartu", "CREDIT", 0, true, false, null);
        creditWallet.creditLimitRupiah = 20_000L;
        creditWallet.billingDay = 1;
        long credit = wallets.insert(creditWallet);
        long now = System.currentTimeMillis() - 10_000L;
        long paymentAt = now + 10_000;
        transactions.insertTransfer(source, credit, 3_000, 0, paymentAt, null, false);
        BalanceRepository repository = new BalanceRepository(database);
        assertEquals(Long.valueOf(20_000), repository.creditUsed(credit, now));
        assertEquals(Long.valueOf(17_000), repository.creditUsed(credit, paymentAt + 1));
    }

    @Test
    public void financialSummaryIncludesSelectedAssetWalletsAndCreditLiabilities() {
        long now = System.currentTimeMillis();
        long cash = wallets.insert(new WalletEntity("Tunai", "CASH", 10_000, true, false, null));
        wallets.insert(new WalletEntity("Tabungan dikecualikan", "BANK", 50_000, false, false, null));
        long source = wallets.insert(new WalletEntity("Bank", "BANK", 5_000, true, false, null));
        WalletEntity creditWallet = new WalletEntity("Kartu", "CREDIT", 0, true, false, null);
        creditWallet.creditLimitRupiah = 20_000L;
        long credit = wallets.insert(creditWallet);
        transactions.insert(new TransactionEntity(TransactionType.INCOME, cash, 2_000, null, null, now, null, null, null));
        transactions.insertTransfer(source, credit, 3_000, 0, now, null, false);

        BalanceRepository.FinancialSummary summary = balances.financialSummary(now);
        assertEquals(14_000L, summary.assetsRupiah);
        assertEquals(17_000L, summary.creditLiabilitiesRupiah);
        assertEquals(-3_000L, summary.netWorthRupiah);
    }

    @Test
    public void historyPagingUsesDescendingDateThenIdAndRefreshesAfterEditOrDelete() {
        long wallet = wallets.insert(new WalletEntity("Tunai", "CASH", 1_000, true, false, null));
        long category = database.categoryDao().insert(new CategoryEntity("Belanja", "EXPENSE", "receipt", 0, false, false, 0));
        long first = insertExpense(wallet, 100, 10_000, category);
        long sameTimeLaterId = insertExpense(wallet, 150, 10_000, category);
        long newest = insertExpense(wallet, 200, 20_000, category);
        List<TransactionEntity> ordered = transactions.activeTransactions();
        ordered.sort((left, right) -> {
            int byTime = Long.compare(right.occurredAtEpochMillis, left.occurredAtEpochMillis);
            return byTime != 0 ? byTime : Long.compare(right.id, left.id);
        });
        assertEquals(newest, ordered.get(0).id);
        assertEquals(sameTimeLaterId, ordered.get(1).id);
        assertEquals(first, ordered.get(2).id);
        assertEquals(1, transactions.updateOrdinary(newest, 250, wallet, category, 20_000, "edited", null));
        assertEquals(250, transactions.transactionById(newest).amountRupiah);
        long deletedAt = System.currentTimeMillis();
        assertEquals(1, transactions.softDeleteOrdinary(sameTimeLaterId, deletedAt));
        assertEquals(1, transactions.restore(sameTimeLaterId, deletedAt));
    }

    @Test
    public void verifiedTransactionWarnsAndMarksRecheckOnEditOrDelete() {
        long wallet = wallets.insert(new WalletEntity("Tunai", "CASH", 1_000, true, false, null));
        long category = database.categoryDao().insert(new CategoryEntity("Verifikasi", "EXPENSE", "receipt", 0, false, false, 0));
        long transactionId = insertExpense(wallet, 100, 10_000, category);
        TransactionDao.ReconciliationResult checkpointResult = transactions.resolveReconciliation(wallet, 800, 20_000,
                "UNTRACKED", java.util.Collections.singletonList(new TransactionDao.ReconciliationPart("ADJUSTMENT", 100, "MINUS", null, "Selisih")));
        assertEquals(1, transactions.isVerified(transactionId));
        ReconciliationCheckpointEntity checkpoint = database.reconciliationCheckpointDao().checkpointById(checkpointResult.checkpointId);
        assertEquals(900L, checkpoint.recordedBalanceRupiah);
        assertEquals(800L, checkpoint.actualBalanceRupiah);
        assertEquals(-100L, checkpoint.differenceRupiah);
        assertEquals(0, transactions.softDeleteOrdinary(transactionId, 20_000));
        assertEquals(1, transactions.updateOrdinaryTrackingCheckpoint(transactionId, 125, wallet, category, 10_000, null, null));
        assertEquals(125, transactions.splitTotal(transactionId));
        TransactionEntity changed = transactions.transactionById(transactionId);
        assertEquals(1, changed.needsRecheck ? 1 : 0);
        assertEquals(1, database.reconciliationCheckpointDao().checkpointById(checkpointResult.checkpointId).needsRecheck ? 1 : 0);
        assertEquals(1, transactions.softDeleteOrdinaryTrackingCheckpoint(transactionId, 30_000));
        assertEquals(1, transactions.transactionById(transactionId).needsRecheck ? 1 : 0);
        assertEquals(1, database.reconciliationCheckpointDao().checkpointById(checkpointResult.checkpointId).needsRecheck ? 1 : 0);
    }

    @Test
    public void reconciliationResolutionsAreAtomicAndReachActualBalance() {
        long wallet = wallets.insert(new WalletEntity("Tunai", "CASH", 1_000, true, false, null));
        long now = System.currentTimeMillis();
        long incomeCategory = database.categoryDao().insert(new CategoryEntity("Bonus", "INCOME", "payments", 0, false, false, 0));
        TransactionDao.ReconciliationResult forgotten = transactions.resolveReconciliation(wallet, 1_250, now,
                "MISSED", java.util.Collections.singletonList(new TransactionDao.ReconciliationPart("INCOME", 250, null, incomeCategory, "Bonus terlupa")));
        assertEquals(1_250L, forgotten.actualBalanceRupiah);
        assertNotEquals(0L, forgotten.checkpointId);
        assertEquals(Long.valueOf(1_250), balances.balance(wallet, now));
        assertEquals(1_250L, database.reconciliationCheckpointDao().checkpointById(forgotten.checkpointId).actualBalanceRupiah);

        long splitTime = now + 1_000L;
        long expenseCategory = database.categoryDao().insert(new CategoryEntity("Belanja", "EXPENSE", "restaurant", 0, false, false, 0));
        TransactionDao.ReconciliationResult split = transactions.resolveReconciliation(wallet, 1_000, splitTime,
                "UNTRACKED", java.util.Collections.singletonList(new TransactionDao.ReconciliationPart("ADJUSTMENT", 250, "MINUS", null, "Selisih tak terlacak")));
        assertEquals(-250L, split.differenceRupiah);
        assertEquals(Long.valueOf(1_000), balances.balance(wallet, splitTime));

        long openingTime = splitTime + 1_000L;
        TransactionDao.ReconciliationResult opening = transactions.resolveReconciliation(wallet, 900, openingTime,
                "OPENING_BALANCE", java.util.Collections.singletonList(new TransactionDao.ReconciliationPart("ADJUSTMENT", 100, "MINUS", null, "Koreksi")));
        assertEquals(900L, opening.actualBalanceRupiah);
        assertEquals(900L, wallets.initialBalance(wallet).longValue());
        assertEquals(Long.valueOf(900), balances.balance(wallet, openingTime));
        assertEquals(2, transactions.activeTransactions().size());
        assertEquals(900L, database.reconciliationCheckpointDao().checkpointById(opening.checkpointId).actualBalanceRupiah);
    }

    @Test
    public void openingBalanceResolutionDoesNotCreateAdjustmentTransaction() {
        long wallet = wallets.insert(new WalletEntity("Tunai", "CASH", 100, true, false, null));
        long now = System.currentTimeMillis();
        TransactionDao.ReconciliationResult result = transactions.resolveReconciliation(wallet, 175, now,
                "OPENING_BALANCE", java.util.Collections.singletonList(
                        new TransactionDao.ReconciliationPart("ADJUSTMENT", 75, "PLUS", null, "Koreksi saldo awal")));

        assertEquals(75L, result.differenceRupiah);
        assertEquals(175L, wallets.initialBalance(wallet).longValue());
        assertEquals(Long.valueOf(175), balances.balance(wallet, now));
        assertEquals(0, transactions.activeTransactions().size());
        assertEquals(175L, database.reconciliationCheckpointDao().checkpointById(result.checkpointId).actualBalanceRupiah);
    }

    @Test
    public void invalidReconciliationRollsBackInsertedRows() {
        long wallet = wallets.insert(new WalletEntity("Tunai", "CASH", 1_000, true, false, null));
        try {
            transactions.resolveReconciliation(wallet, 1_500, System.currentTimeMillis(), "UNTRACKED",
                    java.util.Collections.singletonList(new TransactionDao.ReconciliationPart("ADJUSTMENT", 400, "PLUS", null, "Selisih")));
            throw new AssertionError("Mismatched resolution must fail atomically.");
        } catch (IllegalArgumentException expected) {
            assertEquals(0, transactions.activeTransactions().size());
            assertEquals(Long.valueOf(1_000), balances.balance(wallet, System.currentTimeMillis()));
        }
    }

    private long insertExpense(long walletId, long amount, long occurredAt, Long categoryId) {
        TransactionEntity transaction = new TransactionEntity(TransactionType.EXPENSE, walletId, amount, null,
                categoryId, occurredAt, null, null, null);
        return transactions.insert(transaction);
    }
}
