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

    @Query("SELECT strftime('%Y-%m', occurredAtEpochMillis / 1000, 'unixepoch', 'localtime') AS month, " +
            "SUM(CASE WHEN differenceRupiah < 0 THEN -differenceRupiah ELSE 0 END) AS untrackedRupiah " +
            "FROM reconciliation_checkpoints WHERE walletId = :walletId " +
            "AND occurredAtEpochMillis >= :startMillis AND occurredAtEpochMillis < :endExclusiveMillis " +
            "GROUP BY month ORDER BY month")
    List<MonthlyUntrackedTotal> monthlyUntrackedTotals(long walletId, long startMillis, long endExclusiveMillis);

    @Query("UPDATE reconciliation_checkpoints SET needsRecheck = 1 WHERE id = :checkpointId")
    int markNeedsRecheck(long checkpointId);
}
