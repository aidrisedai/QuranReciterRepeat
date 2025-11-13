package com.repeatquran.util;

import java.util.HashMap;
import java.util.Map;

public class SurahMetadata {
    
    public enum RevelationType {
        MAKKAN,
        MADINAN
    }
    
    private static final Map<Integer, RevelationType> REVELATION_TYPES = new HashMap<>();
    
    static {
        // Makkan surahs (86 total)
        int[] makkan = {
            1, 6, 7, 10, 11, 12, 14, 15, 16, 17, 18, 19, 20, 21, 23, 25, 26, 27, 28, 29,
            30, 31, 32, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 50, 51, 52,
            53, 54, 55, 56, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81,
            82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 99, 100, 101,
            102, 103, 104, 105, 106, 107, 109, 111, 112, 113, 114
        };
        
        for (int surah : makkan) {
            REVELATION_TYPES.put(surah, RevelationType.MAKKAN);
        }
        
        // Madinan surahs (28 total)
        int[] madinan = {
            2, 3, 4, 5, 8, 9, 13, 22, 24, 33, 47, 48, 49, 57, 58, 59, 60, 61, 62, 63,
            64, 65, 66, 98, 108, 110
        };
        
        for (int surah : madinan) {
            REVELATION_TYPES.put(surah, RevelationType.MADINAN);
        }
    }
    
    /**
     * Get revelation type for a surah
     */
    public static RevelationType getRevelationType(int surahNumber) {
        RevelationType type = REVELATION_TYPES.get(surahNumber);
        return (type != null) ? type : RevelationType.MAKKAN;
    }
    
    /**
     * Check if surah is Makkan
     */
    public static boolean isMakkan(int surahNumber) {
        return getRevelationType(surahNumber) == RevelationType.MAKKAN;
    }
    
    /**
     * Check if surah is Madinan
     */
    public static boolean isMadinan(int surahNumber) {
        return getRevelationType(surahNumber) == RevelationType.MADINAN;
    }
    
    /**
     * Get display name for revelation type
     */
    public static String getRevelationTypeName(int surahNumber) {
        RevelationType type = getRevelationType(surahNumber);
        return type == RevelationType.MAKKAN ? "Makkan" : "Madinan";
    }
}
