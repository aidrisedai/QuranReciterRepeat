package com.repeatquran.memorization;

import com.repeatquran.data.db.MemorizationUnitEntity;

/**
 * Implements spaced repetition scheduling using SuperMemo 2 algorithm.
 * Intervals: First review after 1 day, then increasing based on ease factor.
 */
public class ReviewScheduler {
    // Base intervals for first few reviews (in days)
    private static final int[] BASE_INTERVALS = {1, 3, 7, 14, 30, 90};
    
    private static final float MIN_EASE = 1.3f;
    private static final float MAX_EASE = 2.5f;
    
    /**
     * Calculate next review date after successful completion
     */
    public static long calculateNextReview(MemorizationUnitEntity unit, boolean success) {
        long now = System.currentTimeMillis();
        
        if (!success) {
            // Failed: review again in 10 minutes
            return now + (10 * 60 * 1000);
        }
        
        int level = unit.reviewLevel;
        float ease = unit.ease > 0 ? unit.ease : 2.5f;
        
        int intervalDays;
        if (level < BASE_INTERVALS.length) {
            // Use predefined intervals for first reviews
            intervalDays = BASE_INTERVALS[level];
        } else {
            // Calculate interval based on ease factor
            int prevInterval = BASE_INTERVALS[BASE_INTERVALS.length - 1];
            intervalDays = Math.round(prevInterval * ease);
        }
        
        return now + (intervalDays * 24L * 60 * 60 * 1000);
    }
    
    /**
     * Update ease factor based on performance
     */
    public static float updateEase(float currentEase, boolean success, int consecutiveSuccess) {
        if (currentEase == 0) currentEase = 2.5f;
        
        if (success) {
            // Slightly increase ease on success
            currentEase += 0.1f;
        } else {
            // Decrease ease on failure
            currentEase -= 0.2f;
        }
        
        // Clamp to valid range
        return Math.max(MIN_EASE, Math.min(MAX_EASE, currentEase));
    }
    
    /**
     * Determine if a unit should be split after failure
     */
    public static boolean shouldSplit(MemorizationUnitEntity unit) {
        // Split if:
        // 1. User has failed 2+ times
        // 2. Chunk has more than 1 verse
        int versesInUnit = unit.endAyah - unit.startAyah + 1;
        return unit.failCount >= 2 && versesInUnit > 1;
    }
}
