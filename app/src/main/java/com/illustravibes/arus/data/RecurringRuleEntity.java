package com.illustravibes.arus.data;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "recurring_rules", foreignKeys = {
        @ForeignKey(entity = WalletEntity.class, parentColumns = "id", childColumns = "walletId", onDelete = ForeignKey.RESTRICT),
        @ForeignKey(entity = CategoryEntity.class, parentColumns = "id", childColumns = "categoryId", onDelete = ForeignKey.RESTRICT)
}, indices = {@Index("walletId"), @Index("categoryId")})
public class RecurringRuleEntity {
    @PrimaryKey(autoGenerate = true) public long id;
    public String name;
    public String transactionType;
    public long walletId;
    public long categoryId;
    public long amountRupiah;
    public String note;
    public int intervalType;
    public long nextOccurrenceEpochDay;
    public Long endEpochDay;
    public boolean confirmationRequired;
    public boolean isActive;

    public RecurringRuleEntity(String name, String transactionType, long walletId, long categoryId,
                               long amountRupiah, String note, int intervalType,
                               long nextOccurrenceEpochDay, Long endEpochDay,
                               boolean confirmationRequired, boolean isActive) {
        this.name = name;
        this.transactionType = transactionType;
        this.walletId = walletId;
        this.categoryId = categoryId;
        this.amountRupiah = amountRupiah;
        this.note = note;
        this.intervalType = intervalType;
        this.nextOccurrenceEpochDay = nextOccurrenceEpochDay;
        this.endEpochDay = endEpochDay;
        this.confirmationRequired = confirmationRequired;
        this.isActive = isActive;
    }
}
