package com.illustravibes.arus.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.Index;
import androidx.room.Ignore;
import androidx.room.ForeignKey;
import com.illustravibes.arus.domain.TransactionType;

@Entity(tableName = "transactions", foreignKeys = {
        @ForeignKey(entity = WalletEntity.class, parentColumns = "id", childColumns = "walletId", onDelete = ForeignKey.RESTRICT),
        @ForeignKey(entity = CategoryEntity.class, parentColumns = "id", childColumns = "categoryId", onDelete = ForeignKey.RESTRICT)
}, indices = {@Index("walletId"), @Index("categoryId"), @Index(value = {"occurredAtEpochMillis", "id"}), @Index("transferGroupId"), @Index("verifiedCheckpointId")})
public class TransactionEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public TransactionType type;
    public Long walletId;
    public long amountRupiah;
    public String direction;
    public Long categoryId;
    public long occurredAtEpochMillis;
    public String note;
    public String merchant;
    public Long transferGroupId;
    public Long verifiedCheckpointId;
    public boolean needsRecheck;
    public Long deletedAtEpochMillis;

    @Ignore
    public TransactionEntity(TransactionType type, Long walletId, long amountRupiah, String direction, long occurredAtEpochMillis,
                             Long deletedAtEpochMillis) {
        if (amountRupiah <= 0) throw new IllegalArgumentException("Nominal transaksi harus positif.");
        if (type == TransactionType.ADJUSTMENT && !"PLUS".equals(direction) && !"MINUS".equals(direction)) {
            throw new IllegalArgumentException("Penyesuaian saldo harus memiliki arah PLUS atau MINUS.");
        }
        this.type = type;
        this.walletId = walletId;
        this.amountRupiah = amountRupiah;
        this.direction = direction;
        this.occurredAtEpochMillis = occurredAtEpochMillis;
        this.note = null;
        this.merchant = null;
        this.transferGroupId = null;
        this.deletedAtEpochMillis = deletedAtEpochMillis;
    }

    public TransactionEntity(TransactionType type, Long walletId, long amountRupiah, String direction,
                             Long categoryId, long occurredAtEpochMillis, String note, String merchant,
                             Long deletedAtEpochMillis) {
        this(type, walletId, amountRupiah, direction, occurredAtEpochMillis, deletedAtEpochMillis);
        this.categoryId = categoryId;
        this.note = note;
        this.merchant = merchant;
    }

    @Ignore
    public TransactionEntity(TransactionType type, Long walletId, long amountRupiah, String direction,
                             Long categoryId, long occurredAtEpochMillis, String note, String merchant,
                             Long transferGroupId, Long deletedAtEpochMillis) {
        this(type, walletId, amountRupiah, direction, categoryId, occurredAtEpochMillis, note, merchant, deletedAtEpochMillis);
        this.transferGroupId = transferGroupId;
    }
}
