package com.repeatquran.memorization;

import android.content.Context;
import android.content.SharedPreferences;

import com.repeatquran.data.db.MemorizationAttemptDao;
import com.repeatquran.data.db.MemorizationAttemptEntity;
import com.repeatquran.data.db.MemorizationUnitDao;
import com.repeatquran.data.db.MemorizationUnitEntity;
import com.repeatquran.data.db.RepeatQuranDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TimelineCalculator {
    private Context context;
    private MemorizationUnitDao unitDao;
    private MemorizationAttemptDao attemptDao;
    private SharedPreferences prefs;
    
    // Average time estimates (in minutes)
    private static final float NEW_LEARNING_TIME_PER_AYA = 2.0f;  // 2 min per aya (slow×5 + fast×10)
    private static final float REVIEW_TIME_PER_AYA = 1.0f;        // 1 min per aya (reviews)
    
    // Review overhead: as you learn more, more time goes to reviews
    private static final float REVIEW_OVERHEAD_FACTOR = 0.5f;     // 50% of time goes to reviews
    
    public TimelineCalculator(Context context) {
        this.context = context;
        RepeatQuranDatabase db = RepeatQuranDatabase.get(context);
        this.unitDao = db.memorizationUnitDao();
        this.attemptDao = db.memorizationAttemptDao();
        this.prefs = context.getSharedPreferences("rq_prefs", Context.MODE_PRIVATE);
    }
    
    public static class TimelineProjection {
        public int dailyMinutes;
        public float effectiveNewLearningMinutes;
        public float projectedAyahsPerDay;
        public int remainingAyahs;
        public int daysToCompletion;
        public long projectedCompletionDate;
        public String goalName;
        public int goalTotalAyahs;
        public int learnedAyahs;
        public float progressPercent;
        public boolean onTrack;
        public int daysAheadBehind; // positive = ahead, negative = behind
        public float actualAyahsPerDay; // based on recent performance
    }
    
    /**
     * Calculate realistic timeline projection for current goal
     */
    public TimelineProjection calculateTimeline() {
        TimelineProjection proj = new TimelineProjection();
        
        // Get user settings
        proj.dailyMinutes = prefs.getInt("plan.daily.total", 45);
        String goalType = prefs.getString("timeline.goal", "juz_amma");
        
        // Calculate effective new learning time (accounting for review overhead)
        proj.effectiveNewLearningMinutes = proj.dailyMinutes * (1.0f - REVIEW_OVERHEAD_FACTOR);
        
        // Calculate ayahs per day
        proj.projectedAyahsPerDay = proj.effectiveNewLearningMinutes / NEW_LEARNING_TIME_PER_AYA;
        
        // Get goal details
        GoalDefinition goal = getGoalDefinition(goalType);
        proj.goalName = goal.name;
        proj.goalTotalAyahs = goal.totalAyahs;
        
        // Calculate progress
        proj.learnedAyahs = countLearnedAyahsInGoal(goal);
        proj.remainingAyahs = proj.goalTotalAyahs - proj.learnedAyahs;
        proj.progressPercent = proj.goalTotalAyahs > 0 
            ? (proj.learnedAyahs * 100.0f / proj.goalTotalAyahs) 
            : 0;
        
        // Calculate days to completion
        if (proj.projectedAyahsPerDay > 0) {
            proj.daysToCompletion = (int) Math.ceil(proj.remainingAyahs / proj.projectedAyahsPerDay);
        } else {
            proj.daysToCompletion = 999;
        }
        
        // Calculate projected completion date
        long now = System.currentTimeMillis();
        proj.projectedCompletionDate = now + (proj.daysToCompletion * 24L * 60 * 60 * 1000);
        
        // Calculate actual velocity based on last 7 days
        proj.actualAyahsPerDay = calculateActualVelocity();
        
        // Determine if on track
        if (proj.actualAyahsPerDay > 0) {
            proj.onTrack = proj.actualAyahsPerDay >= (proj.projectedAyahsPerDay * 0.8f); // Within 80%
            
            // Calculate days ahead/behind
            float actualDaysToComplete = proj.remainingAyahs / proj.actualAyahsPerDay;
            proj.daysAheadBehind = (int) (proj.daysToCompletion - actualDaysToComplete);
        } else {
            proj.onTrack = false;
            proj.daysAheadBehind = 0;
        }
        
        return proj;
    }
    
    /**
     * Calculate actual learning velocity based on recent performance
     */
    private float calculateActualVelocity() {
        // Get successful learning attempts in last 14 days
        long fourteenDaysAgo = System.currentTimeMillis() - (14 * 24 * 60 * 60 * 1000);
        List<MemorizationAttemptEntity> recentAttempts = attemptDao.getAttemptsSince(fourteenDaysAgo);
        
        if (recentAttempts.isEmpty()) {
            return 0;
        }
        
        // Count total ayahs learned from successful new learning attempts
        int totalAyahsLearned = 0;
        long earliestTimestamp = System.currentTimeMillis();
        long latestTimestamp = 0;
        
        for (MemorizationAttemptEntity attempt : recentAttempts) {
            if (attempt.success) {
                // Get the unit to count ayahs
                MemorizationUnitEntity unit = unitDao.getById(attempt.unitId);
                if (unit != null && "learned".equals(unit.status)) {
                    int ayahsInUnit = unit.endAyah - unit.startAyah + 1;
                    totalAyahsLearned += ayahsInUnit;
                    
                    if (attempt.completedAt < earliestTimestamp) earliestTimestamp = attempt.completedAt;
                    if (attempt.completedAt > latestTimestamp) latestTimestamp = attempt.completedAt;
                }
            }
        }
        
        if (totalAyahsLearned == 0) {
            return 0;
        }
        
        // Calculate days elapsed
        long elapsedMs = latestTimestamp - earliestTimestamp;
        float daysElapsed = elapsedMs / (24.0f * 60 * 60 * 1000);
        
        // Avoid division by zero
        if (daysElapsed < 1) daysElapsed = 1;
        
        return totalAyahsLearned / daysElapsed;
    }
    
    /**
     * Count ayahs learned within the goal range
     */
    private int countLearnedAyahsInGoal(GoalDefinition goal) {
        // For simplicity, use total learned count
        // In production, filter by goal's surah range
        int learned = unitDao.getLearnedCount();
        
        // Count ayahs in learned units
        // This is simplified - you'd need to query actual units and sum ayahs
        return learned * 3; // Rough estimate assuming 3 ayahs per unit on average
    }
    
    /**
     * Recalculate timeline when settings change
     */
    public void recalculateOnSettingsChange(int newDailyMinutes) {
        prefs.edit().putInt("plan.daily.total", newDailyMinutes).apply();
        
        // Recalculate time allocations
        int newMin = Math.round(newDailyMinutes * 0.5f);
        int recentMin = Math.round(newDailyMinutes * 0.3f);
        int oldMin = Math.max(1, newDailyMinutes - newMin - recentMin);
        
        prefs.edit()
            .putInt("plan.daily.new_minutes", newMin)
            .putInt("plan.daily.recent_minutes", recentMin)
            .putInt("plan.daily.old_minutes", oldMin)
            .apply();
    }
    
    /**
     * Format projected completion date
     */
    public static String formatCompletionDate(long timestamp) {
        SimpleDateFormat format = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        return format.format(new Date(timestamp));
    }
    
    /**
     * Get milestone message for current progress
     */
    public static String getMilestoneMessage(float progressPercent) {
        if (progressPercent >= 100) return "🎉 Goal Complete!";
        if (progressPercent >= 75) return "🔥 Final stretch!";
        if (progressPercent >= 50) return "💪 Halfway there!";
        if (progressPercent >= 25) return "📈 Great progress!";
        if (progressPercent >= 10) return "✨ Off to a strong start!";
        return "🌱 Beginning journey";
    }
    
    // Goal definitions
    private static class GoalDefinition {
        String name;
        int totalAyahs;
        int startSurah;
        int endSurah;
        
        GoalDefinition(String name, int totalAyahs, int startSurah, int endSurah) {
            this.name = name;
            this.totalAyahs = totalAyahs;
            this.startSurah = startSurah;
            this.endSurah = endSurah;
        }
    }
    
    private GoalDefinition getGoalDefinition(String goalType) {
        switch (goalType) {
            case "juz_amma":
                return new GoalDefinition("Juz ʿAmma (Surahs 78-114)", 564, 78, 114);
            case "juz_29":
                return new GoalDefinition("Juz 29 (Surahs 67-77)", 390, 67, 77);
            case "last_10":
                return new GoalDefinition("Last 10 Surahs", 129, 105, 114);
            case "full_quran":
                return new GoalDefinition("Full Quran", 6236, 1, 114);
            default:
                return new GoalDefinition("Juz ʿAmma", 564, 78, 114);
        }
    }
}
