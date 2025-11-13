package com.repeatquran.data.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "insight_summaries")
public class InsightSummaryEntity {
    @PrimaryKey(autoGenerate = true)
    public Long id;
    
    // Type: "weekly", "monthly", "milestone"
    public String summaryType;
    
    // Period covered
    public long periodStart;
    public long periodEnd;
    
    // Generated summary content
    public String summaryText;
    
    // Key metrics as JSON or delimited string
    public String metricsSnapshot;
    
    // Top insights from this period
    public String topInsight1;
    public String topInsight2;
    public String topInsight3;
    
    // Top recommendation
    public String topRecommendation;
    
    // Timestamps
    public long createdAt;
    public boolean isRead;
}
