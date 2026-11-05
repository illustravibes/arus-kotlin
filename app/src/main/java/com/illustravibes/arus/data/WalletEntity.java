package com.illustravibes.arus.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.Ignore;
import androidx.room.ColumnInfo;

@Entity(tableName = "wallets")
public class WalletEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public String name;
    public String type;
    public String icon;
    @ColumnInfo(defaultValue = "-10453621")
    public int colorArgb;
    public long initialBalanceRupiah;
    public Long creditLimitRupiah;
    public Integer billingDay;
    public int sortOrder;
    public Long defaultExpenseCategoryId;
    public boolean includeInTotal;
    public boolean isArchived;
    public Long deletedAtEpochMillis;

    @Ignore
    public WalletEntity(String name, String type, long initialBalanceRupiah,
                        boolean includeInTotal, boolean isArchived, Long deletedAtEpochMillis) {
        this(name, type, "account_balance_wallet", 0xFF607D8B, initialBalanceRupiah,
                includeInTotal, isArchived, deletedAtEpochMillis);
    }

    public WalletEntity(String name, String type, String icon, int colorArgb, long initialBalanceRupiah,
                        boolean includeInTotal, boolean isArchived, Long deletedAtEpochMillis) {
        this.name = name;
        this.type = type;
        this.icon = icon;
        this.colorArgb = colorArgb;
        this.initialBalanceRupiah = initialBalanceRupiah;
        this.creditLimitRupiah = null;
        this.billingDay = null;
        this.sortOrder = 0;
        this.defaultExpenseCategoryId = null;
        this.includeInTotal = includeInTotal;
        this.isArchived = isArchived;
        this.deletedAtEpochMillis = deletedAtEpochMillis;
    }
}
