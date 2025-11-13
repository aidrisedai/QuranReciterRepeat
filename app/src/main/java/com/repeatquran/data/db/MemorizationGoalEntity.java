package com.repeatquran.data.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "memorization_goals")
public class MemorizationGoalEntity {
    @PrimaryKey(autoGenerate = true)
    public Long id;
    
    // User's original goal text (for reference)
    public String goalText;
    
    // Goal type: "daily" | "weekly" | "monthly" | "one-time"
    public String goalType;
    
    // Target verse range
    public Integer targetSurahStart;
    public Integer targetAyahStart;
    public Integer targetSurahEnd;
    public Integer targetAyahEnd;
    
    // For recurring goals (daily/weekly/monthly)
    public Integer versesPerDay; // nullable for one-time goals
    
    // Strictness preference for this goal
    // Values: "strict" | "moderate" | "lenient"
    public String strictnessLevel;
    
    // Timeline
    public long startDate;
    public Long targetEndDate; // nullable; calculated for recurring goals
    
    // Status tracking
    public boolean isActive;
    public boolean isCompleted;
    public boolean isPaused;
    
    // Progress tracking
    public int currentProgress; // verses completed so far
    public int totalVerses; // total verses in goal
    
    // Timestamps
    public long createdAt;
    public Long completedAt; // null if not completed
    public Long lastActivityAt; // last time user worked on this goal
    
    // Legacy fields (keep for backward compatibility)
    @Deprecated
    public Integer customStartSurah;
    @Deprecated
    public Integer customEndSurah;
    @Deprecated
    public String goalName;
    @Deprecated
    public int totalAyahs;
    @Deprecated
    public long baselineDate;
    @Deprecated
    public int baselineAyahsLearned;
    @Deprecated
    public int projectedDaysToComplete;
}
