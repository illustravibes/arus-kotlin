package com.illustravibes.arus.data;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "group_order_owners", foreignKeys = {
        @ForeignKey(entity = GroupOrderItemEntity.class, parentColumns = "id", childColumns = "itemId", onDelete = ForeignKey.CASCADE),
        @ForeignKey(entity = ContactEntity.class, parentColumns = "id", childColumns = "contactId", onDelete = ForeignKey.RESTRICT)
}, indices = {@Index("itemId"), @Index("contactId"), @Index(value = {"itemId", "contactId"}, unique = true)})
public class GroupOrderOwnerEntity {
    @PrimaryKey(autoGenerate = true) public long id;
    public long itemId;
    public Long contactId;
    public long shareWeight;
    public Long shareAmountRupiah;

    public GroupOrderOwnerEntity(long itemId, Long contactId, long shareWeight, Long shareAmountRupiah) {
        if (shareWeight <= 0 || (shareAmountRupiah != null && shareAmountRupiah <= 0))
            throw new IllegalArgumentException("Alokasi pemilik item harus positif.");
        this.itemId = itemId; this.contactId = contactId; this.shareWeight = shareWeight;
        this.shareAmountRupiah = shareAmountRupiah;
    }
}
