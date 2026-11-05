package com.illustravibes.arus.data;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "reconciliation_checkpoints", foreignKeys = {
        @ForeignKey(entity = WalletEntity.class, parentColumns = "id", childColumns = "walletId", onDelete = ForeignKey.RESTRICT)
}, indices = {@Index("walletId"), @Index("occurredAtEpochMillis")})
public class ReconciliationCheckpointEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public long walletId;
    public long occurredAtEpochMillis;
    public long recordedBalanceRupiah;
    public long actualBalanceRupiah;
    public long differenceRupiah;
    public String resolution;
    public boolean needsRecheck;

    public ReconciliationCheckpointEntity(long walletId, long occurredAtEpochMillis,
                                          long recordedBalanceRupiah, long actualBalanceRupiah,
                                          long differenceRupiah, String resolution) {
        this.walletId = walletId;
        this.occurredAtEpochMillis = occurredAtEpochMillis;
        this.recordedBalanceRupiah = recordedBalanceRupiah;
        this.actualBalanceRupiah = actualBalanceRupiah;
        this.differenceRupiah = differenceRupiah;
        this.resolution = resolution;
        this.needsRecheck = false;
    }
}
