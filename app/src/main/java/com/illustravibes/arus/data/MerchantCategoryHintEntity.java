package com.illustravibes.arus.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "merchant_category_hints", foreignKeys = {
        @ForeignKey(entity = CategoryEntity.class, parentColumns = "id", childColumns = "categoryId", onDelete = ForeignKey.RESTRICT)
}, indices = {
        @Index(value = {"merchantName"}, unique = true),
        @Index("categoryId")
})
public class MerchantCategoryHintEntity {
    @PrimaryKey
    @NonNull public String merchantName;
    public long categoryId;
    public int useCount;

    public MerchantCategoryHintEntity(@NonNull String merchantName, long categoryId, int useCount) {
        this.merchantName = merchantName;
        this.categoryId = categoryId;
        this.useCount = useCount;
    }
}
