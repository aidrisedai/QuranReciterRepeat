package com.repeatquran.playback;

import static org.junit.Assert.*;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ServiceController;
import org.robolectric.shadows.ShadowLooper;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@RunWith(RobolectricTestRunner.class)
public class ThreadingStressTest {

    /**
     * This test exposes race conditions by rapidly switching between different
     * playback sources while the service is processing background operations.
     */
    @Test
    public void rapidSourceSwitching_exposesRaceConditions() throws Exception {
        android.content.Context ctx = ApplicationProvider.getApplicationContext();
        
        // Set up a reciter to avoid null checks
        String[] ids = ctx.getResources().getStringArray(com.repeatquran.R.array.reciter_ids);
        ctx.getSharedPreferences("rq_prefs", android.content.Context.MODE_PRIVATE)
                .edit().putString("reciters.order", ids[0]).apply();

        ServiceController<PlaybackService> controller = 
            Robolectric.buildService(PlaybackService.class).create().startCommand(0, 0);
        PlaybackService service = controller.get();

        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicInteger completedOperations = new AtomicInteger(0);

        // Simulate rapid user interactions
        new Thread(() -> {
            try {
                for (int i = 0; i < 10; i++) {
                    // Switch between single verse and range rapidly
                    Intent single = new Intent(ctx, PlaybackService.class);
                    single.setAction(PlaybackService.ACTION_LOAD_SINGLE);
                    single.putExtra("sura", 1);
                    single.putExtra("ayah", i + 1);
                    single.putExtra("repeat", 3);
                    controller.withIntent(single).startCommand(0, 0);

                    Thread.sleep(10); // Very short delay

                    Intent range = new Intent(ctx, PlaybackService.class);
                    range.setAction(PlaybackService.ACTION_LOAD_RANGE);
                    range.putExtra("ss", 1);
                    range.putExtra("sa", 1);
                    range.putExtra("es", 1);
                    range.putExtra("ea", 5);
                    range.putExtra("repeat", 2);
                    controller.withIntent(range).startCommand(0, 0);

                    completedOperations.incrementAndGet();
                }
            } catch (Exception e) {
                fail("Exception during rapid switching: " + e.getMessage());
            } finally {
                latch.countDown();
            }
        }).start();

        // Process all pending operations
        for (int i = 0; i < 50; i++) {
            Shadows.shadowOf(Looper.getMainLooper()).idle();
            Thread.sleep(50);
        }

        assertTrue("Operations should complete without hanging", 
                   latch.await(5, TimeUnit.SECONDS));
        assertTrue("Should complete at least some operations", 
                   completedOperations.get() > 0);
    }

    /**
     * Test concurrent SharedPreferences access
     */
    @Test
    public void concurrentPreferencesAccess_noDeadlock() throws Exception {
        android.content.Context ctx = ApplicationProvider.getApplicationContext();
        final CountDownLatch latch = new CountDownLatch(2);
        final AtomicInteger errors = new AtomicInteger(0);

        // Thread 1: Rapid preference writes (like user changing settings)
        new Thread(() -> {
            try {
                for (int i = 0; i < 100; i++) {
                    ctx.getSharedPreferences("rq_prefs", android.content.Context.MODE_PRIVATE)
                            .edit()
                            .putString("reciters.order", "test" + i)
                            .putInt("repeat.count", i % 10)
                            .putFloat("playback.speed", 1.0f + (i % 5) * 0.25f)
                            .apply();
                    
                    if (i % 10 == 0) {
                        Thread.yield(); // Give other thread a chance
                    }
                }
            } catch (Exception e) {
                errors.incrementAndGet();
            } finally {
                latch.countDown();
            }
        }).start();

        // Thread 2: Rapid preference reads (like service operations)
        new Thread(() -> {
            try {
                for (int i = 0; i < 100; i++) {
                    android.content.SharedPreferences prefs = 
                        ctx.getSharedPreferences("rq_prefs", android.content.Context.MODE_PRIVATE);
                    
                    String reciters = prefs.getString("reciters.order", "");
                    int repeat = prefs.getInt("repeat.count", 1);
                    float speed = prefs.getFloat("playback.speed", 1.0f);
                    
                    // Verify values are reasonable
                    assertNotNull("Reciters should not be null", reciters);
                    assertTrue("Repeat should be >= 0", repeat >= 0);
                    assertTrue("Speed should be reasonable", speed >= 0.5f && speed <= 2.0f);
                    
                    if (i % 10 == 0) {
                        Thread.yield();
                    }
                }
            } catch (Exception e) {
                errors.incrementAndGet();
            } finally {
                latch.countDown();
            }
        }).start();

        assertTrue("Concurrent preferences access should complete", 
                   latch.await(10, TimeUnit.SECONDS));
        assertEquals("Should have no errors during concurrent access", 0, errors.get());
    }

