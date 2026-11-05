package com.illustravibes.arus.data;

import androidx.room.Entity;
import androidx.room.ColumnInfo;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "budgets", foreignKeys = {
        @ForeignKey(entity = CategoryEntity.class, parentColumns = "id", childColumns = "categoryId", onDelete = ForeignKey.RESTRICT)
}, indices = {@Index(value = {"categoryId", "periodStartEpochDay", "periodEndEpochDay"}, unique = true)})
public class BudgetEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;
    @ColumnInfo(defaultValue = "")
    public String name;
    public Long categoryId;
    public long amountRupiah;
    public int periodType;
    public long periodStartEpochDay;
    public long periodEndEpochDay;
    public boolean rolloverEnabled;
    public long rolloverRupiah;
    public int warningPercent;
    public boolean isArchived;

    public BudgetEntity(String name, Long categoryId, long amountRupiah, int periodType,
                        long periodStartEpochDay, long periodEndEpochDay,
                        boolean rolloverEnabled, long rolloverRupiah, int warningPercent,
                        boolean isArchived) {
        if (amountRupiah <= 0) throw new IllegalArgumentException("Anggaran harus lebih dari nol.");
        if (periodEndEpochDay <= periodStartEpochDay) throw new IllegalArgumentException("Periode anggaran tidak valid.");
        if (warningPercent < 50 || warningPercent > 99) throw new IllegalArgumentException("Ambang peringatan harus antara 50 dan 99 persen.");
        if (rolloverRupiah < 0) throw new IllegalArgumentException("Rollover tidak boleh negatif.");
        this.name = name;
        this.categoryId = categoryId;
        this.amountRupiah = amountRupiah;
        this.periodType = periodType;
        this.periodStartEpochDay = periodStartEpochDay;
        this.periodEndEpochDay = periodEndEpochDay;
        this.rolloverEnabled = rolloverEnabled;
        this.rolloverRupiah = rolloverRupiah;
        this.warningPercent = warningPercent;
        this.isArchived = isArchived;
    }
}
