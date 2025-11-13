package com.repeatquran.memorization;

import android.content.Context;
import android.content.SharedPreferences;

import com.repeatquran.data.db.MemorizationAttemptDao;
import com.repeatquran.data.db.MemorizationAttemptEntity;
import com.repeatquran.data.db.RepeatQuranDatabase;

import java.util.List;

public class PerformanceTracker {
    private static final int WINDOW_SIZE = 10; // Analyze last 10 sessions
    private static final float SUCCESS_THRESHOLD_HIGH = 0.80f; // 80% for increase
    private static final float SUCCESS_THRESHOLD_LOW = 0.60f; // 60% for decrease
    private static final float OVERTIME_THRESHOLD = 0.20f; // 20% over estimated time
    
    private static final int MIN_CHUNK_SIZE = 1; // ayah
    private static final int MAX_CHUNK_SIZE = 5; // ayahs
    private static final int DEFAULT_CHUNK_SIZE = 3; // ayahs
    
    private Context context;
    private MemorizationAttemptDao attemptDao;
    private SharedPreferences prefs;
    
    public PerformanceTracker(Context context) {
        this.context = context;
        this.attemptDao = RepeatQuranDatabase.get(context).memorizationAttemptDao();
        this.prefs = context.getSharedPreferences("rq_prefs", Context.MODE_PRIVATE);
    }
    
    public static class PerformanceMetrics {
        public int sessionsAnalyzed;
        public float successRate;
        public float avgTimePerSession; // milliseconds
        public float estimatedTimePerSession; // milliseconds (based on chunk size)
        public boolean isOvertime;
        public boolean shouldIncreaseChunk;
        public boolean shouldDecreaseChunk;
        public int currentChunkSize;
        public int recommendedChunkSize;
        public int sessionsUntilNextAdjustment;
        public int currentStreak;
        public long totalStudyTime;
    }
    
    /**
     * Analyze recent performance and generate recommendations
     */
    public PerformanceMetrics analyzePerformance() {
        PerformanceMetrics metrics = new PerformanceMetrics();
        
        // Get current settings
        metrics.currentChunkSize = prefs.getInt("adaptive.chunk_size", DEFAULT_CHUNK_SIZE);
        int sessionsSinceAdjust = prefs.getInt("adaptive.sessions_since_adjust", 0);
        metrics.sessionsUntilNextAdjustment = Math.max(0, WINDOW_SIZE - sessionsSinceAdjust);
        
        // Get recent attempts
        List<MemorizationAttemptEntity> recent = attemptDao.getRecentAttempts(WINDOW_SIZE);
        metrics.sessionsAnalyzed = recent.size();
        
        if (recent.isEmpty()) {
            metrics.recommendedChunkSize = metrics.currentChunkSize;
            return metrics;
        }
        
        // Calculate success rate
        int successes = 0;
        long totalTime = 0;
        int streak = 0;
        
        for (int i = 0; i < recent.size(); i++) {
            MemorizationAttemptEntity attempt = recent.get(i);
            if (attempt.success) {
                successes++;
                if (i == 0) { // Most recent first
                    streak = 1;
                    for (int j = 1; j < recent.size(); j++) {
                        if (recent.get(j).success) streak++;
                        else break;
                    }
                }
            }
            totalTime += attempt.durationMs;
        }
        
        metrics.successRate = (float) successes / recent.size();
        metrics.avgTimePerSession = (float) totalTime / recent.size();
        metrics.currentStreak = streak;
        metrics.totalStudyTime = attemptDao.getTotalStudyTime();
        
        // Estimate expected time per session (rough: 2 min per aya × chunk size)
        metrics.estimatedTimePerSession = metrics.currentChunkSize * 2 * 60 * 1000; // ms
        
        // Check if overtime
        float overtimeRatio = (metrics.avgTimePerSession - metrics.estimatedTimePerSession) / metrics.estimatedTimePerSession;
        metrics.isOvertime = overtimeRatio > OVERTIME_THRESHOLD;
        
        // Only adjust after full window
        if (sessionsSinceAdjust >= WINDOW_SIZE) {
            // Increase chunk: high success + finished early
            if (metrics.successRate >= SUCCESS_THRESHOLD_HIGH && !metrics.isOvertime) {
                metrics.shouldIncreaseChunk = true;
                metrics.recommendedChunkSize = Math.min(MAX_CHUNK_SIZE, metrics.currentChunkSize + 1);
            }
            // Decrease chunk: low success or overtime
            else if (metrics.successRate < SUCCESS_THRESHOLD_LOW || metrics.isOvertime) {
                metrics.shouldDecreaseChunk = true;
                metrics.recommendedChunkSize = Math.max(MIN_CHUNK_SIZE, metrics.currentChunkSize - 1);
            } else {
                metrics.recommendedChunkSize = metrics.currentChunkSize;
            }
        } else {
            metrics.recommendedChunkSize = metrics.currentChunkSize;
        }
        
        return metrics;
    }
    
    /**
     * Apply chunk size adjustment if recommended
     */
    public void applyAdjustment(PerformanceMetrics metrics) {
        if (metrics.shouldIncreaseChunk || metrics.shouldDecreaseChunk) {
            prefs.edit()
                .putInt("adaptive.chunk_size", metrics.recommendedChunkSize)
                .putInt("adaptive.sessions_since_adjust", 0)
                .putLong("adaptive.last_adjust_time", System.currentTimeMillis())
                .apply();
        }
    }
    
    /**
     * Increment session counter after each completed unit
     */
    public void recordSessionComplete() {
        int count = prefs.getInt("adaptive.sessions_since_adjust", 0);
        prefs.edit().putInt("adaptive.sessions_since_adjust", count + 1).apply();
    }
    
    /**
     * Get current chunk size for creating new units
     */
    public int getCurrentChunkSize() {
        return prefs.getInt("adaptive.chunk_size", DEFAULT_CHUNK_SIZE);
    }
    
    /**
     * Reset adaptive settings (for testing or user request)
     */
    public void reset() {
        prefs.edit()
            .putInt("adaptive.chunk_size", DEFAULT_CHUNK_SIZE)
            .putInt("adaptive.sessions_since_adjust", 0)
            .remove("adaptive.last_adjust_time")
            .apply();
    }
}
