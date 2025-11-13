package com.repeatquran.memorization;

import android.util.Log;

import com.repeatquran.util.AyahCounts;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses natural language goal descriptions into structured goal data
 * 
 * Examples:
 * - "Memorize Surah Al-Mulk" → Surah 67 (1-30)
 * - "5 verses per day" → Daily goal with 5 verses/day
 * - "Surah 2 verses 1-10" → Al-Baqarah 2:1-10
 * - "Juz Amma" → Surah 78-114
 * - "Last 10 surahs" → Surah 105-114
 */
public class GoalParser {
    
    private static final String TAG = "GoalParser";
    
    public static class ParsedGoal {
        public String goalText;
        public String goalType; // daily, weekly, monthly, one-time
        public Integer versesPerDay;
        public Integer startSurah;
        public Integer startAyah;
        public Integer endSurah;
        public Integer endAyah;
        public int totalVerses;
        public String error; // if parsing failed
        
        public boolean isValid() {
            return error == null && startSurah != null && startAyah != null 
                    && endSurah != null && endAyah != null;
        }
    }
    
    /**
     * Parse natural language goal description
     */
    public static ParsedGoal parse(String input) {
        ParsedGoal result = new ParsedGoal();
        result.goalText = input;
        
        if (input == null || input.trim().isEmpty()) {
            result.error = "Goal text is empty";
            return result;
        }
        
        String normalized = input.toLowerCase().trim();
        
        // Try to extract verses per day/week/month
        extractRecurrencePattern(normalized, result);
        
        // Try to extract verse range
        if (!extractVerseRange(normalized, result)) {
            // Try named sections (Juz Amma, last 10, etc.)
            if (!extractNamedSection(normalized, result)) {
                // Try surah name
                if (!extractSurahByName(normalized, result)) {
                    result.error = "Could not understand goal. Try: 'Surah 67' or '5 verses per day' or 'Juz Amma'";
                }
            }
        }
        
        // Calculate total verses if we have valid range
        if (result.isValid()) {
            result.totalVerses = calculateTotalVerses(result);
        }
        
        return result;
    }
    
    /**
     * Extract recurrence pattern (X verses per day/week/month)
     */
    private static void extractRecurrencePattern(String input, ParsedGoal result) {
        // Pattern: "5 verses per day" or "3 ayat daily" or "1 page weekly"
        Pattern dailyPattern = Pattern.compile("(\\d+)\\s*(verse|ayah|ayat|page)s?\\s*(per|every|each)?\\s*day", Pattern.CASE_INSENSITIVE);
        Pattern weeklyPattern = Pattern.compile("(\\d+)\\s*(verse|ayah|ayat|surah)s?\\s*(per|every|each)?\\s*(week|weekly)", Pattern.CASE_INSENSITIVE);
        Pattern monthlyPattern = Pattern.compile("(\\d+)\\s*(verse|ayah|ayat|surah)s?\\s*(per|every|each)?\\s*(month|monthly)", Pattern.CASE_INSENSITIVE);
        
        Matcher dailyMatcher = dailyPattern.matcher(input);
        if (dailyMatcher.find()) {
            result.versesPerDay = Integer.parseInt(dailyMatcher.group(1));
            result.goalType = "daily";
            Log.d(TAG, "Detected daily goal: " + result.versesPerDay + " verses/day");
            return;
        }
        
        Matcher weeklyMatcher = weeklyPattern.matcher(input);
        if (weeklyMatcher.find()) {
            int count = Integer.parseInt(weeklyMatcher.group(1));
            result.versesPerDay = count / 7; // Convert to daily
            result.goalType = "weekly";
            Log.d(TAG, "Detected weekly goal: " + count + " (converted to " + result.versesPerDay + " verses/day)");
            return;
        }
        
        Matcher monthlyMatcher = monthlyPattern.matcher(input);
        if (monthlyMatcher.find()) {
            int count = Integer.parseInt(monthlyMatcher.group(1));
            result.versesPerDay = count / 30; // Convert to daily
            result.goalType = "monthly";
            Log.d(TAG, "Detected monthly goal: " + count + " (converted to " + result.versesPerDay + " verses/day)");
            return;
        }
        
        // Default to one-time
        result.goalType = "one-time";
    }
    
