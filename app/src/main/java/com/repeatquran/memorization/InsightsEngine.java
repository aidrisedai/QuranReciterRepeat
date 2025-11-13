package com.repeatquran.memorization;

import android.content.Context;
import android.content.SharedPreferences;

import com.repeatquran.data.db.MemorizationAttemptDao;
import com.repeatquran.data.db.MemorizationAttemptEntity;
import com.repeatquran.data.db.MemorizationUnitDao;
import com.repeatquran.data.db.MemorizationUnitEntity;
import com.repeatquran.data.db.RepeatQuranDatabase;
import com.repeatquran.util.SurahMetadata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InsightsEngine {
    
    private Context context;
    private MemorizationUnitDao unitDao;
    private MemorizationAttemptDao attemptDao;
    private SharedPreferences prefs;
    
    public InsightsEngine(Context context) {
        this.context = context;
        RepeatQuranDatabase db = RepeatQuranDatabase.get(context);
        this.unitDao = db.memorizationUnitDao();
        this.attemptDao = db.memorizationAttemptDao();
        this.prefs = context.getSharedPreferences("rq_prefs", Context.MODE_PRIVATE);
    }
    
    public static class InsightData {
        // Core metrics
        public float avgTimePerAya;            // minutes
        public float overallSuccessRate;       // 0-1
        public float sessionSuccessRate;       // 0-1
        public Map<Integer, Float> retentionByLevel; // review level -> retention rate
        
        // Chunk size evolution
        public int currentChunkSize;
        public int minChunkSizeUsed;
        public int maxChunkSizeUsed;
        public float avgChunkSize;
        
        // Pattern insights
        public List<String> insights;          // Human-readable insights
        public List<String> recommendations;   // Actionable recommendations
        
        // Surah performance
        public float makkanSuccessRate;
        public float madinanSuccessRate;
        public int makkanCount;
        public int madinanCount;
        
        // Time-based patterns
        public float avgSessionDuration;       // minutes
        public boolean accuracyDropsWithTime;
        public int optimalSessionLength;       // minutes (if pattern detected)
        
        // Weekly stats
        public int weeklyAttempts;
        public int weeklySuccesses;
        public long weeklyStudyTime;           // milliseconds
        public int consistencyScore;           // 0-100
        
        public InsightData() {
            insights = new ArrayList<>();
            recommendations = new ArrayList<>();
            retentionByLevel = new HashMap<>();
        }
    }
    
    /**
     * Generate comprehensive insights from memorization data
     */
    public InsightData generateInsights() {
        InsightData data = new InsightData();
        
        // Get recent attempts (last 30 days)
        long thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000);
        List<MemorizationAttemptEntity> recentAttempts = attemptDao.getAttemptsSince(thirtyDaysAgo);
        
        if (recentAttempts.isEmpty()) {
            data.insights.add("📊 No data yet. Start memorizing to see insights!");
            return data;
        }
        
        // Calculate core metrics
        calculateCoreMetrics(data, recentAttempts);
        
        // Analyze retention by review level
        analyzeRetentionRates(data);
        
        // Analyze chunk size evolution
        analyzeChunkSizeEvolution(data);
        
        // Analyze surah type performance
        analyzeSurahTypePerformance(data, recentAttempts);
        
        // Detect time-based patterns
        detectTimePatterns(data, recentAttempts);
        
        // Calculate weekly stats
        calculateWeeklyStats(data);
        
        // Generate insights and recommendations
        generateInsightMessages(data);
        
        return data;
    }
    
    private void calculateCoreMetrics(InsightData data, List<MemorizationAttemptEntity> attempts) {
        long totalDuration = 0;
        int totalAyahs = 0;
        int successes = 0;
        
        for (MemorizationAttemptEntity attempt : attempts) {
            totalDuration += attempt.durationMs;
            if (attempt.success) successes++;
            
            // Get unit to count ayahs
            MemorizationUnitEntity unit = unitDao.getById(attempt.unitId);
            if (unit != null) {
                totalAyahs += (unit.endAyah - unit.startAyah + 1);
            }
        }
        
        // Calculate averages
        if (totalAyahs > 0) {
            data.avgTimePerAya = (totalDuration / 60000.0f) / totalAyahs; // minutes per aya
        }
        
        if (!attempts.isEmpty()) {
            data.sessionSuccessRate = (float) successes / attempts.size();
            data.avgSessionDuration = (totalDuration / 60000.0f) / attempts.size();
        }
        
        // Overall success rate from units
        data.overallSuccessRate = unitDao.getAverageSuccessRate();
    }
    
    private void analyzeRetentionRates(InsightData data) {
        // Analyze retention at each review level
        for (int level = 1; level <= 6; level++) {
            List<MemorizationUnitEntity> units = unitDao.getUnitsByReviewLevel(level);
            
            if (units.isEmpty()) continue;
            
            int totalAttempts = 0;
            int totalSuccesses = 0;
            
            for (MemorizationUnitEntity unit : units) {
                totalAttempts += (unit.successCount + unit.failCount);
                totalSuccesses += unit.successCount;
            }
            
            if (totalAttempts > 0) {
                float retention = (float) totalSuccesses / totalAttempts;
                data.retentionByLevel.put(level, retention);
            }
        }
    }
    
    private void analyzeChunkSizeEvolution(InsightData data) {
        data.currentChunkSize = prefs.getInt("adaptive.chunk_size", 3);
        
        // Get recent units to see chunk size usage
        List<MemorizationUnitEntity> recentUnits = unitDao.getRecentlyLearnedUnits(30);
        
        if (recentUnits.isEmpty()) {
            data.minChunkSizeUsed = data.currentChunkSize;
            data.maxChunkSizeUsed = data.currentChunkSize;
            data.avgChunkSize = data.currentChunkSize;
            return;
        }
        
        int totalChunkSize = 0;
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        
        for (MemorizationUnitEntity unit : recentUnits) {
            int chunkSize = unit.endAyah - unit.startAyah + 1;
            totalChunkSize += chunkSize;
            if (chunkSize < min) min = chunkSize;
            if (chunkSize > max) max = chunkSize;
        }
        
        data.minChunkSizeUsed = min;
        data.maxChunkSizeUsed = max;
        data.avgChunkSize = (float) totalChunkSize / recentUnits.size();
    }
    
    private void analyzeSurahTypePerformance(InsightData data, List<MemorizationAttemptEntity> attempts) {
        int makkanSuccesses = 0;
        int makkanTotal = 0;
        int madinanSuccesses = 0;
        int madinanTotal = 0;
        
        for (MemorizationAttemptEntity attempt : attempts) {
            MemorizationUnitEntity unit = unitDao.getById(attempt.unitId);
            if (unit == null) continue;
            
            if (SurahMetadata.isMakkan(unit.surah)) {
                makkanTotal++;
                if (attempt.success) makkanSuccesses++;
            } else {
                madinanTotal++;
                if (attempt.success) madinanSuccesses++;
            }
        }
        
        data.makkanCount = makkanTotal;
        data.madinanCount = madinanTotal;
        
        if (makkanTotal > 0) {
            data.makkanSuccessRate = (float) makkanSuccesses / makkanTotal;
        }
        
        if (madinanTotal > 0) {
            data.madinanSuccessRate = (float) madinanSuccesses / madinanTotal;
        }
    }
    
    private void detectTimePatterns(InsightData data, List<MemorizationAttemptEntity> attempts) {
        // Group attempts by duration buckets
        Map<Integer, Integer> durationSuccessCounts = new HashMap<>();
        Map<Integer, Integer> durationTotalCounts = new HashMap<>();
        
        for (MemorizationAttemptEntity attempt : attempts) {
            int durationMinutes = (int) (attempt.durationMs / 60000);
            int bucket = (durationMinutes / 5) * 5; // 5-minute buckets
            
            Integer currentTotal = durationTotalCounts.get(bucket);
            durationTotalCounts.put(bucket, (currentTotal != null ? currentTotal : 0) + 1);
            if (attempt.success) {
                Integer currentSuccess = durationSuccessCounts.get(bucket);
                durationSuccessCounts.put(bucket, (currentSuccess != null ? currentSuccess : 0) + 1);
            }
        }
        
        // Find pattern: does success rate drop after certain duration?
        int bestDuration = 0;
        float bestRate = 0;
        
        for (Map.Entry<Integer, Integer> entry : durationTotalCounts.entrySet()) {
            int bucket = entry.getKey();
            int total = entry.getValue();
            
            if (total < 3) continue; // Need at least 3 samples
            
            Integer successCount = durationSuccessCounts.get(bucket);
            int successes = (successCount != null ? successCount : 0);
            float rate = (float) successes / total;
            
            if (rate > bestRate) {
                bestRate = rate;
                bestDuration = bucket;
            }
        }
        
        // Check if accuracy drops significantly in longer sessions
        int shortSessionSuccessRate = 0;
        int shortSessionTotal = 0;
        int longSessionSuccessRate = 0;
        int longSessionTotal = 0;
        
        for (MemorizationAttemptEntity attempt : attempts) {
            int durationMinutes = (int) (attempt.durationMs / 60000);
            
            if (durationMinutes < 20) {
                shortSessionTotal++;
                if (attempt.success) shortSessionSuccessRate++;
            } else if (durationMinutes > 30) {
                longSessionTotal++;
                if (attempt.success) longSessionSuccessRate++;
            }
        }
        
        if (shortSessionTotal >= 5 && longSessionTotal >= 5) {
            float shortRate = (float) shortSessionSuccessRate / shortSessionTotal;
            float longRate = (float) longSessionSuccessRate / longSessionTotal;
            
            // If short sessions are significantly better (>15% difference)
            if (shortRate - longRate > 0.15f) {
                data.accuracyDropsWithTime = true;
                data.optimalSessionLength = 20;
            }
        }
    }
    
    private void calculateWeeklyStats(InsightData data) {
        long oneWeekAgo = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000);
        List<MemorizationAttemptEntity> weekAttempts = attemptDao.getAttemptsSince(oneWeekAgo);
        
        data.weeklyAttempts = weekAttempts.size();
        data.weeklySuccesses = 0;
        data.weeklyStudyTime = 0;
        
        for (MemorizationAttemptEntity attempt : weekAttempts) {
            if (attempt.success) data.weeklySuccesses++;
            data.weeklyStudyTime += attempt.durationMs;
        }
        
        // Calculate consistency score (0-100)
        // Based on: attempts per day (max 1 per day = 14 points)
        //           + study time per day (max 30min/day = 43 points)
        //           + success rate (max 43 points)
        int attemptsPerDay = Math.min(7, data.weeklyAttempts);
        int studyMinutes = (int) (data.weeklyStudyTime / 60000);
        int studyScore = Math.min(210, studyMinutes);
        float weeklySuccessRate = data.weeklyAttempts > 0 ? (float) data.weeklySuccesses / data.weeklyAttempts : 0;
        
        data.consistencyScore = Math.min(100, 
            (attemptsPerDay * 2) +           // 0-14 points
            (studyScore / 5) +                // 0-42 points
            (int) (weeklySuccessRate * 43));  // 0-43 points
    }
    
    private void generateInsightMessages(InsightData data) {
        // Time per aya insight
        if (data.avgTimePerAya > 0) {
            if (data.avgTimePerAya < 1.5f) {
                data.insights.add(String.format("⚡ Fast learner! Averaging %.1f min per aya", data.avgTimePerAya));
            } else if (data.avgTimePerAya > 3.0f) {
                data.insights.add(String.format("🐢 Taking your time at %.1f min per aya. Quality over speed!", data.avgTimePerAya));
                data.recommendations.add("Consider smaller chunk sizes if memorization feels overwhelming");
            } else {
                data.insights.add(String.format("✅ Good pace at %.1f min per aya", data.avgTimePerAya));
            }
        }
        
        // Session duration pattern
        if (data.accuracyDropsWithTime) {
            data.insights.add(String.format("📉 Your accuracy drops after %d minutes", data.optimalSessionLength));
            data.recommendations.add(String.format("Try keeping sessions under %d minutes for better retention", data.optimalSessionLength));
        } else if (data.avgSessionDuration > 0) {
            data.insights.add(String.format("⏱️ Average session: %.0f minutes", data.avgSessionDuration));
        }
        
        // Makkan vs Madinan comparison
        if (data.makkanCount >= 5 && data.madinanCount >= 5) {
            float diff = Math.abs(data.makkanSuccessRate - data.madinanSuccessRate);
            if (diff > 0.15f) {
                if (data.makkanSuccessRate > data.madinanSuccessRate) {
                    data.insights.add(String.format("🕌 You recall Makkan surahs better (%.0f%% vs %.0f%%)", 
                        data.makkanSuccessRate * 100, data.madinanSuccessRate * 100));
                    data.recommendations.add("Allocate more review time for Madinan surahs");
                } else {
                    data.insights.add(String.format("🕌 You recall Madinan surahs better (%.0f%% vs %.0f%%)", 
                        data.madinanSuccessRate * 100, data.makkanSuccessRate * 100));
                    data.recommendations.add("Allocate more review time for Makkan surahs");
                }
            }
        }
        
        // Chunk size evolution
        if (data.maxChunkSizeUsed > data.minChunkSizeUsed) {
            data.insights.add(String.format("📏 Chunk sizes: %d-%d ayahs (avg %.1f)", 
                data.minChunkSizeUsed, data.maxChunkSizeUsed, data.avgChunkSize));
        }
        
        // Retention by level
        if (!data.retentionByLevel.isEmpty()) {
            // Find weakest review level
            int weakestLevel = -1;
            float lowestRetention = 1.0f;
            
            for (Map.Entry<Integer, Float> entry : data.retentionByLevel.entrySet()) {
                if (entry.getValue() < lowestRetention) {
                    lowestRetention = entry.getValue();
                    weakestLevel = entry.getKey();
                }
            }
            
            if (weakestLevel > 0 && lowestRetention < 0.7f) {
                data.insights.add(String.format("🔄 Retention dips at review level %d (%.0f%%)", 
                    weakestLevel, lowestRetention * 100));
                data.recommendations.add(String.format("Focus on reviewing material at level %d more frequently", weakestLevel));
            }
        }
        
        // Weekly consistency
        if (data.consistencyScore >= 80) {
            data.insights.add(String.format("🔥 Excellent consistency! Score: %d/100", data.consistencyScore));
        } else if (data.consistencyScore >= 60) {
            data.insights.add(String.format("👍 Good consistency. Score: %d/100", data.consistencyScore));
        } else if (data.consistencyScore >= 40) {
            data.insights.add(String.format("📊 Building momentum. Score: %d/100", data.consistencyScore));
            data.recommendations.add("Try to study more consistently throughout the week");
        } else {
            data.insights.add(String.format("🌱 Starting your journey. Score: %d/100", data.consistencyScore));
            data.recommendations.add("Establish a daily memorization routine for better progress");
        }
        
        // Overall success rate
        if (data.overallSuccessRate >= 0.8f) {
            data.insights.add(String.format("🎯 Strong performance at %.0f%% success rate", data.overallSuccessRate * 100));
        } else if (data.overallSuccessRate >= 0.6f) {
            data.insights.add(String.format("📈 Solid progress at %.0f%% success rate", data.overallSuccessRate * 100));
        } else if (data.overallSuccessRate > 0) {
            data.insights.add(String.format("💪 Keep pushing! Current %.0f%% success rate", data.overallSuccessRate * 100));
            data.recommendations.add("Consider reducing chunk sizes or increasing repetitions");
        }
        
        // Fallback if no insights
        if (data.insights.isEmpty()) {
            data.insights.add("📊 Keep memorizing to see personalized insights!");
        }
    }
}
