package com.illustravibes.arus.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface ReconciliationCheckpointDao {
    @Insert
    long insert(ReconciliationCheckpointEntity checkpoint);

    @Query("SELECT * FROM reconciliation_checkpoints WHERE id = :id")
    ReconciliationCheckpointEntity checkpointById(long id);

    @Query("SELECT * FROM reconciliation_checkpoints WHERE walletId = :walletId ORDER BY occurredAtEpochMillis DESC, id DESC")
    List<ReconciliationCheckpointEntity> checkpointsForWallet(long walletId);

    @Query("UPDATE reconciliation_checkpoints SET needsRecheck = 1 WHERE id = :checkpointId")
    int markNeedsRecheck(long checkpointId);
}
