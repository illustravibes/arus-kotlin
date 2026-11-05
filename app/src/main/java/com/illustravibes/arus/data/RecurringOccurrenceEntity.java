package com.illustravibes.arus.data;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "recurring_occurrences", foreignKeys = {
        @ForeignKey(entity = RecurringRuleEntity.class, parentColumns = "id", childColumns = "ruleId", onDelete = ForeignKey.CASCADE),
        @ForeignKey(entity = TransactionEntity.class, parentColumns = "id", childColumns = "transactionId", onDelete = ForeignKey.SET_NULL)
}, indices = {@Index(value = {"ruleId", "occurrenceEpochDay"}, unique = true), @Index("transactionId")})
public class RecurringOccurrenceEntity {
    @PrimaryKey(autoGenerate = true) public long id;
    public long ruleId;
    public long occurrenceEpochDay;
    public String status;
    public Long transactionId;

    public RecurringOccurrenceEntity(long ruleId, long occurrenceEpochDay, String status, Long transactionId) {
        this.ruleId = ruleId;
        this.occurrenceEpochDay = occurrenceEpochDay;
        this.status = status;
        this.transactionId = transactionId;
    }
}
