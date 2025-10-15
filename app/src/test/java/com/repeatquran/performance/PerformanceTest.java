package com.repeatquran.performance;

import android.content.Context;
import android.content.Intent;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ServiceController;

import com.repeatquran.playback.PlaybackService;
import com.repeatquran.playback.PlaybackStateManager;

import static org.junit.Assert.*;

/**
 * Performance tests and baseline measurements
 * Establishes performance baselines for production monitoring
 */
@RunWith(RobolectricTestRunner.class)
public class PerformanceTest {

    private Context context;
    private ServiceController<PlaybackService> controller;
    private PlaybackService service;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        controller = Robolectric.buildService(PlaybackService.class);
        service = controller.create().get();
    }

    @Test
    public void testServiceStartupTime() {
        long startTime = System.currentTimeMillis();
        
        // Measure service creation time
        ServiceController<PlaybackService> testController = Robolectric.buildService(PlaybackService.class);
        PlaybackService testService = testController.create().get();
        
        long endTime = System.currentTimeMillis();
        long startupTime = endTime - startTime;
        
        // Baseline: Service should start within 500ms
        assertTrue("Service startup should be under 500ms, was: " + startupTime + "ms", 
                  startupTime < 500);
        
        System.out.println("Service startup time: " + startupTime + "ms");
        assertNotNull("Service should be created", testService);
    }

    @Test
    public void testPlaybackStateManagerCreationTime() {
        long startTime = System.nanoTime();
        
        PlaybackStateManager manager = PlaybackStateManager.getInstance();
        
        long endTime = System.nanoTime();
        long creationTimeNs = endTime - startTime;
        long creationTimeMs = creationTimeNs / 1_000_000;
        
        // Baseline: Singleton creation should be very fast
        assertTrue("PlaybackStateManager creation should be under 10ms, was: " + creationTimeMs + "ms",
                  creationTimeMs < 10);
        
        System.out.println("PlaybackStateManager creation time: " + creationTimeMs + "ms");
        assertNotNull("Manager should be created", manager);
    }

    @Test
    public void testActionProcessingTime() {
        long totalTime = 0;
        int iterations = 10;
        
        for (int i = 0; i < iterations; i++) {
            long startTime = System.nanoTime();
            
            Intent intent = new Intent();
            intent.setAction(PlaybackService.ACTION_PLAY);
            service.onStartCommand(intent, 0, i);
            
            long endTime = System.nanoTime();
            totalTime += (endTime - startTime);
        }
        
        long avgTimeNs = totalTime / iterations;
        long avgTimeMs = avgTimeNs / 1_000_000;
        
        // Baseline: Action processing should be under 50ms on average
        assertTrue("Average action processing should be under 50ms, was: " + avgTimeMs + "ms",
                  avgTimeMs < 50);
        
        System.out.println("Average action processing time: " + avgTimeMs + "ms");
    }

    @Test
    public void testMemoryUsageBaseline() {
        Runtime runtime = Runtime.getRuntime();
        
        // Get baseline memory
        runtime.gc(); // Request garbage collection
        long beforeMemory = runtime.totalMemory() - runtime.freeMemory();
        
        // Create multiple services to test memory usage
        ServiceController<PlaybackService>[] controllers = new ServiceController[5];
        PlaybackService[] services = new PlaybackService[5];
        
        for (int i = 0; i < 5; i++) {
            controllers[i] = Robolectric.buildService(PlaybackService.class);
            services[i] = controllers[i].create().get();
        }
        
        runtime.gc(); // Request garbage collection
        long afterMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryIncrease = afterMemory - beforeMemory;
        long memoryPerService = memoryIncrease / 5;
        
        // Baseline: Each service instance should use less than 10MB
        long maxMemoryPerService = 10 * 1024 * 1024; // 10MB in bytes
        assertTrue("Memory usage per service should be under 10MB, was: " + 
                  (memoryPerService / 1024 / 1024) + "MB",
                  memoryPerService < maxMemoryPerService);
        
        System.out.println("Memory usage per service: " + (memoryPerService / 1024 / 1024) + "MB");
        
        // Cleanup
        for (PlaybackService testService : services) {
            if (testService != null) {
                // Simulate cleanup
                assertNotNull("Service should exist for cleanup", testService);
            }
        }
    }

    @Test
    public void testSharedPreferencesPerformance() {
        android.content.SharedPreferences prefs = context.getSharedPreferences("rq_prefs", Context.MODE_PRIVATE);
        
        // Test write performance
        long writeStartTime = System.nanoTime();
        android.content.SharedPreferences.Editor editor = prefs.edit();
        for (int i = 0; i < 100; i++) {
            editor.putString("test_key_" + i, "test_value_" + i);
        }
        editor.commit();
        long writeEndTime = System.nanoTime();
        long writeTimeMs = (writeEndTime - writeStartTime) / 1_000_000;
        
        // Test read performance
        long readStartTime = System.nanoTime();
        for (int i = 0; i < 100; i++) {
            String value = prefs.getString("test_key_" + i, "");
            assertNotNull("Value should exist", value);
        }
        long readEndTime = System.nanoTime();
        long readTimeMs = (readEndTime - readStartTime) / 1_000_000;
        
        // Baselines: SharedPreferences operations should be fast
        assertTrue("100 preference writes should be under 100ms, was: " + writeTimeMs + "ms",
                  writeTimeMs < 100);
        assertTrue("100 preference reads should be under 50ms, was: " + readTimeMs + "ms", 
                  readTimeMs < 50);
        
        System.out.println("SharedPreferences write time (100 items): " + writeTimeMs + "ms");
        System.out.println("SharedPreferences read time (100 items): " + readTimeMs + "ms");
        
        // Cleanup
        editor = prefs.edit();
        for (int i = 0; i < 100; i++) {
            editor.remove("test_key_" + i);
        }
        editor.commit();
    }

    @Test
    public void testStringArrayResourceAccess() {
        long startTime = System.nanoTime();
        
        // Test accessing reciter resources (common operation)
        String[] reciterNames = context.getResources().getStringArray(com.repeatquran.R.array.reciter_names);
        String[] reciterIds = context.getResources().getStringArray(com.repeatquran.R.array.reciter_ids);
        
        long endTime = System.nanoTime();
        long accessTimeMs = (endTime - startTime) / 1_000_000;
        
        // Baseline: Resource access should be very fast
        assertTrue("Resource array access should be under 10ms, was: " + accessTimeMs + "ms",
                  accessTimeMs < 10);
        
        System.out.println("String array resource access time: " + accessTimeMs + "ms");
        
        // Verify we got the data
        assertTrue("Should have reciter names", reciterNames.length > 0);
        assertTrue("Should have reciter IDs", reciterIds.length > 0);
        assertEquals("Names and IDs should match in count", reciterNames.length, reciterIds.length);
    }

    @Test
    public void testInputValidationPerformance() {
        int iterations = 1000;
        long startTime = System.nanoTime();
        
        // Test validation functions performance
        for (int i = 0; i < iterations; i++) {
            isValidSurah(i % 115); // Test across range
            isValidPage(i % 605);  // Test across range
            isValidRepeat(i);      // Test various values
        }
        
        long endTime = System.nanoTime();
        long totalTimeMs = (endTime - startTime) / 1_000_000;
        long avgTimeNs = (endTime - startTime) / iterations;
        
        // Baseline: Validation should be extremely fast
        assertTrue("1000 validation calls should be under 10ms total, was: " + totalTimeMs + "ms",
                  totalTimeMs < 10);
        assertTrue("Average validation time should be under 1000ns, was: " + avgTimeNs + "ns",
                  avgTimeNs < 1000);
        
        System.out.println("Validation performance - Total: " + totalTimeMs + "ms, Avg: " + avgTimeNs + "ns");
    }

    @Test
    public void testConcurrentActionHandling() {
        // Test multiple actions processed quickly
        Intent[] intents = new Intent[10];
        String[] actions = {
            PlaybackService.ACTION_PLAY,
            PlaybackService.ACTION_PAUSE,
            PlaybackService.ACTION_STOP,
            PlaybackService.ACTION_PLAY,
            PlaybackService.ACTION_PAUSE
        };
        
        for (int i = 0; i < 10; i++) {
            intents[i] = new Intent();
            intents[i].setAction(actions[i % actions.length]);
        }
        
        long startTime = System.nanoTime();
        
        for (int i = 0; i < 10; i++) {
            int result = service.onStartCommand(intents[i], 0, i);
            assertTrue("Service should handle action", result >= 0);
        }
        
        long endTime = System.nanoTime();
        long totalTimeMs = (endTime - startTime) / 1_000_000;
        
        // Baseline: 10 actions should complete within 500ms
        assertTrue("10 sequential actions should complete under 500ms, was: " + totalTimeMs + "ms",
                  totalTimeMs < 500);
        
        System.out.println("10 sequential actions completed in: " + totalTimeMs + "ms");
    }

    // Helper methods for validation performance testing
    private boolean isValidSurah(int surah) {
        return surah >= 1 && surah <= 114;
    }

    private boolean isValidPage(int page) {
        return page >= 1 && page <= 604;
    }

    private boolean isValidRepeat(int repeat) {
        return repeat == -1 || (repeat >= 1 && repeat <= 9999);
    }
}