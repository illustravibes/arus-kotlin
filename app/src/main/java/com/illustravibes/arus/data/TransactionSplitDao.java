package com.illustravibes.arus.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface TransactionSplitDao {
    @Insert
    long insert(TransactionSplitEntity split);

    @Query("SELECT * FROM transaction_splits WHERE transactionId = :transactionId ORDER BY id")
    List<TransactionSplitEntity> splitsForTransaction(long transactionId);

    @Query("SELECT COALESCE(SUM(amountRupiah), 0) FROM transaction_splits WHERE transactionId = :transactionId")
    long splitTotal(long transactionId);

    @Query("DELETE FROM transaction_splits WHERE transactionId = :transactionId")
    int deleteForTransaction(long transactionId);
}
