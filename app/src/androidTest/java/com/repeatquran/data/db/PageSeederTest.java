package com.repeatquran.data.db;

import static org.junit.Assert.*;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.repeatquran.data.PageSeeder;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@RunWith(AndroidJUnit4.class)
public class PageSeederTest {
    @Test
    public void seedsAndQueriesKnownPages() throws Exception {
        Context ctx = ApplicationProvider.getApplicationContext();
        // Force seed (idempotent if already seeded)
        PageSeeder.seedIfNeeded(ctx);
        // Allow background thread to complete briefly
        Thread.sleep(500);
        PageSegmentDao dao = RepeatQuranDatabase.get(ctx).pageSegmentDao();
        List<PageSegmentEntity> p1 = dao.segmentsForPage(1);
        assertEquals(1, p1.size());
        assertEquals(1, p1.get(0).surah);
        assertEquals(1, p1.get(0).startAyah);
        assertEquals(7, p1.get(0).endAyah);

        List<PageSegmentEntity> p2 = dao.segmentsForPage(2);
        assertEquals(1, p2.size());
        assertEquals(2, p2.get(0).surah);
        assertEquals(1, p2.get(0).startAyah);
        assertTrue(p2.get(0).endAyah >= 5);
    }
}

