package com.repeatquran.recitation;

import android.content.Context;
import android.util.Log;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import com.repeatquran.R;

/**
 * Loads authoritative Tajweed rules from raw resources to ground AI responses
 * This prevents hallucination and ensures correct Islamic scholarship
 */
public class TajweedRulesLoader {
    private static final String TAG = "TajweedRulesLoader";
    private static String cachedRules = null;
    
    /**
     * Load the comprehensive Tajweed rules document
     * Rules are cached after first load for performance
     */
    public static String loadTajweedRules(Context context) {
        if (cachedRules != null) {
            return cachedRules;
        }
        
        try {
            InputStream inputStream = context.getResources().openRawResource(R.raw.tajweed_rules);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder rules = new StringBuilder();
            String line;
            
            while ((line = reader.readLine()) != null) {
                rules.append(line).append("\n");
            }
            
            reader.close();
            cachedRules = rules.toString();
            Log.d(TAG, "Tajweed rules loaded: " + cachedRules.length() + " characters");
            return cachedRules;
            
        } catch (Exception e) {
            Log.e(TAG, "Error loading Tajweed rules", e);
            return getFallbackRules();
        }
    }
    
    /**
     * Get a condensed version of rules for contexts with token limits (e.g., real-time API)
     * Loads from the pre-optimized condensed rules file
     */
    public static String getCondensedRules(Context context) {
        try {
            InputStream inputStream = context.getResources().openRawResource(R.raw.tajweed_rules_condensed);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder rules = new StringBuilder();
            String line;
            
            while ((line = reader.readLine()) != null) {
                rules.append(line).append("\n");
            }
            
            reader.close();
            String condensedRules = rules.toString();
            Log.d(TAG, "Condensed Tajweed rules loaded: " + condensedRules.length() + " characters (optimized for tokens)");
            return condensedRules;
            
        } catch (Exception e) {
            Log.e(TAG, "Error loading condensed Tajweed rules, using fallback", e);
            return getFallbackRules();
        }
    }
    
    /**
     * Fallback rules if file cannot be loaded
     */
    private static String getFallbackRules() {
        return "BASIC TAJWEED RULES:\n\n" +
               "1. Makharij: Pronounce each letter from its correct articulation point\n" +
               "2. Sifat: Heavy letters (خ ص ض غ ط ق ظ) must be pronounced heavy\n" +
               "3. Madd: Natural = 2 counts, Connected = 4-5, Necessary = 6\n" +
               "4. Ghunnah: Nasal sound for Noon/Meem rules (2 counts)\n" +
               "5. Qalqalah: Echo/bounce for ق ط ب ج د\n" +
               "6. Waqf: Stop at proper places, drop Tanween\n\n" +
               "Always cite classical sources: Al-Jazariyyah, Tuhfat al-Atfal";
    }
}
