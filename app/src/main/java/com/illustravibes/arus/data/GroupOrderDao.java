package com.illustravibes.arus.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;

import com.illustravibes.arus.domain.GroupFeeSplitMode;
import com.illustravibes.arus.domain.GroupOrderCalculation;
import com.illustravibes.arus.domain.GroupOrderCalculator;
import com.illustravibes.arus.domain.GroupOrderFeeInput;
import com.illustravibes.arus.domain.GroupOrderItemInput;
import com.illustravibes.arus.domain.TransactionType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Dao
public abstract class GroupOrderDao {
    @Insert protected abstract long insertDraft(GroupOrderEntity draft);
    @Insert protected abstract long insertItem(GroupOrderItemEntity item);
    @Insert protected abstract long insertOwner(GroupOrderOwnerEntity owner);
    @Insert protected abstract long insertFee(GroupOrderFeeEntity fee);
    @Insert protected abstract long insertFeeShare(GroupOrderFeeShareEntity share);
    @Insert protected abstract long insertShare(GroupOrderShareEntity share);
    @Insert protected abstract long insertPayment(TransactionEntity transaction);
    @Insert protected abstract long insertDebt(DebtEntity debt);

    @Query("SELECT * FROM group_orders WHERE id = :id AND deletedAtEpochMillis IS NULL")
    public abstract GroupOrderEntity find(long id);

    @Query("SELECT * FROM group_orders WHERE status = 'DRAFT' AND deletedAtEpochMillis IS NULL ORDER BY updatedAtEpochMillis DESC, id DESC")
    public abstract List<GroupOrderEntity> drafts();

    @Query("SELECT * FROM group_orders WHERE status IN ('FINALIZED','SETTLED') AND deletedAtEpochMillis IS NULL ORDER BY occurredAtEpochMillis DESC, id DESC")
    public abstract List<GroupOrderEntity> finalizedOrders();

    @Query("SELECT * FROM group_orders WHERE deletedAtEpochMillis IS NULL ORDER BY occurredAtEpochMillis DESC, id DESC")
    public abstract List<GroupOrderEntity> allOrders();

    @Query("SELECT * FROM group_order_items WHERE groupOrderId = :groupOrderId ORDER BY id")
    public abstract List<GroupOrderItemEntity> items(long groupOrderId);

    @Query("SELECT * FROM group_order_owners WHERE itemId = :itemId ORDER BY id")
    public abstract List<GroupOrderOwnerEntity> owners(long itemId);

    @Query("SELECT * FROM group_order_fees WHERE groupOrderId = :groupOrderId ORDER BY id")
    public abstract List<GroupOrderFeeEntity> fees(long groupOrderId);

    @Query("SELECT * FROM group_order_fee_shares WHERE feeId = :feeId ORDER BY id")
    public abstract List<GroupOrderFeeShareEntity> feeShares(long feeId);

    @Query("SELECT * FROM group_order_shares WHERE groupOrderId = :groupOrderId ORDER BY id")
    public abstract List<GroupOrderShareEntity> shares(long groupOrderId);

    @Query("SELECT * FROM wallets WHERE id = :id AND isArchived = 0 AND deletedAtEpochMillis IS NULL")
    protected abstract WalletEntity activeWallet(long id);

    @Query("SELECT * FROM categories WHERE id = :id AND kind = 'EXPENSE' AND isArchived = 0")
    protected abstract CategoryEntity activeExpenseCategory(long id);

    @Query("SELECT * FROM contacts WHERE id = :id AND isArchived = 0")
    protected abstract ContactEntity activeContact(long id);

    @Query("SELECT * FROM debts WHERE groupOrderId = :groupOrderId AND deletedAtEpochMillis IS NULL")
    public abstract List<DebtEntity> debts(long groupOrderId);

    @Query("SELECT COUNT(*) FROM repayment_allocations a INNER JOIN debts d ON d.id = a.debtId WHERE d.groupOrderId = :groupOrderId")
    protected abstract int repaymentCount(long groupOrderId);

    @Query("UPDATE debts SET deletedAtEpochMillis = :deletedAt WHERE groupOrderId = :groupOrderId AND deletedAtEpochMillis IS NULL")
    protected abstract int softDeleteGroupDebts(long groupOrderId, long deletedAt);

