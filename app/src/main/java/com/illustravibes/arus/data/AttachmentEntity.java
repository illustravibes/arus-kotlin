package com.illustravibes.arus.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "attachments", indices = {@Index(value = {"ownerType", "ownerId"})})
public class AttachmentEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull public String ownerType;
    public Long ownerId;
    @NonNull public String filePath;
    public String ocrRawText;
    public long createdAtEpochMillis;

    public AttachmentEntity(@NonNull String ownerType, Long ownerId, @NonNull String filePath,
                            String ocrRawText, long createdAtEpochMillis) {
        this.ownerType = ownerType;
        this.ownerId = ownerId;
        this.filePath = filePath;
        this.ocrRawText = ocrRawText;
        this.createdAtEpochMillis = createdAtEpochMillis;
    }
}
