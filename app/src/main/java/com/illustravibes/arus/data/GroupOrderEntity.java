package com.illustravibes.arus.data;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "group_orders", foreignKeys = {
        @ForeignKey(entity = WalletEntity.class, parentColumns = "id", childColumns = "payerWalletId", onDelete = ForeignKey.RESTRICT),
        @ForeignKey(entity = CategoryEntity.class, parentColumns = "id", childColumns = "categoryId", onDelete = ForeignKey.RESTRICT),
        @ForeignKey(entity = TransactionEntity.class, parentColumns = "id", childColumns = "paymentTransactionId", onDelete = ForeignKey.RESTRICT)
}, indices = {@Index("payerWalletId"), @Index("categoryId"), @Index("paymentTransactionId"), @Index("status")})
public class GroupOrderEntity {
    @PrimaryKey(autoGenerate = true) public long id;
    public String title;
    public String merchant;
    public long occurredAtEpochMillis;
    public long payerWalletId;
    public long categoryId;
    public long subtotalRupiah;
    public long sharedFeeTotalRupiah;
    public long discountTotalRupiah;
    public long grandTotalRupiah;
    public Long paymentTransactionId;
    public String roundingBearer;
    public String status;
    public long createdAtEpochMillis;
    public long updatedAtEpochMillis;
    public Long deletedAtEpochMillis;

    public GroupOrderEntity(String title, String merchant, long occurredAtEpochMillis, long payerWalletId,
                            long categoryId, long subtotalRupiah, long sharedFeeTotalRupiah,
                            long discountTotalRupiah, long grandTotalRupiah, Long paymentTransactionId,
                            String roundingBearer, String status, long createdAtEpochMillis,
                            long updatedAtEpochMillis, Long deletedAtEpochMillis) {
        if (title == null || title.trim().isEmpty()) throw new IllegalArgumentException("Judul group order wajib diisi.");
        if (!"ME".equals(roundingBearer) && (roundingBearer == null || roundingBearer.trim().isEmpty()))
            throw new IllegalArgumentException("Penanggung pembulatan tidak valid.");
        if (!"DRAFT".equals(status) && !"FINALIZED".equals(status) && !"SETTLED".equals(status) && !"CANCELLED".equals(status))
            throw new IllegalArgumentException("Status group order tidak valid.");
        this.title = title.trim(); this.merchant = merchant; this.occurredAtEpochMillis = occurredAtEpochMillis;
        this.payerWalletId = payerWalletId; this.categoryId = categoryId; this.subtotalRupiah = subtotalRupiah;
        this.sharedFeeTotalRupiah = sharedFeeTotalRupiah; this.discountTotalRupiah = discountTotalRupiah;
        this.grandTotalRupiah = grandTotalRupiah; this.paymentTransactionId = paymentTransactionId;
        this.roundingBearer = roundingBearer; this.status = status; this.createdAtEpochMillis = createdAtEpochMillis;
        this.updatedAtEpochMillis = updatedAtEpochMillis; this.deletedAtEpochMillis = deletedAtEpochMillis;
    }
}
