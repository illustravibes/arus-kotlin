package com.illustravibes.arus.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

@Dao
public abstract class MerchantCategoryHintDao {
    @Query("SELECT categoryId FROM merchant_category_hints WHERE merchantName = :merchantName LIMIT 1")
    public abstract Long suggestedCategoryId(String merchantName);

    @Query("UPDATE merchant_category_hints SET categoryId = :categoryId, useCount = useCount + 1 WHERE merchantName = :merchantName")
    protected abstract int increment(String merchantName, long categoryId);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract long insert(MerchantCategoryHintEntity hint);

    @Transaction
    public void recordConfirmedChoice(String merchantName, long categoryId) {
        if (merchantName == null || merchantName.trim().isEmpty()) return;
        if (increment(merchantName, categoryId) == 0)
            insert(new MerchantCategoryHintEntity(merchantName, categoryId, 1));
    }
}
