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
public class AttachmentDaoTest {
    private ArusDatabase database;

    @Before public void setUp() {
        database = Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(), ArusDatabase.class).build();
    }

    @After public void tearDown() { database.close(); }

    @Test public void pendingScanCanBeAttachedToConfirmedTransaction() {
        long id = database.attachmentDao().insert(new AttachmentEntity(
                "PENDING", null, "receipts/scan.jpg", "TOTAL 12000", 10));

        assertEquals(1, database.attachmentDao().pending().size());
        assertEquals(1, database.attachmentDao().attach(id, "TRANSACTION", 42));
        assertEquals(0, database.attachmentDao().pending().size());
        assertEquals("receipts/scan.jpg", database.attachmentDao().forOwner("TRANSACTION", 42).get(0).filePath);
    }
}
