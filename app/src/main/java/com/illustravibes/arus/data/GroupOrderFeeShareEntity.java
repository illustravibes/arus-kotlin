package com.illustravibes.arus.data;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "group_order_fee_shares", foreignKeys = {
        @ForeignKey(entity = GroupOrderFeeEntity.class, parentColumns = "id", childColumns = "feeId", onDelete = ForeignKey.CASCADE),
        @ForeignKey(entity = ContactEntity.class, parentColumns = "id", childColumns = "contactId", onDelete = ForeignKey.RESTRICT)
}, indices = {@Index("feeId"), @Index("contactId"), @Index(value = {"feeId", "contactId"}, unique = true)})
public class GroupOrderFeeShareEntity {
    @PrimaryKey(autoGenerate = true) public long id;
    public long feeId;
    public Long contactId;
    public long amountRupiah;

    public GroupOrderFeeShareEntity(long feeId, Long contactId, long amountRupiah) {
        this.feeId = feeId; this.contactId = contactId; this.amountRupiah = amountRupiah;
    }
}
