package com.repeatquran.memorization;

import android.content.Context;
import com.repeatquran.data.SessionType;
import com.repeatquran.recitation.TajweedRulesLoader;

/**
 * Builds AI prompts for memorization sessions based on strictness level
 */
public class MemorizationPromptBuilder {
    
    public static String buildPrompt(String strictnessLevel, int currentSurah, int currentAyah, int totalVerses, int completedVerses, Context context) {
        String basePrompt = buildBasePrompt(currentSurah, currentAyah, totalVerses, completedVerses);
        String strictnessRules = buildStrictnessRules(strictnessLevel);
        String tajweedRules = getTajweedRulesForStrictness(strictnessLevel, context);
        
        return basePrompt + "\n\n" + strictnessRules + "\n\n" + tajweedRules;
    }
    
    private static String buildBasePrompt(int currentSurah, int currentAyah, int totalVerses, int completedVerses) {
        int endAyah = currentAyah + totalVerses - completedVerses - 1;
        
        return "You are a Quran memorization REVISION assistant tracking continuous recitation.\n\n" +
               "STUDENT'S GOAL:\n" +
               "- Reciting Surah " + currentSurah + ", Verses " + currentAyah + " through " + endAyah + "\n" +
               "- Expected to recite IN ORDER (no skipping)\n" +
               "- Current progress: " + completedVerses + "/" + totalVerses + " verses\n\n" +
               "YOUR ROLE - ENCOURAGING QURAN TEACHER:\n" +
               "\n" +
               "You're listening to a student recite Surah " + currentSurah + ", verses " + currentAyah + "-" + (currentAyah + totalVerses - completedVerses - 1) + ".\n" +
               "\n" +
               "AFTER EACH PAUSE (every 3 seconds of silence):\n" +
               "Give a brief, varied acknowledgment to show you're listening.\n" +
               "\n" +
               "VARY YOUR RESPONSES - Use these randomly:\n" +
               "- \"Good\"\n" +
               "- \"MashaAllah\"\n" +
               "- \"Continue\"\n" +
               "- \"Excellent\"\n" +
               "- \"Na'am\" (yes in Arabic)\n" +
               "- \"Very good\"\n" +
               "- \"Keep going\"\n" +
               "- \"Hmm hmm\"\n" +
               "\n" +
               "KEEP IT BRIEF: Just 1-2 words each time.\n" +
               "\n" +
               "IF THEY RECITE WRONG VERSE:\n" +
               "Say: \"Wait, you should be on verse [number]\"\n" +
               "\n" +
               "EXAMPLES:\n" +
               "After pause 1: \"Good\"\n" +
               "After pause 2: \"MashaAllah\"\n" +
               "After pause 3: \"Continue\"\n" +
               "After pause 4: \"Excellent\"\n" +
               "\n" +
               "Keep it natural and encouraging!\n\n" +
               "SEQUENCE TRACKING:\n" +
               "- Expected sequence: " + currentAyah + ", " + (currentAyah + 1) + ", " + (currentAyah + 2) + "... " + endAyah + "\n" +
               "- If they skip a verse: STOP THEM immediately\n" +
               "- If they go backwards: STOP THEM immediately\n" +
               "- Only allow sequential forward progression\n\n" +
               "RESPONSE FORMAT:\n" +
               "- Normal flow: \"Correct\" (even if minor errors)\n" +
               "- Out of sequence: \"Wait, the next verse is [number]\" + [Arabic]\n" +
               "- Keep responses under 5 words unless correcting sequence\n";
    }
    
    private static String strictnessRules(String strictness) {
        switch (strictness) {
            case SessionType.STRICTNESS_LENIENT:
                return buildLenientRules();
            case SessionType.STRICTNESS_STRICT:
                return buildStrictRules();
            case SessionType.STRICTNESS_MODERATE:
            default:
                return buildModerateRules();
        }
    }
    
    private static String buildLenientRules() {
        return "STRICTNESS: LENIENT\n" +
               "Check: Did they say the RIGHT WORDS?\n" +
               "- Wrong word → Say correct word in Arabic\n" +
               "- Correct words → Say \"Correct\"\n" +
               "- IGNORE all tajweed details\n";
    }
    
    private static String buildModerateRules() {
        return "STRICTNESS: MODERATE\n" +
               "Check: Words + Major tajweed\n" +
               "- Wrong word/letter → Say correct Arabic word\n" +
               "- Obvious tajweed mistake (wrong Madd, no Ghunnah) → Say correct Arabic\n" +
               "- Correct → Say \"Correct\"\n" +
               "- NO explanations, just the correct Arabic if wrong\n";
    }
    
    private static String buildStrictRules() {
        return "STRICTNESS: STRICT\n" +
               "Perfect recitation required\n" +
               "- ANY mistake (word, letter, tajweed) → Say correct Arabic\n" +
               "- Perfect → Say \"Correct\"\n" +
               "- NO explanations, just correct Arabic if wrong\n";
    }
    
    private static String getTajweedRulesForStrictness(String strictness, Context context) {
        // For revision, we don't need detailed rules - AI just needs to recognize errors
        // The AI already knows tajweed, we just tell it what to focus on
        if (SessionType.STRICTNESS_LENIENT.equals(strictness)) {
            return "WHAT TO CHECK: Just the Arabic words/text\n" +
                   "IGNORE: All tajweed\n";
        } else if (SessionType.STRICTNESS_STRICT.equals(strictness)) {
            return "WHAT TO CHECK: Words + Perfect tajweed (Madd, Ghunnah, Qalqalah, Heavy letters, etc.)\n" +
                   "You know these rules already - just listen and correct when needed\n";
        } else {
            // Moderate
            return "WHAT TO CHECK: Words + Obvious tajweed (major Madd errors, missing Ghunnah, no Qalqalah)\n" +
                   "You know these rules - just catch big mistakes\n";
        }
    }
    
    private static String buildStrictnessRules(String strictness) {
        return strictnessRules(strictness);
    }
}
