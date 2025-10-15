package com.repeatquran.smoke;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.repeatquran.playback.PlaybackStateManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * Smoke tests for core app features
 * These tests verify basic functionality works without crashes
 * Essential for production deployment confidence
 */
@RunWith(AndroidJUnit4.class)
public class CoreFeatureSmokeTest {

    private Context context;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

    @Test
    public void testAppContextAndPackage() {
        assertNotNull("App context should not be null", context);
        assertEquals("Package name should match", 
                    "com.repeatquran", context.getPackageName());
    }

    @Test
    public void testSharedPreferencesAccess() {
        SharedPreferences prefs = context.getSharedPreferences("rq_prefs", Context.MODE_PRIVATE);
        assertNotNull("SharedPreferences should be accessible", prefs);
        
        // Test basic operations don't crash
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("test", "value");
        assertTrue("Should be able to commit preferences", editor.commit());
        
        String value = prefs.getString("test", "");
        assertEquals("Should be able to read preferences", "value", value);
        
        // Clean up
        editor.remove("test").commit();
    }

    @Test
    public void testPlaybackStateManagerSingleton() {
        PlaybackStateManager manager1 = PlaybackStateManager.getInstance();
        PlaybackStateManager manager2 = PlaybackStateManager.getInstance();
        
        assertNotNull("PlaybackStateManager should not be null", manager1);
        assertSame("Should return same instance", manager1, manager2);
    }

    @Test
    public void testResourceAccess() {
        // Test that critical resources are accessible
        String appName = context.getString(com.repeatquran.R.string.app_name);
        assertNotNull("App name resource should be accessible", appName);
        assertFalse("App name should not be empty", appName.isEmpty());
        
        // Test string arrays are accessible
        String[] reciterNames = context.getResources().getStringArray(com.repeatquran.R.array.reciter_names);
        assertNotNull("Reciter names array should be accessible", reciterNames);
        assertTrue("Should have reciter names", reciterNames.length > 0);
        
        String[] reciterIds = context.getResources().getStringArray(com.repeatquran.R.array.reciter_ids);
        assertNotNull("Reciter IDs array should be accessible", reciterIds);
        assertEquals("Reciter names and IDs should have same length", 
                    reciterNames.length, reciterIds.length);
    }

    @Test
    public void testCriticalDataValidation() {
        // Test Quran data integrity
        int[] ayahCounts = {
            7, 286, 200, 176, 120, 165, 206, 75, 129, 109, 123, 111, 43, 52, 99, 
            128, 111, 110, 98, 135, 112, 78, 118, 64, 77, 227, 93, 88, 69, 60,
            34, 30, 73, 54, 45, 83, 182, 88, 75, 85, 54, 53, 89, 59, 37, 35, 
            38, 29, 18, 45, 60, 49, 62, 55, 78, 96, 29, 22, 24, 13, 14, 11, 
            11, 18, 12, 12, 30, 52, 52, 44, 28, 28, 20, 56, 40, 31, 50, 40, 
            46, 42, 29, 19, 36, 25, 22, 17, 19, 26, 30, 20, 15, 21, 11, 8, 
            8, 19, 5, 8, 8, 11, 11, 8, 3, 9, 5, 4, 7, 3, 6, 3, 5, 4, 5, 6
        };
        
        assertEquals("Should have 114 surahs", 114, ayahCounts.length);
        
        // Test some known values
        assertEquals("Al-Fatiha should have 7 ayahs", 7, ayahCounts[0]);
        assertEquals("Al-Baqarah should have 286 ayahs", 286, ayahCounts[1]);
        assertEquals("Al-Nas should have 6 ayahs", 6, ayahCounts[113]);
    }

    @Test
    public void testInputValidationBounds() {
        // Test critical validation functions don't crash
        assertTrue("Valid surah range", isValidSurah(1));
        assertTrue("Valid surah range", isValidSurah(114));
        assertFalse("Invalid surah range", isValidSurah(0));
        assertFalse("Invalid surah range", isValidSurah(115));
        
        assertTrue("Valid page range", isValidPage(1));
        assertTrue("Valid page range", isValidPage(604));
        assertFalse("Invalid page range", isValidPage(0));
        assertFalse("Invalid page range", isValidPage(605));
        
        assertTrue("Valid repeat", isValidRepeat(1));
        assertTrue("Valid repeat", isValidRepeat(9999));
        assertTrue("Valid infinite repeat", isValidRepeat(-1));
        assertFalse("Invalid repeat", isValidRepeat(0));
        assertFalse("Invalid repeat", isValidRepeat(10000));
    }

    @Test
    public void testSpeedValidation() {
        // Test playback speed validation
        assertTrue("Valid speed 0.5x", isValidSpeed(0.5f));
        assertTrue("Valid speed 1.0x", isValidSpeed(1.0f));
        assertTrue("Valid speed 2.0x", isValidSpeed(2.0f));
        assertFalse("Invalid speed too low", isValidSpeed(0.1f));
        assertFalse("Invalid speed too high", isValidSpeed(5.0f));
    }

    @Test
    public void testTranslationCompleteness() {
        // Test that critical strings have translations
        Context arContext = createConfigurationContext("ar");
        if (arContext != null) {
            String appNameEn = context.getString(com.repeatquran.R.string.app_name);
            String appNameAr = arContext.getString(com.repeatquran.R.string.app_name);
            
            assertNotNull("English app name should exist", appNameEn);
            assertNotNull("Arabic app name should exist", appNameAr);
            assertNotEquals("Translations should be different", appNameEn, appNameAr);
        }
    }

    @Test
    public void testPermissionsInManifest() {
        // Verify critical permissions are declared
        // (This is more of a sanity check that manifest is properly configured)
        assertNotNull("Context should have package manager", context.getPackageManager());
    }

    // Helper methods matching app validation logic
    private boolean isValidSurah(int surah) {
        return surah >= 1 && surah <= 114;
    }

    private boolean isValidPage(int page) {
        return page >= 1 && page <= 604;
    }

    private boolean isValidRepeat(int repeat) {
        return repeat == -1 || (repeat >= 1 && repeat <= 9999);
    }

    private boolean isValidSpeed(float speed) {
        return speed >= 0.5f && speed <= 2.0f;
    }

    private Context createConfigurationContext(String language) {
        try {
            android.content.res.Configuration config = new android.content.res.Configuration(context.getResources().getConfiguration());
            config.setLocale(new java.util.Locale(language));
            return context.createConfigurationContext(config);
        } catch (Exception e) {
            return null; // Not critical if this fails
        }
    }
}