package com.illustravibes.arus.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Update;
import java.util.List;

@Dao
public interface WalletDao {
    @Insert
    long insert(WalletEntity wallet);

    @Update
    int update(WalletEntity wallet);

    @androidx.room.Query("SELECT * FROM wallets WHERE deletedAtEpochMillis IS NULL AND isArchived = 0 ORDER BY sortOrder, id")
    List<WalletEntity> activeWallets();

    @androidx.room.Query("SELECT * FROM wallets WHERE deletedAtEpochMillis IS NULL AND isArchived = 1 ORDER BY sortOrder, id")
    List<WalletEntity> archivedWallets();

    @androidx.room.Query("SELECT * FROM wallets WHERE deletedAtEpochMillis IS NULL ORDER BY sortOrder, id")
    List<WalletEntity> allWallets();

    @androidx.room.Query("SELECT * FROM wallets WHERE id = :id AND deletedAtEpochMillis IS NULL")
    WalletEntity findById(long id);

    @androidx.room.Query("UPDATE wallets SET isArchived = 1 WHERE id = :id")
    int archive(long id);

    @androidx.room.Query("UPDATE wallets SET sortOrder = :sortOrder WHERE id = :id")
    int updateSortOrder(long id, int sortOrder);

    @androidx.room.Query("UPDATE wallets SET defaultExpenseCategoryId = :categoryId WHERE id = :walletId")
    int updateDefaultExpenseCategory(long walletId, Long categoryId);

    @androidx.room.Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM wallets WHERE deletedAtEpochMillis IS NULL")
    int nextSortOrder();

    @androidx.room.Query("SELECT COALESCE(SUM(amountRupiah), 0) FROM transactions WHERE walletId = :id AND deletedAtEpochMillis IS NULL")
    long transactionTotal(long id);

    @androidx.room.Query("SELECT COUNT(*) FROM wallets WHERE deletedAtEpochMillis IS NULL AND isArchived = 0")
    int activeWalletCount();

    @androidx.room.Query("SELECT COUNT(*) FROM wallets WHERE name = :name AND type = :type AND deletedAtEpochMillis IS NULL")
    int countByNameAndType(String name, String type);

    @androidx.room.Query("SELECT initialBalanceRupiah FROM wallets WHERE id = :walletId AND deletedAtEpochMillis IS NULL")
    Long initialBalance(long walletId);
}
