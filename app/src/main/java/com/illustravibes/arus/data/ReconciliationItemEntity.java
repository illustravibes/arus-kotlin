package com.illustravibes.arus.data;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "reconciliation_items", foreignKeys = {
        @ForeignKey(entity = ReconciliationCheckpointEntity.class, parentColumns = "id",
                childColumns = "checkpointId", onDelete = ForeignKey.CASCADE),
        @ForeignKey(entity = TransactionEntity.class, parentColumns = "id",
                childColumns = "transactionId", onDelete = ForeignKey.CASCADE)
}, indices = {
        @Index("checkpointId"),
        @Index("transactionId"),
        @Index(value = {"checkpointId", "transactionId"}, unique = true),
})
public class ReconciliationItemEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public long checkpointId;
    public long transactionId;
    public boolean matched;

    public ReconciliationItemEntity(long checkpointId, long transactionId, boolean matched) {
        if (checkpointId <= 0 || transactionId <= 0)
            throw new IllegalArgumentException("Checkpoint dan transaksi wajib diisi.");
        this.checkpointId = checkpointId;
        this.transactionId = transactionId;
        this.matched = matched;
    }
}
