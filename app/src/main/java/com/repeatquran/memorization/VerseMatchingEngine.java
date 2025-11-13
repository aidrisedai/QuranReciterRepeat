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
     * Uses multiple methods and returns the BEST score
     * @return Match percentage (0.0 to 1.0)
     */
    public static double calculateSimilarity(String transcribed, String expected) {
        if (transcribed == null || expected == null) return 0.0;
        
        // Normalize: lowercase, remove diacritics, trim
        String normalizedTranscribed = normalizeArabic(transcribed);
        String normalizedExpected = normalizeArabic(expected);
        
        Log.d(TAG, "\n===== VERSE COMPARISON =====");
        Log.d(TAG, "Original Transcribed: " + transcribed);
        Log.d(TAG, "Original Expected: " + expected);
        Log.d(TAG, "Normalized Transcribed: " + normalizedTranscribed);
        Log.d(TAG, "Normalized Expected: " + normalizedExpected);
        
        // Method 1: Word matching (most important)
        double wordMatchScore = calculateWordMatchScore(normalizedTranscribed, normalizedExpected);
        Log.d(TAG, "Word Match Score: " + (wordMatchScore * 100) + "%");
        
        // Method 2: Character containment (does transcript contain the expected text?)
        double containmentScore = calculateContainmentScore(normalizedTranscribed, normalizedExpected);
        Log.d(TAG, "Containment Score: " + (containmentScore * 100) + "%");
        
        // Method 3: Levenshtein distance (edit distance)
        double levenshteinScore = calculateLevenshteinScore(normalizedTranscribed, normalizedExpected);
        Log.d(TAG, "Levenshtein Score: " + (levenshteinScore * 100) + "%");
        
        // Take the BEST score (most lenient)
        double finalScore = Math.max(wordMatchScore, Math.max(containmentScore, levenshteinScore));
        
        Log.d(TAG, "FINAL SCORE: " + (finalScore * 100) + "%");
        Log.d(TAG, "============================\n");
        
        return finalScore;
    }
    
    /**
     * Calculate word-based matching score
     */
    private static double calculateWordMatchScore(String transcribed, String expected) {
        String[] expectedWords = expected.split("\\s+");
        String[] transcribedWords = transcribed.split("\\s+");
        
        int matched = 0;
        for (String expectedWord : expectedWords) {
            if (expectedWord.length() <= 1) continue; // Skip single chars
            
            for (String transcribedWord : transcribedWords) {
                if (expectedWord.equals(transcribedWord)) {
                    matched++;
                    break;
                }
            }
        }
        
        return expectedWords.length > 0 ? (double) matched / expectedWords.length : 0.0;
    }
    
    /**
     * Calculate containment score (how much of expected is in transcribed)
     */
    private static double calculateContainmentScore(String transcribed, String expected) {
        if (expected.length() == 0) return 0.0;
        
        // Check how many characters from expected appear in transcribed (in order)
        int matchedChars = 0;
        int transcribedIndex = 0;
        
        for (int i = 0; i < expected.length() && transcribedIndex < transcribed.length(); i++) {
            char expectedChar = expected.charAt(i);
            
            // Skip spaces
            if (expectedChar == ' ') {
                matchedChars++;
                continue;
            }
            
            // Look for this character in the transcribed text
            while (transcribedIndex < transcribed.length()) {
                if (transcribed.charAt(transcribedIndex) == expectedChar) {
                    matchedChars++;
                    transcribedIndex++;
                    break;
                }
                transcribedIndex++;
            }
        }
        
        return (double) matchedChars / expected.length();
    }
    
    /**
     * Calculate Levenshtein-based score
     */
    private static double calculateLevenshteinScore(String transcribed, String expected) {
        int distance = levenshteinDistance(transcribed, expected);
        int maxLength = Math.max(transcribed.length(), expected.length());
        
        if (maxLength == 0) return 1.0;
        
        return 1.0 - ((double) distance / maxLength);
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
     * Normalize Arabic text for comparison - AGGRESSIVE MODE
     * - Remove ALL diacritics
     * - Normalize ALL character variations
     * - Remove punctuation and numbers
     * - Trim and normalize spacing
     */
    private static String normalizeArabic(String text) {
        if (text == null || text.isEmpty()) return "";
        
        String normalized = text;
        
        // Remove BOM
        if (normalized.startsWith("\uFEFF")) {
            normalized = normalized.substring(1);
        }
        
        // Remove ALL Arabic diacritics and marks (very aggressive)
        normalized = normalized.replaceAll("[\\u064B-\\u065F]", ""); // Harakat
        normalized = normalized.replaceAll("[\\u0670]", ""); // Superscript alif
        normalized = normalized.replaceAll("[\\u06D6-\\u06ED]", ""); // Quranic marks
        normalized = normalized.replaceAll("[\\u0617-\\u061A]", ""); // Small high signs
        normalized = normalized.replaceAll("[\\u064B-\\u0652]", ""); // More diacritics
        
        // Normalize ALL Alif variations to plain Alif
        normalized = normalized.replace('\u0622', '\u0627'); // Alif madda
        normalized = normalized.replace('\u0623', '\u0627'); // Alif hamza above
        normalized = normalized.replace('\u0625', '\u0627'); // Alif hamza below
        normalized = normalized.replace('\u0671', '\u0627'); // Alif wasla
        normalized = normalized.replace('\u0672', '\u0627'); // Alif wavy hamza above
        normalized = normalized.replace('\u0673', '\u0627'); // Alif wavy hamza below
        normalized = normalized.replace('\u0675', '\u0627'); // High hamza alif
        
        // Normalize Ya variations
        normalized = normalized.replace('\u0649', '\u064A'); // Alif maqsura -> Ya
        normalized = normalized.replace('\u06CC', '\u064A'); // Farsi Ya -> Arabic Ya
        normalized = normalized.replace('\u06CD', '\u064A'); // Ya with tail -> Ya
        
        // Normalize Ha variations
        normalized = normalized.replace('\u0629', '\u0647'); // Ta marbuta -> Ha
        normalized = normalized.replace('\u06C1', '\u0647'); // Urdu Ha -> Arabic Ha
        
        // Normalize Waw
        normalized = normalized.replace('\u0624', '\u0648'); // Waw hamza -> Waw
        
        // Remove punctuation, numbers, and special characters
        normalized = normalized.replaceAll("[\\p{Punct}\\p{Digit}]", "");
        normalized = normalized.replaceAll("[،؛؟]", ""); // Arabic punctuation
        normalized = normalized.replaceAll("[۝۩۞﴾﴿]", ""); // Decorative marks
        
        // Remove extra whitespace and trim
        normalized = normalized.trim().replaceAll("\\s+", " ");
        
        // Convert to lowercase (for consistency)
        normalized = normalized.toLowerCase();
        
        Log.d(TAG, "Normalized: '" + text + "' -> '" + normalized + "'");
        
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
