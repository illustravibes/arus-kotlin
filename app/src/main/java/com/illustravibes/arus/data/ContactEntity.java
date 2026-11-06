package com.illustravibes.arus.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "contacts")
public class ContactEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public String name;
    public String preferredWalletNote;
    public String note;
    public boolean isArchived;
    public long createdAtEpochMillis;
    public long updatedAtEpochMillis;

    public ContactEntity(String name, String preferredWalletNote, String note, boolean isArchived,
                         long createdAtEpochMillis, long updatedAtEpochMillis) {
        if (name == null || name.trim().isEmpty()) throw new IllegalArgumentException("Nama kontak wajib diisi.");
        this.name = name;
        this.preferredWalletNote = preferredWalletNote;
        this.note = note;
        this.isArchived = isArchived;
        this.createdAtEpochMillis = createdAtEpochMillis;
        this.updatedAtEpochMillis = updatedAtEpochMillis;
    }
}
