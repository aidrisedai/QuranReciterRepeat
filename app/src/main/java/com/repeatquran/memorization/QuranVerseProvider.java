package com.repeatquran.memorization;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Provides Arabic verse text from Quran API with caching
 */
public class QuranVerseProvider {
    private static final String TAG = "QuranVerseProvider";
    private static final String API_URL = "https://api.alquran.cloud/v1/ayah/";
    private static final int TIMEOUT_MS = 10000;
    
    private Map<String, String> cache = new HashMap<>();
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    
    public interface VerseCallback {
        void onVerseLoaded(String arabicText);
        void onError(String error);
    }
    
    /**
     * Fetches Arabic text for a specific verse
     * @param surah Surah number (1-114)
     * @param ayah Ayah number within the surah
     * @param callback Callback for results
     */
    public void getVerseText(int surah, int ayah, VerseCallback callback) {
        String key = surah + ":" + ayah;
        
        // Check cache first
        if (cache.containsKey(key)) {
            Log.d(TAG, "Returning cached verse: " + key);
            callback.onVerseLoaded(cache.get(key));
            return;
        }
        
        // Fetch from API in background thread
        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                String urlString = API_URL + surah + ":" + ayah;
                Log.d(TAG, "Fetching verse from API: " + urlString);
                
                URL url = new URL(urlString);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(TIMEOUT_MS);
                conn.setReadTimeout(TIMEOUT_MS);
                conn.setRequestProperty("Accept", "application/json");
                
                Log.d(TAG, "Connecting to API...");
                conn.connect();
                
                int responseCode = conn.getResponseCode();
                Log.d(TAG, "Response code: " + responseCode);
                
                if (responseCode != 200) {
                    throw new Exception("API returned error code: " + responseCode);
                }
                
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), "UTF-8"));
                StringBuilder response = new StringBuilder();
                String line;
                
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                
                Log.d(TAG, "API response received: " + response.length() + " chars");
                
                JSONObject json = new JSONObject(response.toString());
                String verseText = json.getJSONObject("data").getString("text");
                
                // Remove Bismillah if present at the start (not part of the verse)
                verseText = removeBismillah(verseText);
                
                // Cache the result
                cache.put(key, verseText);
                
                Log.d(TAG, "Successfully fetched verse " + key + ": " + verseText.substring(0, Math.min(50, verseText.length())) + "...");
                
                // Call callback on main thread
                final String finalText = verseText;
                mainHandler.post(() -> callback.onVerseLoaded(finalText));
                
            } catch (Exception e) {
                Log.e(TAG, "Error fetching verse " + key + ": " + e.getClass().getSimpleName(), e);
                String errorMsg = e.getMessage() != null ? e.getMessage() : "Unknown error: " + e.getClass().getSimpleName();
                Log.e(TAG, "Full error: " + errorMsg);
                
                // Call error callback on main thread
                mainHandler.post(() -> callback.onError(errorMsg));
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }).start();
    }
    
    /**
     * Pre-fetch multiple verses for smoother experience
     * @param surah Surah number
     * @param startAyah Starting ayah
     * @param endAyah Ending ayah
     */
    public void prefetchVerses(int surah, int startAyah, int endAyah) {
        for (int ayah = startAyah; ayah <= endAyah; ayah++) {
            final int currentAyah = ayah;
            getVerseText(surah, currentAyah, new VerseCallback() {
                @Override
                public void onVerseLoaded(String arabicText) {
                    Log.d(TAG, "Prefetched " + surah + ":" + currentAyah);
                }
                
                @Override
                public void onError(String error) {
                    Log.w(TAG, "Failed to prefetch " + surah + ":" + currentAyah);
                }
            });
        }
    }
    
    /**
     * Clear the cache
     */
    public void clearCache() {
        cache.clear();
    }
    
    /**
     * Remove Bismillah from verse text if present
     * Bismillah is not part of the verse content in most surahs
     */
    private String removeBismillah(String verseText) {
        if (verseText == null) return "";
        
        // Common variations of Bismillah
        String[] bismillahVariations = {
            "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
            "بِسْمِ اللَّهِ الرَّحْمَنِ الرَّحِيمِ",
            "بسم الله الرحمن الرحيم",
            "﷽" // Bismillah symbol
        };
        
        String cleaned = verseText.trim();
        
        // Remove BOM if present
        if (cleaned.startsWith("\uFEFF")) {
            cleaned = cleaned.substring(1).trim();
        }
        
        // Check and remove each variation
        for (String bismillah : bismillahVariations) {
            if (cleaned.startsWith(bismillah)) {
                cleaned = cleaned.substring(bismillah.length()).trim();
                Log.d(TAG, "Removed Bismillah from verse text");
                break;
            }
        }
        
        return cleaned;
    }
}
