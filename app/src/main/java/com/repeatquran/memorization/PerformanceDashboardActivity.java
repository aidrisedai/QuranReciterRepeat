package com.repeatquran.memorization;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.repeatquran.R;
import com.repeatquran.data.db.MemorizationAttemptDao;
import com.repeatquran.data.db.MemorizationAttemptEntity;
import com.repeatquran.data.db.MemorizationUnitDao;
import com.repeatquran.data.db.MemorizationUnitEntity;
import com.repeatquran.data.db.RepeatQuranDatabase;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PerformanceDashboardActivity extends AppCompatActivity {
    private TextView txtOverview, txtUpcomingReviews, txtRecentAttempts, txtInsights;
    private PerformanceTracker performanceTracker;
    private InsightsEngine insightsEngine;
    private MemorizationUnitDao unitDao;
    private MemorizationAttemptDao attemptDao;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_performance_dashboard);

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.dashboardToolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Performance Analytics");
        }

        txtOverview = findViewById(R.id.txtPerformanceOverview);
        txtUpcomingReviews = findViewById(R.id.txtUpcomingReviews);
        txtRecentAttempts = findViewById(R.id.txtRecentAttempts);
        txtInsights = findViewById(R.id.txtInsights);

        RepeatQuranDatabase db = RepeatQuranDatabase.get(this);
        unitDao = db.memorizationUnitDao();
        attemptDao = db.memorizationAttemptDao();
        performanceTracker = new PerformanceTracker(this);
        insightsEngine = new InsightsEngine(this);

        loadDashboard();
    }

    private void loadDashboard() {
        new Thread(() -> {
            // Performance overview
            PerformanceTracker.PerformanceMetrics metrics = performanceTracker.analyzePerformance();
            StringBuilder overview = new StringBuilder();
            overview.append("═══ Current Performance ═══\n\n");
            overview.append(String.format("Success Rate: %.0f%%\n", metrics.successRate * 100));
            overview.append(String.format("Current Streak: %d sessions\n", metrics.currentStreak));
            overview.append(String.format("Sessions Analyzed: %d/10\n", metrics.sessionsAnalyzed));
            overview.append(String.format("Avg Time per Unit: %.1f min\n", metrics.avgTimePerSession / 60000f));
            overview.append(String.format("Total Study Time: %.1f hours\n\n", metrics.totalStudyTime / 3600000f));
            overview.append(String.format("Current Chunk Size: %d ayahs\n", metrics.currentChunkSize));
            overview.append(String.format("Next Adjustment: %d sessions\n\n", metrics.sessionsUntilNextAdjustment));
            
            if (metrics.shouldIncreaseChunk) {
                overview.append("🎉 Recommendation: Increase chunk size\n");
            } else if (metrics.shouldDecreaseChunk) {
                overview.append("💡 Recommendation: Decrease chunk size\n");
            } else {
                overview.append("✓ Chunk size is optimal\n");
            }

            // Upcoming reviews with calendar
            long now = System.currentTimeMillis();
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd", Locale.getDefault());
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            
            // Get all future reviews
            List<MemorizationUnitEntity> allFuture = unitDao.getFutureReviews(now);
            List<MemorizationUnitEntity> dueToday = unitDao.getDueReviews(now);
            
            // Group by date
            java.util.Map<String, List<MemorizationUnitEntity>> reviewsByDate = new java.util.LinkedHashMap<>();
            
            // Add today's reviews
            if (!dueToday.isEmpty()) {
                reviewsByDate.put("🔴 Today", dueToday);
            }
            
            // Add future reviews grouped by date
            for (MemorizationUnitEntity unit : allFuture) {
                if (unit.nextReviewAt == null) continue;
                
                long daysUntil = (unit.nextReviewAt - now) / (24 * 60 * 60 * 1000);
                String dateKey;
                
                if (daysUntil == 0) {
                    continue; // Already in today
                } else if (daysUntil == 1) {
                    dateKey = "🟡 Tomorrow";
                } else if (daysUntil <= 7) {
                    dateKey = "🟢 " + dateFormat.format(new Date(unit.nextReviewAt)) + " (" + daysUntil + "d)";
                } else if (daysUntil <= 30) {
                    dateKey = "⚪ " + dateFormat.format(new Date(unit.nextReviewAt)) + " (" + daysUntil + "d)";
                } else {
                    continue; // Don't show reviews > 30 days out
                }
                
                if (!reviewsByDate.containsKey(dateKey)) {
                    reviewsByDate.put(dateKey, new ArrayList<>());
                }
                reviewsByDate.get(dateKey).add(unit);
            }
            
            StringBuilder reviews = new StringBuilder();
            reviews.append("═══ Review Calendar ═══\n\n");
            
            if (reviewsByDate.isEmpty()) {
                reviews.append("No reviews scheduled in next 30 days\n");
            } else {
                for (java.util.Map.Entry<String, List<MemorizationUnitEntity>> entry : reviewsByDate.entrySet()) {
                    reviews.append(entry.getKey()).append(": ").append(entry.getValue().size()).append(" units\n");
                    
                    // Show first 3 units for each date
                    for (int i = 0; i < Math.min(3, entry.getValue().size()); i++) {
                        MemorizationUnitEntity unit = entry.getValue().get(i);
                        reviews.append(String.format("  • %03d:%d-%d (L%d)\n", 
                            unit.surah, unit.startAyah, unit.endAyah, unit.reviewLevel));
                    }
                    if (entry.getValue().size() > 3) {
                        reviews.append(String.format("  ... and %d more\n", entry.getValue().size() - 3));
                    }
                    reviews.append("\n");
                }
            }

            // Recent attempts
            List<MemorizationAttemptEntity> recent = attemptDao.getRecentAttempts(10);
            StringBuilder attempts = new StringBuilder();
            attempts.append("═══ Recent Sessions ═══\n\n");
            
            for (MemorizationAttemptEntity attempt : recent) {
                String result = attempt.success ? "✓" : "✗";
                String time = dateFormat.format(new Date(attempt.completedAt));
                int mins = (int) (attempt.durationMs / 60000);
                attempts.append(String.format("%s %s - %d min\n", result, time, mins));
            }

            // Generate insights
            InsightsEngine.InsightData insights = insightsEngine.generateInsights();
            StringBuilder insightsText = new StringBuilder();
            insightsText.append("═══ Personalized Insights ═══\n\n");
            
            // Core metrics
            insightsText.append("📊 KEY METRICS:\n");
            if (insights.avgTimePerAya > 0) {
                insightsText.append(String.format("  • Avg time per aya: %.1f min\n", insights.avgTimePerAya));
            }
            if (insights.overallSuccessRate > 0) {
                insightsText.append(String.format("  • Overall success: %.0f%%\n", insights.overallSuccessRate * 100));
            }
            insightsText.append(String.format("  • Chunk size range: %d-%d ayahs\n", 
                insights.minChunkSizeUsed, insights.maxChunkSizeUsed));
            insightsText.append(String.format("  • Consistency score: %d/100\n\n", insights.consistencyScore));
            
            // Retention by level
            if (!insights.retentionByLevel.isEmpty()) {
                insightsText.append("🔄 RETENTION BY REVIEW LEVEL:\n");
                for (int level = 1; level <= 6; level++) {
                    if (insights.retentionByLevel.containsKey(level)) {
                        float retention = insights.retentionByLevel.get(level);
                        String bar = getProgressBar(retention);
                        insightsText.append(String.format("  Level %d: %s %.0f%%\n", level, bar, retention * 100));
                    }
                }
                insightsText.append("\n");
            }
            
            // Surah type comparison
            if (insights.makkanCount >= 3 && insights.madinanCount >= 3) {
                insightsText.append("🕌 SURAH TYPE PERFORMANCE:\n");
                insightsText.append(String.format("  Makkan: %.0f%% (%d sessions)\n", 
                    insights.makkanSuccessRate * 100, insights.makkanCount));
                insightsText.append(String.format("  Madinan: %.0f%% (%d sessions)\n\n", 
                    insights.madinanSuccessRate * 100, insights.madinanCount));
            }
            
            // Insights
            if (!insights.insights.isEmpty()) {
                insightsText.append("💡 INSIGHTS:\n");
                for (String insight : insights.insights) {
                    insightsText.append("  ").append(insight).append("\n");
                }
                insightsText.append("\n");
            }
            
            // Recommendations
            if (!insights.recommendations.isEmpty()) {
                insightsText.append("✨ RECOMMENDATIONS:\n");
                for (String rec : insights.recommendations) {
                    insightsText.append("  • ").append(rec).append("\n");
                }
            }
            
            String finalOverview = overview.toString();
            String finalReviews = reviews.toString();
            String finalAttempts = attempts.toString();
            String finalInsights = insightsText.toString();

            runOnUiThread(() -> {
                txtOverview.setText(finalOverview);
                txtUpcomingReviews.setText(finalReviews);
                txtRecentAttempts.setText(finalAttempts);
                txtInsights.setText(finalInsights);
            });
        }).start();
    }
    
    private String getProgressBar(float value) {
        int filled = (int) (value * 10);
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            bar.append(i < filled ? "█" : "░");
        }
        return bar.toString();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
