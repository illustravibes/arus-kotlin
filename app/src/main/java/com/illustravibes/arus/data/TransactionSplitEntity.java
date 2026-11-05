package com.illustravibes.arus.data;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "transaction_splits", foreignKeys = {
        @ForeignKey(entity = TransactionEntity.class, parentColumns = "id", childColumns = "transactionId", onDelete = ForeignKey.CASCADE),
        @ForeignKey(entity = CategoryEntity.class, parentColumns = "id", childColumns = "categoryId", onDelete = ForeignKey.RESTRICT)
}, indices = {@Index("transactionId"), @Index("categoryId")})
public class TransactionSplitEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public long transactionId;
    public long amountRupiah;
    public Long categoryId;
    public boolean countsInReport;
    public boolean countsInBudget;

    public TransactionSplitEntity(long transactionId, long amountRupiah, Long categoryId,
                                  boolean countsInReport, boolean countsInBudget) {
        if (amountRupiah <= 0) throw new IllegalArgumentException("Nominal split harus positif.");
        this.transactionId = transactionId;
        this.amountRupiah = amountRupiah;
        this.categoryId = categoryId;
        this.countsInReport = countsInReport;
        this.countsInBudget = countsInBudget;
    }
}