    @Query("DELETE FROM group_order_owners WHERE itemId = :itemId")
    protected abstract int deleteOwners(long itemId);

    @Query("DELETE FROM group_order_shares WHERE groupOrderId = :groupOrderId")
    protected abstract int deleteShares(long groupOrderId);

    @Query("UPDATE group_orders SET subtotalRupiah = :subtotal, sharedFeeTotalRupiah = :fees, discountTotalRupiah = :discount, grandTotalRupiah = :grandTotal, paymentTransactionId = :transactionId, status = 'FINALIZED', updatedAtEpochMillis = :updatedAt WHERE id = :id AND status = 'DRAFT'")
    protected abstract int markFinalized(long id, long subtotal, long fees, long discount,
                                         long grandTotal, long transactionId, long updatedAt);

    @Query("UPDATE group_orders SET status = 'CANCELLED', updatedAtEpochMillis = :deletedAt WHERE id = :id AND status IN ('FINALIZED','SETTLED')")
    protected abstract int markCancelled(long id, long deletedAt);

    @Query("UPDATE group_orders SET status = 'SETTLED', updatedAtEpochMillis = :updatedAt WHERE id = :groupOrderId AND status = 'FINALIZED' AND NOT EXISTS (SELECT 1 FROM debts WHERE groupOrderId = :groupOrderId AND deletedAtEpochMillis IS NULL AND status NOT IN ('PAID','WRITTEN_OFF'))")
    protected abstract int markSettledIfComplete(long groupOrderId, long updatedAt);

    @Query("INSERT INTO transaction_splits (transactionId, amountRupiah, categoryId, countsInReport, countsInBudget) VALUES (:transactionId, :amount, :categoryId, :countsInReport, :countsInBudget)")
    protected abstract void insertPaymentSplit(long transactionId, long amount, Long categoryId,
                                               boolean countsInReport, boolean countsInBudget);

    @Query("SELECT COALESCE(SUM(amountRupiah), 0) FROM transaction_splits WHERE transactionId = :transactionId")
    protected abstract long splitTotal(long transactionId);

    @Transaction
    public long createDraft(GroupOrderEntity draft) {
        if (!"DRAFT".equals(draft.status) || draft.paymentTransactionId != null)
            throw new IllegalArgumentException("Group order baru harus berstatus draft.");
        if (activeWallet(draft.payerWalletId) == null || activeExpenseCategory(draft.categoryId) == null)
            throw new IllegalArgumentException("Dompet pembayar dan kategori pengeluaran harus aktif.");
        return insertDraft(draft);
    }

    @Transaction
    public long addItem(long groupOrderId, String name, long unitPriceRupiah, int quantity,
                        String source, List<GroupOrderOwnerEntity> owners) {
        GroupOrderEntity draft = requireDraft(groupOrderId);
        if (unitPriceRupiah <= 0 || quantity <= 0) throw new IllegalArgumentException("Harga dan jumlah item harus positif.");
        long itemId = insertItem(new GroupOrderItemEntity(groupOrderId, name, unitPriceRupiah,
                quantity, source, owners == null || owners.isEmpty() ? "UNASSIGNED" : "ASSIGNED"));
        if (owners != null) for (GroupOrderOwnerEntity owner : owners) {
            validateOwner(owner);
            insertOwner(new GroupOrderOwnerEntity(itemId, owner.contactId, owner.shareWeight, owner.shareAmountRupiah));
        }
        touchDraft(draft.id, System.currentTimeMillis());
        return itemId;
    }

    @Transaction
    public void assignOwners(long itemId, List<GroupOrderOwnerEntity> assignments) {
        GroupOrderItemEntity item = itemById(itemId);
        if (item == null) throw new IllegalArgumentException("Item group order tidak ditemukan.");
        GroupOrderEntity draft = requireDraft(item.groupOrderId);
        if (assignments == null || assignments.isEmpty()) throw new IllegalArgumentException("Item wajib memiliki pemilik.");
        for (GroupOrderOwnerEntity owner : assignments) validateOwner(owner);
        deleteOwners(itemId);
        for (GroupOrderOwnerEntity owner : assignments)
            insertOwner(new GroupOrderOwnerEntity(itemId, owner.contactId, owner.shareWeight, owner.shareAmountRupiah));
        setItemAssigned(itemId, true);
        touchDraft(draft.id, System.currentTimeMillis());
    }

