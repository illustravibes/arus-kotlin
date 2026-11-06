package com.illustravibes.arus.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ReconciliationItemDao {
    @Insert
    long insert(ReconciliationItemEntity item);

    @Query("SELECT * FROM reconciliation_items WHERE checkpointId = :checkpointId ORDER BY id")
    List<ReconciliationItemEntity> itemsForCheckpoint(long checkpointId);
}
