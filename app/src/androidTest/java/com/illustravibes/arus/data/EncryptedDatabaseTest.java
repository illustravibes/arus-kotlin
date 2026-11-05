package com.illustravibes.arus.data;

import android.content.Context;

import androidx.room.Room;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SupportFactory;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

@RunWith(AndroidJUnit4.class)
public class EncryptedDatabaseTest {
    private static final String DATABASE_NAME = "encrypted_database_test.db";
    private final Context context = ApplicationProvider.getApplicationContext();

    @Before
    public void setUp() {
        SQLiteDatabase.loadLibs(context);
        context.deleteDatabase(DATABASE_NAME);
    }

    @After
    public void tearDown() {
        context.deleteDatabase(DATABASE_NAME);
    }

    @Test
    public void databaseReopensWithCorrectPassphraseAndRejectsWrongOne() {
        byte[] correctKey = new byte[32];
        byte[] wrongKey = new byte[32];
        Arrays.fill(correctKey, (byte) 7);
        Arrays.fill(wrongKey, (byte) 9);
        try {
            ArusDatabase first = open(correctKey);
            long walletId = first.walletDao().insert(new WalletEntity("Test", "CASH", 0, true, false, null));
            long id = first.transactionDao().insert(new TransactionEntity(
                    com.illustravibes.arus.domain.TransactionType.EXPENSE, walletId, 500, null, 10, null));
            first.close();

            ArusDatabase reopened = open(correctKey);
            assertEquals(id, reopened.transactionDao().activeTransactions().get(0).id);
            reopened.close();

            assertThrows(RuntimeException.class, () -> {
                ArusDatabase wrongKeyDatabase = open(wrongKey);
                try {
                    wrongKeyDatabase.getOpenHelper().getWritableDatabase();
                } finally {
                    wrongKeyDatabase.close();
                }
            });
            ArusDatabase stillReadable = open(correctKey);
            assertEquals(1, stillReadable.transactionDao().activeTransactions().size());
            stillReadable.close();
        } finally {
            Arrays.fill(correctKey, (byte) 0);
            Arrays.fill(wrongKey, (byte) 0);
        }
    }

    private ArusDatabase open(byte[] key) {
        return Room.databaseBuilder(context, ArusDatabase.class, DATABASE_NAME)
                .openHelperFactory(new SupportFactory(key.clone(), null, true))
                .addMigrations(ArusDatabase.MIGRATION_1_14, ArusDatabase.MIGRATION_2_14,
                        ArusDatabase.MIGRATION_3_14, ArusDatabase.MIGRATION_4_14, ArusDatabase.MIGRATION_5_14,
                        ArusDatabase.MIGRATION_6_14, ArusDatabase.MIGRATION_7_14, ArusDatabase.MIGRATION_8_14,
                        ArusDatabase.MIGRATION_9_14, ArusDatabase.MIGRATION_10_14, ArusDatabase.MIGRATION_11_14,
                        ArusDatabase.MIGRATION_12_14, ArusDatabase.MIGRATION_13_14,
                        ArusDatabase.MIGRATION_1_13, ArusDatabase.MIGRATION_2_13,
                        ArusDatabase.MIGRATION_3_13, ArusDatabase.MIGRATION_4_13, ArusDatabase.MIGRATION_5_13,
                        ArusDatabase.MIGRATION_6_13, ArusDatabase.MIGRATION_7_13, ArusDatabase.MIGRATION_8_13,
                        ArusDatabase.MIGRATION_9_13, ArusDatabase.MIGRATION_10_13, ArusDatabase.MIGRATION_11_13,
                        ArusDatabase.MIGRATION_12_13, ArusDatabase.MIGRATION_1_12, ArusDatabase.MIGRATION_2_12,
                        ArusDatabase.MIGRATION_3_12, ArusDatabase.MIGRATION_4_12, ArusDatabase.MIGRATION_5_12,
                        ArusDatabase.MIGRATION_6_12, ArusDatabase.MIGRATION_7_12, ArusDatabase.MIGRATION_8_12,
                        ArusDatabase.MIGRATION_9_12, ArusDatabase.MIGRATION_10_12, ArusDatabase.MIGRATION_11_12,
                        ArusDatabase.MIGRATION_1_11, ArusDatabase.MIGRATION_2_11,
                        ArusDatabase.MIGRATION_3_11, ArusDatabase.MIGRATION_4_11, ArusDatabase.MIGRATION_5_11,
                        ArusDatabase.MIGRATION_6_11, ArusDatabase.MIGRATION_7_11, ArusDatabase.MIGRATION_8_11,
                        ArusDatabase.MIGRATION_9_11, ArusDatabase.MIGRATION_10_11,
                        ArusDatabase.MIGRATION_1_10, ArusDatabase.MIGRATION_2_10,
                        ArusDatabase.MIGRATION_3_10, ArusDatabase.MIGRATION_4_10, ArusDatabase.MIGRATION_5_10,
                        ArusDatabase.MIGRATION_6_10, ArusDatabase.MIGRATION_7_10, ArusDatabase.MIGRATION_8_10, ArusDatabase.MIGRATION_9_10, ArusDatabase.MIGRATION_1_6, ArusDatabase.MIGRATION_2_6,
                        ArusDatabase.MIGRATION_3_6, ArusDatabase.MIGRATION_4_6, ArusDatabase.MIGRATION_5_6)
                .build();
    }
}