    /**
     * Extract specific verse range (e.g., "Surah 2 verses 1-10")
     */
    private static boolean extractVerseRange(String input, ParsedGoal result) {
        // Pattern: "surah 2 verses 1-10" or "2:1-10" or "surah 67"
        Pattern rangePattern = Pattern.compile("surah\\s+(\\d+)\\s*(?:verse|ayah|ayat)?s?\\s*(\\d+)?\\s*-\\s*(\\d+)?", Pattern.CASE_INSENSITIVE);
        Pattern colonPattern = Pattern.compile("(\\d+):(\\d+)(?:-(\\d+))?");
        
        Matcher rangeMatcher = rangePattern.matcher(input);
        if (rangeMatcher.find()) {
            result.startSurah = Integer.parseInt(rangeMatcher.group(1));
            result.endSurah = result.startSurah;
            
            if (rangeMatcher.group(2) != null && rangeMatcher.group(3) != null) {
                // Has verse range
                result.startAyah = Integer.parseInt(rangeMatcher.group(2));
                result.endAyah = Integer.parseInt(rangeMatcher.group(3));
            } else {
                // Whole surah
                result.startAyah = 1;
                result.endAyah = AyahCounts.getCount(result.startSurah);
            }
            
            Log.d(TAG, "Extracted range: Surah " + result.startSurah + ":" + result.startAyah + "-" + result.endAyah);
            return true;
        }
        
        Matcher colonMatcher = colonPattern.matcher(input);
        if (colonMatcher.find()) {
            result.startSurah = Integer.parseInt(colonMatcher.group(1));
            result.startAyah = Integer.parseInt(colonMatcher.group(2));
            result.endSurah = result.startSurah;
            result.endAyah = colonMatcher.group(3) != null 
                    ? Integer.parseInt(colonMatcher.group(3)) 
                    : AyahCounts.getCount(result.startSurah);
            
            Log.d(TAG, "Extracted colon format: " + result.startSurah + ":" + result.startAyah + "-" + result.endAyah);
            return true;
        }
        
        // Just surah number
        Pattern surahOnlyPattern = Pattern.compile("surah\\s+(\\d+)", Pattern.CASE_INSENSITIVE);
        Matcher surahMatcher = surahOnlyPattern.matcher(input);
        if (surahMatcher.find()) {
            result.startSurah = Integer.parseInt(surahMatcher.group(1));
            result.endSurah = result.startSurah;
            result.startAyah = 1;
            result.endAyah = AyahCounts.getCount(result.startSurah);
            
            Log.d(TAG, "Extracted surah only: Surah " + result.startSurah);
            return true;
        }
        
        return false;
    }
    
    /**
     * Extract named sections (Juz Amma, last 10, etc.)
     */
    private static boolean extractNamedSection(String input, ParsedGoal result) {
        // Juz Amma (Juz 30)
        if (input.contains("juz amma") || input.contains("juz 30")) {
            result.startSurah = 78;
            result.startAyah = 1;
            result.endSurah = 114;
            result.endAyah = AyahCounts.getCount(114);
            Log.d(TAG, "Detected Juz Amma");
            return true;
        }
        
        // Last 10 surahs
        if (input.contains("last 10") || input.contains("last ten")) {
            result.startSurah = 105;
            result.startAyah = 1;
            result.endSurah = 114;
            result.endAyah = AyahCounts.getCount(114);
            Log.d(TAG, "Detected last 10 surahs");
            return true;
        }
        
        // Juz 29
        if (input.contains("juz 29")) {
            result.startSurah = 67;
            result.startAyah = 1;
            result.endSurah = 77;
            result.endAyah = AyahCounts.getCount(77);
            Log.d(TAG, "Detected Juz 29");
            return true;
        }
        
        return false;
    }
    
    /**
     * Extract surah by common English names
     */
    private static boolean extractSurahByName(String input, ParsedGoal result) {
        // Map common names to surah numbers
        if (input.contains("al-mulk") || input.contains("mulk")) {
            result.startSurah = 67;
        } else if (input.contains("al-kahf") || input.contains("kahf")) {
            result.startSurah = 18;
        } else if (input.contains("yasin") || input.contains("ya-sin")) {
            result.startSurah = 36;
        } else if (input.contains("al-fatiha") || input.contains("fatiha")) {
            result.startSurah = 1;
        } else if (input.contains("al-baqarah") || input.contains("baqarah")) {
            result.startSurah = 2;
        } else if (input.contains("al-ikhlas") || input.contains("ikhlas")) {
            result.startSurah = 112;
        } else if (input.contains("al-falaq") || input.contains("falaq")) {
            result.startSurah = 113;
        } else if (input.contains("an-nas") || input.contains("nas")) {
            result.startSurah = 114;
        } else {
            return false;
        }
        
        result.endSurah = result.startSurah;
        result.startAyah = 1;
        result.endAyah = AyahCounts.getCount(result.startSurah);
        
        Log.d(TAG, "Detected surah by name: " + result.startSurah);
        return true;
    }
    
    /**
     * Calculate total verses in the range
     */
    private static int calculateTotalVerses(ParsedGoal goal) {
        if (goal.startSurah == null || goal.endSurah == null) return 0;
        
        int total = 0;
        
        if (goal.startSurah.equals(goal.endSurah)) {
            // Same surah
            total = goal.endAyah - goal.startAyah + 1;
        } else {
            // Multiple surahs
            // First surah (from startAyah to end)
            total += AyahCounts.getCount(goal.startSurah) - goal.startAyah + 1;
            
            // Middle surahs (complete)
            for (int s = goal.startSurah + 1; s < goal.endSurah; s++) {
                total += AyahCounts.getCount(s);
            }
            
            // Last surah (from 1 to endAyah)
            total += goal.endAyah;
        }
        
        return total;
    }
}
