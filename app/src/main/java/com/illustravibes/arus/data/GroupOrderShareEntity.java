package com.illustravibes.arus.data;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "group_order_shares", foreignKeys = {
        @ForeignKey(entity = GroupOrderEntity.class, parentColumns = "id", childColumns = "groupOrderId", onDelete = ForeignKey.CASCADE),
        @ForeignKey(entity = ContactEntity.class, parentColumns = "id", childColumns = "contactId", onDelete = ForeignKey.RESTRICT)
}, indices = {@Index("groupOrderId"), @Index("contactId"), @Index(value = {"groupOrderId", "contactId"}, unique = true)})
public class GroupOrderShareEntity {
    @PrimaryKey(autoGenerate = true) public long id;
    public long groupOrderId;
    public Long contactId;
    public long itemsAmountRupiah;
    public long feeAmountRupiah;
    public long totalAmountRupiah;

    public GroupOrderShareEntity(long groupOrderId, Long contactId, long itemsAmountRupiah,
                                 long feeAmountRupiah, long totalAmountRupiah) {
        this.groupOrderId = groupOrderId; this.contactId = contactId;
        this.itemsAmountRupiah = itemsAmountRupiah; this.feeAmountRupiah = feeAmountRupiah;
        this.totalAmountRupiah = totalAmountRupiah;
    }
}
