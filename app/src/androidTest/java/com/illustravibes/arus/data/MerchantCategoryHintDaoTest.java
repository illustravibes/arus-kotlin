package com.illustravibes.arus.data;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class MerchantCategoryHintDaoTest {
    private ArusDatabase database;
    private long foodCategory;
    private long transportCategory;

    @Before public void setUp() {
        database = Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(), ArusDatabase.class).build();
        foodCategory = database.categoryDao().insert(new CategoryEntity("Makan", "EXPENSE", "restaurant", 0, false, false, 0));
        transportCategory = database.categoryDao().insert(new CategoryEntity("Transportasi", "EXPENSE", "car", 0, false, false, 1));
    }

    @After public void tearDown() { database.close(); }

    @Test public void confirmedCategoryChoiceUpdatesMerchantHintAndUsageCount() {
        MerchantCategoryHintDao hints = database.merchantCategoryHintDao();

        hints.recordConfirmedChoice("WARUNG BUDI", foodCategory);
        assertEquals(foodCategory, hints.suggestedCategoryId("WARUNG BUDI").longValue());
        hints.recordConfirmedChoice("WARUNG BUDI", transportCategory);

        assertEquals(transportCategory, hints.suggestedCategoryId("WARUNG BUDI").longValue());
        try (android.database.Cursor cursor = database.getOpenHelper().getWritableDatabase()
                .query("SELECT useCount FROM merchant_category_hints WHERE merchantName='WARUNG BUDI'")) {
            if (!cursor.moveToFirst()) throw new AssertionError("Merchant hint was lost.");
            assertEquals(2, cursor.getInt(0));
        }
    }
}
