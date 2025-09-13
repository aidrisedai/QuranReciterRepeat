package com.repeatquran.data;

import static org.junit.Assert.*;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.repeatquran.data.db.SessionEntity;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class HistoryDbTest {

    @Test
    public void insertAndRetrieveLastSessions() {
        Context ctx = ApplicationProvider.getApplicationContext();
        SessionRepository repo = new SessionRepository(ctx);

        long now = System.currentTimeMillis();
        for (int i = 0; i < 3; i++) {
            SessionEntity e = new SessionEntity();
            e.startedAt = now + i;
            e.sourceType = "single";
            e.startSurah = 1;
            e.startAyah = 1 + i;
            e.recitersCsv = "test";
            e.repeatCount = 1;
            e.cyclesRequested = 1;
            repo.insert(e);
        }

        List<SessionEntity> last2 = repo.getLastSessions(2);
        assertEquals(2, last2.size());
        assertTrue(last2.get(0).startedAt > last2.get(1).startedAt);
    }
}

