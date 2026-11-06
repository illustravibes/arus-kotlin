package com.illustravibes.arus.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface CashDenominationCountDao {
    @Insert
    long insert(CashDenominationCountEntity count);

    @Query("SELECT * FROM cash_denomination_counts WHERE checkpointId = :checkpointId ORDER BY denominationRupiah DESC")
    List<CashDenominationCountEntity> forCheckpoint(long checkpointId);
}
