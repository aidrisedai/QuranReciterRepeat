package com.repeatquran.playback;

import android.content.Intent;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ServiceController;

import static org.junit.Assert.*;

/**
 * Unit tests for PlaybackService core functionality
 * Tests critical service operations without requiring full Android framework
 */
@RunWith(RobolectricTestRunner.class)
public class PlaybackServiceTest {

    private ServiceController<PlaybackService> controller;
    private PlaybackService service;

    @Before
    public void setUp() {
        controller = Robolectric.buildService(PlaybackService.class);
        service = controller.create().get();
    }

    @Test
    public void testServiceCreation() {
        assertNotNull("Service should be created successfully", service);
    }

    @Test
    public void testServiceStartWithNoAction() {
        Intent intent = new Intent();
        int result = service.onStartCommand(intent, 0, 1);
        assertEquals("Service should return START_STICKY for null action", 
                    android.app.Service.START_STICKY, result);
    }

    @Test
    public void testServiceStartWithStartAction() {
        Intent intent = new Intent();
        intent.setAction(PlaybackService.ACTION_START);
        int result = service.onStartCommand(intent, 0, 1);
        assertEquals("Service should return START_STICKY for START action", 
                    android.app.Service.START_STICKY, result);
    }

    @Test
    public void testServiceStopAction() {
        Intent intent = new Intent();
        intent.setAction(PlaybackService.ACTION_STOP);
        int result = service.onStartCommand(intent, 0, 1);
        assertEquals("Service should return START_NOT_STICKY for STOP action", 
                    android.app.Service.START_NOT_STICKY, result);
    }

    @Test
    public void testServicePlayAction() {
        Intent intent = new Intent();
        intent.setAction(PlaybackService.ACTION_PLAY);
        int result = service.onStartCommand(intent, 0, 1);
        assertEquals("Service should handle PLAY action", 
                    android.app.Service.START_STICKY, result);
    }

    @Test
    public void testServicePauseAction() {
        Intent intent = new Intent();
        intent.setAction(PlaybackService.ACTION_PAUSE);
        int result = service.onStartCommand(intent, 0, 1);
        assertEquals("Service should handle PAUSE action", 
                    android.app.Service.START_STICKY, result);
    }

    @Test
    public void testPlaybackStateConstants() {
        // Test that all required action constants are defined
        assertNotNull("ACTION_PLAY should be defined", PlaybackService.ACTION_PLAY);
        assertNotNull("ACTION_PAUSE should be defined", PlaybackService.ACTION_PAUSE);
        assertNotNull("ACTION_STOP should be defined", PlaybackService.ACTION_STOP);
        assertNotNull("ACTION_LOAD_SINGLE should be defined", PlaybackService.ACTION_LOAD_SINGLE);
        assertNotNull("ACTION_LOAD_RANGE should be defined", PlaybackService.ACTION_LOAD_RANGE);
        assertNotNull("ACTION_LOAD_SURAH should be defined", PlaybackService.ACTION_LOAD_SURAH);
        assertNotNull("ACTION_LOAD_PAGE should be defined", PlaybackService.ACTION_LOAD_PAGE);
    }

    @Test
    public void testServiceStateManager() {
        // Verify PlaybackStateManager integration
        assertNotNull("PlaybackStateManager should be accessible", 
                     PlaybackStateManager.getInstance());
    }
}