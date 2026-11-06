package com.illustravibes.arus.data;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "group_order_items", foreignKeys = {
        @ForeignKey(entity = GroupOrderEntity.class, parentColumns = "id", childColumns = "groupOrderId", onDelete = ForeignKey.CASCADE)
}, indices = @Index("groupOrderId"))
public class GroupOrderItemEntity {
    @PrimaryKey(autoGenerate = true) public long id;
    public long groupOrderId;
    public String name;
    public long unitPriceRupiah;
    public int quantity;
    public String source;
    public String status;

    public GroupOrderItemEntity(long groupOrderId, String name, long unitPriceRupiah, int quantity,
                                String source, String status) {
        if (name == null || name.trim().isEmpty()) throw new IllegalArgumentException("Nama item wajib diisi.");
        if (unitPriceRupiah <= 0 || quantity <= 0) throw new IllegalArgumentException("Harga dan jumlah item harus positif.");
        if (!"MANUAL".equals(source) && !"OCR".equals(source)) throw new IllegalArgumentException("Sumber item tidak valid.");
        if (!"UNASSIGNED".equals(status) && !"ASSIGNED".equals(status)) throw new IllegalArgumentException("Status item tidak valid.");
        this.groupOrderId = groupOrderId; this.name = name.trim(); this.unitPriceRupiah = unitPriceRupiah;
        this.quantity = quantity; this.source = source; this.status = status;
    }
}
