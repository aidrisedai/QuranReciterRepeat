package com.repeatquran.data.db;

import static org.junit.Assert.*;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class SessionDaoTest {
    private RepeatQuranDatabase db;
    private SessionDao dao;

    @Before
    public void createDb() {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, RepeatQuranDatabase.class).build();
        dao = db.sessionDao();
    }

    @After
    public void closeDb() throws IOException {
        db.close();
    }

    @Test
    public void insertAndQuery() {
        SessionEntity e = new SessionEntity();
        e.startedAt = 123L;
        e.sourceType = "single";
        e.startSurah = 1;
        e.startAyah = 1;
        e.recitersCsv = "Abdurrahmaan_As-Sudais_64kbps";
        e.repeatCount = 3;
        e.cyclesRequested = 3;
        long id = dao.insert(e);
        assertTrue(id > 0);

        List<SessionEntity> all = dao.getAllOrdered();
        assertEquals(1, all.size());
        SessionEntity row = all.get(0);
        assertEquals("single", row.sourceType);
        assertEquals(Integer.valueOf(1), row.startSurah);
        assertEquals(Integer.valueOf(1), row.startAyah);
        assertEquals("Abdurrahmaan_As-Sudais_64kbps", row.recitersCsv);
        assertEquals(3, row.repeatCount);
        assertNull(row.endedAt);

        int updated = dao.markEnded(id, 456L, 3);
        assertEquals(1, updated);

        all = dao.getAllOrdered();
        assertEquals(1, all.size());
        assertEquals(Long.valueOf(456L), all.get(0).endedAt);
        assertEquals(Integer.valueOf(3), all.get(0).cyclesCompleted);
    }
}

