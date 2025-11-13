package com.repeatquran.memorization;

import android.content.Context;

import com.repeatquran.data.db.InsightSummaryDao;
import com.repeatquran.data.db.InsightSummaryEntity;
import com.repeatquran.data.db.RepeatQuranDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class WeeklySummaryGenerator {
    
    private Context context;
    private InsightsEngine insightsEngine;
    private InsightSummaryDao summaryDao;
    
    public WeeklySummaryGenerator(Context context) {
        this.context = context;
        this.insightsEngine = new InsightsEngine(context);
        this.summaryDao = RepeatQuranDatabase.get(context).insightSummaryDao();
    }
    
    /**
     * Generate and store a weekly summary
     */
    public long generateWeeklySummary() {
        long now = System.currentTimeMillis();
        long oneWeekAgo = now - (7L * 24 * 60 * 60 * 1000);
        
        // Check if we already have a summary for this week
        InsightSummaryEntity existing = summaryDao.getLatestAfter(oneWeekAgo);
        if (existing != null) {
            return existing.id; // Already generated
        }
        
        // Generate insights
        InsightsEngine.InsightData insights = insightsEngine.generateInsights();
        
        // Create summary
        InsightSummaryEntity summary = new InsightSummaryEntity();
        summary.summaryType = "weekly";
        summary.periodStart = oneWeekAgo;
        summary.periodEnd = now;
        summary.createdAt = now;
        summary.isRead = false;
        
        // Build summary text
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd", Locale.getDefault());
        StringBuilder summaryText = new StringBuilder();
        summaryText.append("📅 Weekly Summary\\n");
        summaryText.append(dateFormat.format(new Date(oneWeekAgo)));
        summaryText.append(" - ");
        summaryText.append(dateFormat.format(new Date(now)));
        summaryText.append("\\n\\n");
        
        // Add key stats
        summaryText.append("📊 This Week:\\n");
        summaryText.append(String.format("  • %d sessions\\n", insights.weeklyAttempts));
        summaryText.append(String.format("  • %d successful\\n", insights.weeklySuccesses));
        summaryText.append(String.format("  • %.0f min total study\\n", insights.weeklyStudyTime / 60000f));
        summaryText.append(String.format("  • %d/100 consistency\\n", insights.consistencyScore));
        
        if (insights.avgTimePerAya > 0) {
            summaryText.append(String.format("  • %.1f min/aya average\\n", insights.avgTimePerAya));
        }
        
        summary.summaryText = summaryText.toString();
        
        // Store metrics snapshot
        StringBuilder metricsJson = new StringBuilder();
        metricsJson.append("{");
        metricsJson.append(String.format("\"weeklyAttempts\":%d,", insights.weeklyAttempts));
        metricsJson.append(String.format("\"weeklySuccesses\":%d,", insights.weeklySuccesses));
        metricsJson.append(String.format("\"weeklyStudyTime\":%d,", insights.weeklyStudyTime));
        metricsJson.append(String.format("\"consistencyScore\":%d,", insights.consistencyScore));
        metricsJson.append(String.format("\"avgTimePerAya\":%.2f,", insights.avgTimePerAya));
        metricsJson.append(String.format("\"overallSuccessRate\":%.2f", insights.overallSuccessRate));
        metricsJson.append("}");
        summary.metricsSnapshot = metricsJson.toString();
        
        // Store top insights
        if (!insights.insights.isEmpty()) {
            summary.topInsight1 = insights.insights.get(0);
            if (insights.insights.size() > 1) {
                summary.topInsight2 = insights.insights.get(1);
            }
            if (insights.insights.size() > 2) {
                summary.topInsight3 = insights.insights.get(2);
            }
        }
        
        // Store top recommendation
        if (!insights.recommendations.isEmpty()) {
            summary.topRecommendation = insights.recommendations.get(0);
        }
        
        // Save to database
        return summaryDao.insert(summary);
    }
    
    /**
     * Check if weekly summary should be generated (call this on app start)
     */
    public boolean shouldGenerateWeeklySummary() {
        long oneWeekAgo = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000);
        InsightSummaryEntity latest = summaryDao.getLatestAfter(oneWeekAgo);
        
        // Generate if no summary exists for this week
        return latest == null;
    }
    
    /**
     * Auto-generate weekly summary if needed
     */
    public void autoGenerateIfNeeded() {
        if (shouldGenerateWeeklySummary()) {
            new Thread(() -> {
                generateWeeklySummary();
            }).start();
        }
    }
}
