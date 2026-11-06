package com.illustravibes.arus.data;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.room.Ignore;

@Entity(tableName = "debts", foreignKeys = {
        @ForeignKey(entity = ContactEntity.class, parentColumns = "id", childColumns = "contactId", onDelete = ForeignKey.RESTRICT),
        @ForeignKey(entity = TransactionEntity.class, parentColumns = "id", childColumns = "originTransactionId", onDelete = ForeignKey.RESTRICT),
        @ForeignKey(entity = GroupOrderEntity.class, parentColumns = "id", childColumns = "groupOrderId", onDelete = ForeignKey.RESTRICT)
}, indices = {@Index("contactId"), @Index("originTransactionId"), @Index("groupOrderId"), @Index("status")})
public class DebtEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public long contactId;
    public String direction;
    public String source;
    public Long originTransactionId;
    public Long groupOrderId;
    public long principalRupiah;
    public String status;
    public Long dueDateEpochMillis;
    public String note;
    public long createdAtEpochMillis;
    public long updatedAtEpochMillis;
    public Long deletedAtEpochMillis;

    @Ignore
    public DebtEntity(long contactId, String direction, String source, Long originTransactionId,
                      long principalRupiah, String status, Long dueDateEpochMillis, String note,
                      long createdAtEpochMillis, long updatedAtEpochMillis, Long deletedAtEpochMillis) {
        this(contactId, direction, source, originTransactionId, null, principalRupiah, status,
                dueDateEpochMillis, note, createdAtEpochMillis, updatedAtEpochMillis, deletedAtEpochMillis);
    }

    public DebtEntity(long contactId, String direction, String source, Long originTransactionId, Long groupOrderId,
                      long principalRupiah, String status, Long dueDateEpochMillis, String note,
                      long createdAtEpochMillis, long updatedAtEpochMillis, Long deletedAtEpochMillis) {
        if (direction == null || source == null || status == null) throw new IllegalArgumentException("Arah, sumber, dan status utang wajib diisi.");
        if (!"LENT".equals(direction) && !"BORROWED".equals(direction)) throw new IllegalArgumentException("Arah utang tidak valid.");
        if (!"MANUAL".equals(source) && !"GROUP_ORDER".equals(source)) throw new IllegalArgumentException("Sumber utang tidak valid.");
        if (principalRupiah <= 0) throw new IllegalArgumentException("Pokok utang harus positif.");
        if (!"OPEN".equals(status) && !"PARTIAL".equals(status) && !"PAID".equals(status) && !"WRITTEN_OFF".equals(status)) throw new IllegalArgumentException("Status utang tidak valid.");
        this.contactId = contactId;
        this.direction = direction;
        this.source = source;
        this.originTransactionId = originTransactionId;
        this.groupOrderId = groupOrderId;
        this.principalRupiah = principalRupiah;
        this.status = status;
        this.dueDateEpochMillis = dueDateEpochMillis;
        this.note = note;
        this.createdAtEpochMillis = createdAtEpochMillis;
        this.updatedAtEpochMillis = updatedAtEpochMillis;
        this.deletedAtEpochMillis = deletedAtEpochMillis;
    }
}
