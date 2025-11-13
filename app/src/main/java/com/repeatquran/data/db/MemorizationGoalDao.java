package com.repeatquran.data.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface MemorizationGoalDao {
    @Insert
    long insert(MemorizationGoalEntity goal);
    
    @Update
    void update(MemorizationGoalEntity goal);
    
    @Query("SELECT * FROM memorization_goals WHERE id = :id")
    MemorizationGoalEntity getById(long id);
    
    @Query("SELECT * FROM memorization_goals WHERE isActive = 1 AND isPaused = 0 LIMIT 1")
    MemorizationGoalEntity getActiveGoal();
    
    @Query("SELECT * FROM memorization_goals WHERE isActive = 1")
    List<MemorizationGoalEntity> getAllActive();
    
    @Query("SELECT * FROM memorization_goals WHERE isCompleted = 1 ORDER BY completedAt DESC")
    List<MemorizationGoalEntity> getCompleted();
    
    @Query("SELECT * FROM memorization_goals WHERE isPaused = 1 ORDER BY createdAt DESC")
    List<MemorizationGoalEntity> getPaused();
    
    @Query("SELECT * FROM memorization_goals ORDER BY createdAt DESC")
    List<MemorizationGoalEntity> getAll();
    
    @Query("UPDATE memorization_goals SET isActive = 0")
    void deactivateAll();
    
    @Query("UPDATE memorization_goals SET isPaused = :paused WHERE id = :id")
    void setPaused(long id, boolean paused);
    
    @Query("UPDATE memorization_goals SET currentProgress = :progress, lastActivityAt = :timestamp WHERE id = :id")
    void updateProgress(long id, int progress, long timestamp);
    
    @Query("UPDATE memorization_goals SET isCompleted = 1, completedAt = :timestamp, isActive = 0 WHERE id = :id")
    void markCompleted(long id, long timestamp);
}