    @Transaction
    public long addFee(long groupOrderId, String label, long amountRupiah, String splitMode,
                       Map<Long, Long> manualShares) {
        GroupOrderEntity draft = requireDraft(groupOrderId);
        GroupFeeSplitMode mode;
        try { mode = GroupFeeSplitMode.valueOf(splitMode); }
        catch (Exception invalid) { throw new IllegalArgumentException("Mode pembagian biaya tidak valid."); }
        long feeId = insertFee(new GroupOrderFeeEntity(groupOrderId, label, amountRupiah, mode.name()));
        if (mode == GroupFeeSplitMode.MANUAL && manualShares != null) {
            for (Map.Entry<Long, Long> share : manualShares.entrySet())
                insertFeeShare(new GroupOrderFeeShareEntity(feeId, share.getKey(), share.getValue()));
        }
        touchDraft(draft.id, System.currentTimeMillis());
        return feeId;
    }

    @Transaction
    public long finalizeDraft(long groupOrderId, long nowEpochMillis) {
        GroupOrderEntity draft = requireDraft(groupOrderId);
        List<GroupOrderItemEntity> orderItems = items(groupOrderId);
        if (orderItems.isEmpty()) throw new IllegalArgumentException("Group order harus memiliki item.");
        Set<Long> participantIds = new HashSet<>();
        participantIds.add(null);
        List<GroupOrderItemInput> itemInputs = new ArrayList<>();
        for (GroupOrderItemEntity item : orderItems) {
            List<GroupOrderOwnerEntity> itemOwners = owners(item.id);
            if (!"ASSIGNED".equals(item.status) || itemOwners.isEmpty())
                throw new IllegalArgumentException("Semua item harus memiliki pemilik sebelum finalisasi.");
            Map<Long, Long> weights = new HashMap<>();
            long manualTotal = 0;
            boolean hasManual = false;
            for (GroupOrderOwnerEntity owner : itemOwners) {
                if (owner.contactId != null) {
                    if (activeContact(owner.contactId) == null) throw new IllegalArgumentException("Kontak pemilik item tidak aktif.");
                    participantIds.add(owner.contactId);
                }
                long key = owner.contactId == null ? 0L : owner.contactId;
                if (weights.put(key, owner.shareAmountRupiah == null ? owner.shareWeight : owner.shareAmountRupiah) != null)
                    throw new IllegalArgumentException("Pemilik item duplikat.");
                if (owner.shareAmountRupiah != null) { hasManual = true; manualTotal = Math.addExact(manualTotal, owner.shareAmountRupiah); }
            }
            long itemAmount = Math.multiplyExact(item.unitPriceRupiah, (long) item.quantity);
            if (hasManual && (itemOwners.stream().anyMatch(owner -> owner.shareAmountRupiah == null) || manualTotal != itemAmount))
                throw new IllegalArgumentException("Total pembagian manual item harus sama dengan harga item.");
            itemInputs.add(new GroupOrderItemInput(itemAmount, mapNullableParticipantWeights(weights)));
        }

        List<GroupOrderFeeInput> feeInputs = new ArrayList<>();
        for (GroupOrderFeeEntity fee : fees(groupOrderId)) {
            GroupFeeSplitMode mode = GroupFeeSplitMode.valueOf(fee.splitMode);
            Map<Long, Long> feeWeights = new HashMap<>();
            if (mode == GroupFeeSplitMode.MANUAL) {
                for (GroupOrderFeeShareEntity share : feeShares(fee.id)) {
                    if (share.contactId != null) {
                        if (activeContact(share.contactId) == null) throw new IllegalArgumentException("Kontak pembagi biaya tidak aktif.");
                        participantIds.add(share.contactId);
                    }
                    feeWeights.put(share.contactId == null ? 0L : share.contactId, share.amountRupiah);
                }
            }
            feeInputs.add(new GroupOrderFeeInput(fee.amountRupiah, mode, mapNullableParticipantWeights(feeWeights)));
        }

        List<Long> participants = new ArrayList<>();
        participants.add(null);
        for (Long participantId : participantIds) if (participantId != null) participants.add(participantId);
        Long roundingBearer = "ME".equals(draft.roundingBearer) ? null : parseBearer(draft.roundingBearer);
        GroupOrderCalculation calculation = GroupOrderCalculator.INSTANCE.calculate(participants, itemInputs,
                normalizeManualFeeInputs(feeInputs, participants), roundingBearer);
        if (calculation.getGrandTotalRupiah() <= 0) throw new IllegalArgumentException("Total group order harus positif.");
        WalletEntity wallet = activeWallet(draft.payerWalletId);
        CategoryEntity category = activeExpenseCategory(draft.categoryId);
        if (wallet == null || category == null) throw new IllegalArgumentException("Dompet pembayar atau kategori tidak aktif.");
        if (!"CREDIT".equals(wallet.type)) {
            long available = Math.addExact(wallet.initialBalanceRupiah,
                    databaseTransactionEffect(wallet.id, nowEpochMillis));
            if (available < calculation.getGrandTotalRupiah()) throw new IllegalArgumentException("Saldo dompet tidak cukup untuk group order.");
        }

        long transactionId = insertPayment(new TransactionEntity(TransactionType.EXPENSE,
                draft.payerWalletId, calculation.getGrandTotalRupiah(), null, draft.categoryId,
                draft.occurredAtEpochMillis, draft.title, draft.merchant, null));
        for (Map.Entry<Long, com.illustravibes.arus.domain.GroupOrderParticipantShare> share : calculation.getParticipants().entrySet()) {
            long amount = share.getValue().getTotalAmountRupiah();
            if (amount <= 0) continue;
            Long splitCategory = share.getKey() == null ? draft.categoryId : null;
            insertPaymentSplit(transactionId, amount, splitCategory, share.getKey() == null, share.getKey() == null);
            insertShare(new GroupOrderShareEntity(groupOrderId, share.getKey(),
                    share.getValue().getItemsAmountRupiah(), share.getValue().getFeeAmountRupiah(), amount));
            if (share.getKey() != null) {
                insertDebt(new DebtEntity(share.getKey(), "LENT", "GROUP_ORDER", transactionId,
                        groupOrderId, amount, "OPEN", null, draft.title, nowEpochMillis,
                        nowEpochMillis, null));
            }
        }
        if (splitTotal(transactionId) != calculation.getGrandTotalRupiah())
            throw new IllegalStateException("Total split harus sama dengan total pembayaran group order.");
        if (markFinalized(groupOrderId, calculation.getSubtotalRupiah(), calculation.getSharedFeeTotalRupiah(),
                calculation.getDiscountTotalRupiah(), calculation.getGrandTotalRupiah(), transactionId, nowEpochMillis) != 1)
            throw new IllegalStateException("Draft group order berubah sebelum finalisasi.");
        markSettledIfComplete(groupOrderId, nowEpochMillis);
        return transactionId;
    }

