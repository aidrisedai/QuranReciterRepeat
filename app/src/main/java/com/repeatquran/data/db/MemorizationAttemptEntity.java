package com.repeatquran.data.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "memorization_attempts")
public class MemorizationAttemptEntity {
    @PrimaryKey(autoGenerate = true)
    public Long id;
    
    public long unitId; // FK to MemorizationUnitEntity
    public long startedAt;
    public long completedAt;
    
    public int slowRepetitions; // how many slow reps (usually 5)
    public int fastRepetitions; // how many fast reps (usually 10)
    
    public boolean success; // true if user answered "Yes", false if "No"
    public boolean wasSplit; // true if this failure led to splitting
    
    public long durationMs; // total time for this attempt
}
