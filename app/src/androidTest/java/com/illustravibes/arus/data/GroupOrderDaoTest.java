package com.illustravibes.arus.data;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.illustravibes.arus.domain.TransactionType;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@RunWith(AndroidJUnit4.class)
public class GroupOrderDaoTest {
    private ArusDatabase database;
    private long walletId;
    private long categoryId;
    private long contactId;

    @Before public void setUp() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), ArusDatabase.class).build();
        walletId = database.walletDao().insert(new WalletEntity("Tunai", "CASH", 200_000, true, false, null));
        categoryId = database.categoryDao().insert(new CategoryEntity("Makan", "EXPENSE", "restaurant", 0, false, false, 0));
        contactId = database.debtDao().insertContact(new ContactEntity("Budi", null, null, false, 1, 1));
    }

    @After public void tearDown() { database.close(); }

    @Test public void finalizationCreatesBalancedPaymentSplitsAndParticipantDebtAtomically() {
        long draftId = database.groupOrderDao().createDraft(draft("Makan siang"));
        assertEquals(1, database.groupOrderDao().updateDraftInfo(draftId, "Makan siang", "Warung",
                10, walletId, categoryId, "ME", 10));
        assertEquals("Makan siang", database.groupOrderDao().find(draftId).title);
        database.groupOrderDao().addItem(draftId, "Nasi", 30_000, 1, "MANUAL", Arrays.asList(
                new GroupOrderOwnerEntity(0, null, 1, null),
                new GroupOrderOwnerEntity(0, contactId, 1, null)));
        database.groupOrderDao().addItem(draftId, "Es teh", 20_000, 1, "MANUAL",
                Collections.singletonList(new GroupOrderOwnerEntity(0, contactId, 1, null)));
        database.groupOrderDao().addFee(draftId, "Ongkir", 10_000, "EQUAL", null);

        long paymentId = database.groupOrderDao().finalizeDraft(draftId, 10);

        GroupOrderEntity finalized = database.groupOrderDao().find(draftId);
        assertEquals("FINALIZED", finalized.status);
        assertEquals(Long.valueOf(paymentId), finalized.paymentTransactionId);
        assertEquals(50_000, finalized.subtotalRupiah);
        assertEquals(10_000, finalized.sharedFeeTotalRupiah);
        assertEquals(60_000, finalized.grandTotalRupiah);
        TransactionEntity payment = database.transactionDao().transactionById(paymentId);
        assertEquals(TransactionType.EXPENSE, payment.type);
        assertEquals(60_000, payment.amountRupiah);
        assertEquals(60_000, database.transactionSplitDao().splitTotal(paymentId));
        assertEquals(2, database.transactionSplitDao().splitsForTransaction(paymentId).size());
        assertEquals(1, database.groupOrderDao().debts(draftId).size());
        DebtEntity debt = database.groupOrderDao().debts(draftId).get(0);
        assertEquals(contactId, debt.contactId);
        assertEquals("GROUP_ORDER", debt.source);
        assertEquals(Long.valueOf(draftId), debt.groupOrderId);
        assertEquals(Long.valueOf(paymentId), debt.originTransactionId);
        assertEquals(40_000, debt.principalRupiah);
        assertEquals(2, database.groupOrderDao().shares(draftId).size());
        assertEquals(-60_000, database.transactionDao().currentWalletEffect(walletId, 10));
    }

    @Test public void finalizationRejectsUnassignedItemAndManualFeeMismatch() {
        long draftId = database.groupOrderDao().createDraft(draft("Draft"));
        long itemId = database.groupOrderDao().addItem(draftId, "Item belum punya pemilik", 100, 1,
                "MANUAL", Collections.emptyList());
        try {
            database.groupOrderDao().finalizeDraft(draftId, 10);
            throw new AssertionError("Unassigned items must block finalization.");
        } catch (IllegalArgumentException expected) { }

        database.groupOrderDao().assignOwners(itemId,
                Collections.singletonList(new GroupOrderOwnerEntity(0, null, 1, null)));
        database.groupOrderDao().addFee(draftId, "Biaya", 10, "MANUAL", Collections.singletonMap(null, 5L));
        try {
            database.groupOrderDao().finalizeDraft(draftId, 10);
            throw new AssertionError("Unbalanced manual fee shares must block finalization.");
        } catch (IllegalArgumentException expected) { }
        assertNull(database.groupOrderDao().find(draftId).paymentTransactionId);
        assertEquals(0, database.transactionDao().activeTransactions().size());
    }

    @Test public void finalizationHonorsCustomItemSharesManualFeesAndDiscounts() {
        long draftId = database.groupOrderDao().createDraft(draft("Tagihan manual"));
        database.groupOrderDao().updateDraftInfo(draftId, "Tagihan manual", "Toko", 10, walletId,
                categoryId, Long.toString(contactId), 10);
        database.groupOrderDao().addItem(draftId, "Paket patungan", 101, 1, "MANUAL", Arrays.asList(
                new GroupOrderOwnerEntity(0, null, 1, 40L),
                new GroupOrderOwnerEntity(0, contactId, 1, 61L)));
        HashMap<Long, Long> manual = new HashMap<>();
        manual.put(null, 3L);
        manual.put(contactId, 7L);
        database.groupOrderDao().addFee(draftId, "Biaya manual", 10, "MANUAL", manual);
        database.groupOrderDao().addFee(draftId, "Voucher", -2, "EQUAL", null);

        long transactionId = database.groupOrderDao().finalizeDraft(draftId, 10);

        GroupOrderEntity finalized = database.groupOrderDao().find(draftId);
        assertEquals(109, finalized.grandTotalRupiah);
        assertEquals(2, database.groupOrderDao().shares(draftId).size());
        assertEquals(67, database.groupOrderDao().debts(draftId).get(0).principalRupiah);
        assertEquals(109, database.transactionSplitDao().splitTotal(transactionId));
    }

    @Test public void completingParticipantRepaymentChangesOrderStatusToSettled() {
        long orderId = createFinalizedReceivableOrder();
        DebtEntity debt = database.groupOrderDao().debts(orderId).get(0);

        database.debtDao().recordSettlement(contactId, "LENT", walletId, 200,
                Collections.singletonList(new DebtDao.AllocationRequest(debt.id, 200)),
                null, null, 11, "Pelunasan penuh", 11);

        assertEquals("SETTLED", database.groupOrderDao().find(orderId).status);
    }

    @Test public void cancellationWithoutRepaymentRemovesLedgerEffectButKeepsCancelledOrder() {
        long orderId = createFinalizedReceivableOrder();
        GroupOrderEntity order = database.groupOrderDao().find(orderId);
        long effectBefore = database.transactionDao().currentWalletEffect(walletId, 20);

        database.groupOrderDao().cancelGroupOrder(orderId, 20);

        assertEquals("CANCELLED", database.groupOrderDao().find(orderId).status);
        assertEquals(effectBefore + order.grandTotalRupiah, database.transactionDao().currentWalletEffect(walletId, 20));
        assertEquals(0, database.groupOrderDao().debts(orderId).size());
        assertEquals(0, database.transactionSplitDao().splitsForTransaction(order.paymentTransactionId).size());
    }

    @Test public void cancellationWithAnyRepaymentAllocationIsRejectedAtomically() {
        long orderId = createFinalizedReceivableOrder();
        DebtEntity debt = database.groupOrderDao().debts(orderId).get(0);
        database.debtDao().recordSettlement(contactId, "LENT", walletId, 20,
                Collections.singletonList(new DebtDao.AllocationRequest(debt.id, 20)),
                null, null, 12, "Cicilan", 12);
        long effectBeforeCancel = database.transactionDao().currentWalletEffect(walletId, 20);

        try {
            database.groupOrderDao().cancelGroupOrder(orderId, 20);
            throw new AssertionError("An order with a repayment must not be cancelled.");
        } catch (IllegalStateException expected) { }

        assertEquals("FINALIZED", database.groupOrderDao().find(orderId).status);
        assertEquals(effectBeforeCancel, database.transactionDao().currentWalletEffect(walletId, 20));
        assertEquals(1, database.groupOrderDao().debts(orderId).size());
    }

    private GroupOrderEntity draft(String title) {
        return new GroupOrderEntity(title, "Kedai", 10, walletId, categoryId,
                0, 0, 0, 0, null, "ME", "DRAFT", 1, 1, null);
    }

    private long createFinalizedReceivableOrder() {
        long orderId = database.groupOrderDao().createDraft(draft("Makan bersama"));
        database.groupOrderDao().addItem(orderId, "Makanan", 200, 1, "MANUAL",
                Collections.singletonList(new GroupOrderOwnerEntity(0, contactId, 1, null)));
        database.groupOrderDao().finalizeDraft(orderId, 10);
        return orderId;
    }
}
