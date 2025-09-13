package com.repeatquran.data;

import static org.junit.Assert.*;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.File;

@RunWith(RobolectricTestRunner.class)
public class CacheManagerTest {

    @Test
    public void targetFileAndIsCachedWork() throws Exception {
        Context ctx = ApplicationProvider.getApplicationContext();
        CacheManager cm = CacheManager.get(ctx);
        File f = cm.getTargetFile("Test_Reciter", "001", "001");
        assertTrue(f.getAbsolutePath().contains("/files/audio/Test_Reciter/001001.mp3"));
        // Ensure parent exists
        if (!f.getParentFile().exists()) {
            assertTrue(f.getParentFile().mkdirs());
        }
        // Write a small file
        try (java.io.FileOutputStream out = new java.io.FileOutputStream(f)) {
            out.write(new byte[]{0,1,2});
        }
        assertTrue(cm.isCached("Test_Reciter", "001", "001"));
    }
}

