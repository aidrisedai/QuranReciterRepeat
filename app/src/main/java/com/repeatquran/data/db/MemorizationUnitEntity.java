package com.repeatquran.data.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "memorization_units")
public class MemorizationUnitEntity {
    @PrimaryKey(autoGenerate = true)
    public Long id;
    
    public int surah;
    public int startAyah;
    public int endAyah;
    
    // Status: "new", "learning", "learned", "reviewing"
    public String status;
    
    public long createdAt;
    public Long learnedAt; // null if not yet learned
    public Long nextReviewAt; // null if new, or timestamp for next review
    
    public int totalRepetitions; // cumulative across all attempts
    public int successCount; // times user answered "Yes"
    public int failCount; // times user answered "No"
    public long totalTimeMs; // cumulative time spent on this unit
    
    // Spaced repetition metadata
    public int reviewLevel; // 0=new, 1=first review, 2=second, etc.
    public float ease; // ease factor for spaced repetition (default 2.5)
    
    // Parent tracking for splits
    public Long parentUnitId; // if this was split from another unit
}
