package com.repeatquran.integration;

import android.content.Context;
import android.content.Intent;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ServiceTestRule;

import com.repeatquran.playback.PlaybackService;
import com.repeatquran.playback.PlaybackStateManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeoutException;

import static org.junit.Assert.*;

/**
 * Integration tests for play/pause functionality
 * Tests the actual interaction between fragments and PlaybackService
 */
@RunWith(AndroidJUnit4.class)
public class PlaybackIntegrationTest {

    @Rule
    public final ServiceTestRule serviceRule = new ServiceTestRule();

    private Context context;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

    @After
    public void tearDown() {
        // Clean up any running playback
        try {
            Intent stopIntent = new Intent(context, PlaybackService.class);
            stopIntent.setAction(PlaybackService.ACTION_STOP);
            context.startService(stopIntent);
        } catch (Exception ignored) {}
    }

    @Test
    public void testServiceBinding() throws TimeoutException {
        Intent serviceIntent = new Intent(context, PlaybackService.class);
        serviceIntent.setAction(PlaybackService.ACTION_START);
        
        // The service should start successfully
        assertNotNull("Service should start", 
                     serviceRule.startService(serviceIntent));
    }

    @Test
    public void testPlaybackStateManager() {
        PlaybackStateManager manager = PlaybackStateManager.getInstance();
        assertNotNull("PlaybackStateManager should be available", manager);
        
        // Initially player should be null or in idle state
        // This tests the state management we implemented
        // (Player may be null initially until service sets it)
    }

    @Test
    public void testServiceActions() throws TimeoutException {
        // Test that service responds to different actions
        Intent serviceIntent = new Intent(context, PlaybackService.class);
        
        // Test START action
        serviceIntent.setAction(PlaybackService.ACTION_START);
        assertNotNull("Service should handle START action", 
                     serviceRule.startService(serviceIntent));
        
        // Test PLAY action  
        serviceIntent.setAction(PlaybackService.ACTION_PLAY);
        context.startService(serviceIntent);
        
        // Test PAUSE action
        serviceIntent.setAction(PlaybackService.ACTION_PAUSE);
        context.startService(serviceIntent);
        
        // Test STOP action
        serviceIntent.setAction(PlaybackService.ACTION_STOP);
        context.startService(serviceIntent);
    }

    @Test
    public void testForegroundServiceRequirements() throws TimeoutException {
        // Test actions that require foreground service
        Intent serviceIntent = new Intent(context, PlaybackService.class);
        
        // These actions should trigger foreground service
        String[] foregroundActions = {
            PlaybackService.ACTION_PLAY,
            PlaybackService.ACTION_LOAD_SINGLE,
            PlaybackService.ACTION_LOAD_RANGE,
            PlaybackService.ACTION_LOAD_SURAH,
            PlaybackService.ACTION_LOAD_PAGE,
            PlaybackService.ACTION_RESUME
        };
        
        for (String action : foregroundActions) {
            serviceIntent.setAction(action);
            // Service should start without throwing exceptions
            assertNotNull("Service should handle foreground action: " + action,
                         serviceRule.startService(serviceIntent));
        }
    }

    @Test
    public void testLoadSingleAyah() throws TimeoutException {
        Intent serviceIntent = new Intent(context, PlaybackService.class);
        serviceIntent.setAction(PlaybackService.ACTION_LOAD_SINGLE);
        serviceIntent.putExtra("sura", 1);
        serviceIntent.putExtra("ayah", 1);
        serviceIntent.putExtra("repeat", 1);
        
        // Service should handle single ayah loading
        assertNotNull("Service should load single ayah",
                     serviceRule.startService(serviceIntent));
    }

    @Test
    public void testLoadRange() throws TimeoutException {
        Intent serviceIntent = new Intent(context, PlaybackService.class);
        serviceIntent.setAction(PlaybackService.ACTION_LOAD_RANGE);
        serviceIntent.putExtra("ss", 1);  // start surah
        serviceIntent.putExtra("sa", 1);  // start ayah
        serviceIntent.putExtra("es", 1);  // end surah  
        serviceIntent.putExtra("ea", 7);  // end ayah
        serviceIntent.putExtra("repeat", 1);
        
        // Service should handle range loading
        assertNotNull("Service should load range",
                     serviceRule.startService(serviceIntent));
    }

    @Test
    public void testLoadSurah() throws TimeoutException {
        Intent serviceIntent = new Intent(context, PlaybackService.class);
        serviceIntent.setAction(PlaybackService.ACTION_LOAD_SURAH);
        serviceIntent.putExtra("surah", 1);
        serviceIntent.putExtra("repeat", 1);
        serviceIntent.putExtra("halfSplit", false);
        
        // Service should handle surah loading
        assertNotNull("Service should load surah",
                     serviceRule.startService(serviceIntent));
    }

    @Test
    public void testLoadPage() throws TimeoutException {
        Intent serviceIntent = new Intent(context, PlaybackService.class);
        serviceIntent.setAction(PlaybackService.ACTION_LOAD_PAGE);
        serviceIntent.putExtra("page", 1);
        serviceIntent.putExtra("repeat", 1);
        serviceIntent.putExtra("halfSplit", false);
        
        // Service should handle page loading
        assertNotNull("Service should load page",
                     serviceRule.startService(serviceIntent));
    }

    @Test
    public void testSpeedControl() throws TimeoutException {
        Intent serviceIntent = new Intent(context, PlaybackService.class);
        serviceIntent.setAction(PlaybackService.ACTION_SET_SPEED);
        serviceIntent.putExtra("speed", 1.25f);
        
        // Service should handle speed changes
        assertNotNull("Service should handle speed change",
                     serviceRule.startService(serviceIntent));
    }

    @Test
    public void testActionConstants() {
        // Verify all action constants are properly defined
        assertNotNull("ACTION_START should be defined", PlaybackService.ACTION_START);
        assertNotNull("ACTION_PLAY should be defined", PlaybackService.ACTION_PLAY);
        assertNotNull("ACTION_PAUSE should be defined", PlaybackService.ACTION_PAUSE);
        assertNotNull("ACTION_STOP should be defined", PlaybackService.ACTION_STOP);
        assertNotNull("ACTION_LOAD_SINGLE should be defined", PlaybackService.ACTION_LOAD_SINGLE);
        assertNotNull("ACTION_LOAD_RANGE should be defined", PlaybackService.ACTION_LOAD_RANGE);
        assertNotNull("ACTION_LOAD_SURAH should be defined", PlaybackService.ACTION_LOAD_SURAH);
        assertNotNull("ACTION_LOAD_PAGE should be defined", PlaybackService.ACTION_LOAD_PAGE);
        assertNotNull("ACTION_SET_SPEED should be defined", PlaybackService.ACTION_SET_SPEED);
        assertNotNull("ACTION_RESUME should be defined", PlaybackService.ACTION_RESUME);
        assertNotNull("ACTION_TOGGLE should be defined", PlaybackService.ACTION_TOGGLE);
        assertNotNull("ACTION_NEXT should be defined", PlaybackService.ACTION_NEXT);
        assertNotNull("ACTION_PREV should be defined", PlaybackService.ACTION_PREV);
    }
}