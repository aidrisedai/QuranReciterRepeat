package com.repeatquran.data.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Tracks individual verse playback in real-time
 * Each row represents one verse listened/read
 */
@Entity(tableName = "verse_progress")
public class VerseProgressEntity {
    @PrimaryKey(autoGenerate = true)
    public Long id;
    
    public long timestamp; // When this verse was played/read
    
    public int surah;
    public int ayah;
    
    public String activityType; // "listening", "reading", "memorization"
    
    public String reciterId; // Which reciter was used (for listening)
    
    public Long sessionId; // Link to session if applicable
}
