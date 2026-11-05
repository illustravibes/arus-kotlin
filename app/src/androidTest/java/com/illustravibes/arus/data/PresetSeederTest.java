package com.illustravibes.arus.data;

import android.content.Context;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class PresetSeederTest {
    @Test public void seedsAssetsIdempotentlyAndStoresWalletPresetMetadata() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        String name = "preset-seeder-test";
        context.deleteDatabase(name);
        ArusDatabase db = Room.databaseBuilder(context, ArusDatabase.class, name).build();
        try {
            PresetSeeder.seed(db, context);
            assertEquals(26, db.categoryDao().count());
            assertTrue(db.categoryDao().activeCategories().stream().anyMatch(category -> "Makan & Minum".equals(category.name)));
            PresetSeeder.seed(db, context);
            assertEquals(26, db.categoryDao().count());
            assertEquals(6, db.categoryDao().systemCount());
            try (java.io.InputStream input = context.getAssets().open("wallet-presets.json")) {
                byte[] bytes = new byte[input.available()];
                assertEquals(bytes.length, input.read(bytes));
                assertTrue(new String(bytes, java.nio.charset.StandardCharsets.UTF_8).contains("\"name\":\"BCA\""));
            }
        } finally {
            db.close();
            context.deleteDatabase(name);
        }
    }
}
