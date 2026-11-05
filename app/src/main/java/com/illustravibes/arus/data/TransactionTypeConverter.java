package com.illustravibes.arus.data;

import androidx.room.TypeConverter;
import com.illustravibes.arus.domain.TransactionType;

public final class TransactionTypeConverter {
    @TypeConverter
    public String fromTransactionType(TransactionType value) {
        return value.name();
    }

    @TypeConverter
    public TransactionType toTransactionType(String value) {
        return TransactionType.valueOf(value);
    }
}
