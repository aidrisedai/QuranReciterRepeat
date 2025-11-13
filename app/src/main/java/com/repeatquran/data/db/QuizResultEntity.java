package com.repeatquran.data.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "quiz_results")
public class QuizResultEntity {
    @PrimaryKey(autoGenerate = true)
    public Long id;
    
    // Link to parent session
    public long sessionId; // FK to SessionEntity
    
    // Verse identification
    public int surah;
    public int ayah;
    
    // Result tracking
    public boolean wasCorrect;
    
    // Error details (if incorrect)
    public String errorType; // nullable; e.g., "wrong_word", "tajweed_error", "skipped_verse"
    public String errorDetails; // nullable; AI's explanation of what went wrong
    
    // Attempt tracking
    public int attemptNumber; // 1st attempt, 2nd attempt, etc.
    public int totalAttempts; // how many tries before correct (or gave up)
    
    // Timing
    public long timestamp; // when this verse was attempted
    public long durationMs; // how long user took to recite this verse
}