    @Transaction
    public void cancelGroupOrder(long groupOrderId, long deletedAtEpochMillis) {
        GroupOrderEntity order = find(groupOrderId);
        if (order == null || !"FINALIZED".equals(order.status) && !"SETTLED".equals(order.status))
            throw new IllegalArgumentException("Group order final tidak ditemukan.");
        if (repaymentCount(groupOrderId) > 0)
            throw new IllegalStateException("Group order yang memiliki pelunasan tidak dapat dibatalkan sebelum pelunasan dihapus atau diikhlaskan.");
        if (order.paymentTransactionId == null) throw new IllegalStateException("Transaksi pembayaran group order tidak ditemukan.");
        int deleted = softDeletePayment(order.paymentTransactionId, deletedAtEpochMillis);
        if (deleted != 1) throw new IllegalStateException("Transaksi pembayaran group order tidak aktif.");
        deletePaymentSplits(order.paymentTransactionId);
        softDeleteGroupDebts(groupOrderId, deletedAtEpochMillis);
        if (markCancelled(groupOrderId, deletedAtEpochMillis) != 1)
            throw new IllegalStateException("Group order berubah sebelum pembatalan.");
    }

    @Query("UPDATE transactions SET deletedAtEpochMillis = :deletedAt WHERE id = :id AND deletedAtEpochMillis IS NULL")
    protected abstract int softDeletePayment(long id, long deletedAt);

