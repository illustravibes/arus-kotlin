package com.illustravibes.arus.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface AttachmentDao {
    @Insert
    long insert(AttachmentEntity attachment);

    @Query("SELECT * FROM attachments WHERE ownerType = :ownerType AND ownerId = :ownerId ORDER BY createdAtEpochMillis, id")
    List<AttachmentEntity> forOwner(String ownerType, long ownerId);

    @Query("SELECT * FROM attachments WHERE ownerType = 'PENDING' ORDER BY createdAtEpochMillis, id")
    List<AttachmentEntity> pending();

    @Query("SELECT * FROM attachments ORDER BY id")
    List<AttachmentEntity> allAttachments();

    @Query("UPDATE attachments SET ownerType = :ownerType, ownerId = :ownerId WHERE id = :id AND ownerType = 'PENDING'")
    int attach(long id, String ownerType, long ownerId);

    @Query("DELETE FROM attachments WHERE id = :id")
    int delete(long id);
}
