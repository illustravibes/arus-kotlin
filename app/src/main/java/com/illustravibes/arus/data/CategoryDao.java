package com.illustravibes.arus.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface CategoryDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(CategoryEntity category);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertAll(List<CategoryEntity> categories);

    @Query("SELECT * FROM categories WHERE isArchived = 0 ORDER BY kind, sortOrder, id")
    List<CategoryEntity> activeCategories();

    @Query("SELECT * FROM categories ORDER BY kind, sortOrder, id")
    List<CategoryEntity> allCategories();

    @Update
    int update(CategoryEntity category);

    @Query("SELECT * FROM categories WHERE id = :id")
    CategoryEntity findById(long id);

    @Query("SELECT COUNT(*) FROM transactions WHERE categoryId = :id AND deletedAtEpochMillis IS NULL")
    int activeTransactionCount(long id);

    @Query("UPDATE transactions SET categoryId = :targetId WHERE categoryId = :sourceId AND deletedAtEpochMillis IS NULL")
    int moveTransactions(long sourceId, long targetId);

    @Query("SELECT COUNT(*) FROM wallets WHERE defaultExpenseCategoryId = :id")
    int walletReferenceCount(long id);

    @Query("UPDATE wallets SET defaultExpenseCategoryId = NULL WHERE defaultExpenseCategoryId = :id")
    int clearWalletDefaults(long id);

    @Query("UPDATE transactions SET categoryId = NULL WHERE categoryId = :id AND deletedAtEpochMillis IS NULL")
    int clearTransactionCategories(long id);

    @Query("UPDATE categories SET isArchived = 1 WHERE id = :id AND isSystem = 0")
    int archive(long id);

    @Query("DELETE FROM categories WHERE id = :id AND isSystem = 0")
    int deleteCustom(long id);

    @Query("UPDATE categories SET sortOrder = :sortOrder WHERE id = :id")
    int updateSortOrder(long id, int sortOrder);

    @Query("UPDATE categories SET name = :name, icon = :icon, colorArgb = :color WHERE id = :id AND isSystem = 0")
    int editCustom(long id, String name, String icon, int color);

    @Query("SELECT COUNT(*) FROM categories")
    int count();

    @Query("SELECT COUNT(*) FROM categories WHERE kind = :kind AND name = :name AND id != :ignoreId")
    int countName(String kind, String name, long ignoreId);

    @Query("SELECT COUNT(*) FROM categories WHERE kind = :kind AND name = :name")
    int countName(String kind, String name);

    @Query("SELECT COUNT(*) FROM categories WHERE isSystem = 1")
    int systemCount();

    @Query("SELECT * FROM categories WHERE kind = :kind AND isArchived = 0 ORDER BY sortOrder, id")
    List<CategoryEntity> activeCategories(String kind);

    @Query("SELECT * FROM categories WHERE kind = :kind AND isArchived = 0 AND name LIKE '%' || :query || '%' ORDER BY sortOrder, id")
    List<CategoryEntity> search(String kind, String query);
}
