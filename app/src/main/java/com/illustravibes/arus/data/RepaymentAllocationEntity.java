package com.illustravibes.arus.data;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "repayment_allocations", foreignKeys = {
        @ForeignKey(entity = TransactionEntity.class, parentColumns = "id", childColumns = "transactionId", onDelete = ForeignKey.CASCADE),
        @ForeignKey(entity = DebtEntity.class, parentColumns = "id", childColumns = "debtId", onDelete = ForeignKey.RESTRICT)
}, indices = {@Index("transactionId"), @Index("debtId"), @Index(value = {"transactionId", "debtId"}, unique = true)})
public class RepaymentAllocationEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public long transactionId;
    public long debtId;
    public long amountRupiah;

    public RepaymentAllocationEntity(long transactionId, long debtId, long amountRupiah) {
        if (amountRupiah <= 0) throw new IllegalArgumentException("Nominal alokasi harus positif.");
        this.transactionId = transactionId;
        this.debtId = debtId;
        this.amountRupiah = amountRupiah;
    }
}