    @Query("DELETE FROM transaction_splits WHERE transactionId = :transactionId")
    protected abstract int deletePaymentSplits(long transactionId);


    @Query("SELECT * FROM group_order_items WHERE id = :itemId")
    protected abstract GroupOrderItemEntity itemById(long itemId);

    @Query("UPDATE group_order_items SET status = :status WHERE id = :itemId")
    protected abstract int setItemStatus(long itemId, String status);

    protected void setItemAssigned(long itemId, boolean assigned) { setItemStatus(itemId, assigned ? "ASSIGNED" : "UNASSIGNED"); }

    @Query("UPDATE group_orders SET updatedAtEpochMillis = :updatedAt WHERE id = :groupOrderId AND status = 'DRAFT'")
    protected abstract int touchDraft(long groupOrderId, long updatedAt);

    @Query("UPDATE group_orders SET title = :title, merchant = :merchant, occurredAtEpochMillis = :occurredAt, payerWalletId = :walletId, categoryId = :categoryId, roundingBearer = :roundingBearer, updatedAtEpochMillis = :updatedAt WHERE id = :groupOrderId AND status = 'DRAFT'")
    public abstract int updateDraftInfo(long groupOrderId, String title, String merchant, long occurredAt,
                                        long walletId, long categoryId, String roundingBearer, long updatedAt);

    @Query("SELECT COALESCE(SUM(CASE WHEN type IN ('INCOME','TRANSFER_IN','REPAYMENT_IN','LOAN_IN') THEN amountRupiah WHEN type IN ('EXPENSE','TRANSFER_OUT','REFUND_OUT','LOAN_OUT','DEBT_PAY_OUT') THEN -amountRupiah WHEN type = 'ADJUSTMENT' AND direction = 'PLUS' THEN amountRupiah WHEN type = 'ADJUSTMENT' AND direction = 'MINUS' THEN -amountRupiah ELSE 0 END), 0) FROM transactions WHERE walletId = :walletId AND deletedAtEpochMillis IS NULL AND occurredAtEpochMillis <= :now")
    protected abstract long databaseTransactionEffect(long walletId, long now);

    private GroupOrderEntity requireDraft(long id) {
        GroupOrderEntity draft = find(id);
        if (draft == null || !"DRAFT".equals(draft.status)) throw new IllegalArgumentException("Draft group order tidak ditemukan.");
        return draft;
    }

    private void validateOwner(GroupOrderOwnerEntity owner) {
        if (owner.contactId != null && activeContact(owner.contactId) == null)
            throw new IllegalArgumentException("Kontak pemilik item tidak aktif.");
    }

    private static Long parseBearer(String value) {
        try { return Long.valueOf(value); }
        catch (Exception invalid) { throw new IllegalArgumentException("Penanggung pembulatan tidak valid."); }
    }

    private static Map<Long, Long> mapNullableParticipantWeights(Map<Long, Long> source) {
        Map<Long, Long> result = new HashMap<>(source);
        if (result.containsKey(0L)) {
            Long amount = result.remove(0L);
            result.put(null, amount);
        }
        return result;
    }

    private List<GroupOrderFeeInput> normalizeManualFeeInputs(List<GroupOrderFeeInput> inputs,
                                                               List<Long> participants) {
        List<GroupOrderFeeInput> result = new ArrayList<>();
        for (int i = 0; i < inputs.size(); i++) {
            GroupOrderFeeInput input = inputs.get(i);
            if (input.getMode() != GroupFeeSplitMode.MANUAL) { result.add(input); continue; }
            Map<Long, Long> manual = new HashMap<>(input.getManualShares());
            if (!manual.containsKey(null)) manual.put(null, 0L);
            for (Long participant : participants) manual.putIfAbsent(participant, 0L);
            result.add(new GroupOrderFeeInput(input.getAmountRupiah(), input.getMode(), mapNullableParticipantWeights(manual)));
        }
        return result;
    }
}
