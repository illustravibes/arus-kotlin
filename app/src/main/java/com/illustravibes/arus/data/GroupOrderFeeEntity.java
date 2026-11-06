package com.illustravibes.arus.data;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "group_order_fees", foreignKeys = {
        @ForeignKey(entity = GroupOrderEntity.class, parentColumns = "id", childColumns = "groupOrderId", onDelete = ForeignKey.CASCADE)
}, indices = @Index("groupOrderId"))
public class GroupOrderFeeEntity {
    @PrimaryKey(autoGenerate = true) public long id;
    public long groupOrderId;
    public String label;
    public long amountRupiah;
    public String splitMode;

    public GroupOrderFeeEntity(long groupOrderId, String label, long amountRupiah, String splitMode) {
        if (label == null || label.trim().isEmpty()) throw new IllegalArgumentException("Nama biaya wajib diisi.");
        if (!"EQUAL".equals(splitMode) && !"PROPORTIONAL".equals(splitMode) && !"MANUAL".equals(splitMode))
            throw new IllegalArgumentException("Mode pembagian biaya tidak valid.");
        this.groupOrderId = groupOrderId; this.label = label.trim(); this.amountRupiah = amountRupiah;
        this.splitMode = splitMode;
    }
}
