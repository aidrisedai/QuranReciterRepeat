package com.repeatquran.data.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface MemorizationAttemptDao {
    @Insert
    long insert(MemorizationAttemptEntity attempt);
    
    @Query("SELECT * FROM memorization_attempts WHERE unitId = :unitId ORDER BY startedAt DESC")
    List<MemorizationAttemptEntity> getAttemptsForUnit(long unitId);
    
    @Query("SELECT * FROM memorization_attempts ORDER BY startedAt DESC LIMIT :limit")
    List<MemorizationAttemptEntity> getRecentAttempts(int limit);
    
    @Query("SELECT COUNT(*) FROM memorization_attempts WHERE success = 1")
    int getSuccessCount();
    
    @Query("SELECT SUM(durationMs) FROM memorization_attempts")
    long getTotalStudyTime();
    
    @Query("SELECT * FROM memorization_attempts WHERE completedAt >= :startTimestamp ORDER BY completedAt DESC")
    List<MemorizationAttemptEntity> getAttemptsSince(long startTimestamp);
    
    @Query("SELECT COUNT(*) FROM memorization_attempts WHERE completedAt >= :startTimestamp AND success = 1")
    int getSuccessCountSince(long startTimestamp);
    
    @Query("SELECT AVG(durationMs) FROM memorization_attempts WHERE success = 1")
    long getAverageSuccessfulDuration();
    
    @Query("SELECT * FROM memorization_attempts WHERE completedAt >= :startTimestamp AND completedAt < :endTimestamp ORDER BY completedAt ASC")
    List<MemorizationAttemptEntity> getAttemptsBetween(long startTimestamp, long endTimestamp);
    
    @Query("SELECT COUNT(*) FROM memorization_attempts WHERE completedAt >= :startTimestamp")
    int getAttemptCountSince(long startTimestamp);
}
