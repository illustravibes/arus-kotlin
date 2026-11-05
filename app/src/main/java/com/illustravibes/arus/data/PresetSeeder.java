package com.illustravibes.arus.data;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class PresetSeeder {
    private PresetSeeder() {}

    public static void seed(ArusDatabase database, Context context) throws Exception {
        if (database.categoryDao().count() == 0) {
            JSONObject root = readAsset(context, "categories.json");
            List<CategoryEntity> categories = new ArrayList<>();
            JSONArray values = root.getJSONArray("categories");
            for (int i = 0; i < values.length(); i++) {
                JSONObject item = values.getJSONObject(i);
                categories.add(new CategoryEntity(item.getString("name"), item.getString("kind"),
                        item.getString("icon"), (int) Long.parseLong(item.getString("colorArgb"), 16), false, false, i));
            }
            database.runInTransaction(() -> database.categoryDao().insertAll(categories));
        }
        if (database.categoryDao().systemCount() == 0) {
            String[][] system = {
                    {"ADMIN_FEE", "Admin Bank", "receipt", "FF607D8B", "EXPENSE"},
                    {"UNTRACKED_DIFF", "Selisih Tak Terlacak", "difference", "FF9E9E9E", "EXPENSE"},
                    {"WRITE_OFF", "Ikhlas", "volunteer_activism", "FFE91E63", "EXPENSE"},
                    {"INTEREST_PENALTY", "Bunga & Denda", "percent", "FFF44336", "EXPENSE"},
                    {"BALANCE_ADJUST", "Penyesuaian Saldo", "tune", "FF607D8B", "EXPENSE"},
                    {"DEBT_FORGIVEN", "Utang Dihapuskan", "handshake", "FF4CAF50", "INCOME"}
            };
            List<CategoryEntity> systemCategories = new ArrayList<>();
            for (String[] item : system) {
                systemCategories.add(new CategoryEntity(item[0], item[4], item[2],
                        (int) Long.parseLong(item[3], 16), true, false, systemCategories.size()));
            }
            database.runInTransaction(() -> database.categoryDao().insertAll(systemCategories));
        }
    }

    private static JSONObject readAsset(Context context, String name) throws Exception {
        try (InputStream input = context.getAssets().open(name); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096]; int read;
            while ((read = input.read(buffer)) != -1) output.write(buffer, 0, read);
            return new JSONObject(new String(output.toByteArray(), StandardCharsets.UTF_8));
        }
    }
}
