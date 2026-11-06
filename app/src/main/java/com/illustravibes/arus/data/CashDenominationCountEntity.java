package com.illustravibes.arus.data;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "cash_denomination_counts", foreignKeys = {
        @ForeignKey(entity = ReconciliationCheckpointEntity.class, parentColumns = "id",
                childColumns = "checkpointId", onDelete = ForeignKey.CASCADE)
}, indices = {@Index(value = {"checkpointId", "denominationRupiah"}, unique = true)})
public class CashDenominationCountEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public long checkpointId;
    public long denominationRupiah;
    public long pieceCount;

    public CashDenominationCountEntity(long checkpointId, long denominationRupiah, long pieceCount) {
        if (checkpointId <= 0 || denominationRupiah <= 0 || pieceCount <= 0)
            throw new IllegalArgumentException("Pecahan, jumlah, dan checkpoint harus positif.");
        this.checkpointId = checkpointId;
        this.denominationRupiah = denominationRupiah;
        this.pieceCount = pieceCount;
    }
}
