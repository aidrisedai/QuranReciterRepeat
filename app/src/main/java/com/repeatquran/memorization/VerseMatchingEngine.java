package com.repeatquran.memorization;

import android.util.Log;

/**
 * Engine for matching transcribed Arabic text with expected Quran verses
 * Uses Levenshtein distance for fuzzy matching
 */
public class VerseMatchingEngine {
    
    private static final String TAG = "VerseMatchingEngine";
    private static final double MATCH_THRESHOLD = 0.60; // 60% similarity required (reduced for testing)
    
    /**
     * Calculate similarity between transcribed text and expected verse
     * @return Match percentage (0.0 to 1.0)
     */
    public static double calculateSimilarity(String transcribed, String expected) {
        if (transcribed == null || expected == null) return 0.0;
        
        // Normalize: lowercase, remove diacritics, trim
        String normalizedTranscribed = normalizeArabic(transcribed);
        String normalizedExpected = normalizeArabic(expected);
        
        Log.d(TAG, "Comparing:");
        Log.d(TAG, "  Transcribed: " + normalizedTranscribed);
        Log.d(TAG, "  Expected: " + normalizedExpected);
        
        // Calculate Levenshtein distance
        int distance = levenshteinDistance(normalizedTranscribed, normalizedExpected);
        int maxLength = Math.max(normalizedTranscribed.length(), normalizedExpected.length());
        
        if (maxLength == 0) return 1.0;
        
        double similarity = 1.0 - ((double) distance / maxLength);
        
        Log.d(TAG, "  Similarity: " + (similarity * 100) + "%");
        
        return similarity;
    }
    
    /**
     * Check if transcribed text matches expected verse well enough
     */
    public static boolean isGoodMatch(String transcribed, String expected) {
        double similarity = calculateSimilarity(transcribed, expected);
        return similarity >= MATCH_THRESHOLD;
    }
    
    /**
     * Check if transcribed text likely contains the complete verse
     */
    public static boolean containsCompleteVerse(String transcribed, String expected) {
        String normalizedTranscribed = normalizeArabic(transcribed);
        String normalizedExpected = normalizeArabic(expected);
        
        // Check if transcription contains most of the expected verse
        String[] expectedWords = normalizedExpected.split("\\s+");
        int matchedWords = 0;
        
        for (String word : expectedWords) {
            if (word.length() > 2 && normalizedTranscribed.contains(word)) {
                matchedWords++;
            }
        }
        
        double wordMatchRatio = (double) matchedWords / expectedWords.length;
        
        Log.d(TAG, "Word match: " + matchedWords + "/" + expectedWords.length + " = " + (wordMatchRatio * 100) + "%");
        
        return wordMatchRatio >= 0.5; // 50% of words present (reduced for testing)
    }
    
    /**
     * Public method to normalize Arabic text
     */
    public static String normalizeArabicPublic(String text) {
        return normalizeArabic(text);
    }
    
    /**
     * Normalize Arabic text for comparison
     * - Remove diacritics (harakat)
     * - Normalize certain characters
     * - Trim and lowercase
     */
    private static String normalizeArabic(String text) {
        if (text == null) return "";
        
        String normalized = text;
        
        // Remove Arabic diacritics (tashkeel)
        normalized = normalized.replaceAll("[\\u064B-\\u065F]", ""); // Remove harakat
        normalized = normalized.replaceAll("[\\u0670]", ""); // Remove superscript alif
        normalized = normalized.replaceAll("[\\u06D6-\\u06ED]", ""); // Remove Quranic annotation marks
        
        // Normalize variations
        normalized = normalized.replace('\u0622', '\u0627'); // Alif with madda -> Alif
        normalized = normalized.replace('\u0623', '\u0627'); // Alif with hamza above -> Alif
        normalized = normalized.replace('\u0625', '\u0627'); // Alif with hamza below -> Alif
        normalized = normalized.replace('\u0671', '\u0627'); // Alif wasla -> Alif
        normalized = normalized.replace('\u0649', '\u064A'); // Alif maqsura -> Ya
        normalized = normalized.replace('\u0629', '\u0647'); // Ta marbuta -> Ha
        
        // Remove extra whitespace
        normalized = normalized.trim().replaceAll("\\s+", " ");
        
        return normalized;
    }
    
    /**
     * Calculate Levenshtein distance between two strings
     */
    private static int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        
        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }
        
        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }
        
        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                int cost = s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1;
                
                dp[i][j] = Math.min(
                    Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                );
            }
        }
        
        return dp[s1.length()][s2.length()];
    }
}
