package com.repeatquran.memorization;

import android.content.Context;
import android.content.SharedPreferences;

import com.repeatquran.data.db.MemorizationUnitDao;
import com.repeatquran.data.db.MemorizationUnitEntity;
import com.repeatquran.data.db.RepeatQuranDatabase;

import java.util.ArrayList;
import java.util.List;

public class DailyPlanGenerator {
    private Context context;
    private MemorizationUnitDao dao;
    
    public DailyPlanGenerator(Context context) {
        this.context = context;
        this.dao = RepeatQuranDatabase.get(context).memorizationUnitDao();
    }
    
    public static class DailyPlan {
        // New learning session (50% of time)
        public List<MemorizationUnitEntity> newUnits = new ArrayList<>();
        
        // Review sessions (50% of time total)
        public List<MemorizationUnitEntity> recentReviews = new ArrayList<>();  // Last 14 days (30%)
        public List<MemorizationUnitEntity> oldReviews = new ArrayList<>();      // Older than 14 days (20%)
        
        public int newEstimatedMinutes() {
            int totalVerses = 0;
            for (MemorizationUnitEntity u : newUnits) totalVerses += (u.endAyah - u.startAyah + 1);
            return Math.max(1, totalVerses * 2); // 2 min per verse for new learning
        }
        
        public int reviewEstimatedMinutes() {
            int totalVerses = 0;
            for (MemorizationUnitEntity u : recentReviews) totalVerses += (u.endAyah - u.startAyah + 1);
            for (MemorizationUnitEntity u : oldReviews) totalVerses += (u.endAyah - u.startAyah + 1);
            return Math.max(1, totalVerses); // 1 min per verse for reviews (faster)
        }
        
        public int estimatedMinutes() {
            return newEstimatedMinutes() + reviewEstimatedMinutes();
        }
        
        public boolean hasNewLearning() {
            return !newUnits.isEmpty();
        }
        
        public boolean hasReviews() {
            return !recentReviews.isEmpty() || !oldReviews.isEmpty();
        }
    }
    
    /**
     * Generate today's plan based on user preferences
     */
    public DailyPlan generate() {
        DailyPlan plan = new DailyPlan();
        long now = System.currentTimeMillis();
        
        SharedPreferences prefs = context.getSharedPreferences("rq_prefs", Context.MODE_PRIVATE);
        int totalMinutes = prefs.getInt("plan.daily.total", 45);
        int newMin = prefs.getInt("plan.daily.new_minutes", Math.round(totalMinutes * 0.5f));
        int recentMin = prefs.getInt("plan.daily.recent_minutes", Math.round(totalMinutes * 0.3f));
        int oldMin = prefs.getInt("plan.daily.old_minutes", Math.round(totalMinutes * 0.2f));
        
        // Load reviews first (priority)
        List<MemorizationUnitEntity> allDue = dao.getDueReviews(now);
        
        // Categorize reviews by age (14 days threshold)
        long fourteenDaysAgo = now - (14 * 24 * 60 * 60 * 1000);
        for (MemorizationUnitEntity unit : allDue) {
            if (unit.learnedAt != null && unit.learnedAt > fourteenDaysAgo) {
                plan.recentReviews.add(unit);
            } else {
                plan.oldReviews.add(unit);
            }
        }
        
        // Limit reviews by allocated time (rough estimate: 1 verse = 1 minute for review)
        plan.recentReviews = limitByTime(plan.recentReviews, recentMin);
        plan.oldReviews = limitByTime(plan.oldReviews, oldMin);
        
        // Load new units to fill remaining time
        int newVerses = Math.max(1, newMin / 2); // 2 min per verse for new learning
        plan.newUnits = dao.getNewUnits(newVerses);
        
        // If no new units exist, create initial ones from user's start point
        if (plan.newUnits.isEmpty()) {
            ensureInitialUnits();
            plan.newUnits = dao.getNewUnits(newVerses);
        }
        
        return plan;
    }
    
    private List<MemorizationUnitEntity> limitByTime(List<MemorizationUnitEntity> units, int minutes) {
        List<MemorizationUnitEntity> limited = new ArrayList<>();
        int totalVerses = 0;
        int maxVerses = Math.max(1, minutes); // 1 verse = 1 min for review
        
        for (MemorizationUnitEntity unit : units) {
            int verses = unit.endAyah - unit.startAyah + 1;
            if (totalVerses + verses <= maxVerses) {
                limited.add(unit);
                totalVerses += verses;
            } else {
                break;
            }
        }
        return limited;
    }
    
    private void ensureInitialUnits() {
        SharedPreferences prefs = context.getSharedPreferences("rq_prefs", Context.MODE_PRIVATE);
        int startSurah = prefs.getInt("plan.start.surah", 1);
        int startAyah = prefs.getInt("plan.start.ayah", 1);
        
        // Use adaptive chunk size from PerformanceTracker
        PerformanceTracker tracker = new PerformanceTracker(context);
        int chunkSize = tracker.getCurrentChunkSize();
        
        // Get max ayahs in this surah
        int maxAyah = getAyahCount(startSurah);
        
        // Create initial units with adaptive chunk size
        int currentAyah = startAyah;
        int unitsCreated = 0;
        
        while (currentAyah <= maxAyah && unitsCreated < 3) {
            MemorizationUnitEntity unit = new MemorizationUnitEntity();
            unit.surah = startSurah;
            unit.startAyah = currentAyah;
            unit.endAyah = Math.min(currentAyah + chunkSize - 1, maxAyah);
            unit.status = "new";
            unit.createdAt = System.currentTimeMillis();
            unit.totalRepetitions = 0;
            unit.successCount = 0;
            unit.failCount = 0;
            unit.totalTimeMs = 0;
            unit.reviewLevel = 0;
            unit.ease = 2.5f;
            
            dao.insert(unit);
            currentAyah = unit.endAyah + 1;
            unitsCreated++;
        }
    }
    
    private int getAyahCount(int surah) {
        final int[] AYAH_COUNTS = new int[] {
            7, 286, 200, 176, 120, 165, 206, 75, 129, 109,
            123, 111, 43, 52, 99, 128, 111, 110, 98, 135,
            112, 78, 118, 64, 77, 227, 93, 88, 69, 60,
            34, 30, 73, 54, 45, 83, 182, 88, 75, 85,
            54, 53, 89, 59, 37, 35, 38, 29, 18, 45,
            60, 49, 62, 55, 78, 96, 29, 22, 24, 13,
            14, 11, 11, 18, 12, 12, 30, 52, 52, 44,
            28, 28, 20, 56, 40, 31, 50, 40, 46, 42,
            29, 19, 36, 25, 22, 17, 19, 26, 30, 20,
            15, 21, 11, 8, 8, 19, 5, 8, 8, 11,
            11, 8, 3, 9, 5, 4, 7, 3, 6, 3,
            5, 4, 5, 6
        };
        return AYAH_COUNTS[surah - 1];
    }
}
