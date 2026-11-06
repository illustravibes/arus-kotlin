package com.illustravibes.arus.data;

import android.content.Context;

import androidx.room.Room;

import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SupportFactory;

import java.util.Arrays;

public final class DatabaseFactory {
    public static final String DATABASE_NAME = "arus.db";

    private DatabaseFactory() {}

    public static ArusDatabase open(Context context) throws Exception {
        return open(context, DATABASE_NAME);
    }

    public static ArusDatabase open(Context context, String databaseName) throws Exception {
        SQLiteDatabase.loadLibs(context.getApplicationContext());
        byte[] passphrase = new DatabaseKeyStore(context).getOrCreatePassphrase();
        try {
            return openWithPassphrase(context, databaseName, passphrase);
        } finally {
            Arrays.fill(passphrase, (byte) 0);
        }
    }

    public static ArusDatabase openWithPassphrase(Context context, String databaseName, byte[] passphrase) throws Exception {
        SQLiteDatabase.loadLibs(context.getApplicationContext());
        byte[] factoryPassphrase = passphrase.clone();
        try {
            SupportFactory factory = new SupportFactory(factoryPassphrase, null, true);
            ArusDatabase database = Room.databaseBuilder(context.getApplicationContext(), ArusDatabase.class, databaseName)
                    .openHelperFactory(factory)
                    .addMigrations(ArusDatabase.MIGRATION_20_21, ArusDatabase.MIGRATION_19_20,
                            ArusDatabase.MIGRATION_18_19,
                            ArusDatabase.MIGRATION_17_18,
                            ArusDatabase.MIGRATION_16_17, ArusDatabase.MIGRATION_15_16,
                            ArusDatabase.MIGRATION_1_15, ArusDatabase.MIGRATION_2_15,
                            ArusDatabase.MIGRATION_3_15, ArusDatabase.MIGRATION_4_15,
                            ArusDatabase.MIGRATION_5_15, ArusDatabase.MIGRATION_6_15,
                            ArusDatabase.MIGRATION_7_15, ArusDatabase.MIGRATION_8_15,
                            ArusDatabase.MIGRATION_9_15, ArusDatabase.MIGRATION_10_15,
                            ArusDatabase.MIGRATION_1_14, ArusDatabase.MIGRATION_2_14,
                            ArusDatabase.MIGRATION_3_14, ArusDatabase.MIGRATION_4_14,
                            ArusDatabase.MIGRATION_5_14, ArusDatabase.MIGRATION_6_14,
                            ArusDatabase.MIGRATION_7_14, ArusDatabase.MIGRATION_8_14,
                            ArusDatabase.MIGRATION_9_14, ArusDatabase.MIGRATION_10_14,
                            ArusDatabase.MIGRATION_11_14, ArusDatabase.MIGRATION_12_14,
                            ArusDatabase.MIGRATION_13_15, ArusDatabase.MIGRATION_12_15, ArusDatabase.MIGRATION_11_15,
                            ArusDatabase.MIGRATION_14_15, ArusDatabase.MIGRATION_13_14,
                            ArusDatabase.MIGRATION_1_13, ArusDatabase.MIGRATION_2_13,
                            ArusDatabase.MIGRATION_3_13, ArusDatabase.MIGRATION_4_13,
                            ArusDatabase.MIGRATION_5_13, ArusDatabase.MIGRATION_6_13,
                            ArusDatabase.MIGRATION_7_13, ArusDatabase.MIGRATION_8_13,
                            ArusDatabase.MIGRATION_9_13, ArusDatabase.MIGRATION_10_13,
                            ArusDatabase.MIGRATION_11_13,
                            ArusDatabase.MIGRATION_1_12, ArusDatabase.MIGRATION_2_12,
                             ArusDatabase.MIGRATION_3_12, ArusDatabase.MIGRATION_4_12,
                            ArusDatabase.MIGRATION_5_12, ArusDatabase.MIGRATION_6_12,
                            ArusDatabase.MIGRATION_7_12, ArusDatabase.MIGRATION_8_12,
                            ArusDatabase.MIGRATION_9_12, ArusDatabase.MIGRATION_10_12,
                            ArusDatabase.MIGRATION_11_12,
                            ArusDatabase.MIGRATION_1_11, ArusDatabase.MIGRATION_2_11,
                            ArusDatabase.MIGRATION_3_11, ArusDatabase.MIGRATION_4_11,
                            ArusDatabase.MIGRATION_5_11, ArusDatabase.MIGRATION_6_11,
                            ArusDatabase.MIGRATION_7_11, ArusDatabase.MIGRATION_8_11,
                            ArusDatabase.MIGRATION_9_11, ArusDatabase.MIGRATION_10_11,
                            ArusDatabase.MIGRATION_1_10, ArusDatabase.MIGRATION_2_10,
                            ArusDatabase.MIGRATION_3_10, ArusDatabase.MIGRATION_4_10, ArusDatabase.MIGRATION_5_10,
                            ArusDatabase.MIGRATION_6_10, ArusDatabase.MIGRATION_7_10, ArusDatabase.MIGRATION_8_10,
                            ArusDatabase.MIGRATION_9_10,
                            ArusDatabase.MIGRATION_1_6, ArusDatabase.MIGRATION_2_6,
                            ArusDatabase.MIGRATION_3_6, ArusDatabase.MIGRATION_4_6, ArusDatabase.MIGRATION_5_6,
                            ArusDatabase.MIGRATION_3_4, ArusDatabase.MIGRATION_4_5, ArusDatabase.MIGRATION_2_3)
                    .build();
            database.getOpenHelper().getWritableDatabase();
            return database;
        } finally {
            Arrays.fill(factoryPassphrase, (byte) 0);
        }
    }
}