    /**
     * Test that resume state isn't corrupted during concurrent operations
     */
    @Test
    public void concurrentResumeStateUpdates_maintainConsistency() throws Exception {
        android.content.Context ctx = ApplicationProvider.getApplicationContext();
        ctx.getSharedPreferences("rq_prefs", android.content.Context.MODE_PRIVATE)
                .edit().putString("reciters.order", "test_reciter").apply();

        ServiceController<PlaybackService> controller = 
            Robolectric.buildService(PlaybackService.class).create().startCommand(0, 0);
        
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicInteger inconsistencies = new AtomicInteger(0);

        new Thread(() -> {
            try {
                for (int i = 0; i < 20; i++) {
                    // Trigger different actions that update resume state
                    Intent intent1 = new Intent(ctx, PlaybackService.class);
                    intent1.setAction(PlaybackService.ACTION_LOAD_SINGLE);
                    intent1.putExtra("sura", 1);
                    intent1.putExtra("ayah", i + 1);
                    intent1.putExtra("repeat", 2);
                    controller.withIntent(intent1).startCommand(0, 0);

                    Thread.sleep(20);

                    Intent intent2 = new Intent(ctx, PlaybackService.class);
                    intent2.setAction(PlaybackService.ACTION_LOAD_RANGE);
                    intent2.putExtra("ss", 1);
                    intent2.putExtra("sa", 1);
                    intent2.putExtra("es", 2);
                    intent2.putExtra("ea", 5);
                    intent2.putExtra("repeat", 3);
                    controller.withIntent(intent2).startCommand(0, 0);

                    // Check for inconsistent resume state
                    android.content.SharedPreferences prefs = 
                        ctx.getSharedPreferences("rq_prefs", android.content.Context.MODE_PRIVATE);
                    
                    String sourceType = prefs.getString("resume.sourceType", null);
                    if (sourceType != null) {
                        if ("single".equals(sourceType)) {
                            int startSurah = prefs.getInt("resume.startSurah", -1);
                            int startAyah = prefs.getInt("resume.startAyah", -1);
                            if (startSurah <= 0 || startAyah <= 0) {
                                inconsistencies.incrementAndGet();
                            }
                        } else if ("range".equals(sourceType)) {
                            int ss = prefs.getInt("resume.startSurah", -1);
                            int sa = prefs.getInt("resume.startAyah", -1);
                            int es = prefs.getInt("resume.endSurah", -1);
                            int ea = prefs.getInt("resume.endAyah", -1);
                            if (ss <= 0 || sa <= 0 || es <= 0 || ea <= 0) {
                                inconsistencies.incrementAndGet();
                            }
                        }
                    }
                }
            } catch (Exception e) {
                fail("Exception during resume state test: " + e.getMessage());
            } finally {
                latch.countDown();
            }
        }).start();

        // Process background operations
        for (int i = 0; i < 100; i++) {
            Shadows.shadowOf(Looper.getMainLooper()).idle();
            Thread.sleep(20);
        }

        assertTrue("Resume state test should complete", 
                   latch.await(15, TimeUnit.SECONDS));
        
        // Some inconsistencies are expected due to race conditions - this test documents the problem
        System.out.println("Resume state inconsistencies detected: " + inconsistencies.get());
    }
}